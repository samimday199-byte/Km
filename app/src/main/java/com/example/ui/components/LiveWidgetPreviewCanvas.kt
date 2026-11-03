package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ActiveRoutineState
import com.example.data.BundledFont
import com.example.data.IdleTextMode
import com.example.data.RoutineCalculator
import com.example.data.WidgetStyleEntity
import com.example.data.WidgetTextAlignment
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.PlusJakartaSansFontFamily
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioCardBg
import com.example.ui.theme.StudioPanelBg
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.toComposeFontFamily
import com.example.widget.WidgetBitmapRenderer
import java.time.LocalTime
import kotlin.math.min

/**
 * Interactive Pro-Studio Wallpaper Canvas rendering the live widget preview using the exact same
 * WidgetBitmapRenderer + real typography + live countdown as the Android Home Screen RemoteViews.
 */
@Composable
fun LiveWidgetPreviewCanvas(
    style: WidgetStyleEntity,
    routineState: ActiveRoutineState,
    wallpaperIndex: Int,
    showGrid: Boolean,
    onCycleWallpaper: () -> Unit,
    onToggleGrid: () -> Unit,
    onToggle24Hour: () -> Unit,
    modifier: Modifier = Modifier
) {
    val wallpaperBrush = remember(wallpaperIndex) {
        when (wallpaperIndex % 5) {
            0 -> Brush.linearGradient(
                colors = listOf(Color(0xFF070B19), Color(0xFF1A103C), Color(0xFF092A3A))
            )
            1 -> Brush.linearGradient(
                colors = listOf(Color(0xFF1F0924), Color(0xFF3B0764), Color(0xFF0F172A))
            )
            2 -> Brush.linearGradient(
                colors = listOf(Color(0xFF042F2E), Color(0xFF064E3B), Color(0xFF0F172A))
            )
            3 -> Brush.linearGradient(
                colors = listOf(Color(0xFF27120B), Color(0xFF4C1D95), Color(0xFF090D16))
            )
            else -> Brush.linearGradient(
                colors = listOf(Color(0xFF111827), Color(0xFF1F2937), Color(0xFF030712))
            )
        }
    }

    val nowMinuteOfDay = remember(routineState) {
        val now = LocalTime.now()
        now.hour * 60 + now.minute
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(wallpaperBrush)
            .border(1.dp, StudioBorder, RoundedCornerShape(18.dp))
            .testTag("live_widget_preview_canvas")
    ) {
        // Decorative Ambient Glow Orbs on the Simulated Home Screen Wallpaper
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0x3800E5FF), Color.Transparent),
                    center = Offset(size.width * 0.25f, size.height * 0.3f),
                    radius = size.minDimension * 0.65f
                ),
                center = Offset(size.width * 0.25f, size.height * 0.3f),
                radius = size.minDimension * 0.65f
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0x38B388FF), Color.Transparent),
                    center = Offset(size.width * 0.78f, size.height * 0.72f),
                    radius = size.minDimension * 0.65f
                ),
                center = Offset(size.width * 0.78f, size.height * 0.72f),
                radius = size.minDimension * 0.65f
            )

            if (showGrid) {
                val step = 24.dp.toPx()
                val gridColor = Color.White.copy(alpha = 0.05f)
                var x = step
                while (x < size.width) {
                    drawLine(gridColor, Offset(x, 0f), Offset(x, size.height), strokeWidth = 1f)
                    x += step
                }
                var y = step
                while (y < size.height) {
                    drawLine(gridColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
                    y += step
                }
            }
        }

        // Top HUD Overlay Bar (Live Clock, Aspect Ratio Badge, Wallpaper & Grid Toggles)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Live System Clock Pill
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(StudioPanelBg.copy(alpha = 0.82f))
                    .border(1.dp, StudioBorder, RoundedCornerShape(50))
                    .clickable { onToggle24Hour() }
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(NeonEmerald)
                )
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = "Clock Format",
                    tint = ElectricCyan,
                    modifier = Modifier.size(13.dp)
                )
                Text(
                    text = "${RoutineCalculator.formatMinuteOfDay(nowMinuteOfDay, style.use24HourFormat)} (${if (style.use24HourFormat) "24H" else "12H"})",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 11.sp,
                    color = TextPrimary
                )
            }

            // Aspect Ratio & Canvas Tools
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(StudioPanelBg.copy(alpha = 0.82f))
                        .border(1.dp, StudioBorder, RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = style.aspectRatioKey,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 10.sp,
                        color = ElectricCyan
                    )
                }

                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(StudioPanelBg.copy(alpha = 0.82f))
                        .border(1.dp, StudioBorder, RoundedCornerShape(8.dp))
                        .clickable { onToggleGrid() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.GridOn,
                        contentDescription = "Toggle Grid",
                        tint = if (showGrid) ElectricCyan else TextSecondary,
                        modifier = Modifier.size(15.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(StudioPanelBg.copy(alpha = 0.82f))
                        .border(1.dp, StudioBorder, RoundedCornerShape(8.dp))
                        .clickable { onCycleWallpaper() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Wallpaper,
                        contentDescription = "Change Preview Wallpaper",
                        tint = TextPrimary,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
        }

        // Centered Live Widget Preview sized according to Aspect Ratio
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 40.dp, bottom = 14.dp, start = 20.dp, end = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            val density = LocalDensity.current.density
            val maxW = maxWidth
            val maxH = maxHeight
            val ratio = style.effectiveAspectRatio()

            val availWdp = maxW.value.coerceAtLeast(100f)
            val availHdp = maxH.value.coerceAtLeast(70f)

            val widgetWdp: Float
            val widgetHdp: Float
            if (availWdp / availHdp > ratio) {
                widgetHdp = min(availHdp, 180f)
                widgetWdp = (widgetHdp * ratio).coerceAtMost(availWdp)
            } else {
                widgetWdp = min(availWdp, 340f)
                widgetHdp = (widgetWdp / ratio).coerceAtMost(availHdp)
            }

            val targetWidthPx = (widgetWdp * density).toInt().coerceAtLeast(96)
            val targetHeightPx = (widgetHdp * density).toInt().coerceAtLeast(56)

            val renderedBitmap = remember(style, targetWidthPx, targetHeightPx, density) {
                WidgetBitmapRenderer.renderBackgroundBitmap(
                    style = style,
                    targetWidthPx = targetWidthPx,
                    targetHeightPx = targetHeightPx,
                    density = density
                ).asImageBitmap()
            }

            val alignment = WidgetTextAlignment.fromKey(style.textAlignmentKey)
            val horizontalAlignment = when (alignment) {
                WidgetTextAlignment.START -> Alignment.Start
                WidgetTextAlignment.CENTER -> Alignment.CenterHorizontally
                WidgetTextAlignment.END -> Alignment.End
            }
            val textAlign = when (alignment) {
                WidgetTextAlignment.START -> TextAlign.Start
                WidgetTextAlignment.CENTER -> TextAlign.Center
                WidgetTextAlignment.END -> TextAlign.End
            }

            val baseTextColor = Color(style.textColorArgb.toInt())
            val globalAlpha = style.globalOpacity.coerceIn(0.1f, 1f)
            val primaryColor = baseTextColor.copy(alpha = baseTextColor.alpha * globalAlpha)
            val secondaryAlpha = (style.secondaryTextOpacity.coerceIn(0.2f, 1f) * globalAlpha).coerceIn(0.1f, 1f)
            val secondaryColor = baseTextColor.copy(alpha = secondaryAlpha)

            val titleSp = style.titleSizeSp.coerceIn(12f, 44f)
            val secondarySp = (titleSp * style.secondaryTextScale.coerceIn(0.5f, 1f)).coerceIn(9f, 34f)
            val durationSp = (secondarySp * 0.90f).coerceIn(9f, 30f)

            val activeBlock = routineState.activeBlock
            val nextBlock = routineState.nextBlock

            val titleText: String
            val timeRangeText: String
            val durationLineText: String

            if (activeBlock != null) {
                titleText = activeBlock.title
                timeRangeText = RoutineCalculator.formatTimeRange(activeBlock, style.use24HourFormat)
                val totalDur = RoutineCalculator.formatTotalDuration(routineState.totalDurationMinutes)
                val remText = RoutineCalculator.formatRemainingTime(routineState.remainingMillis)
                durationLineText = "$totalDur • $remText"
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
                    "Add routine blocks below"
                }
                durationLineText = if (nextBlock != null) {
                    "In ${RoutineCalculator.formatRemainingTime(routineState.remainingMillis).removeSuffix(" left")}"
                } else {
                    "Idle"
                }
            }

            val selectedFontFamily = BundledFont.fromKey(style.fontFamilyKey).toComposeFontFamily()

            Box(
                modifier = Modifier
                    .width(widgetWdp.dp)
                    .height(widgetHdp.dp),
                contentAlignment = Alignment.Center
            ) {
                // Canvas-rendered background ImageView preview
                Image(
                    bitmap = renderedBitmap,
                    contentDescription = "Rendered Widget Background",
                    contentScale = ContentScale.FillBounds,
                    modifier = Modifier.fillMaxSize()
                )

                // Foreground TextViews & Progress Bar preview
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = horizontalAlignment
                ) {
                    // Line 1: Block Title (Bold, Bundled Font)
                    Text(
                        text = titleText,
                        fontFamily = selectedFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = titleSp.sp,
                        color = primaryColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = textAlign
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    // Line 2: Time Range
                    Text(
                        text = timeRangeText,
                        fontFamily = PlusJakartaSansFontFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = secondarySp.sp,
                        color = secondaryColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = textAlign
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    // Line 3: Total Duration + Live Countdown
                    Text(
                        text = durationLineText,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Normal,
                        fontSize = durationSp.sp,
                        color = secondaryColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = textAlign
                    )

                    // Optional Thin Progress Bar
                    if (style.showProgressBar && activeBlock != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { routineState.progressFraction.coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(3.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = primaryColor,
                            trackColor = primaryColor.copy(alpha = 0.22f)
                        )
                    }
                }
            }
        }
    }
}
