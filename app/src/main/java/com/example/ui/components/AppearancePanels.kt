package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AspectRatioPreset
import com.example.data.BackgroundMode
import com.example.data.BundledFont
import com.example.data.WidgetStyleEntity
import com.example.data.WidgetTextAlignment
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioCardBg
import com.example.ui.theme.StudioElevatedBg
import com.example.ui.theme.StudioViolet
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.toComposeFontFamily
import java.util.Locale

@Composable
fun BackgroundInspectorPanel(
    style: WidgetStyleEntity,
    onStyleChange: ((WidgetStyleEntity) -> WidgetStyleEntity) -> Unit
) {
    val activeMode = BackgroundMode.fromKey(style.backgroundModeKey)
    var editingColorIndex by remember { mutableIntStateOf(0) } // 1..4

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // Mode Selector: Solid | 2-Color Gradient | 4-Color Gradient
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            BackgroundMode.entries.forEach { mode ->
                val selected = activeMode == mode
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (selected) ElectricCyan.copy(alpha = 0.2f) else StudioCardBg)
                        .border(
                            1.dp,
                            if (selected) ElectricCyan else StudioBorder,
                            RoundedCornerShape(10.dp)
                        )
                        .clickable {
                            onStyleChange { it.copy(backgroundModeKey = mode.key) }
                        }
                        .padding(vertical = 9.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = mode.label,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (selected) ElectricCyan else TextSecondary
                    )
                }
            }
        }

        // Color Swatches Row (1, 2, or 4 stops depending on mode)
        val stopCount = when (activeMode) {
            BackgroundMode.SOLID -> 1
            BackgroundMode.GRADIENT_2 -> 2
            BackgroundMode.GRADIENT_4 -> 4
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(StudioCardBg)
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val colors = listOf(style.color1Argb, style.color2Argb, style.color3Argb, style.color4Argb)
            for (i in 0 until stopCount) {
                val argb = colors[i]
                val hex = String.format(Locale.US, "#%06X", (argb.toInt() and 0xFFFFFF))
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.clickable { editingColorIndex = i + 1 }
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(argb.toInt()))
                            .border(2.dp, Color.White, CircleShape)
                    )
                    Text(
                        text = "Stop ${i + 1}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Text(
                        text = hex,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 10.sp,
                        color = ElectricCyan
                    )
                }
            }
        }

        if (activeMode != BackgroundMode.SOLID) {
            ProNumericSlider(
                label = "Gradient Angle",
                value = style.gradientAngle,
                valueRange = 0f..360f,
                unitSuffix = "°",
                onValueChange = { angle ->
                    onStyleChange { it.copy(gradientAngle = angle) }
                }
            )
        }

        if (activeMode == BackgroundMode.GRADIENT_4) {
            Text(
                text = "4-COLOR GRADIENT STOP POSITIONS",
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 10.sp,
                color = StudioViolet
            )
            ProNumericSlider(
                label = "Stop 1 Position",
                value = style.stop1 * 100f,
                valueRange = 0f..90f,
                unitSuffix = "%",
                accentColor = StudioViolet,
                onValueChange = { pct ->
                    onStyleChange { it.copy(stop1 = (pct / 100f).coerceIn(0f, 0.9f)) }
                }
            )
            ProNumericSlider(
                label = "Stop 2 Position",
                value = style.stop2 * 100f,
                valueRange = 5f..95f,
                unitSuffix = "%",
                accentColor = StudioViolet,
                onValueChange = { pct ->
                    onStyleChange { it.copy(stop2 = (pct / 100f).coerceIn(0.05f, 0.95f)) }
                }
            )
            ProNumericSlider(
                label = "Stop 3 Position",
                value = style.stop3 * 100f,
                valueRange = 10f..98f,
                unitSuffix = "%",
                accentColor = StudioViolet,
                onValueChange = { pct ->
                    onStyleChange { it.copy(stop3 = (pct / 100f).coerceIn(0.10f, 0.98f)) }
                }
            )
            ProNumericSlider(
                label = "Stop 4 Position",
                value = style.stop4 * 100f,
                valueRange = 20f..100f,
                unitSuffix = "%",
                accentColor = StudioViolet,
                onValueChange = { pct ->
                    onStyleChange { it.copy(stop4 = (pct / 100f).coerceIn(0.20f, 1.0f)) }
                }
            )
        }
    }

    if (editingColorIndex in 1..4) {
        val currentArgb = when (editingColorIndex) {
            1 -> style.color1Argb
            2 -> style.color2Argb
            3 -> style.color3Argb
            else -> style.color4Argb
        }
        HsvColorPickerDialog(
            title = "Pick Stop $editingColorIndex Color",
            initialColorArgb = currentArgb,
            onDismiss = { editingColorIndex = 0 },
            onColorSelected = { newArgb ->
                onStyleChange {
                    when (editingColorIndex) {
                        1 -> it.copy(color1Argb = newArgb)
                        2 -> it.copy(color2Argb = newArgb)
                        3 -> it.copy(color3Argb = newArgb)
                        else -> it.copy(color4Argb = newArgb)
                    }
                }
            }
        )
    }
}

@Composable
fun ShapeInspectorPanel(
    style: WidgetStyleEntity,
    onStyleChange: ((WidgetStyleEntity) -> WidgetStyleEntity) -> Unit
) {
    val activeAspect = AspectRatioPreset.fromKey(style.aspectRatioKey)

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "ASPECT RATIO PRESETS",
            fontFamily = JetBrainsMonoFontFamily,
            fontSize = 10.sp,
            color = ElectricCyan
        )

        // Aspect Ratio Grid: 1:1, 2:1, 3:2, 4:3, 16:9, 4:1, Custom
        val presets = AspectRatioPreset.entries
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            presets.forEach { ratio ->
                val selected = activeAspect == ratio
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (selected) ElectricCyan else StudioCardBg)
                        .border(1.dp, if (selected) ElectricCyan else StudioBorder, RoundedCornerShape(8.dp))
                        .clickable {
                            onStyleChange { it.copy(aspectRatioKey = ratio.key) }
                        }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = ratio.label,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (selected) Color(0xFF041218) else TextSecondary
                    )
                }
            }
        }

        if (activeAspect == AspectRatioPreset.CUSTOM) {
            ProNumericSlider(
                label = "Custom Width Ratio (W)",
                value = style.customAspectW,
                valueRange = 1f..24f,
                decimals = 1,
                onValueChange = { w ->
                    onStyleChange { it.copy(customAspectW = w) }
                }
            )
            ProNumericSlider(
                label = "Custom Height Ratio (H)",
                value = style.customAspectH,
                valueRange = 1f..24f,
                decimals = 1,
                accentColor = StudioViolet,
                onValueChange = { h ->
                    onStyleChange { it.copy(customAspectH = h) }
                }
            )
        }

        // Uniform Corner Radius Slider (0 - 60dp)
        if (!style.useIndependentCorners) {
            ProNumericSlider(
                label = "Corner Radius",
                value = style.cornerRadiusDp,
                valueRange = 0f..60f,
                unitSuffix = "dp",
                onValueChange = { radius ->
                    onStyleChange {
                        it.copy(
                            cornerRadiusDp = radius,
                            topLeftRadiusDp = radius,
                            topRightRadiusDp = radius,
                            bottomRightRadiusDp = radius,
                            bottomLeftRadiusDp = radius
                        )
                    }
                }
            )
        }

        // Independent Per-Corner Radius Toggle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(StudioCardBg)
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Independent Per-Corner Radius",
                fontSize = 12.sp,
                color = TextPrimary
            )
            Switch(
                checked = style.useIndependentCorners,
                onCheckedChange = { independent ->
                    onStyleChange { it.copy(useIndependentCorners = independent) }
                },
                colors = SwitchDefaults.colors(checkedThumbColor = ElectricCyan)
            )
        }

        if (style.useIndependentCorners) {
            ProNumericSlider(
                label = "Top-Left Corner",
                value = style.topLeftRadiusDp,
                valueRange = 0f..60f,
                unitSuffix = "dp",
                onValueChange = { r -> onStyleChange { it.copy(topLeftRadiusDp = r) } }
            )
            ProNumericSlider(
                label = "Top-Right Corner",
                value = style.topRightRadiusDp,
                valueRange = 0f..60f,
                unitSuffix = "dp",
                accentColor = StudioViolet,
                onValueChange = { r -> onStyleChange { it.copy(topRightRadiusDp = r) } }
            )
            ProNumericSlider(
                label = "Bottom-Right Corner",
                value = style.bottomRightRadiusDp,
                valueRange = 0f..60f,
                unitSuffix = "dp",
                onValueChange = { r -> onStyleChange { it.copy(bottomRightRadiusDp = r) } }
            )
            ProNumericSlider(
                label = "Bottom-Left Corner",
                value = style.bottomLeftRadiusDp,
                valueRange = 0f..60f,
                unitSuffix = "dp",
                accentColor = StudioViolet,
                onValueChange = { r -> onStyleChange { it.copy(bottomLeftRadiusDp = r) } }
            )
        }
    }
}

@Composable
fun GlassInspectorPanel(
    style: WidgetStyleEntity,
    onStyleChange: ((WidgetStyleEntity) -> WidgetStyleEntity) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Liquid Glass Master Toggle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(StudioCardBg)
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Apple \"Liquid Glass\" Simulation",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "Specular rim, frost diffusion, inner glow & micro-grain",
                    fontSize = 11.sp,
                    color = TextSecondary
                )
            }
            Switch(
                checked = style.glassEnabled,
                onCheckedChange = { enabled ->
                    onStyleChange { it.copy(glassEnabled = enabled) }
                },
                colors = SwitchDefaults.colors(checkedThumbColor = ElectricCyan)
            )
        }

        ProNumericSlider(
            label = "Global Widget Opacity",
            value = style.globalOpacity * 100f,
            valueRange = 10f..100f,
            unitSuffix = "%",
            onValueChange = { pct ->
                onStyleChange { it.copy(globalOpacity = (pct / 100f).coerceIn(0.1f, 1f)) }
            }
        )

        if (style.glassEnabled) {
            ProNumericSlider(
                label = "Glass Fill Opacity",
                value = style.glassBgOpacity * 100f,
                valueRange = 5f..100f,
                unitSuffix = "%",
                accentColor = StudioViolet,
                onValueChange = { pct ->
                    onStyleChange { it.copy(glassBgOpacity = (pct / 100f).coerceIn(0.05f, 1f)) }
                }
            )
            ProNumericSlider(
                label = "Frost Diffusion Intensity",
                value = style.glassFrostIntensity * 100f,
                valueRange = 0f..100f,
                unitSuffix = "%",
                onValueChange = { pct ->
                    onStyleChange { it.copy(glassFrostIntensity = (pct / 100f).coerceIn(0f, 1f)) }
                }
            )
            ProNumericSlider(
                label = "Border Highlight Thickness",
                value = style.glassBorderThicknessDp,
                valueRange = 0f..6f,
                decimals = 1,
                unitSuffix = "dp",
                accentColor = StudioViolet,
                onValueChange = { dp ->
                    onStyleChange { it.copy(glassBorderThicknessDp = dp.coerceIn(0f, 6f)) }
                }
            )
            ProNumericSlider(
                label = "Border Highlight Opacity",
                value = style.glassBorderOpacity * 100f,
                valueRange = 0f..100f,
                unitSuffix = "%",
                onValueChange = { pct ->
                    onStyleChange { it.copy(glassBorderOpacity = (pct / 100f).coerceIn(0f, 1f)) }
                }
            )
            ProNumericSlider(
                label = "Top-Left Specular Sheen",
                value = style.glassSpecularIntensity * 100f,
                valueRange = 0f..100f,
                unitSuffix = "%",
                accentColor = StudioViolet,
                onValueChange = { pct ->
                    onStyleChange { it.copy(glassSpecularIntensity = (pct / 100f).coerceIn(0f, 1f)) }
                }
            )
            ProNumericSlider(
                label = "Soft Inner Glow",
                value = style.glassInnerGlowIntensity * 100f,
                valueRange = 0f..100f,
                unitSuffix = "%",
                onValueChange = { pct ->
                    onStyleChange { it.copy(glassInnerGlowIntensity = (pct / 100f).coerceIn(0f, 1f)) }
                }
            )
            ProNumericSlider(
                label = "Frosted Noise Grain",
                value = style.glassNoiseGrain * 100f,
                valueRange = 0f..100f,
                unitSuffix = "%",
                accentColor = StudioViolet,
                onValueChange = { pct ->
                    onStyleChange { it.copy(glassNoiseGrain = (pct / 100f).coerceIn(0f, 1f)) }
                }
            )
        }
    }
}

@Composable
fun TextInspectorPanel(
    style: WidgetStyleEntity,
    onStyleChange: ((WidgetStyleEntity) -> WidgetStyleEntity) -> Unit
) {
    var showTextColorPicker by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "BUNDLED STUDIO FONTS (6 FAMILIES)",
            fontFamily = JetBrainsMonoFontFamily,
            fontSize = 10.sp,
            color = ElectricCyan
        )

        // 2x3 Grid of Bundled Fonts
        val fonts = BundledFont.entries
        for (rowIdx in 0 until 2) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (colIdx in 0 until 3) {
                    val font = fonts[rowIdx * 3 + colIdx]
                    val selected = style.fontFamilyKey == font.key
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (selected) ElectricCyan.copy(alpha = 0.2f) else StudioCardBg)
                            .border(
                                1.dp,
                                if (selected) ElectricCyan else StudioBorder,
                                RoundedCornerShape(10.dp)
                            )
                            .clickable {
                                onStyleChange { it.copy(fontFamilyKey = font.key) }
                            }
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = font.displayName,
                            fontFamily = font.toComposeFontFamily(),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = if (selected) ElectricCyan else TextPrimary,
                            maxLines = 1
                        )
                        Text(
                            text = font.category,
                            fontSize = 9.sp,
                            color = TextSecondary,
                            maxLines = 1
                        )
                    }
                }
            }
        }

        // Alignment & Text Color Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            WidgetTextAlignment.entries.forEach { align ->
                val selected = style.textAlignmentKey == align.key
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (selected) ElectricCyan else StudioCardBg)
                        .border(1.dp, if (selected) ElectricCyan else StudioBorder, RoundedCornerShape(8.dp))
                        .clickable {
                            onStyleChange { it.copy(textAlignmentKey = align.key) }
                        }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = align.label,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (selected) Color(0xFF041218) else TextSecondary
                    )
                }
            }

            // Text Color Swatch Button
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(StudioCardBg)
                    .border(1.dp, StudioBorder, RoundedCornerShape(8.dp))
                    .clickable { showTextColorPicker = true }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(Color(style.textColorArgb.toInt()))
                        .border(1.dp, Color.White, CircleShape)
                )
                Text(
                    text = "Color",
                    fontSize = 11.sp,
                    color = TextPrimary
                )
            }
        }

        ProNumericSlider(
            label = "Title Font Size",
            value = style.titleSizeSp,
            valueRange = 14f..42f,
            unitSuffix = "sp",
            onValueChange = { sp ->
                onStyleChange { it.copy(titleSizeSp = sp) }
            }
        )

        ProNumericSlider(
            label = "Secondary Text Scale (% of Title)",
            value = style.secondaryTextScale * 100f,
            valueRange = 50f..100f,
            unitSuffix = "%",
            accentColor = StudioViolet,
            onValueChange = { pct ->
                onStyleChange { it.copy(secondaryTextScale = (pct / 100f).coerceIn(0.5f, 1.0f)) }
            }
        )

        ProNumericSlider(
            label = "Secondary Text Opacity",
            value = style.secondaryTextOpacity * 100f,
            valueRange = 20f..100f,
            unitSuffix = "%",
            onValueChange = { pct ->
                onStyleChange { it.copy(secondaryTextOpacity = (pct / 100f).coerceIn(0.2f, 1.0f)) }
            }
        )
    }

    if (showTextColorPicker) {
        HsvColorPickerDialog(
            title = "Select Widget Text Color",
            initialColorArgb = style.textColorArgb,
            onDismiss = { showTextColorPicker = false },
            onColorSelected = { argb ->
                onStyleChange { it.copy(textColorArgb = argb) }
            }
        )
    }
}
