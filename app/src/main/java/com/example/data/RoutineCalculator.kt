package com.example.data

import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZonedDateTime
import java.util.Locale

data class ActiveRoutineState(
    val activeBlock: RoutineBlockEntity?,
    val nextBlock: RoutineBlockEntity?,
    val blockStartDateTime: ZonedDateTime?,
    val blockEndDateTime: ZonedDateTime?,
    val nextStartDateTime: ZonedDateTime?,
    val totalDurationMinutes: Int,
    val remainingMillis: Long,
    val progressFraction: Float,
    val nextBoundaryEpochMillis: Long
)

object RoutineCalculator {

    /**
     * Evaluates the active block and next upcoming block for [now] from [blocks].
     * Correctly handles 12:00 AM (00:00) and blocks that cross midnight (end <= start).
     */
    fun evaluate(
        blocks: List<RoutineBlockEntity>,
        now: ZonedDateTime = ZonedDateTime.now()
    ): ActiveRoutineState {
        if (blocks.isEmpty()) {
            val defaultNextBoundary = now.plusMinutes(30).toInstant().toEpochMilli()
            return ActiveRoutineState(
                activeBlock = null,
                nextBlock = null,
                blockStartDateTime = null,
                blockEndDateTime = null,
                nextStartDateTime = null,
                totalDurationMinutes = 0,
                remainingMillis = 0L,
                progressFraction = 0f,
                nextBoundaryEpochMillis = defaultNextBoundary
            )
        }

        val zone = now.zone
        val today: LocalDate = now.toLocalDate()
        val yesterday: LocalDate = today.minusDays(1)

        // Generate concrete block intervals from yesterday through the next 8 days
        // so midnight-crossing blocks from yesterday and upcoming blocks across the week are captured.
        data class ConcreteInterval(
            val block: RoutineBlockEntity,
            val start: ZonedDateTime,
            val end: ZonedDateTime,
            val durationMinutes: Int
        )

        val intervals = mutableListOf<ConcreteInterval>()
        for (dayOffset in -1..7) {
            val startDay = today.plusDays(dayOffset.toLong())
            val isoWeekday = startDay.dayOfWeek.value // 1 (Mon) .. 7 (Sun)
            for (block in blocks) {
                if (!block.isEnabledOnDay(isoWeekday)) continue

                val startMin = block.startMinuteOfDay.coerceIn(0, 1439)
                val endMin = block.endMinuteOfDay.coerceIn(0, 1439)
                val startLocalTime = LocalTime.of(startMin / 60, startMin % 60)
                val endLocalTime = LocalTime.of(endMin / 60, endMin % 60)

                val startZdt = LocalDateTime.of(startDay, startLocalTime).atZone(zone)
                val endZdt = if (block.crossesMidnight) {
                    LocalDateTime.of(startDay.plusDays(1), endLocalTime).atZone(zone)
                } else {
                    LocalDateTime.of(startDay, endLocalTime).atZone(zone)
                }

                val durationMins = if (block.crossesMidnight) {
                    (1440 - startMin) + endMin
                } else {
                    endMin - startMin
                }.coerceAtLeast(1)

                intervals.add(
                    ConcreteInterval(
                        block = block,
                        start = startZdt,
                        end = endZdt,
                        durationMinutes = durationMins
                    )
                )
            }
        }

        intervals.sortBy { it.start.toInstant().toEpochMilli() }

        // Find active interval containing [now] (start <= now < end)
        val activeInterval = intervals.firstOrNull { interval ->
            !now.isBefore(interval.start) && now.isBefore(interval.end)
        }

        // Find the earliest upcoming interval starting after [now]
        val nextInterval = intervals.firstOrNull { interval ->
            interval.start.isAfter(now)
        }

        return if (activeInterval != null) {
            val totalMillis = Duration.between(activeInterval.start, activeInterval.end)
                .toMillis()
                .coerceAtLeast(1000L)
            val elapsedMillis = Duration.between(activeInterval.start, now)
                .toMillis()
                .coerceAtLeast(0L)
            val remainingMillis = (totalMillis - elapsedMillis).coerceAtLeast(0L)
            val progress = (elapsedMillis.toFloat() / totalMillis.toFloat()).coerceIn(0f, 1f)

            // Next boundary is the earlier of activeInterval.end or a nested interval's start
            val activeEndEpoch = activeInterval.end.toInstant().toEpochMilli()
            val nextStartEpoch = nextInterval?.start?.toInstant()?.toEpochMilli() ?: Long.MAX_VALUE
            val boundaryEpoch = minOf(activeEndEpoch, nextStartEpoch)

            ActiveRoutineState(
                activeBlock = activeInterval.block,
                nextBlock = nextInterval?.block,
                blockStartDateTime = activeInterval.start,
                blockEndDateTime = activeInterval.end,
                nextStartDateTime = nextInterval?.start,
                totalDurationMinutes = activeInterval.durationMinutes,
                remainingMillis = remainingMillis,
                progressFraction = progress,
                nextBoundaryEpochMillis = boundaryEpoch
            )
        } else {
            val nextStartEpoch = nextInterval?.start?.toInstant()?.toEpochMilli()
                ?: now.plusHours(1).toInstant().toEpochMilli()
            val waitMillis = (nextStartEpoch - now.toInstant().toEpochMilli()).coerceAtLeast(0L)

            ActiveRoutineState(
                activeBlock = null,
                nextBlock = nextInterval?.block,
                blockStartDateTime = null,
                blockEndDateTime = null,
                nextStartDateTime = nextInterval?.start,
                totalDurationMinutes = nextInterval?.durationMinutes ?: 0,
                remainingMillis = waitMillis,
                progressFraction = 0f,
                nextBoundaryEpochMillis = nextStartEpoch
            )
        }
    }

    /**
     * Formats minute-of-day (0..1439) where 0 = 12:00 AM (midnight) in 12h or 00:00 in 24h.
     */
    fun formatMinuteOfDay(minuteOfDay: Int, use24Hour: Boolean): String {
        val normalized = ((minuteOfDay % 1440) + 1440) % 1440
        val hour24 = normalized / 60
        val minute = normalized % 60
        return if (use24Hour) {
            String.format(Locale.US, "%02d:%02d", hour24, minute)
        } else {
            val period = if (hour24 < 12) "AM" else "PM"
            val hour12 = when {
                hour24 == 0 -> 12 // 12:00 AM = Midnight
                hour24 > 12 -> hour24 - 12
                else -> hour24
            }
            String.format(Locale.US, "%d:%02d %s", hour12, minute, period)
        }
    }

    /**
     * Formats a block's time range, e.g., "10:00 AM - 12:00 PM" or "22:30 - 06:30 (+1d)".
     */
    fun formatTimeRange(block: RoutineBlockEntity, use24Hour: Boolean): String {
        val startText = formatMinuteOfDay(block.startMinuteOfDay, use24Hour)
        val endText = formatMinuteOfDay(block.endMinuteOfDay, use24Hour)
        return "$startText - $endText"
    }

    /**
     * Formats total duration in compact form: "2h", "1h 30m", "45m".
     */
    fun formatTotalDuration(totalMinutes: Int): String {
        val safeMins = totalMinutes.coerceAtLeast(1)
        val hours = safeMins / 60
        val mins = safeMins % 60
        return when {
            hours > 0 && mins > 0 -> "${hours}h ${mins}m"
            hours > 0 -> "${hours}h"
            else -> "${mins}m"
        }
    }

    /**
     * Formats remaining milliseconds as "1h 24m left" or "18m 42s left".
     */
    fun formatRemainingTime(remainingMillis: Long): String {
        val totalSeconds = (remainingMillis.coerceAtLeast(0L) / 1000L).toInt()
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return when {
            hours > 0 -> "${hours}h ${minutes}m left"
            minutes > 0 -> "${minutes}m ${seconds}s left"
            else -> "${seconds}s left"
        }
    }

    /**
     * Computes duration in minutes for any start and end minute-of-day (handling midnight wrap).
     */
    fun calculateBlockDurationMinutes(startMinute: Int, endMinute: Int): Int {
        return if (endMinute <= startMinute) {
            (1440 - startMinute) + endMinute
        } else {
            endMinute - startMinute
        }.coerceAtLeast(1)
    }

    /**
     * Formats weekday bitmask into a concise summary ("Every day", "Weekdays", "Mon, Wed, Fri").
     */
    fun formatDaysMask(mask: Int): String {
        val cleanMask = mask and RoutineBlockEntity.ALL_DAYS_MASK
        if (cleanMask == RoutineBlockEntity.ALL_DAYS_MASK) return "Every day"
        if (cleanMask == RoutineBlockEntity.WEEKDAYS_MASK) return "Weekdays"
        if (cleanMask == RoutineBlockEntity.WEEKENDS_MASK) return "Weekends"
        val labels = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        val active = labels.filterIndexed { index, _ -> (cleanMask and (1 shl index)) != 0 }
        return if (active.isEmpty()) "No days" else active.joinToString(", ")
    }
}
