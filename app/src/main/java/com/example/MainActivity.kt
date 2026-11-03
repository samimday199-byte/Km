package com.example

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.ui.StudioMainScreen
import com.example.ui.StudioViewModel
import com.example.ui.theme.RoutineWidgetStudioTheme
import com.example.widget.RoutineWidgetUpdater

class MainActivity : ComponentActivity() {

    private val studioViewModel: StudioViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleWidgetTargetIntent(intent)

        setContent {
            RoutineWidgetStudioTheme {
                StudioMainScreen(viewModel = studioViewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleWidgetTargetIntent(intent)
    }

    private fun handleWidgetTargetIntent(intent: Intent?) {
        val extras = intent?.extras ?: return
        val targetId = extras.getInt(
            RoutineWidgetUpdater.EXTRA_TARGET_WIDGET_ID,
            extras.getInt(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID
            )
        )
        if (targetId != AppWidgetManager.INVALID_APPWIDGET_ID && targetId > 0) {
            studioViewModel.selectWidgetProfile(targetId)
        }
    }
}
