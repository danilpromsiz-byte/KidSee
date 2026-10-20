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
 * Failsafe: gracefully operates in offline/local mode if Firebase is not configured.
 */
class FirebaseClient(
    database: FirebaseDatabase? = null
) {
    private val database: FirebaseDatabase? = database ?: try {
        FirebaseDatabase.getInstance()
    } catch (e: Throwable) {
        Timber.w(e, "FirebaseDatabase not available, running in offline/local mode")
        null
    }

    // --- Device Status & Heartbeat ---

    suspend fun updateDeviceStatus(device: ChildDevice) {
        val db = database ?: return
        try {
            db.getReference("families/${device.familyId}/devices/${device.id}")
                .setValue(device)
                .await()
        } catch (e: Exception) {
            Timber.e(e, "Error updating device status: ${device.id}")
        }
    }

    fun observeFamilyDevices(familyId: String): Flow<List<ChildDevice>> = callbackFlow {
        trySend(emptyList())
        val db = database
        if (db == null) {
            awaitClose {}
            return@callbackFlow
        }
        val ref = db.getReference("families/$familyId/devices")
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
        trySend(null)
        val db = database
        if (db == null) {
            awaitClose {}
            return@callbackFlow
        }
        val ref = db.getReference("families/$familyId/devices/$deviceId")
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
        val db = database ?: return
        try {
            val ref = db.getReference("commands/${command.targetDeviceId}").push()
            val commandWithId = command.copy(id = ref.key ?: System.currentTimeMillis().toString())
            ref.setValue(commandWithId).await()
            Timber.d("Command sent: ${command.type} to ${command.targetDeviceId}")
        } catch (e: Exception) {
            Timber.e(e, "Failed to send command ${command.type}")
        }
    }

    fun observeIncomingCommands(deviceId: String): Flow<DeviceCommand> = callbackFlow {
        val db = database
        if (db == null) {
            awaitClose {}
            return@callbackFlow
        }
        val ref = db.getReference("commands/$deviceId")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (child in snapshot.children) {
                    val command = child.getValue(DeviceCommand::class.java)
                    if (command != null && command.status == CommandStatus.PENDING) {
                        trySend(command)
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
        val db = database ?: return
        try {
            val ref = db.getReference("voice_messages/${message.targetDeviceId}").push()
            val msgWithId = message.copy(id = ref.key ?: System.currentTimeMillis().toString())
            ref.setValue(msgWithId).await()
            Timber.d("Voice message sent to ${message.targetDeviceId}")
        } catch (e: Exception) {
            Timber.e(e, "Failed to send voice message")
        }
    }

    fun observeVoiceMessages(deviceId: String): Flow<VoiceMessage> = callbackFlow {
        val db = database
        if (db == null) {
            awaitClose {}
            return@callbackFlow
        }
        val ref = db.getReference("voice_messages/$deviceId")
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
        val db = database ?: return
        try {
            val key = rule.id.ifEmpty { db.getReference("schedules/${rule.deviceId}").push().key ?: "" }
            db.getReference("schedules/${rule.deviceId}/$key")
                .setValue(rule.copy(id = key))
                .await()
        } catch (e: Exception) {
            Timber.e(e, "Failed to save schedule rule")
        }
    }

    fun observeSchedules(deviceId: String): Flow<List<ScheduleRule>> = callbackFlow {
        val db = database
        if (db == null) {
            awaitClose {}
            return@callbackFlow
        }
        val ref = db.getReference("schedules/$deviceId")
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
