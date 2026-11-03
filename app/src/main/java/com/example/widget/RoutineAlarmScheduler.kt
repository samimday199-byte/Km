package com.example.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.data.AppDatabase
import com.example.data.RoutineCalculator
import com.example.data.RoutineRepository
import java.time.ZonedDateTime

object RoutineAlarmScheduler {

    const val ACTION_ROUTINE_BOUNDARY_ALARM = "com.example.widget.ACTION_ROUTINE_BOUNDARY_ALARM"
    private const val ALARM_REQUEST_CODE = 9401

    /**
     * Checks whether the app currently has permission to schedule exact alarms on Android 12+ (API 31+).
     */
    fun canScheduleExactAlarms(context: Context): Boolean {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
            ?: return false
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }

    /**
     * Evaluates all routine blocks across all widgets to find the earliest next block start/end boundary,
     * and schedules an exact wakeup via AlarmManager.setExactAndAllowWhileIdle (with automatic inexact fallback).
     */
    suspend fun scheduleNextBoundaryAlarm(context: Context) {
        val db = AppDatabase.getInstance(context)
        val repository = RoutineRepository(db.routineDao())
        val allBlocks = repository.getAllBlocks()
        if (allBlocks.isEmpty()) return

        val now = ZonedDateTime.now()
        val nowEpoch = now.toInstant().toEpochMilli()

        // Group blocks by widget so we compute each widget's next boundary and pick the earliest one
        val grouped = allBlocks.groupBy { it.appWidgetId }
        var earliestBoundaryEpoch = Long.MAX_VALUE

        for ((_, widgetBlocks) in grouped) {
            val state = RoutineCalculator.evaluate(widgetBlocks, now)
            if (state.nextBoundaryEpochMillis > nowEpoch && state.nextBoundaryEpochMillis < earliestBoundaryEpoch) {
                earliestBoundaryEpoch = state.nextBoundaryEpochMillis
            }
        }

        if (earliestBoundaryEpoch == Long.MAX_VALUE) {
            earliestBoundaryEpoch = nowEpoch + 15 * 60 * 1000L
        }

        // Add 250ms buffer past the exact minute boundary so ZonedDateTime.now() is cleanly inside the new block
        val triggerAtMillis = (earliestBoundaryEpoch + 250L).coerceAtLeast(nowEpoch + 1000L)

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, RoutineBoundaryReceiver::class.java).apply {
            action = ACTION_ROUTINE_BOUNDARY_ALARM
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            ALARM_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (canScheduleExactAlarms(context)) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            }
        } catch (_: SecurityException) {
            // Fallback to inexact while-idle alarm if exact alarm permission was revoked
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        }
    }
}
