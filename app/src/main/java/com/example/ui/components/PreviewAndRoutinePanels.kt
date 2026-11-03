package com.example.ui.components

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AlarmOn
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.IdleTextMode
import com.example.data.RoutineBlockEntity
import com.example.data.RoutineCalculator
import com.example.data.StylePresetEntity
import com.example.data.WidgetStyleEntity
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.PlayheadCoral
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioCardBg
import com.example.ui.theme.StudioElevatedBg
import com.example.ui.theme.StudioPanelBg
import com.example.ui.theme.StudioViolet
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.widget.RoutineWidgetProvider
import com.example.widget.RoutineWidgetProvider2x1
import com.example.widget.RoutineWidgetProvider4x4

@Composable
fun PreviewInspectorPanel(
    style: WidgetStyleEntity,
    presets: List<StylePresetEntity>,
    exactAlarmPermitted: Boolean,
    onApplyPreset: (StylePresetEntity) -> Unit,
    onSavePreset: (String) -> Unit,
    onDuplicatePreset: (StylePresetEntity) -> Unit,
    onDeletePreset: (StylePresetEntity) -> Unit,
    onShowBatteryTip: () -> Unit,
    onRefreshExactAlarm: () -> Unit
) {
    val context = LocalContext.current
    var showSavePresetDialog by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Quick Home-Screen Widget Pin Row (2x1, 4x2, 4x4)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(StudioCardBg)
                .border(1.dp, StudioBorder, RoundedCornerShape(12.dp))
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Widgets,
                    contentDescription = null,
                    tint = ElectricCyan,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "ADD RESIZABLE WIDGET TO HOME SCREEN",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PinWidgetSizeButton(
                    label = "2×1 Compact",
                    modifier = Modifier.weight(1f),
                    onClick = {
                        requestPinRoutineWidget(context, RoutineWidgetProvider2x1::class.java)
                    }
                )
                PinWidgetSizeButton(
                    label = "4×2 Studio",
                    modifier = Modifier.weight(1f),
                    onClick = {
                        requestPinRoutineWidget(context, RoutineWidgetProvider::class.java)
                    }
                )
                PinWidgetSizeButton(
                    label = "4×4 Large",
                    modifier = Modifier.weight(1f),
                    onClick = {
                        requestPinRoutineWidget(context, RoutineWidgetProvider4x4::class.java)
                    }
                )
            }
        }

        // System Reliability Row: Exact Alarms & Battery Optimization Tip
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(StudioCardBg)
                    .border(
                        1.dp,
                        if (exactAlarmPermitted) NeonEmerald.copy(alpha = 0.5f) else AmberWarning,
                        RoundedCornerShape(12.dp)
                    )
                    .clickable {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !exactAlarmPermitted) {
                            try {
                                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                                    data = Uri.parse("package:${context.packageName}")
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                }
                                context.startActivity(intent)
                            } catch (_: Exception) {
                                // Fallback if system screen unavailable
                            }
                        }
                        onRefreshExactAlarm()
                    }
                    .padding(10.dp)
                    .testTag("exact_alarm_status_card"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AlarmOn,
                    contentDescription = "Exact Alarm Status",
                    tint = if (exactAlarmPermitted) NeonEmerald else AmberWarning,
                    modifier = Modifier.size(18.dp)
                )
                Column {
                    Text(
                        text = if (exactAlarmPermitted) "Exact Alarms Active" else "Enable Exact Alarms",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Text(
                        text = if (exactAlarmPermitted) "Updates on the second" else "Tap to grant Android 12+",
                        fontSize = 10.sp,
                        color = TextSecondary
                    )
                }
            }

            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(StudioCardBg)
                    .border(1.dp, StudioBorder, RoundedCornerShape(12.dp))
                    .clickable { onShowBatteryTip() }
                    .padding(10.dp)
                    .testTag("battery_optimization_tip_card"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.BatteryChargingFull,
                    contentDescription = "Battery Optimization Tip",
                    tint = StudioViolet,
                    modifier = Modifier.size(18.dp)
                )
                Column {
                    Text(
                        text = "Battery Tip",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Keep Chronometer live",
                        fontSize = 10.sp,
                        color = TextSecondary
                    )
                }
            }
        }

        // Style Presets Header + Save Current Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "STYLE PRESETS (${presets.size} AVAILABLE)",
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = StudioViolet
            )
            OutlinedButton(
                onClick = { showSavePresetDialog = true },
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .height(32.dp)
                    .testTag("save_preset_button")
            ) {
                Icon(
                    imageVector = Icons.Default.BookmarkAdd,
                    contentDescription = null,
                    tint = ElectricCyan,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Save Preset",
                    fontSize = 11.sp,
                    color = ElectricCyan
                )
            }
        }

        // Presets List (8 Built-in + User Presets with Load, Duplicate, Delete)
        presets.forEach { preset ->
            val snap = preset.styleSnapshot
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(StudioCardBg)
                    .border(1.dp, StudioBorder, RoundedCornerShape(12.dp))
                    .clickable { onApplyPreset(preset) }
                    .padding(horizontal = 12.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(9.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        Color(snap.color1Argb.toInt()),
                                        Color(snap.color2Argb.toInt()),
                                        Color(snap.color3Argb.toInt())
                                    )
                                )
                            )
                            .border(1.dp, Color.White.copy(alpha = 0.45f), RoundedCornerShape(9.dp))
                    )
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = preset.name,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                            if (preset.isBuiltIn) {
                                Text(
                                    text = "BUILT-IN",
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 9.sp,
                                    color = ElectricCyan
                                )
                            }
                        }
                        Text(
                            text = "${if (snap.glassEnabled) "Liquid Glass" else "Flat"} • ${snap.backgroundModeKey} • ${snap.fontFamilyKey}",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { onDuplicatePreset(preset) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Duplicate Preset",
                            tint = TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    if (!preset.isBuiltIn) {
                        IconButton(
                            onClick = { onDeletePreset(preset) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete Preset",
                                tint = PlayheadCoral,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    if (showSavePresetDialog) {
        var presetName by remember { mutableStateOf("${style.widgetName} Style") }
        AlertDialog(
            onDismissRequest = { showSavePresetDialog = false },
            containerColor = StudioPanelBg,
            titleContentColor = TextPrimary,
            title = { Text("Save Style Preset", style = MaterialTheme.typography.titleMedium) },
            text = {
                OutlinedTextField(
                    value = presetName,
                    onValueChange = { presetName = it },
                    label = { Text("Preset Name") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ElectricCyan,
                        unfocusedBorderColor = StudioBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onSavePreset(presetName)
                        showSavePresetDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ElectricCyan,
                        contentColor = Color(0xFF041218)
                    )
                ) {
                    Text("Save", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSavePresetDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }
}

@Composable
private fun PinWidgetSizeButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(StudioElevatedBg)
            .border(1.dp, ElectricCyan.copy(alpha = 0.45f), RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontFamily = JetBrainsMonoFontFamily,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = ElectricCyan
        )
    }
}

private fun requestPinRoutineWidget(context: Context, providerClass: Class<*>) {
    val appWidgetManager = AppWidgetManager.getInstance(context)
    val provider = ComponentName(context, providerClass)
    if (appWidgetManager.isRequestPinAppWidgetSupported) {
        appWidgetManager.requestPinAppWidget(provider, null, null)
    }
}

@Composable
fun RoutineInspectorPanel(
    style: WidgetStyleEntity,
    blocks: List<RoutineBlockEntity>,
    activeBlockId: Long?,
    onStyleChange: ((WidgetStyleEntity) -> WidgetStyleEntity) -> Unit,
    onAddBlock: () -> Unit,
    onEditBlock: (RoutineBlockEntity) -> Unit,
    onDeleteBlock: (Long) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // Clock Format (12h/24h) & Thin Progress Bar Toggles
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(StudioCardBg)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("24-Hour Clock", fontSize = 12.sp, color = TextPrimary)
                Switch(
                    checked = style.use24HourFormat,
                    onCheckedChange = { checked ->
                        onStyleChange { it.copy(use24HourFormat = checked) }
                    },
                    colors = SwitchDefaults.colors(checkedThumbColor = ElectricCyan)
                )
            }

            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(StudioCardBg)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Progress Bar", fontSize = 12.sp, color = TextPrimary)
                Switch(
                    checked = style.showProgressBar,
                    onCheckedChange = { checked ->
                        onStyleChange { it.copy(showProgressBar = checked) }
                    },
                    colors = SwitchDefaults.colors(checkedThumbColor = ElectricCyan)
                )
            }
        }

        // Idle Fallback Mode Selector ("Next: <title>" vs Custom Idle Text)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(StudioCardBg)
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "WHEN NO BLOCK IS ACTIVE",
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 10.sp,
                color = TextSecondary
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IdleTextMode.entries.forEach { mode ->
                    val selected = style.idleTextMode == mode.key
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (selected) ElectricCyan.copy(alpha = 0.2f) else StudioElevatedBg)
                            .border(
                                1.dp,
                                if (selected) ElectricCyan else StudioBorder,
                                RoundedCornerShape(8.dp)
                            )
                            .clickable {
                                onStyleChange { it.copy(idleTextMode = mode.key) }
                            }
                            .padding(vertical = 7.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = mode.label,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (selected) ElectricCyan else TextSecondary
                        )
                    }
                }
            }

            if (IdleTextMode.fromKey(style.idleTextMode) == IdleTextMode.CUSTOM) {
                OutlinedTextField(
                    value = style.customIdleText,
                    onValueChange = { text ->
                        onStyleChange { it.copy(customIdleText = text) }
                    },
                    label = { Text("Custom Idle Text") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ElectricCyan,
                        unfocusedBorderColor = StudioBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Routine Blocks Header + Add Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "SCHEDULED BLOCKS (${blocks.size})",
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = ElectricCyan
            )
            Button(
                onClick = onAddBlock,
                colors = ButtonDefaults.buttonColors(
                    containerColor = ElectricCyan,
                    contentColor = Color(0xFF041218)
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .height(32.dp)
                    .testTag("routine_panel_add_block_button")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(15.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Block", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        blocks.forEach { block ->
            val isActive = block.id == activeBlockId
            val durMins = RoutineCalculator.calculateBlockDurationMinutes(
                block.startMinuteOfDay,
                block.endMinuteOfDay
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isActive) Color(0xFF142436) else StudioCardBg)
                    .border(
                        width = if (isActive) 1.5.dp else 1.dp,
                        color = if (isActive) ElectricCyan else StudioBorder,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .clickable { onEditBlock(block) }
                    .padding(horizontal = 12.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(Color(block.accentColorArgb.toInt()))
                    )
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = block.title,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            if (isActive) {
                                Text(
                                    text = "LIVE NOW",
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NeonEmerald
                                )
                            }
                            if (block.crossesMidnight) {
                                Icon(
                                    imageVector = Icons.Default.NightsStay,
                                    contentDescription = "Crosses Midnight",
                                    tint = AmberWarning,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        }
                        Text(
                            text = "${RoutineCalculator.formatTimeRange(block, style.use24HourFormat)} • ${RoutineCalculator.formatTotalDuration(durMins)} • ${RoutineCalculator.formatDaysMask(block.daysOfWeekMask)}",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                }

                Row {
                    IconButton(
                        onClick = { onEditBlock(block) },
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Block",
                            tint = ElectricCyan,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    IconButton(
                        onClick = { onDeleteBlock(block.id) },
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Block",
                            tint = PlayheadCoral,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}
