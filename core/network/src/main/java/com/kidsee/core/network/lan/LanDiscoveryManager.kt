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
 *
 * Utilizes dual sockets (listening on 42542 and dedicated sender socket),
 * subnet broadcast, and local /24 subnet unicast ping sweep to bypass
 * any router broadcast filters or AP isolation.
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

    // Dedicated listening socket (bound to port 42542, solely for receive())
    private var listenSocket: DatagramSocket? = null
    // Dedicated sending socket (ephemeral port, solely for sendto())
    private var senderSocket: DatagramSocket? = null

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
        if (isRunning && listenSocket != null && listenSocket?.isClosed == false) return
        isRunning = true

        acquireMulticastLock()
        initSockets()

        listenJob?.cancel()
        listenJob = scope.launch {
            val buffer = ByteArray(8192)
            while (isActive && isRunning) {
                try {
                    val s = listenSocket ?: break
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
                            if (dev != null && dev.id.isNotBlank()) {
                                val payload = PREFIX_DEVICE + json.encodeToString(dev)
                                // Reply directly via unicast to sender at DISCOVERY_PORT and at sender's port
                                if (senderAddress != null) {
                                    sendDirectUdpPacket(payload, senderAddress, DISCOVERY_PORT)
                                    if (packet.port != DISCOVERY_PORT && packet.port > 0) {
                                        sendDirectUdpPacket(payload, senderAddress, packet.port)
                                    }
                                }
                                // Also broadcast locally
                                sendUdpPacket(payload)
                            }
                        }

                        text.startsWith(PREFIX_DEVICE) -> {
                            val payload = text.removePrefix(PREFIX_DEVICE)
                            try {
                                val device = json.decodeFromString<ChildDevice>(payload)
                                val selfId = activeDeviceProvider?.invoke()?.id
                                // Ignore packet if it came from this same device
                                if (device.id.isNotBlank() && device.id != selfId) {
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

    private fun initSockets() {
        try {
            if (listenSocket == null || listenSocket?.isClosed == true) {
                listenSocket = DatagramSocket(null).apply {
                    reuseAddress = true
                    // Bind explicitly to IPv4 wildcard 0.0.0.0 to prevent IPv6 stack dropping IPv4 broadcast packets
                    bind(InetSocketAddress(InetAddress.getByName("0.0.0.0"), DISCOVERY_PORT))
                    broadcast = true
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to bind listenSocket on 0.0.0.0:$DISCOVERY_PORT")
            try {
                listenSocket = DatagramSocket(DISCOVERY_PORT).apply { broadcast = true }
            } catch (e2: Exception) {
                Timber.e(e2, "Fallback listenSocket bind failed")
            }
        }

        try {
            if (senderSocket == null || senderSocket?.isClosed == true) {
                senderSocket = DatagramSocket(null).apply {
                    reuseAddress = true
                    bind(InetSocketAddress(InetAddress.getByName("0.0.0.0"), 0))
                    broadcast = true
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to create senderSocket")
            try {
                senderSocket = DatagramSocket().apply { broadcast = true }
            } catch (_: Exception) {}
        }
    }

    /**
     * Probes a specific IP address directly (useful for manual pairing or bypassing AP isolation).
     */
    fun probeIp(targetIp: String) {
        startListening()
        scope.launch {
            try {
                val pingData = PREFIX_PING.toByteArray(Charsets.UTF_8)
                val target = InetAddress.getByName(targetIp.trim())
                val s = senderSocket ?: DatagramSocket().apply { broadcast = true }
                repeat(3) {
                    s.send(DatagramPacket(pingData, pingData.size, target, DISCOVERY_PORT))
                    delay(80L)
                }
                Timber.d("Sent direct LAN probe to $targetIp:$DISCOVERY_PORT")
            } catch (e: Exception) {
                Timber.w(e, "Failed to probe IP $targetIp")
            }
        }
    }

    /**
     * Triggers active rescan by broadcasting discovery ping probes across all interfaces
     * and sweeping all local Wi-Fi / Ethernet /24 subnets.
     */
    fun rescan() {
        startListening()
        scope.launch {
            // 1. Broadcast pings across all interfaces
            repeat(3) {
                sendUdpPacket(PREFIX_PING)
                delay(60L)
            }

            // 2. Unicast sweep across local Wi-Fi / Ethernet /24 subnets (essential if router blocks broadcast)
            val prefixes = getSubnetBasePrefixes()
            val pingData = PREFIX_PING.toByteArray(Charsets.UTF_8)
            val s = senderSocket ?: return@launch
            for (prefix in prefixes) {
                for (i in 1..254) {
                    try {
                        val target = InetAddress.getByName("$prefix.$i")
                        s.send(DatagramPacket(pingData, pingData.size, target, DISCOVERY_PORT))
                    } catch (_: Exception) {}
                }
                Timber.d("Completed LAN subnet sweep for $prefix.1..254")
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
            // Send initial ping sweep and broadcast to announce presence immediately
            val dev = deviceProvider()
            if (dev != null) {
                broadcastDevice(dev)
            }
            rescan()

            while (isActive && isRunning) {
                val currentDev = deviceProvider()
                if (currentDev != null) {
                    broadcastDevice(currentDev)
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
                // Also send direct unicast to all known peer addresses
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

    /**
     * Returns all local IPv4 addresses on Wi-Fi and Ethernet interfaces,
     * strictly excluding cellular / mobile data interfaces.
     */
    fun getLocalWifiIpAddresses(): List<String> {
        val result = mutableListOf<String>()
        try {
            val interfaces = java.net.NetworkInterface.getNetworkInterfaces() ?: return emptyList()
            while (interfaces.hasMoreElements()) {
                val iface = interfaces.nextElement()
                if (iface.isLoopback || !iface.isUp) continue
                val name = iface.name.lowercase()
                // Strictly exclude cellular, mobile data, and virtual tunnels
                if (name.startsWith("rmnet") || name.startsWith("ccmni") || name.startsWith("pdp") ||
                    name.startsWith("dummy") || name.startsWith("tun") || name.startsWith("wwan") ||
                    name.startsWith("cellular") || name.startsWith("ppp")) {
                    continue
                }
                for (addr in iface.inetAddresses) {
                    if (addr is java.net.Inet4Address && !addr.isLoopbackAddress) {
                        val host = addr.hostAddress ?: continue
                        if (!host.startsWith("127.")) {
                            result.add(host)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Timber.w(e, "Error getting local Wi-Fi IP addresses")
        }
        return result
    }

    /**
     * Primary Wi-Fi or local network IP (e.g. 192.168.1.45)
     */
    fun getPrimaryLocalIp(): String? {
        val list = getLocalWifiIpAddresses()
        return list.firstOrNull { it.startsWith("192.168.") }
            ?: list.firstOrNull { it.startsWith("10.") || it.startsWith("172.") }
            ?: list.firstOrNull()
    }

    private fun getSubnetBasePrefixes(): List<String> {
        val prefixes = mutableSetOf<String>()

        // 1. Wi-Fi DHCP Info
        try {
            val wifi = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            val dhcp = wifi?.dhcpInfo
            if (dhcp != null && dhcp.ipAddress != 0) {
                val ip = dhcp.ipAddress
                val b0 = ip and 0xFF
                val b1 = (ip shr 8) and 0xFF
                val b2 = (ip shr 16) and 0xFF
                prefixes.add("$b0.$b1.$b2")
            }
        } catch (_: Exception) {}

        // 2. Wi-Fi & Ethernet Network Interfaces
        for (ip in getLocalWifiIpAddresses()) {
            val parts = ip.split(".")
            if (parts.size == 4) {
                prefixes.add("${parts[0]}.${parts[1]}.${parts[2]}")
            }
        }
        return prefixes.toList()
    }

    private fun getSubnetBasePrefix(): String? {
        return getSubnetBasePrefixes().firstOrNull()
    }

    private fun getBroadcastAddresses(): List<InetAddress> {
        val list = mutableListOf<InetAddress>()
        try {
            // 1. Universal IPv4 Broadcast
            list.add(InetAddress.getByName(BROADCAST_IP))

            // 2. Wi-Fi DHCP calculated subnet broadcast
            val wifi = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            val dhcp = wifi?.dhcpInfo
            if (dhcp != null && dhcp.ipAddress != 0) {
                val broadcast = (dhcp.ipAddress and dhcp.netmask) or dhcp.netmask.inv()
                val quads = ByteArray(4)
                for (k in 0..3) {
                    quads[k] = ((broadcast shr (k * 8)) and 0xFF).toByte()
                }
                val bcastAddr = InetAddress.getByAddress(quads)
                if (!list.contains(bcastAddr)) list.add(bcastAddr)
            }

            // 3. NetworkInterface interfaceAddresses (Wi-Fi and Ethernet only)
            val interfaces = java.net.NetworkInterface.getNetworkInterfaces()
            if (interfaces != null) {
                while (interfaces.hasMoreElements()) {
                    val iface = interfaces.nextElement()
                    if (iface.isLoopback || !iface.isUp) continue
                    val name = iface.name.lowercase()
                    if (name.startsWith("rmnet") || name.startsWith("ccmni") || name.startsWith("pdp") ||
                        name.startsWith("dummy") || name.startsWith("tun") || name.startsWith("wwan")) {
                        continue
                    }
                    for (ifAddr in iface.interfaceAddresses) {
                        val bcast = ifAddr.broadcast
                        if (bcast != null && !list.contains(bcast)) {
                            list.add(bcast)
                        }
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
            val s = senderSocket ?: DatagramSocket().apply { broadcast = true }
            s.send(packet)
        } catch (e: Exception) {
            Timber.w(e, "Failed to send direct UDP packet to $address:$port")
        }
    }

    private fun sendUdpPacket(text: String) {
        try {
            val data = text.toByteArray(Charsets.UTF_8)
            val addresses = getBroadcastAddresses()
            val s = senderSocket ?: DatagramSocket().apply { broadcast = true }

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
            listenSocket?.close()
        } catch (_: Exception) {}
        listenSocket = null

        try {
            senderSocket?.close()
        } catch (_: Exception) {}
        senderSocket = null

        try {
            if (multicastLock?.isHeld == true) {
                multicastLock?.release()
            }
        } catch (_: Exception) {}
        multicastLock = null
    }
}
