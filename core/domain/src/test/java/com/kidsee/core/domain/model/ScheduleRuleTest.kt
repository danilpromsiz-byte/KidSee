package com.kidsee.core.domain.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScheduleRuleTest {

    @Test
    fun testDaytimeSchedule_activeWithinRange() {
        // School time: Mon-Fri, 08:30 to 14:30
        val rule = ScheduleRule(
            id = "school_rule",
            deviceId = "dev_1",
            title = "School Hours",
            daysOfWeek = listOf(1, 2, 3, 4, 5), // Mon-Fri
            startHour = 8,
            startMinute = 30,
            endHour = 14,
            endMinute = 30,
            isEnabled = true
        )

        // Monday 10:15 -> Active
        assertTrue(rule.isActiveAt(dayOfWeek = 1, currentHour = 10, currentMinute = 15))

        // Monday 08:29 -> Not active
        assertFalse(rule.isActiveAt(dayOfWeek = 1, currentHour = 8, currentMinute = 29))

        // Monday 14:31 -> Not active
        assertFalse(rule.isActiveAt(dayOfWeek = 1, currentHour = 14, currentMinute = 31))

        // Saturday 10:15 -> Not active (weekend)
        assertFalse(rule.isActiveAt(dayOfWeek = 6, currentHour = 10, currentMinute = 15))
    }

    @Test
    fun testBedtimeSchedule_spansMidnight() {
        // Bedtime: Everyday, 21:00 to 07:00 next morning
        val rule = ScheduleRule(
            id = "bedtime_rule",
            deviceId = "dev_1",
            title = "Bedtime",
            daysOfWeek = listOf(1, 2, 3, 4, 5, 6, 7),
            startHour = 21,
            startMinute = 0,
            endHour = 7,
            endMinute = 0,
            isEnabled = true
        )

        // 23:30 at night -> Active
        assertTrue(rule.isActiveAt(dayOfWeek = 1, currentHour = 23, currentMinute = 30))

        // 03:00 early morning -> Active
        assertTrue(rule.isActiveAt(dayOfWeek = 1, currentHour = 3, currentMinute = 0))

        // 06:59 morning -> Active
        assertTrue(rule.isActiveAt(dayOfWeek = 1, currentHour = 6, currentMinute = 59))

        // 15:00 afternoon -> Not active
        assertFalse(rule.isActiveAt(dayOfWeek = 1, currentHour = 15, currentMinute = 0))
    }

    @Test
    fun testDisabledSchedule_neverActive() {
        val rule = ScheduleRule(
            id = "disabled_rule",
            deviceId = "dev_1",
            title = "Disabled",
            daysOfWeek = listOf(1, 2, 3, 4, 5, 6, 7),
            startHour = 0,
            startMinute = 0,
            endHour = 23,
            endMinute = 59,
            isEnabled = false
        )

        assertFalse(rule.isActiveAt(dayOfWeek = 1, currentHour = 12, currentMinute = 0))
    }
}
