package com.example.ui.components

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SweepGradientShader
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioCardBg
import com.example.ui.theme.StudioElevatedBg
import com.example.ui.theme.StudioPanelBg
import com.example.ui.theme.StudioViolet
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Compact Alight-Motion-inspired numeric property slider.
 * Long-pressing (or tapping the value pill) opens an exact numeric input dialog.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ProNumericSlider(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    unitSuffix: String = "",
    decimals: Int = 0,
    accentColor: Color = ElectricCyan
) {
    var showExactInputDialog by remember { mutableStateOf(false) }

    val formattedValue = remember(value, decimals, unitSuffix) {
        if (decimals <= 0) {
            "${value.roundToInt()}$unitSuffix"
        } else {
            String.format(Locale.US, "%.${decimals}f%s", value, unitSuffix)
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(StudioCardBg)
            .combinedClickable(
                onClick = { },
                onLongClick = { showExactInputDialog = true }
            )
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(StudioElevatedBg)
                    .border(1.dp, StudioBorder, RoundedCornerShape(6.dp))
                    .clickable { showExactInputDialog = true }
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    text = formattedValue,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = accentColor
                )
            }
        }

        Slider(
            value = value.coerceIn(valueRange.start, valueRange.endInclusive),
            onValueChange = onValueChange,
            valueRange = valueRange,
            colors = SliderDefaults.colors(
                thumbColor = accentColor,
                activeTrackColor = accentColor,
                inactiveTrackColor = StudioElevatedBg
            ),
            modifier = Modifier.height(26.dp)
        )
    }

    if (showExactInputDialog) {
        var textInput by remember(value) {
            mutableStateOf(
                if (decimals <= 0) value.roundToInt().toString()
                else String.format(Locale.US, "%.${decimals}f", value)
            )
        }

        AlertDialog(
            onDismissRequest = { showExactInputDialog = false },
            containerColor = StudioPanelBg,
            titleContentColor = TextPrimary,
            textContentColor = TextSecondary,
            title = {
                Text(
                    text = "Exact Value: $label",
                    style = MaterialTheme.typography.titleMedium
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Enter a number between ${valueRange.start} and ${valueRange.endInclusive}$unitSuffix",
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = textInput,
                        onValueChange = { textInput = it },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = accentColor,
                            unfocusedBorderColor = StudioBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val parsed = textInput.toFloatOrNull()
                        if (parsed != null) {
                            onValueChange(parsed.coerceIn(valueRange.start, valueRange.endInclusive))
                        }
                        showExactInputDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accentColor,
                        contentColor = Color(0xFF051016)
                    )
                ) {
                    Text("Apply", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showExactInputDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }
}

/**
 * Full HSV Color Wheel + Value/Alpha Sliders + Hex Input Modal Dialog.
 */
@Composable
fun HsvColorPickerDialog(
    title: String,
    initialColorArgb: Long,
    onDismiss: () -> Unit,
    onColorSelected: (Long) -> Unit
) {
    val initialInt = initialColorArgb.toInt()
    val initialHsv = remember(initialColorArgb) {
        FloatArray(3).also { AndroidColor.colorToHSV(initialInt, it) }
    }
    var hue by remember { mutableFloatStateOf(initialHsv[0]) }
    var saturation by remember { mutableFloatStateOf(initialHsv[1]) }
    var value by remember { mutableFloatStateOf(initialHsv[2]) }
    var alpha by remember { mutableFloatStateOf(((initialInt ushr 24) and 0xFF) / 255f) }

    val currentColorInt = remember(hue, saturation, value, alpha) {
        val a = (alpha * 255f).roundToInt().coerceIn(0, 255)
        AndroidColor.HSVToColor(a, floatArrayOf(hue, saturation, value))
    }

    var hexInput by remember(currentColorInt) {
        mutableStateOf(String.format(Locale.US, "%08X", currentColorInt))
    }

    val quickSwatches = remember {
        listOf(
            0xFF00E5FFL, 0xFF7C3AEDL, 0xFF0F172AL, 0xFF1E1B4BL,
            0xFF10B981L, 0xFFF43F5EL, 0xFFF59E0BL, 0xFFFFFFFFL
        )
    }

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
                Text(text = title, style = MaterialTheme.typography.titleMedium)
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color(currentColorInt))
                        .border(1.5.dp, Color.White, CircleShape)
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Interactive HSV Wheel Canvas
                Box(
                    modifier = Modifier
                        .size(185.dp)
                        .pointerInput(Unit) {
                            detectTapGestures { offset ->
                                val cx = size.width / 2f
                                val cy = size.height / 2f
                                val r = min(cx, cy)
                                val dx = offset.x - cx
                                val dy = offset.y - cy
                                val dist = hypot(dx, dy)
                                val angle = (Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat() + 360f) % 360f
                                hue = angle
                                saturation = (dist / r).coerceIn(0f, 1f)
                            }
                        }
                        .pointerInput(Unit) {
                            detectDragGestures { change, _ ->
                                change.consume()
                                val cx = size.width / 2f
                                val cy = size.height / 2f
                                val r = min(cx, cy)
                                val dx = change.position.x - cx
                                val dy = change.position.y - cy
                                val dist = hypot(dx, dy)
                                val angle = (Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat() + 360f) % 360f
                                hue = angle
                                saturation = (dist / r).coerceIn(0f, 1f)
                            }
                        }
                ) {
                    Canvas(modifier = Modifier.matchParentSize()) {
                        val cx = size.width / 2f
                        val cy = size.height / 2f
                        val radius = min(cx, cy) - 4.dp.toPx()

                        val hueColors = listOf(
                            Color.Red,
                            Color.Yellow,
                            Color.Green,
                            Color.Cyan,
                            Color.Blue,
                            Color.Magenta,
                            Color.Red
                        )
                        drawCircle(
                            brush = Brush.sweepGradient(hueColors, Offset(cx, cy)),
                            radius = radius,
                            center = Offset(cx, cy)
                        )
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(Color.White, Color.Transparent),
                                center = Offset(cx, cy),
                                radius = radius
                            ),
                            radius = radius,
                            center = Offset(cx, cy)
                        )
                        // Brightness dim overlay
                        drawCircle(
                            color = Color.Black.copy(alpha = (1f - value).coerceIn(0f, 0.85f)),
                            radius = radius,
                            center = Offset(cx, cy)
                        )

                        // Selector handle
                        val rad = Math.toRadians(hue.toDouble())
                        val thumbDist = saturation * radius
                        val tx = cx + (cos(rad) * thumbDist).toFloat()
                        val ty = cy + (sin(rad) * thumbDist).toFloat()

                        drawCircle(
                            color = Color.White,
                            radius = 9.dp.toPx(),
                            center = Offset(tx, ty),
                            style = Stroke(width = 2.5.dp.toPx())
                        )
                        drawCircle(
                            color = Color(currentColorInt),
                            radius = 7.dp.toPx(),
                            center = Offset(tx, ty)
                        )
                    }
                }

                // Value (Brightness) Slider
                ProNumericSlider(
                    label = "Brightness (V)",
                    value = value * 100f,
                    valueRange = 0f..100f,
                    unitSuffix = "%",
                    onValueChange = { value = (it / 100f).coerceIn(0f, 1f) }
                )

                // Alpha Slider
                ProNumericSlider(
                    label = "Channel Alpha",
                    value = alpha * 100f,
                    valueRange = 5f..100f,
                    unitSuffix = "%",
                    accentColor = StudioViolet,
                    onValueChange = { alpha = (it / 100f).coerceIn(0.05f, 1f) }
                )

                // Hex Code Input (#AARRGGBB or #RRGGBB)
                OutlinedTextField(
                    value = hexInput,
                    onValueChange = { raw ->
                        val cleaned = raw.uppercase(Locale.US).filter { it in "0123456789ABCDEF" }.take(8)
                        hexInput = cleaned
                        if (cleaned.length == 6 || cleaned.length == 8) {
                            val fullHex = if (cleaned.length == 6) "FF$cleaned" else cleaned
                            val parsed = fullHex.toLongOrNull(16)
                            if (parsed != null) {
                                val hsv = FloatArray(3)
                                AndroidColor.colorToHSV(parsed.toInt(), hsv)
                                hue = hsv[0]
                                saturation = hsv[1]
                                value = hsv[2]
                                alpha = ((parsed.toInt() ushr 24) and 0xFF) / 255f
                            }
                        }
                    },
                    label = { Text("HEX (AARRGGBB / RRGGBB)") },
                    prefix = { Text("#", color = ElectricCyan) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ElectricCyan,
                        unfocusedBorderColor = StudioBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Quick Swatches
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    quickSwatches.forEach { swatchArgb ->
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color(swatchArgb.toInt()))
                                .border(1.dp, StudioBorder, CircleShape)
                                .clickable {
                                    val hsv = FloatArray(3)
                                    AndroidColor.colorToHSV(swatchArgb.toInt(), hsv)
                                    hue = hsv[0]
                                    saturation = hsv[1]
                                    value = hsv[2]
                                    alpha = 1f
                                }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val unsignedArgb = currentColorInt.toLong() and 0xFFFFFFFFL
                    onColorSelected(unsignedArgb)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = ElectricCyan,
                    contentColor = Color(0xFF041218)
                )
            ) {
                Text("Select Color", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}
