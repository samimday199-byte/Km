package com.example

import com.example.data.RoutineBlockEntity
import com.example.data.RoutineCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class ExampleUnitTest {

    @Test
    fun midnightFormatting_handles12AmAnd24HourCorrectly() {
        assertEquals("12:00 AM", RoutineCalculator.formatMinuteOfDay(0, use24Hour = false))
        assertEquals("00:00", RoutineCalculator.formatMinuteOfDay(0, use24Hour = true))
        assertEquals("12:00 PM", RoutineCalculator.formatMinuteOfDay(720, use24Hour = false))
        assertEquals("12:00", RoutineCalculator.formatMinuteOfDay(720, use24Hour = true))
    }

    @Test
    fun midnightCrossingBlock_isDetectedDuringEarlyMorningHours() {
        val sleepBlock = RoutineBlockEntity(
            id = 10L,
            appWidgetId = 1,
            title = "Sleep & Recovery",
            startMinuteOfDay = 22 * 60 + 30, // 10:30 PM
            endMinuteOfDay = 6 * 60 + 30,    // 6:30 AM next day
            daysOfWeekMask = RoutineBlockEntity.ALL_DAYS_MASK
        )
        assertTrue(sleepBlock.crossesMidnight)

        // Test at 1:30 AM (should still be inside yesterday's 10:30 PM -> 6:30 AM block)
        val earlyMorning = ZonedDateTime.of(2026, 11, 3, 1, 30, 0, 0, ZoneId.of("UTC"))
        val state = RoutineCalculator.evaluate(listOf(sleepBlock), earlyMorning)

        assertNotNull(state.activeBlock)
        assertEquals("Sleep & Recovery", state.activeBlock?.title)
        assertEquals(480, state.totalDurationMinutes) // 8 hours
    }
}
