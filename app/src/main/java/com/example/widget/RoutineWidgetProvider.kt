package com.example.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.os.Bundle
import com.example.data.AppDatabase
import com.example.data.RoutineRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Primary 4x2 Resizable AppWidgetProvider for Routine Widget Studio.
 */
open class RoutineWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                for (id in appWidgetIds) {
                    RoutineWidgetUpdater.updateSingleWidget(context, id)
                }
                RoutineAlarmScheduler.scheduleNextBoundaryAlarm(context)
            } finally {
                pendingResult.finish()
            }
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                RoutineWidgetUpdater.updateSingleWidget(context, appWidgetId, newOptions)
            } finally {
                pendingResult.finish()
            }
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val repo = RoutineRepository(AppDatabase.getInstance(context).routineDao())
                for (id in appWidgetIds) {
                    repo.deleteWidgetData(id)
                }
                RoutineAlarmScheduler.scheduleNextBoundaryAlarm(context)
            } finally {
                pendingResult.finish()
            }
        }
    }
}

/**
 * 2x1 Compact Resizable AppWidgetProvider.
 */
class RoutineWidgetProvider2x1 : RoutineWidgetProvider()

/**
 * 4x4 Large Resizable AppWidgetProvider.
 */
class RoutineWidgetProvider4x4 : RoutineWidgetProvider()
