package com.kidsee.core.data.repository

import com.kidsee.core.data.local.KidSeePreferences
import com.kidsee.core.domain.model.ChildDevice
import com.kidsee.core.domain.model.DeviceCommand
import com.kidsee.core.domain.model.ScheduleRule
import com.kidsee.core.domain.model.UserRole
import com.kidsee.core.domain.model.VoiceMessage
import com.kidsee.core.network.FirebaseClient
import com.kidsee.core.network.lan.LanDiscoveryManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.merge

class KidSeeRepository(
    private val preferences: KidSeePreferences,
    private val firebaseClient: FirebaseClient = FirebaseClient(),
    val lanDiscoveryManager: LanDiscoveryManager? = null
) {
    constructor(preferences: KidSeePreferences) : this(preferences, FirebaseClient(), null)
    constructor(preferences: KidSeePreferences, lanDiscoveryManager: LanDiscoveryManager) : this(preferences, FirebaseClient(), lanDiscoveryManager)

    val userRoleFlow = preferences.roleFlow
    val familyIdFlow = preferences.familyIdFlow
    val deviceIdFlow = preferences.deviceIdFlow
    val isSetupCompleteFlow = preferences.isSetupCompleteFlow
    val parentPinFlow = preferences.parentPinFlow
    val languageFlow = preferences.languageFlow

    suspend fun saveRole(role: UserRole) {
        preferences.setUserRole(role)
    }

    suspend fun saveLanguage(langCode: String) {
        preferences.setLanguage(langCode)
    }

    suspend fun saveParentPin(pin: String) {
        preferences.setParentPin(pin)
    }

    suspend fun saveFamilyId(familyId: String) {
        preferences.setFamilyId(familyId)
    }

    suspend fun saveDeviceId(deviceId: String) {
        preferences.setDeviceId(deviceId)
    }

    suspend fun completeSetup() {
        preferences.setSetupComplete(true)
    }

    // --- Devices (Merges local LAN discovery + Firebase) ---

    suspend fun updateDeviceStatus(device: ChildDevice) {
        firebaseClient.updateDeviceStatus(device)
        lanDiscoveryManager?.broadcastDevice(device)
    }

    fun observeFamilyDevices(familyId: String): Flow<List<ChildDevice>> {
        val lanFlow = lanDiscoveryManager?.discoveredDevicesFlow ?: flowOf(emptyList())
        val fbFlow = firebaseClient.observeFamilyDevices(familyId)

        return combine(lanFlow, fbFlow) { lanList, fbList ->
            val map = LinkedHashMap<String, ChildDevice>()
            // Remote Firebase devices
            for (d in fbList) {
                map[d.id] = d
            }
            // Real-time LAN devices take precedence and update immediately
            for (d in lanList) {
                map[d.id] = d
            }
            map.values.toList()
        }
    }

    fun observeSingleDevice(familyId: String, deviceId: String): Flow<ChildDevice?> {
        val lanFlow = lanDiscoveryManager?.discoveredDevicesFlow ?: flowOf(emptyList())
        val fbFlow = firebaseClient.observeSingleDevice(familyId, deviceId)

        return combine(lanFlow, fbFlow) { lanList, fbDevice ->
            val lanDevice = lanList.find { it.id == deviceId }
            lanDevice ?: fbDevice
        }
    }

    // --- Commands ---

    suspend fun sendCommand(command: DeviceCommand) {
        firebaseClient.sendCommand(command)
        lanDiscoveryManager?.sendCommand(command)
    }

    fun observeIncomingCommands(deviceId: String): Flow<DeviceCommand> {
        val lanFlow = lanDiscoveryManager?.incomingCommandsFlow ?: emptyFlow()
        val fbFlow = firebaseClient.observeIncomingCommands(deviceId)
        return merge(lanFlow, fbFlow)
    }

    // --- Voice Messages ---

    suspend fun sendVoiceMessage(message: VoiceMessage) {
        firebaseClient.sendVoiceMessage(message)
    }

    fun observeVoiceMessages(deviceId: String): Flow<VoiceMessage> {
        return firebaseClient.observeVoiceMessages(deviceId)
    }

    // --- Scheduling ---

    suspend fun saveScheduleRule(rule: ScheduleRule) {
        firebaseClient.saveScheduleRule(rule)
    }

    fun observeSchedules(deviceId: String): Flow<List<ScheduleRule>> {
        return firebaseClient.observeSchedules(deviceId)
    }
}
