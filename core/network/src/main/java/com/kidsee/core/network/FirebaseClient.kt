package com.kidsee.core.network

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.kidsee.core.domain.model.ChildDevice
import com.kidsee.core.domain.model.CommandStatus
import com.kidsee.core.domain.model.DeviceCommand
import com.kidsee.core.domain.model.ScheduleRule
import com.kidsee.core.domain.model.VoiceMessage
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import timber.log.Timber

/**
 * Handles all real-time communication via Firebase Realtime Database.
 */
class FirebaseClient(
    private val database: FirebaseDatabase
) {
    constructor() : this(FirebaseDatabase.getInstance())

    // --- Device Status & Heartbeat ---

    suspend fun updateDeviceStatus(device: ChildDevice) {
        try {
            database.getReference("families/${device.familyId}/devices/${device.id}")
                .setValue(device)
                .await()
        } catch (e: Exception) {
            Timber.e(e, "Error updating device status: ${device.id}")
        }
    }

    fun observeFamilyDevices(familyId: String): Flow<List<ChildDevice>> = callbackFlow {
        val ref = database.getReference("families/$familyId/devices")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val devices = mutableListOf<ChildDevice>()
                for (child in snapshot.children) {
                    child.getValue(ChildDevice::class.java)?.let { devices.add(it) }
                }
                trySend(devices)
            }

            override fun onCancelled(error: DatabaseError) {
                Timber.w("observeFamilyDevices cancelled: ${error.message}")
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    fun observeSingleDevice(familyId: String, deviceId: String): Flow<ChildDevice?> = callbackFlow {
        val ref = database.getReference("families/$familyId/devices/$deviceId")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                trySend(snapshot.getValue(ChildDevice::class.java))
            }

            override fun onCancelled(error: DatabaseError) {
                Timber.w("observeSingleDevice cancelled: ${error.message}")
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    // --- Real-time Commands (Ultra-low latency) ---

    suspend fun sendCommand(command: DeviceCommand) {
        try {
            val ref = database.getReference("commands/${command.targetDeviceId}").push()
            val commandWithId = command.copy(id = ref.key ?: System.currentTimeMillis().toString())
            ref.setValue(commandWithId).await()
            Timber.d("Command sent: ${command.type} to ${command.targetDeviceId}")
        } catch (e: Exception) {
            Timber.e(e, "Failed to send command ${command.type}")
        }
    }

    fun observeIncomingCommands(deviceId: String): Flow<DeviceCommand> = callbackFlow {
        val ref = database.getReference("commands/$deviceId")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (child in snapshot.children) {
                    val command = child.getValue(DeviceCommand::class.java)
                    if (command != null && command.status == CommandStatus.PENDING) {
                        trySend(command)
                        // Acknowledge and clear command
                        child.ref.removeValue()
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Timber.w("observeIncomingCommands cancelled: ${error.message}")
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    // --- Voice Messages ---

    suspend fun sendVoiceMessage(message: VoiceMessage) {
        try {
            val ref = database.getReference("voice_messages/${message.targetDeviceId}").push()
            val msgWithId = message.copy(id = ref.key ?: System.currentTimeMillis().toString())
            ref.setValue(msgWithId).await()
            Timber.d("Voice message sent to ${message.targetDeviceId}")
        } catch (e: Exception) {
            Timber.e(e, "Failed to send voice message")
        }
    }

    fun observeVoiceMessages(deviceId: String): Flow<VoiceMessage> = callbackFlow {
        val ref = database.getReference("voice_messages/$deviceId")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (child in snapshot.children) {
                    val msg = child.getValue(VoiceMessage::class.java)
                    if (msg != null && !msg.isPlayed) {
                        trySend(msg)
                        child.ref.child("isPlayed").setValue(true)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Timber.w("observeVoiceMessages cancelled: ${error.message}")
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    // --- Scheduling ---

    suspend fun saveScheduleRule(rule: ScheduleRule) {
        try {
            val key = rule.id.ifEmpty { database.getReference("schedules/${rule.deviceId}").push().key ?: "" }
            database.getReference("schedules/${rule.deviceId}/$key")
                .setValue(rule.copy(id = key))
                .await()
        } catch (e: Exception) {
            Timber.e(e, "Failed to save schedule rule")
        }
    }

    fun observeSchedules(deviceId: String): Flow<List<ScheduleRule>> = callbackFlow {
        val ref = database.getReference("schedules/$deviceId")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val rules = mutableListOf<ScheduleRule>()
                for (child in snapshot.children) {
                    child.getValue(ScheduleRule::class.java)?.let { rules.add(it) }
                }
                trySend(rules)
            }

            override fun onCancelled(error: DatabaseError) {
                Timber.w("observeSchedules cancelled: ${error.message}")
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }
}
