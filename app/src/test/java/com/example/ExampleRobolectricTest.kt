package com.example

import com.example.data.RoutineBlockEntity
import com.example.data.RoutineCalculator
import com.example.data.WidgetStyleEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleRobolectricTest {

    @Test
    fun widgetStyle_aspectRatioAndRoutineEvaluationWorks() {
        val style = WidgetStyleEntity(
            widgetName = "Routine Widget Studio",
            aspectRatioKey = "2:1"
        )
        assertEquals(2.0f, style.effectiveAspectRatio(), 0.01f)

        val blocks = listOf(
            RoutineBlockEntity(
                id = 1L,
                title = "Writing",
                startMinuteOfDay = 10 * 60,
                endMinuteOfDay = 12 * 60
            )
        )
        assertEquals("10:00 AM - 12:00 PM", RoutineCalculator.formatTimeRange(blocks[0], use24Hour = false))
        assertEquals("2h", RoutineCalculator.formatTotalDuration(120))
        assertTrue(blocks.isNotEmpty())
    }
}
