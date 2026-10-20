package com.kidsee.core.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.kidsee.core.domain.model.ChildDevice
import com.kidsee.core.domain.model.DeviceType
import com.kidsee.core.domain.model.UserRole
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "kidsee_settings")

class KidSeePreferences(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true; encodeDefaults = true }

    companion object {
        val KEY_ROLE = stringPreferencesKey("user_role")
        val KEY_FAMILY_ID = stringPreferencesKey("family_id")
        val KEY_DEVICE_ID = stringPreferencesKey("device_id")
        val KEY_DEVICE_NAME = stringPreferencesKey("device_name")
        val KEY_DEVICE_TYPE = stringPreferencesKey("device_type")
        val KEY_LANGUAGE = stringPreferencesKey("app_language")
        val KEY_IS_SETUP_COMPLETE = booleanPreferencesKey("is_setup_complete")
        val KEY_PARENT_PIN = stringPreferencesKey("parent_pin")
        val KEY_SAVED_DEVICES = stringPreferencesKey("saved_devices_json")
    }

    val roleFlow: Flow<UserRole?> = context.dataStore.data.map { prefs ->
        prefs[KEY_ROLE]?.let { runCatching { UserRole.valueOf(it) }.getOrNull() }
    }

    val familyIdFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_FAMILY_ID] ?: "KID-7777"
    }

    val deviceIdFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_DEVICE_ID] ?: android.provider.Settings.Secure.getString(
            context.contentResolver,
            android.provider.Settings.Secure.ANDROID_ID
        ) ?: ("dev_" + android.os.Build.MODEL.hashCode().toString())
    }

    val isSetupCompleteFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_IS_SETUP_COMPLETE] ?: false
    }

    val parentPinFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_PARENT_PIN] ?: "1234"
    }

    val languageFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_LANGUAGE] ?: "ru"
    }

    suspend fun setUserRole(role: UserRole) {
        context.dataStore.edit { it[KEY_ROLE] = role.name }
    }

    suspend fun setFamilyId(familyId: String) {
        context.dataStore.edit { it[KEY_FAMILY_ID] = familyId }
    }

    suspend fun setDeviceId(deviceId: String) {
        context.dataStore.edit { it[KEY_DEVICE_ID] = deviceId }
    }

    suspend fun setDeviceName(name: String) {
        context.dataStore.edit { it[KEY_DEVICE_NAME] = name }
    }

    suspend fun setDeviceType(type: DeviceType) {
        context.dataStore.edit { it[KEY_DEVICE_TYPE] = type.name }
    }

    suspend fun setLanguage(langCode: String) {
        context.dataStore.edit { it[KEY_LANGUAGE] = langCode }
    }

    suspend fun setSetupComplete(complete: Boolean) {
        context.dataStore.edit { it[KEY_IS_SETUP_COMPLETE] = complete }
    }

    suspend fun setParentPin(pin: String) {
        context.dataStore.edit { it[KEY_PARENT_PIN] = pin }
    }

    val savedDevicesFlow: Flow<List<ChildDevice>> = context.dataStore.data.map { prefs ->
        val jsonString = prefs[KEY_SAVED_DEVICES] ?: return@map emptyList()
        try {
            json.decodeFromString<List<ChildDevice>>(jsonString)
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun saveOrUpdateDevice(device: ChildDevice) {
        context.dataStore.edit { prefs ->
            val currentJson = prefs[KEY_SAVED_DEVICES] ?: "[]"
            val list = try {
                json.decodeFromString<List<ChildDevice>>(currentJson).toMutableList()
            } catch (_: Exception) {
                mutableListOf()
            }
            val idx = list.indexOfFirst { it.id == device.id }
            if (idx >= 0) {
                list[idx] = device
            } else {
                list.add(device)
            }
            prefs[KEY_SAVED_DEVICES] = json.encodeToString(list)
        }
    }

    suspend fun removeDevice(deviceId: String) {
        context.dataStore.edit { prefs ->
            val currentJson = prefs[KEY_SAVED_DEVICES] ?: "[]"
            val list = try {
                json.decodeFromString<List<ChildDevice>>(currentJson).toMutableList()
            } catch (_: Exception) {
                mutableListOf()
            }
            list.removeAll { it.id == deviceId }
            prefs[KEY_SAVED_DEVICES] = json.encodeToString(list)
        }
    }
}
