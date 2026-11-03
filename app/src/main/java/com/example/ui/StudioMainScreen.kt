package com.example.ui

import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.RoundedCorner
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.WidgetStyleEntity
import com.example.ui.components.BackgroundInspectorPanel
import com.example.ui.components.GlassInspectorPanel
import com.example.ui.components.Horizontal24HourTimeline
import com.example.ui.components.LiveWidgetPreviewCanvas
import com.example.ui.components.PreviewInspectorPanel
import com.example.ui.components.RoutineBlockEditorDialog
import com.example.ui.components.RoutineInspectorPanel
import com.example.ui.components.ShapeInspectorPanel
import com.example.ui.components.TextInspectorPanel
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioCarbonBg
import com.example.ui.theme.StudioCardBg
import com.example.ui.theme.StudioElevatedBg
import com.example.ui.theme.StudioPanelBg
import com.example.ui.theme.StudioViolet
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun StudioMainScreen(
    viewModel: StudioViewModel
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    var showNewProfileDialog by remember { mutableStateOf(false) }

    // Refresh exact alarm permission status when returning from Android system settings
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshExactAlarmStatus()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = StudioCarbonBg
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
        ) {
            // 1. Top Studio Header Bar (Profile Switcher, Undo/Redo, Sync)
            StudioTopBar(
                currentStyle = uiState.currentStyle,
                allProfiles = uiState.allProfiles,
                canUndo = uiState.canUndo,
                canRedo = uiState.canRedo,
                onSelectProfile = viewModel::selectWidgetProfile,
                onCreateProfileClick = { showNewProfileDialog = true },
                onUndo = viewModel::undo,
                onRedo = viewModel::redo,
                onSyncWidgets = viewModel::forceRefreshAllWidgets
            )

            // Optional floating status banner
            AnimatedVisibility(
                visible = uiState.statusBannerMessage != null,
                enter = fadeIn(animationSpec = spring()),
                exit = fadeOut(animationSpec = spring())
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 2.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(NeonEmerald.copy(alpha = 0.16f))
                        .border(1.dp, NeonEmerald.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = uiState.statusBannerMessage.orEmpty(),
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 11.sp,
                        color = NeonEmerald
                    )
                }
            }

            // 2. Large Live Widget Preview on Wallpaper Canvas
            LiveWidgetPreviewCanvas(
                style = uiState.currentStyle,
                routineState = uiState.activeRoutineState,
                wallpaperIndex = uiState.selectedWallpaperIndex,
                showGrid = uiState.showCanvasGrid,
                onCycleWallpaper = viewModel::cycleWallpaper,
                onToggleGrid = viewModel::toggleCanvasGrid,
                onToggle24Hour = {
                    viewModel.updateStyle { it.copy(use24HourFormat = !it.use24HourFormat) }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp)
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            )

            // 3. Horizontal 24-Hour Timeline with Draggable Routine Blocks
            Horizontal24HourTimeline(
                blocks = uiState.currentBlocks,
                activeBlockId = uiState.activeRoutineState.activeBlock?.id,
                use24HourFormat = uiState.currentStyle.use24HourFormat,
                onAddBlockClick = viewModel::openNewBlockEditor,
                onBlockTap = viewModel::openEditBlockEditor,
                onBlockShiftMinutes = viewModel::shiftBlockMinutesOnTimeline,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            )

            // 4. Bottom-Sheet Style Inspector Panel for Active Tab
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
                    .background(StudioPanelBg)
                    .border(
                        1.dp,
                        StudioBorder,
                        RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp)
                    )
            ) {
                // Inspector Sheet Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${uiState.selectedTab.label.uppercase()} INSPECTOR",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = ElectricCyan
                    )
                    Text(
                        text = "Long-press slider value for exact input",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 9.sp,
                        color = TextMuted
                    )
                }

                // Scrollable Inspector Body
                val inspectorScroll = rememberScrollState()
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(inspectorScroll)
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    AnimatedContent(
                        targetState = uiState.selectedTab,
                        transitionSpec = {
                            fadeIn(animationSpec = spring()) togetherWith fadeOut(animationSpec = spring())
                        },
                        label = "inspector_tab_transition"
                    ) { tab ->
                        when (tab) {
                            StudioTab.PREVIEW -> PreviewInspectorPanel(
                                style = uiState.currentStyle,
                                presets = uiState.allPresets,
                                exactAlarmPermitted = uiState.exactAlarmPermitted,
                                onApplyPreset = viewModel::applyStylePreset,
                                onSavePreset = viewModel::saveCurrentStyleAsPreset,
                                onDuplicatePreset = viewModel::duplicateStylePreset,
                                onDeletePreset = viewModel::deleteUserStylePreset,
                                onShowBatteryTip = { viewModel.setShowBatteryTipDialog(true) },
                                onRefreshExactAlarm = viewModel::refreshExactAlarmStatus
                            )

                            StudioTab.ROUTINE -> RoutineInspectorPanel(
                                style = uiState.currentStyle,
                                blocks = uiState.currentBlocks,
                                activeBlockId = uiState.activeRoutineState.activeBlock?.id,
                                onStyleChange = { transform -> viewModel.updateStyle(true, transform) },
                                onAddBlock = viewModel::openNewBlockEditor,
                                onEditBlock = viewModel::openEditBlockEditor,
                                onDeleteBlock = viewModel::deleteRoutineBlock
                            )

                            StudioTab.BACKGROUND -> BackgroundInspectorPanel(
                                style = uiState.currentStyle,
                                onStyleChange = { transform -> viewModel.updateStyle(true, transform) }
                            )

                            StudioTab.SHAPE -> ShapeInspectorPanel(
                                style = uiState.currentStyle,
                                onStyleChange = { transform -> viewModel.updateStyle(true, transform) }
                            )

                            StudioTab.GLASS -> GlassInspectorPanel(
                                style = uiState.currentStyle,
                                onStyleChange = { transform -> viewModel.updateStyle(true, transform) }
                            )

                            StudioTab.TEXT -> TextInspectorPanel(
                                style = uiState.currentStyle,
                                onStyleChange = { transform -> viewModel.updateStyle(true, transform) }
                            )
                        }
                    }
                }
            }

            // 5. Pro Video-Editor Bottom Icon Toolbar (Preview | Routine | Background | Shape | Glass | Text)
            StudioBottomToolbar(
                selectedTab = uiState.selectedTab,
                onSelectTab = viewModel::selectTab
            )
        }
    }

    // Routine Block Create/Edit Modal
    uiState.editingBlock?.let { draftBlock ->
        RoutineBlockEditorDialog(
            initialBlock = draftBlock,
            isNewBlock = uiState.isCreatingNewBlock,
            use24HourFormat = uiState.currentStyle.use24HourFormat,
            onDismiss = viewModel::dismissBlockEditor,
            onSave = viewModel::saveRoutineBlock,
            onDelete = viewModel::deleteRoutineBlock
        )
    }

    // New Widget Profile Dialog
    if (showNewProfileDialog) {
        var profileName by remember { mutableStateOf("Workout & Evening Widget") }
        AlertDialog(
            onDismissRequest = { showNewProfileDialog = false },
            containerColor = StudioPanelBg,
            titleContentColor = TextPrimary,
            title = { Text("New Widget Style & Routine Profile", style = MaterialTheme.typography.titleMedium) },
            text = {
                OutlinedTextField(
                    value = profileName,
                    onValueChange = { profileName = it },
                    label = { Text("Profile Name") },
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
                        viewModel.createNewProfile(profileName)
                        showNewProfileDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ElectricCyan,
                        contentColor = Color(0xFF041218)
                    )
                ) {
                    Text("Create", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewProfileDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    // Battery Optimization Tip Dialog
    if (uiState.showBatteryTipDialog) {
        val context = LocalContext.current
        AlertDialog(
            onDismissRequest = { viewModel.setShowBatteryTipDialog(false) },
            containerColor = StudioPanelBg,
            titleContentColor = TextPrimary,
            textContentColor = TextSecondary,
            title = {
                Text("Battery Optimization & Exact Boundary Tips", style = MaterialTheme.typography.titleMedium)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "1. Zero-Wakeup Live Countdown: The widget uses a native RemoteViews Chronometer countdown, so remaining time ticks live every second on your home screen without waking your CPU.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        "2. Exact Boundary Alarms: Routine Widget Studio uses AlarmManager.setExactAndAllowWhileIdle() to switch blocks the exact second a routine block starts or ends.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        "3. Unrestricted Battery Tip: For OEM launchers that aggressively delay background broadcasts, set Routine Widget Studio to 'Unrestricted' in system Battery settings.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        try {
                            val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            context.startActivity(intent)
                        } catch (_: Exception) {
                            // Ignore if unavailable on emulator
                        }
                        viewModel.setShowBatteryTipDialog(false)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ElectricCyan,
                        contentColor = Color(0xFF041218)
                    )
                ) {
                    Text("Open Battery Settings", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.setShowBatteryTipDialog(false) }) {
                    Text("Got It", color = TextSecondary)
                }
            }
        )
    }
}

@Composable
private fun StudioTopBar(
    currentStyle: WidgetStyleEntity,
    allProfiles: List<WidgetStyleEntity>,
    canUndo: Boolean,
    canRedo: Boolean,
    onSelectProfile: (Int) -> Unit,
    onCreateProfileClick: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onSyncWidgets: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Multi-Widget Profile Selector Pill
        Box {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(StudioPanelBg)
                    .border(1.dp, StudioBorder, RoundedCornerShape(10.dp))
                    .clickable { menuExpanded = true }
                    .padding(horizontal = 12.dp, vertical = 7.dp)
                    .testTag("widget_profile_selector"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Column {
                    Text(
                        text = "ROUTINE WIDGET STUDIO",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 8.sp,
                        color = ElectricCyan
                    )
                    Text(
                        text = currentStyle.widgetName,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = "Switch Widget Profile",
                    tint = ElectricCyan
                )
            }

            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false },
                modifier = Modifier.background(StudioElevatedBg)
            ) {
                allProfiles.forEach { profile ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = "${profile.widgetName} (#${profile.appWidgetId})",
                                color = if (profile.appWidgetId == currentStyle.appWidgetId) ElectricCyan else TextPrimary,
                                fontWeight = if (profile.appWidgetId == currentStyle.appWidgetId) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        onClick = {
                            menuExpanded = false
                            onSelectProfile(profile.appWidgetId)
                        }
                    )
                }
                DropdownMenuItem(
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = StudioViolet,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "New Widget Profile...",
                                color = StudioViolet,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    },
                    onClick = {
                        menuExpanded = false
                        onCreateProfileClick()
                    }
                )
            }
        }

        // Undo / Redo / Sync Controls
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            IconButton(
                onClick = onUndo,
                enabled = canUndo,
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(StudioPanelBg)
                    .border(1.dp, StudioBorder, RoundedCornerShape(8.dp))
                    .testTag("undo_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Undo,
                    contentDescription = "Undo",
                    tint = if (canUndo) ElectricCyan else TextMuted,
                    modifier = Modifier.size(18.dp)
                )
            }

            IconButton(
                onClick = onRedo,
                enabled = canRedo,
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(StudioPanelBg)
                    .border(1.dp, StudioBorder, RoundedCornerShape(8.dp))
                    .testTag("redo_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Redo,
                    contentDescription = "Redo",
                    tint = if (canRedo) ElectricCyan else TextMuted,
                    modifier = Modifier.size(18.dp)
                )
            }

            IconButton(
                onClick = onSyncWidgets,
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(StudioPanelBg)
                    .border(1.dp, StudioBorder, RoundedCornerShape(8.dp))
                    .testTag("sync_widgets_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Sync,
                    contentDescription = "Sync Home Screen Widgets",
                    tint = StudioViolet,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun StudioBottomToolbar(
    selectedTab: StudioTab,
    onSelectTab: (StudioTab) -> Unit
) {
    val tabIcons: Map<StudioTab, ImageVector> = remember {
        mapOf(
            StudioTab.PREVIEW to Icons.Default.Visibility,
            StudioTab.ROUTINE to Icons.Default.Schedule,
            StudioTab.BACKGROUND to Icons.Default.Palette,
            StudioTab.SHAPE to Icons.Default.RoundedCorner,
            StudioTab.GLASS to Icons.Default.BlurOn,
            StudioTab.TEXT to Icons.Default.TextFields
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(StudioPanelBg)
            .border(1.dp, StudioBorder)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 6.dp, vertical = 6.dp)
            .testTag("studio_bottom_toolbar"),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        StudioTab.entries.forEach { tab ->
            val isSelected = tab == selectedTab
            val icon = tabIcons[tab] ?: Icons.Default.Visibility
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isSelected) ElectricCyan.copy(alpha = 0.16f) else Color.Transparent)
                    .clickable { onSelectTab(tab) }
                    .padding(vertical = 6.dp)
                    .testTag("tab_${tab.name.lowercase()}"),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = tab.label,
                    tint = if (isSelected) ElectricCyan else TextSecondary,
                    modifier = Modifier.size(19.dp)
                )
                Text(
                    text = tab.label,
                    fontSize = 10.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) ElectricCyan else TextSecondary,
                    maxLines = 1
                )
            }
        }
    }
}
