package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.RoutineBlockEntity
import com.example.data.RoutineCalculator
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.PlayheadCoral
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioCardBg
import com.example.ui.theme.StudioElevatedBg
import com.example.ui.theme.StudioPanelBg
import com.example.ui.theme.StudioViolet
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.time.LocalTime
import kotlin.math.roundToInt

/**
 * Alight-Motion-style horizontal 24-hour timeline dock with draggable routine blocks,
 * live red playhead needle, and midnight-crossing visualization.
 */
@Composable
fun Horizontal24HourTimeline(
    blocks: List<RoutineBlockEntity>,
    activeBlockId: Long?,
    use24HourFormat: Boolean,
    onAddBlockClick: () -> Unit,
    onBlockTap: (RoutineBlockEntity) -> Unit,
    onBlockShiftMinutes: (RoutineBlockEntity, Int, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val hourWidthDp = 56f
    val totalTimelineWidthDp = (hourWidthDp * 24f).dp
    val density = LocalDensity.current.density
    val pxPerMinute = (hourWidthDp * density) / 60f

    val now = LocalTime.now()
    val nowMinuteOfDay = now.hour * 60 + now.minute

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(StudioPanelBg)
            .border(1.dp, StudioBorder, RoundedCornerShape(14.dp))
            .padding(vertical = 8.dp)
            .testTag("routine_24h_timeline")
    ) {
        // Timeline Header Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(PlayheadCoral)
                )
                Text(
                    text = "24H ROUTINE TIMELINE • DRAG BLOCKS TO SHIFT",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary
                )
            }

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(ElectricCyan.copy(alpha = 0.16f))
                    .border(1.dp, ElectricCyan.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                    .clickable { onAddBlockClick() }
                    .padding(horizontal = 8.dp, vertical = 3.dp)
                    .testTag("timeline_add_block_button"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Routine Block",
                    tint = ElectricCyan,
                    modifier = Modifier.size(13.dp)
                )
                Text(
                    text = "Add Block",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = ElectricCyan
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Scrollable 24-Hour Ruler + Draggable Block Track
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp)
                .horizontalScroll(scrollState)
                .padding(horizontal = 12.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(totalTimelineWidthDp)
                    .fillMaxHeight()
            ) {
                // Ruler Ticks & Hour Labels
                Canvas(modifier = Modifier.matchParentSize()) {
                    val hourW = size.width / 24f
                    for (h in 0..24) {
                        val x = h * hourW
                        drawLine(
                            color = StudioBorder,
                            start = Offset(x, 0f),
                            end = Offset(x, size.height),
                            strokeWidth = if (h % 6 == 0) 2f else 1f
                        )
                        if (h < 24) {
                            val halfX = x + hourW * 0.5f
                            drawLine(
                                color = StudioBorder.copy(alpha = 0.45f),
                                start = Offset(halfX, 14.dp.toPx()),
                                end = Offset(halfX, size.height),
                                strokeWidth = 1f
                            )
                        }
                    }
                }

                // Hour Marker Labels along top
                for (h in 0..23) {
                    val label = if (use24HourFormat) {
                        String.format("%02d:00", h)
                    } else {
                        when (h) {
                            0 -> "12AM"
                            12 -> "12PM"
                            in 1..11 -> "${h}AM"
                            else -> "${h - 12}PM"
                        }
                    }
                    Text(
                        text = label,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 9.sp,
                        color = if (h == 0 || h == 12) ElectricCyan else TextMuted,
                        modifier = Modifier
                            .offset(x = (h * hourWidthDp + 3f).dp, y = 0.dp)
                    )
                }

                // Render Routine Blocks on Track
                blocks.forEach { block ->
                    val isActive = block.id == activeBlockId
                    if (!block.crossesMidnight) {
                        TimelineBlockChip(
                            block = block,
                            startMin = block.startMinuteOfDay,
                            endMin = block.endMinuteOfDay,
                            isActive = isActive,
                            isMidnightContinuation = false,
                            hourWidthDp = hourWidthDp,
                            pxPerMinute = pxPerMinute,
                            use24HourFormat = use24HourFormat,
                            onTap = { onBlockTap(block) },
                            onShiftMinutes = { delta, commit ->
                                onBlockShiftMinutes(block, delta, commit)
                            }
                        )
                    } else {
                        // Segment 1: startMinuteOfDay -> 1440 (Midnight)
                        TimelineBlockChip(
                            block = block,
                            startMin = block.startMinuteOfDay,
                            endMin = 1440,
                            isActive = isActive,
                            isMidnightContinuation = false,
                            hourWidthDp = hourWidthDp,
                            pxPerMinute = pxPerMinute,
                            use24HourFormat = use24HourFormat,
                            onTap = { onBlockTap(block) },
                            onShiftMinutes = { delta, commit ->
                                onBlockShiftMinutes(block, delta, commit)
                            }
                        )
                        // Segment 2: 0 (12:00 AM) -> endMinuteOfDay
                        if (block.endMinuteOfDay > 0) {
                            TimelineBlockChip(
                                block = block,
                                startMin = 0,
                                endMin = block.endMinuteOfDay,
                                isActive = isActive,
                                isMidnightContinuation = true,
                                hourWidthDp = hourWidthDp,
                                pxPerMinute = pxPerMinute,
                                use24HourFormat = use24HourFormat,
                                onTap = { onBlockTap(block) },
                                onShiftMinutes = { delta, commit ->
                                    onBlockShiftMinutes(block, delta, commit)
                                }
                            )
                        }
                    }
                }

                // Live System Clock Playhead Needle
                val playheadOffsetDp = ((nowMinuteOfDay / 60f) * hourWidthDp).dp
                Box(
                    modifier = Modifier
                        .offset(x = playheadOffsetDp)
                        .width(2.dp)
                        .fillMaxHeight()
                        .background(PlayheadCoral)
                )
                Box(
                    modifier = Modifier
                        .offset(x = playheadOffsetDp - 4.dp, y = 0.dp)
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(PlayheadCoral)
                )
            }
        }
    }
}

@Composable
private fun TimelineBlockChip(
    block: RoutineBlockEntity,
    startMin: Int,
    endMin: Int,
    isActive: Boolean,
    isMidnightContinuation: Boolean,
    hourWidthDp: Float,
    pxPerMinute: Float,
    use24HourFormat: Boolean,
    onTap: () -> Unit,
    onShiftMinutes: (deltaMinutes: Int, commitToUndo: Boolean) -> Unit
) {
    var dragAccumulatedPx by remember(block.id, startMin, endMin) { mutableFloatStateOf(0f) }

    val startDp = ((startMin / 60f) * hourWidthDp) + (dragAccumulatedPx / LocalDensity.current.density)
    val durationMin = (endMin - startMin).coerceAtLeast(15)
    val widthDp = ((durationMin / 60f) * hourWidthDp).coerceAtLeast(28f)
    val accentColor = Color(block.accentColorArgb.toInt())

    Box(
        modifier = Modifier
            .offset(x = startDp.dp, y = 18.dp)
            .width(widthDp.dp)
            .height(44.dp)
            .clip(RoundedCornerShape(9.dp))
            .background(
                if (isActive) accentColor.copy(alpha = 0.34f)
                else accentColor.copy(alpha = 0.20f)
            )
            .border(
                width = if (isActive) 1.8.dp else 1.dp,
                color = if (isActive) Color.White else accentColor,
                shape = RoundedCornerShape(9.dp)
            )
            .clickable { onTap() }
            .pointerInput(block.id, block.startMinuteOfDay, block.endMinuteOfDay) {
                detectHorizontalDragGestures(
                    onDragStart = { dragAccumulatedPx = 0f },
                    onDragEnd = {
                        val rawDeltaMinutes = (dragAccumulatedPx / pxPerMinute).roundToInt()
                        val snappedDelta = ((rawDeltaMinutes / 15f).roundToInt()) * 15
                        dragAccumulatedPx = 0f
                        if (snappedDelta != 0) {
                            onShiftMinutes(snappedDelta, true)
                        }
                    },
                    onDragCancel = { dragAccumulatedPx = 0f },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        dragAccumulatedPx += dragAmount
                    }
                )
            }
            .padding(horizontal = 6.dp, vertical = 4.dp)
    ) {
        Column(verticalArrangement = Arrangement.SpaceBetween) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                if (block.crossesMidnight) {
                    Icon(
                        imageVector = Icons.Default.NightsStay,
                        contentDescription = "Crosses Midnight",
                        tint = AmberWarning,
                        modifier = Modifier.size(10.dp)
                    )
                }
                Text(
                    text = if (isMidnightContinuation) "↳ ${block.title}" else block.title,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = RoutineCalculator.formatTimeRange(block, use24HourFormat),
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 9.sp,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Modal Dialog to Create or Edit a Routine Block (Title, Start/End Time with 12AM = Midnight &
 * Midnight-crossing indicator, Weekday bitmask selector, and Accent Color).
 */
@Composable
fun RoutineBlockEditorDialog(
    initialBlock: RoutineBlockEntity,
    isNewBlock: Boolean,
    use24HourFormat: Boolean,
    onDismiss: () -> Unit,
    onSave: (RoutineBlockEntity) -> Unit,
    onDelete: (Long) -> Unit
) {
    var title by remember(initialBlock) { mutableStateOf(initialBlock.title) }
    var startMin by remember(initialBlock) { mutableIntStateOf(initialBlock.startMinuteOfDay) }
    var endMin by remember(initialBlock) { mutableIntStateOf(initialBlock.endMinuteOfDay) }
    var daysMask by remember(initialBlock) { mutableIntStateOf(initialBlock.daysOfWeekMask) }
    var accentArgb by remember(initialBlock) { mutableLongStateOf(initialBlock.accentColorArgb) }

    val crossesMidnight = endMin <= startMin
    val durationMins = RoutineCalculator.calculateBlockDurationMinutes(startMin, endMin)

    val dayLabels = listOf("M", "T", "W", "T", "F", "S", "S")
    val colorChoices = listOf(
        0xFF00E5FFL, 0xFFA855F7L, 0xFF34D399L, 0xFFF43F5EL,
        0xFFF59E0BL, 0xFF38BDF8L, 0xFF6366F1L, 0xFFEC4899L
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = StudioPanelBg,
        titleContentColor = TextPrimary,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isNewBlock) "Add Routine Block" else "Edit Routine Block",
                    style = MaterialTheme.typography.titleMedium
                )
                if (!isNewBlock && initialBlock.id != 0L) {
                    IconButton(
                        onClick = { onDelete(initialBlock.id) },
                        modifier = Modifier.testTag("delete_routine_block_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Block",
                            tint = PlayheadCoral
                        )
                    }
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Block Title (e.g. Writing)") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ElectricCyan,
                        unfocusedBorderColor = StudioBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("block_title_input")
                )

                // Start Time Slider (0..1439 where 0 = 12:00 AM Midnight)
                TimeOfDaySelectorCard(
                    label = "Start Time",
                    minuteOfDay = startMin,
                    use24Hour = use24HourFormat,
                    onMinuteChange = { startMin = it }
                )

                // End Time Slider (0..1439)
                TimeOfDaySelectorCard(
                    label = "End Time",
                    minuteOfDay = endMin,
                    use24Hour = use24HourFormat,
                    onMinuteChange = { endMin = it }
                )

                // Duration & Midnight-Crossing Status Banner
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(StudioCardBg)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Duration: ${RoutineCalculator.formatTotalDuration(durationMins)}",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 11.sp,
                        color = ElectricCyan
                    )
                    if (crossesMidnight) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.NightsStay,
                                contentDescription = null,
                                tint = AmberWarning,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "Crosses Midnight (+1d)",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 10.sp,
                                color = AmberWarning
                            )
                        }
                    }
                }

                // Active Weekdays Selector
                Text(
                    text = "Active Days (${RoutineCalculator.formatDaysMask(daysMask)})",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    dayLabels.forEachIndexed { index, dayChar ->
                        val bit = 1 shl index
                        val enabled = (daysMask and bit) != 0
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(if (enabled) ElectricCyan else StudioCardBg)
                                .border(
                                    1.dp,
                                    if (enabled) ElectricCyan else StudioBorder,
                                    CircleShape
                                )
                                .clickable {
                                    val toggled = daysMask xor bit
                                    if (toggled != 0) daysMask = toggled
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = dayChar,
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = if (enabled) Color(0xFF041218) else TextSecondary
                            )
                        }
                    }
                }

                // Timeline Block Accent Color
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    colorChoices.forEach { argb ->
                        val isChosen = accentArgb == argb
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .clip(CircleShape)
                                .background(Color(argb.toInt()))
                                .border(
                                    width = if (isChosen) 2.5.dp else 1.dp,
                                    color = if (isChosen) Color.White else StudioBorder,
                                    shape = CircleShape
                                )
                                .clickable { accentArgb = argb }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        initialBlock.copy(
                            title = title,
                            startMinuteOfDay = startMin,
                            endMinuteOfDay = endMin,
                            daysOfWeekMask = daysMask,
                            accentColorArgb = accentArgb
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = ElectricCyan,
                    contentColor = Color(0xFF041218)
                ),
                modifier = Modifier.testTag("save_routine_block_button")
            ) {
                Text("Save Block", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}

@Composable
private fun TimeOfDaySelectorCard(
    label: String,
    minuteOfDay: Int,
    use24Hour: Boolean,
    onMinuteChange: (Int) -> Unit
) {
    val hour24 = (minuteOfDay / 60).coerceIn(0, 23)
    val minute = (minuteOfDay % 60).coerceIn(0, 59)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(StudioCardBg)
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = label, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            Text(
                text = RoutineCalculator.formatMinuteOfDay(minuteOfDay, use24Hour) +
                    if (minuteOfDay == 0) " (Midnight)" else "",
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = ElectricCyan
            )
        }

        ProNumericSlider(
            label = "Hour (0 = 12AM Midnight)",
            value = hour24.toFloat(),
            valueRange = 0f..23f,
            onValueChange = { newH ->
                onMinuteChange(newH.roundToInt().coerceIn(0, 23) * 60 + minute)
            }
        )

        ProNumericSlider(
            label = "Minute",
            value = minute.toFloat(),
            valueRange = 0f..59f,
            accentColor = StudioViolet,
            onValueChange = { newM ->
                onMinuteChange(hour24 * 60 + newM.roundToInt().coerceIn(0, 59))
            }
        )
    }
}
