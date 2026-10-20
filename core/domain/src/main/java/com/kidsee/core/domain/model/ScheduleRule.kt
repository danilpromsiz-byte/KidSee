package com.kidsee.core.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class ScheduleRule(
    val id: String = "",
    val deviceId: String = "",
    val title: String = "",
    val daysOfWeek: List<Int> = listOf(1, 2, 3, 4, 5, 6, 7), // 1=Mon, 7=Sun
    val startHour: Int = 22,
    val startMinute: Int = 0,
    val endHour: Int = 7,
    val endMinute: Int = 0,
    val isEnabled: Boolean = true
) {
    fun isActiveAt(dayOfWeek: Int, currentHour: Int, currentMinute: Int): Boolean {
        if (!isEnabled || !daysOfWeek.contains(dayOfWeek)) return false
        val currentTotalMinutes = currentHour * 60 + currentMinute
        val startTotalMinutes = startHour * 60 + startMinute
        val endTotalMinutes = endHour * 60 + endMinute

        return if (startTotalMinutes <= endTotalMinutes) {
            currentTotalMinutes in startTotalMinutes..endTotalMinutes
        } else {
            // Spans midnight (e.g., 22:00 to 07:00)
            currentTotalMinutes >= startTotalMinutes || currentTotalMinutes <= endTotalMinutes
        }
    }
}

@Serializable
data class FamilyGroup(
    val id: String = "",
    val name: String = "",
    val parentUids: List<String> = emptyList(),
    val pairingCode: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
