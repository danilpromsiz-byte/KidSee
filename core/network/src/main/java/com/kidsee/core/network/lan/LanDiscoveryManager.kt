package com.kidsee.core.network.lan

import android.content.Context
import android.net.wifi.WifiManager
import com.kidsee.core.domain.model.ChildDevice
import com.kidsee.core.domain.model.DeviceCommand
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import timber.log.Timber
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.util.concurrent.ConcurrentHashMap

/**
 * Handles zero-config local Wi-Fi / LAN discovery and real-time commands
 * without needing any external cloud or Firebase connection.
 */
class LanDiscoveryManager(
    private val context: Context
) {
    companion object {
        const val DISCOVERY_PORT = 42542
        private const val BROADCAST_IP = "255.255.255.255"
        private const val PREFIX_DEVICE = "KIDSEE_DEV:"
        private const val PREFIX_COMMAND = "KIDSEE_CMD:"
        private const val PREFIX_PING = "KIDSEE_PING:"
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val json = Json { ignoreUnknownKeys = true }

    private var socket: DatagramSocket? = null
    private var multicastLock: WifiManager.MulticastLock? = null
    private var listenJob: Job? = null
    private var broadcastJob: Job? = null
    private var cleanupJob: Job? = null

    private var activeDeviceProvider: (() -> ChildDevice?)? = null
    private val knownPeerAddresses = ConcurrentHashMap.newKeySet<InetAddress>()

    private val discoveredDevices = ConcurrentHashMap<String, ChildDevice>()
    private val _discoveredDevicesFlow = MutableStateFlow<List<ChildDevice>>(emptyList())
    val discoveredDevicesFlow: StateFlow<List<ChildDevice>> = _discoveredDevicesFlow.asStateFlow()

    private val _incomingCommandsFlow = MutableSharedFlow<DeviceCommand>(extraBufferCapacity = 64)
    val incomingCommandsFlow: SharedFlow<DeviceCommand> = _incomingCommandsFlow.asSharedFlow()

    @Volatile
    private var isRunning = false

    @Synchronized
    fun startListening() {
        if (isRunning && socket != null && socket?.isClosed == false) return
        isRunning = true

        acquireMulticastLock()

        try {
            socket = DatagramSocket(null).apply {
                reuseAddress = true
                bind(InetSocketAddress(DISCOVERY_PORT))
                broadcast = true
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to bind DatagramSocket on port $DISCOVERY_PORT")
            try {
                socket = DatagramSocket(DISCOVERY_PORT).apply { broadcast = true }
            } catch (e2: Exception) {
                Timber.e(e2, "Fallback socket bind failed")
            }
        }

        listenJob?.cancel()
        listenJob = scope.launch {
            val buffer = ByteArray(4096)
            while (isActive && isRunning) {
                try {
                    val s = socket ?: break
                    val packet = DatagramPacket(buffer, buffer.size)
                    s.receive(packet)

                    val senderAddress = packet.address
                    if (senderAddress != null) {
                        knownPeerAddresses.add(senderAddress)
                    }

                    val text = String(packet.data, 0, packet.length, Charsets.UTF_8).trim()
                    when {
                        text.startsWith(PREFIX_PING) -> {
                            // Peer is asking for active devices
                            val dev = activeDeviceProvider?.invoke()
                            if (dev != null) {
                                val payload = PREFIX_DEVICE + json.encodeToString(dev)
                                // Reply via direct unicast to the sender
                                if (senderAddress != null) {
                                    sendDirectUdpPacket(payload, senderAddress, packet.port)
                                }
                                // Also broadcast
                                sendUdpPacket(payload)
                            }
                        }

                        text.startsWith(PREFIX_DEVICE) -> {
                            val payload = text.removePrefix(PREFIX_DEVICE)
                            try {
                                val device = json.decodeFromString<ChildDevice>(payload)
                                if (device.id.isNotBlank()) {
                                    val now = System.currentTimeMillis()
                                    val updatedDevice = device.copy(lastSeenTimestamp = now)
                                    discoveredDevices[device.id] = updatedDevice
                                    _discoveredDevicesFlow.value = discoveredDevices.values.toList()
                                    Timber.d("Discovered LAN child device: ${device.name} (${device.id}) from $senderAddress")
                                }
                            } catch (e: Exception) {
                                Timber.w(e, "Failed to parse LAN device packet")
                            }
                        }

                        text.startsWith(PREFIX_COMMAND) -> {
                            val payload = text.removePrefix(PREFIX_COMMAND)
                            try {
                                val command = json.decodeFromString<DeviceCommand>(payload)
                                _incomingCommandsFlow.tryEmit(command)
                                Timber.d("Received LAN command: ${command.type}")
                            } catch (e: Exception) {
                                Timber.w(e, "Failed to parse LAN command packet")
                            }
                        }
                    }
                } catch (e: Exception) {
                    if (isRunning) {
                        Timber.w(e, "Error receiving LAN packet")
                    }
                }
            }
        }

        cleanupJob?.cancel()
        cleanupJob = scope.launch {
            while (isActive && isRunning) {
                delay(6_000L)
                val now = System.currentTimeMillis()
                val removed = discoveredDevices.entries.removeIf { now - it.value.lastSeenTimestamp > 25_000L }
                if (removed) {
                    _discoveredDevicesFlow.value = discoveredDevices.values.toList()
                }
            }
        }
    }

    /**
     * Triggers active rescan by broadcasting discovery ping probes across all interfaces.
     */
    fun rescan() {
        startListening()
        scope.launch {
            repeat(3) {
                sendUdpPacket(PREFIX_PING)
                delay(120L)
            }
        }
    }

    /**
     * Called on child device to broadcast its status periodically to the local network.
     */
    fun startBroadcasting(deviceProvider: () -> ChildDevice?) {
        activeDeviceProvider = deviceProvider
        startListening()

        broadcastJob?.cancel()
        broadcastJob = scope.launch {
            while (isActive && isRunning) {
                val dev = deviceProvider()
                if (dev != null) {
                    broadcastDevice(dev)
                }
                delay(2_500L)
            }
        }
    }

    fun broadcastDevice(device: ChildDevice) {
        scope.launch {
            try {
                val payload = PREFIX_DEVICE + json.encodeToString(device)
                sendUdpPacket(payload)
                // Also send direct unicast to any known parent addresses
                for (peer in knownPeerAddresses) {
                    sendDirectUdpPacket(payload, peer, DISCOVERY_PORT)
                }
            } catch (e: Exception) {
                Timber.w(e, "Failed to broadcast LAN device")
            }
        }
    }

    fun sendCommand(command: DeviceCommand) {
        scope.launch {
            try {
                val payload = PREFIX_COMMAND + json.encodeToString(command)
                sendUdpPacket(payload)
                for (peer in knownPeerAddresses) {
                    sendDirectUdpPacket(payload, peer, DISCOVERY_PORT)
                }
                Timber.d("Sent LAN command: ${command.type} to ${command.targetDeviceId}")
            } catch (e: Exception) {
                Timber.e(e, "Failed to send LAN command")
            }
        }
    }

    private fun getBroadcastAddresses(): List<InetAddress> {
        val list = mutableListOf<InetAddress>()
        try {
            list.add(InetAddress.getByName(BROADCAST_IP))
            val interfaces = java.net.NetworkInterface.getNetworkInterfaces() ?: return list
            while (interfaces.hasMoreElements()) {
                val iface = interfaces.nextElement()
                if (iface.isLoopback || !iface.isUp) continue
                for (ifAddr in iface.interfaceAddresses) {
                    val bcast = ifAddr.broadcast
                    if (bcast != null && !list.contains(bcast)) {
                        list.add(bcast)
                    }
                }
            }
        } catch (e: Exception) {
            Timber.w(e, "Error collecting broadcast addresses")
        }
        return list
    }

    private fun sendDirectUdpPacket(text: String, address: InetAddress, port: Int) {
        try {
            val data = text.toByteArray(Charsets.UTF_8)
            val packet = DatagramPacket(data, data.size, address, port)
            val s = socket ?: DatagramSocket().apply { broadcast = true }
            s.send(packet)
        } catch (e: Exception) {
            Timber.w(e, "Failed to send direct UDP packet to $address:$port")
        }
    }

    private fun sendUdpPacket(text: String) {
        try {
            val data = text.toByteArray(Charsets.UTF_8)
            val addresses = getBroadcastAddresses()
            val s = socket ?: DatagramSocket().apply { broadcast = true }

            for (addr in addresses) {
                try {
                    val packet = DatagramPacket(data, data.size, addr, DISCOVERY_PORT)
                    s.send(packet)
                } catch (e: Exception) {
                    Timber.w(e, "Failed to send to broadcast $addr")
                }
            }
        } catch (e: Exception) {
            Timber.w(e, "Failed to send UDP broadcast packets")
        }
    }

    private fun acquireMulticastLock() {
        try {
            if (multicastLock == null || multicastLock?.isHeld == false) {
                val wifi = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
                multicastLock = wifi?.createMulticastLock("KidSeeLanLock")?.apply {
                    setReferenceCounted(false)
                    acquire()
                }
                Timber.d("MulticastLock acquired successfully")
            }
        } catch (e: Exception) {
            Timber.w(e, "Could not acquire MulticastLock")
        }
    }

    @Synchronized
    fun stop() {
        isRunning = false
        listenJob?.cancel()
        broadcastJob?.cancel()
        cleanupJob?.cancel()

        try {
            socket?.close()
        } catch (_: Exception) {}
        socket = null

        try {
            if (multicastLock?.isHeld == true) {
                multicastLock?.release()
            }
        } catch (_: Exception) {}
        multicastLock = null
    }
}
