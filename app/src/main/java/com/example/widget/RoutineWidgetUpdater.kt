package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.SystemClock
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.data.AppDatabase
import com.example.data.BundledFont
import com.example.data.IdleTextMode
import com.example.data.RoutineCalculator
import com.example.data.RoutineRepository
import com.example.data.WidgetTextAlignment
import java.time.ZonedDateTime

object RoutineWidgetUpdater {

    const val EXTRA_TARGET_WIDGET_ID = "extra_target_widget_id"

    /**
     * Collects all placed home-screen widget IDs across the 2x1, 4x2, and 4x4 providers.
     */
    fun getAllPlacedWidgetIds(context: Context): IntArray {
        val manager = AppWidgetManager.getInstance(context)
        val ids4x2 = manager.getAppWidgetIds(ComponentName(context, RoutineWidgetProvider::class.java))
        val ids2x1 = manager.getAppWidgetIds(ComponentName(context, RoutineWidgetProvider2x1::class.java))
        val ids4x4 = manager.getAppWidgetIds(ComponentName(context, RoutineWidgetProvider4x4::class.java))
        return ids4x2 + ids2x1 + ids4x4
    }

    /**
     * Updates all placed home-screen widgets and reschedules the next exact block boundary alarm.
     */
    suspend fun updateAllWidgets(context: Context) {
        val appWidgetIds = getAllPlacedWidgetIds(context)
        for (id in appWidgetIds) {
            updateSingleWidget(context, id)
        }
        RoutineAlarmScheduler.scheduleNextBoundaryAlarm(context)
    }

    /**
     * Renders and pushes RemoteViews for a specific [appWidgetId].
     */
    suspend fun updateSingleWidget(
        context: Context,
        appWidgetId: Int,
        optionsBundle: Bundle? = null
    ) {
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) return
        val manager = AppWidgetManager.getInstance(context)
        val db = AppDatabase.getInstance(context)
        val repository = RoutineRepository(db.routineDao())

        val style = repository.getOrCreateWidgetStyle(appWidgetId)
        val blocks = repository.getBlocksForWidget(appWidgetId)
        val now = ZonedDateTime.now()
        val routineState = RoutineCalculator.evaluate(blocks, now)

        val density = context.resources.displayMetrics.density
        val options = optionsBundle ?: manager.getAppWidgetOptions(appWidgetId)
        val minWdp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 250).coerceAtLeast(110)
        val minHdp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 110).coerceAtLeast(48)

        val targetWidthPx = (minWdp * density).toInt()
        val targetHeightPx = (minHdp * density).toInt()

        val bgBitmap = WidgetBitmapRenderer.renderBackgroundBitmap(
            style = style,
            targetWidthPx = targetWidthPx,
            targetHeightPx = targetHeightPx,
            density = density
        )

        val views = RemoteViews(context.packageName, R.layout.widget_routine)
        views.setImageViewBitmap(R.id.widget_bg_image, bgBitmap)

        // Configure Content Container Alignment
        val alignment = WidgetTextAlignment.fromKey(style.textAlignmentKey)
        val horizontalGravity = when (alignment) {
            WidgetTextAlignment.START -> Gravity.START
            WidgetTextAlignment.CENTER -> Gravity.CENTER_HORIZONTAL
            WidgetTextAlignment.END -> Gravity.END
        }
        views.setInt(
            R.id.widget_content_container,
            "setGravity",
            Gravity.CENTER_VERTICAL or horizontalGravity
        )

        // Colors & Opacities
        val baseTextColor = style.textColorArgb.toInt()
        val globalAlpha = style.globalOpacity.coerceIn(0.1f, 1f)
        val primaryTextColor = applyAlpha(baseTextColor, globalAlpha)
        val secondaryAlpha = (style.secondaryTextOpacity.coerceIn(0.2f, 1f) * globalAlpha).coerceIn(0.1f, 1f)
        val secondaryTextColor = applyAlpha(baseTextColor, secondaryAlpha)

        // Typography Scaling
        val titleSp = style.titleSizeSp.coerceIn(12f, 44f)
        val secondarySp = (titleSp * style.secondaryTextScale.coerceIn(0.5f, 1.0f)).coerceIn(9f, 36f)
        val durationSp = (secondarySp * 0.90f).coerceIn(9f, 32f)

        // Determine Line 1, Line 2, Line 3 Content
        val activeBlock = routineState.activeBlock
        val nextBlock = routineState.nextBlock

        val titleText: String
        val timeRangeText: String
        val durationPrefixText: String
        val showChronometer: Boolean

        if (activeBlock != null) {
            titleText = activeBlock.title
            timeRangeText = RoutineCalculator.formatTimeRange(activeBlock, style.use24HourFormat)
            val totalDur = RoutineCalculator.formatTotalDuration(routineState.totalDurationMinutes)
            durationPrefixText = "$totalDur • "
            showChronometer = true
        } else {
            val idleMode = IdleTextMode.fromKey(style.idleTextMode)
            titleText = if (idleMode == IdleTextMode.NEXT_BLOCK && nextBlock != null) {
                "Next: ${nextBlock.title}"
            } else {
                style.customIdleText.ifBlank {
                    nextBlock?.let { "Next: ${it.title}" } ?: "No Active Block"
                }
            }
            timeRangeText = if (nextBlock != null) {
                "Starts ${RoutineCalculator.formatMinuteOfDay(nextBlock.startMinuteOfDay, style.use24HourFormat)}"
            } else {
                "Tap widget to edit routine"
            }
            durationPrefixText = if (nextBlock != null) "In " else "Idle"
            showChronometer = nextBlock != null && routineState.remainingMillis > 0L
        }

        // Line 1: Activate only the chosen BundledFont TextView
        val selectedFont = BundledFont.fromKey(style.fontFamilyKey)
        for (font in BundledFont.entries) {
            if (font == selectedFont) {
                views.setViewVisibility(font.widgetViewId, View.VISIBLE)
                views.setTextViewText(font.widgetViewId, titleText)
                views.setTextViewTextSize(font.widgetViewId, TypedValue.COMPLEX_UNIT_SP, titleSp)
                views.setTextColor(font.widgetViewId, primaryTextColor)
            } else {
                views.setViewVisibility(font.widgetViewId, View.GONE)
            }
        }

        // Line 2: Time Range
        views.setTextViewText(R.id.widget_time_range, timeRangeText)
        views.setTextViewTextSize(R.id.widget_time_range, TypedValue.COMPLEX_UNIT_SP, secondarySp)
        views.setTextColor(R.id.widget_time_range, secondaryTextColor)

        // Line 3: Duration + Live Chronometer Countdown
        views.setTextViewText(R.id.widget_duration_prefix, durationPrefixText)
        views.setTextViewTextSize(R.id.widget_duration_prefix, TypedValue.COMPLEX_UNIT_SP, durationSp)
        views.setTextColor(R.id.widget_duration_prefix, secondaryTextColor)

        if (showChronometer) {
            views.setViewVisibility(R.id.widget_chronometer, View.VISIBLE)
            views.setTextViewTextSize(R.id.widget_chronometer, TypedValue.COMPLEX_UNIT_SP, durationSp)
            views.setTextColor(R.id.widget_chronometer, secondaryTextColor)
            views.setChronometerCountDown(R.id.widget_chronometer, true)
            val baseRealtime = SystemClock.elapsedRealtime() + routineState.remainingMillis
            val formatPattern = if (activeBlock != null) "%s left" else "%s"
            views.setChronometer(R.id.widget_chronometer, baseRealtime, formatPattern, true)
        } else {
            views.setViewVisibility(R.id.widget_chronometer, View.GONE)
            views.setChronometer(R.id.widget_chronometer, SystemClock.elapsedRealtime(), null, false)
        }

        // Optional Thin Progress Bar
        if (style.showProgressBar && activeBlock != null) {
            views.setViewVisibility(R.id.widget_progress, View.VISIBLE)
            val progressPermille = (routineState.progressFraction * 1000f).toInt().coerceIn(0, 1000)
            views.setProgressBar(R.id.widget_progress, 1000, progressPermille, false)
        } else {
            views.setViewVisibility(R.id.widget_progress, View.GONE)
        }

        // Tap Widget -> Open MainActivity targeting this appWidgetId
        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            putExtra(EXTRA_TARGET_WIDGET_ID, appWidgetId)
        }
        val clickPendingIntent = PendingIntent.getActivity(
            context,
            appWidgetId,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_root, clickPendingIntent)

        manager.updateAppWidget(appWidgetId, views)
    }

    private fun applyAlpha(color: Int, factor: Float): Int {
        val alpha = ((Color.alpha(color)) * factor).toInt().coerceIn(0, 255)
        return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color))
    }
}
