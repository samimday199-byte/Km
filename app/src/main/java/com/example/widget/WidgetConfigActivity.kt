package com.example.widget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.MainActivity
import com.example.data.AppDatabase
import com.example.data.BuiltInPresets
import com.example.data.RoutineBlockEntity
import com.example.data.RoutineRepository
import com.example.data.StylePresetEntity
import com.example.data.WidgetStyleEntity
import com.example.ui.theme.RoutineWidgetStudioTheme
import kotlinx.coroutines.launch

/**
 * Configuration activity launched by the Android Home Screen Launcher when placing or reconfiguring a widget.
 */
class WidgetConfigActivity : ComponentActivity() {

    private var appWidgetId: Int = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Default to CANCELED in case the user backs out before confirming
        setResult(Activity.RESULT_CANCELED)

        appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        val repository = RoutineRepository(AppDatabase.getInstance(applicationContext).routineDao())

        setContent {
            RoutineWidgetStudioTheme {
                WidgetConfigScreen(
                    appWidgetId = appWidgetId,
                    repository = repository,
                    onConfirmPlacement = { openFullStudioAfter ->
                        val resultValue = Intent().apply {
                            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                        }
                        setResult(Activity.RESULT_OK, resultValue)
                        if (openFullStudioAfter) {
                            val studioIntent = Intent(this, MainActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                                putExtra(RoutineWidgetUpdater.EXTRA_TARGET_WIDGET_ID, appWidgetId)
                            }
                            startActivity(studioIntent)
                        }
                        finish()
                    }
                )
            }
        }
    }
}

@Composable
private fun WidgetConfigScreen(
    appWidgetId: Int,
    repository: RoutineRepository,
    onConfirmPlacement: (openFullStudioAfter: Boolean) -> Unit
) {
    val scope = rememberCoroutineScope()
    var existingProfiles by remember { mutableStateOf<List<WidgetStyleEntity>>(emptyList()) }
    var presets by remember { mutableStateOf<List<StylePresetEntity>>(BuiltInPresets.all()) }
    var widgetName by remember { mutableStateOf("Home Widget #$appWidgetId") }
    var selectedSourceProfileId by remember { mutableIntStateOf(RoutineBlockEntity.DEFAULT_STUDIO_WIDGET_ID) }
    var selectedPresetId by remember { mutableLongStateOf(1L) }
    var usePresetOverride by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        repository.ensureSeeded()
        existingProfiles = repository.getAllStyles()
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF090B10)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF00E5FF), Color(0xFF7C3AED))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Widgets,
                        contentDescription = "Configure Widget",
                        tint = Color.White
                    )
                }
                Column {
                    Text(
                        text = "Configure Widget #$appWidgetId",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Bind a routine schedule & liquid-glass style preset",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF94A3B8)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            OutlinedTextField(
                value = widgetName,
                onValueChange = { widgetName = it },
                label = { Text("Widget Label") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF00E5FF),
                    unfocusedBorderColor = Color(0xFF262D40),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedLabelColor = Color(0xFF00E5FF),
                    unfocusedLabelColor = Color(0xFF94A3B8)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("config_widget_name_input")
            )

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                item {
                    Text(
                        text = "1. COPY ROUTINE BLOCKS FROM PROFILE",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFF00E5FF),
                        letterSpacing = 1.1.sp
                    )
                }

                items(existingProfiles, key = { it.appWidgetId }) { profile ->
                    val isSelected = selectedSourceProfileId == profile.appWidgetId
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isSelected) Color(0xFF162032) else Color(0xFF121621))
                            .border(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) Color(0xFF00E5FF) else Color(0xFF22293A),
                                shape = RoundedCornerShape(14.dp)
                            )
                            .clickable {
                                selectedSourceProfileId = profile.appWidgetId
                            }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = profile.widgetName,
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp
                            )
                            Text(
                                text = "Aspect ${profile.aspectRatioKey} • Font ${profile.fontFamilyKey}",
                                color = Color(0xFF94A3B8),
                                fontSize = 12.sp
                            )
                        }
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Selected",
                                tint = Color(0xFF00E5FF)
                            )
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "2. OPTIONAL STYLE PRESET OVERRIDE",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFFB388FF),
                        letterSpacing = 1.1.sp
                    )
                }

                items(presets, key = { it.id }) { preset ->
                    val isSelected = usePresetOverride && selectedPresetId == preset.id
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isSelected) Color(0xFF1E1633) else Color(0xFF121621))
                            .border(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) Color(0xFFB388FF) else Color(0xFF22293A),
                                shape = RoundedCornerShape(14.dp)
                            )
                            .clickable {
                                usePresetOverride = true
                                selectedPresetId = preset.id
                            }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(
                                            listOf(
                                                Color(preset.styleSnapshot.color1Argb.toInt()),
                                                Color(preset.styleSnapshot.color3Argb.toInt()),
                                                Color(preset.styleSnapshot.color4Argb.toInt())
                                            )
                                        )
                                    )
                            )
                            Column {
                                Text(
                                    text = preset.name,
                                    color = Color.White,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = if (preset.styleSnapshot.glassEnabled) "Liquid Glass • ${preset.styleSnapshot.fontFamilyKey}" else "Gradient",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 12.sp
                                )
                            }
                        }
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "Preset Selected",
                                tint = Color(0xFFB388FF)
                            )
                        }
                    }
                }
            }

            val context = androidx.compose.ui.platform.LocalContext.current

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            applyConfiguration(
                                context = context,
                                repository = repository,
                                appWidgetId = appWidgetId,
                                widgetName = widgetName.ifBlank { "Widget #$appWidgetId" },
                                sourceProfileId = selectedSourceProfileId,
                                presetOverride = presets.firstOrNull { usePresetOverride && it.id == selectedPresetId }
                            )
                            onConfirmPlacement(true)
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("config_open_studio_button"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = null,
                        tint = Color(0xFF00E5FF)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Edit in Studio", color = Color(0xFF00E5FF))
                }

                Button(
                    onClick = {
                        scope.launch {
                            applyConfiguration(
                                context = context,
                                repository = repository,
                                appWidgetId = appWidgetId,
                                widgetName = widgetName.ifBlank { "Widget #$appWidgetId" },
                                sourceProfileId = selectedSourceProfileId,
                                presetOverride = presets.firstOrNull { usePresetOverride && it.id == selectedPresetId }
                            )
                            onConfirmPlacement(false)
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("config_place_widget_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF00E5FF),
                        contentColor = Color(0xFF061018)
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(imageVector = Icons.Default.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Place Widget", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

private suspend fun applyConfiguration(
    context: android.content.Context,
    repository: RoutineRepository,
    appWidgetId: Int,
    widgetName: String,
    sourceProfileId: Int,
    presetOverride: StylePresetEntity?
) {
    repository.cloneProfileToWidget(
        sourceWidgetId = sourceProfileId,
        targetWidgetId = appWidgetId,
        targetName = widgetName
    )
    if (presetOverride != null) {
        val current = repository.getOrCreateWidgetStyle(appWidgetId, widgetName)
        val merged = presetOverride.styleSnapshot.copy(
            appWidgetId = appWidgetId,
            widgetName = widgetName,
            use24HourFormat = current.use24HourFormat,
            idleTextMode = current.idleTextMode,
            customIdleText = current.customIdleText,
            showProgressBar = current.showProgressBar
        )
        repository.saveStyle(merged)
    }
    RoutineWidgetUpdater.updateSingleWidget(context, appWidgetId)
    RoutineAlarmScheduler.scheduleNextBoundaryAlarm(context)
}
