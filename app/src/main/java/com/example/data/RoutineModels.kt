package com.example.data

import androidx.annotation.FontRes
import androidx.annotation.IdRes
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.R

enum class BundledFont(
    val key: String,
    val displayName: String,
    val category: String,
    @FontRes val fontResId: Int,
    @IdRes val widgetViewId: Int
) {
    SPACE_GROTESK(
        key = "SPACE_GROTESK",
        displayName = "Space Grotesk",
        category = "Geometric Tech",
        fontResId = R.font.space_grotesk,
        widgetViewId = R.id.widget_title_space_grotesk
    ),
    OUTFIT(
        key = "OUTFIT",
        displayName = "Outfit",
        category = "Modern Clean",
        fontResId = R.font.outfit,
        widgetViewId = R.id.widget_title_outfit
    ),
    JETBRAINS_MONO(
        key = "JETBRAINS_MONO",
        displayName = "JetBrains Mono",
        category = "Studio Code",
        fontResId = R.font.jetbrains_mono,
        widgetViewId = R.id.widget_title_jetbrains_mono
    ),
    PLAYFAIR_DISPLAY(
        key = "PLAYFAIR_DISPLAY",
        displayName = "Playfair Display",
        category = "Editorial Serif",
        fontResId = R.font.playfair_display,
        widgetViewId = R.id.widget_title_playfair_display
    ),
    SYNE(
        key = "SYNE",
        displayName = "Syne",
        category = "Bold Cinema",
        fontResId = R.font.syne,
        widgetViewId = R.id.widget_title_syne
    ),
    PLUS_JAKARTA_SANS(
        key = "PLUS_JAKARTA_SANS",
        displayName = "Plus Jakarta",
        category = "Neo Grotesque",
        fontResId = R.font.plus_jakarta_sans,
        widgetViewId = R.id.widget_title_plus_jakarta_sans
    );

    companion object {
        fun fromKey(key: String): BundledFont =
            entries.firstOrNull { it.key == key } ?: SPACE_GROTESK
    }
}

enum class BackgroundMode(val key: String, val label: String) {
    SOLID("SOLID", "Solid"),
    GRADIENT_2("GRADIENT_2", "2-Color Gradient"),
    GRADIENT_4("GRADIENT_4", "4-Color Mesh/Linear");

    companion object {
        fun fromKey(key: String): BackgroundMode =
            entries.firstOrNull { it.key == key } ?: GRADIENT_4
    }
}

enum class WidgetTextAlignment(val key: String, val label: String) {
    START("START", "Left"),
    CENTER("CENTER", "Center"),
    END("END", "Right");

    companion object {
        fun fromKey(key: String): WidgetTextAlignment =
            entries.firstOrNull { it.key == key } ?: START
    }
}

enum class AspectRatioPreset(
    val key: String,
    val label: String,
    val widthRatio: Float,
    val heightRatio: Float
) {
    RATIO_1_1("1:1", "1:1", 1f, 1f),
    RATIO_2_1("2:1", "2:1", 2f, 1f),
    RATIO_3_2("3:2", "3:2", 3f, 2f),
    RATIO_4_3("4:3", "4:3", 4f, 3f),
    RATIO_16_9("16:9", "16:9", 16f, 9f),
    RATIO_4_1("4:1", "4:1", 4f, 1f),
    CUSTOM("CUSTOM", "Custom", 2.2f, 1f);

    companion object {
        fun fromKey(key: String): AspectRatioPreset =
            entries.firstOrNull { it.key == key } ?: RATIO_2_1
    }
}

enum class IdleTextMode(val key: String, val label: String) {
    NEXT_BLOCK("NEXT_BLOCK", "Show 'Next: <Title>'"),
    CUSTOM("CUSTOM", "Custom Idle Message");

    companion object {
        fun fromKey(key: String): IdleTextMode =
            entries.firstOrNull { it.key == key } ?: NEXT_BLOCK
    }
}

/**
 * Represents a scheduled daily routine block belonging to a specific widget (appWidgetId).
 *
 * @param startMinuteOfDay 0..1439 (0 = 12:00 AM midnight, 720 = 12:00 PM noon)
 * @param endMinuteOfDay 0..1439. If endMinuteOfDay <= startMinuteOfDay, the block crosses midnight!
 * @param daysOfWeekMask Bitmask where bit 0 = Monday (1<<0) ... bit 6 = Sunday (1<<6). 127 = every day.
 */
@Entity(tableName = "routine_blocks")
data class RoutineBlockEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val appWidgetId: Int = DEFAULT_STUDIO_WIDGET_ID,
    val title: String,
    val startMinuteOfDay: Int,
    val endMinuteOfDay: Int,
    val daysOfWeekMask: Int = ALL_DAYS_MASK,
    val accentColorArgb: Long = 0xFF00E5FFL
) {
    val crossesMidnight: Boolean
        get() = endMinuteOfDay <= startMinuteOfDay

    fun isEnabledOnDay(isoDayOfWeek: Int): Boolean {
        // isoDayOfWeek: 1 (Monday) .. 7 (Sunday)
        val bit = 1 shl ((isoDayOfWeek - 1).coerceIn(0, 6))
        return (daysOfWeekMask and bit) != 0
    }

    companion object {
        const val ALL_DAYS_MASK = 0b1111111 // 127
        const val WEEKDAYS_MASK = 0b0011111 // Mon-Fri
        const val WEEKENDS_MASK = 0b1100000 // Sat-Sun
        const val DEFAULT_STUDIO_WIDGET_ID = 1
    }
}

/**
 * Complete per-widget visual style and routine preferences mapped by appWidgetId.
 */
@Entity(tableName = "widget_styles")
data class WidgetStyleEntity(
    @PrimaryKey
    val appWidgetId: Int = RoutineBlockEntity.DEFAULT_STUDIO_WIDGET_ID,
    val widgetName: String = "Main Studio Widget",

    // Routine & Clock Display
    val use24HourFormat: Boolean = false,
    val idleTextMode: String = IdleTextMode.NEXT_BLOCK.key,
    val customIdleText: String = "Free Flow • Recharge",
    val showProgressBar: Boolean = true,

    // Typography
    val fontFamilyKey: String = BundledFont.SPACE_GROTESK.key,
    val titleSizeSp: Float = 24f,
    val secondaryTextScale: Float = 0.62f, // 50% - 100% of title
    val secondaryTextOpacity: Float = 0.82f, // 20% - 100%
    val textAlignmentKey: String = WidgetTextAlignment.START.key,
    val textColorArgb: Long = 0xFFFFFFFFL,

    // Background Fill & Gradients
    val backgroundModeKey: String = BackgroundMode.GRADIENT_4.key,
    val color1Argb: Long = 0xFF0F172AL,
    val color2Argb: Long = 0xFF1E1B4BL,
    val color3Argb: Long = 0xFF0E7490L,
    val color4Argb: Long = 0xFF6D28D9L,
    val gradientAngle: Float = 135f, // 0 - 360 degrees
    val stop1: Float = 0.0f,
    val stop2: Float = 0.35f,
    val stop3: Float = 0.72f,
    val stop4: Float = 1.0f,

    // Shape & Aspect Ratio
    val aspectRatioKey: String = AspectRatioPreset.RATIO_2_1.key,
    val customAspectW: Float = 16f,
    val customAspectH: Float = 7f,
    val cornerRadiusDp: Float = 26f, // 0 - 60dp
    val useIndependentCorners: Boolean = false,
    val topLeftRadiusDp: Float = 26f,
    val topRightRadiusDp: Float = 26f,
    val bottomRightRadiusDp: Float = 26f,
    val bottomLeftRadiusDp: Float = 26f,

    // Apple "Liquid Glass" Simulation
    val glassEnabled: Boolean = true,
    val glassBgOpacity: Float = 0.76f, // 0.05 - 1.0
    val glassFrostIntensity: Float = 0.42f, // 0.0 - 1.0
    val glassBorderThicknessDp: Float = 1.6f, // 0.0 - 6.0dp
    val glassBorderOpacity: Float = 0.68f, // 0.0 - 1.0
    val glassSpecularIntensity: Float = 0.55f, // 0.0 - 1.0
    val glassInnerGlowIntensity: Float = 0.38f, // 0.0 - 1.0
    val glassNoiseGrain: Float = 0.18f, // 0.0 - 1.0

    // Global Widget Opacity
    val globalOpacity: Float = 0.96f // 0.1 - 1.0
) {
    fun effectiveAspectRatio(): Float {
        val preset = AspectRatioPreset.fromKey(aspectRatioKey)
        return if (preset == AspectRatioPreset.CUSTOM) {
            (customAspectW.coerceAtLeast(0.5f) / customAspectH.coerceAtLeast(0.5f)).coerceIn(0.4f, 6f)
        } else {
            preset.widthRatio / preset.heightRatio
        }
    }
}

/**
 * Saved style preset (either built-in or user-created) for one-tap loading, duplication, or deletion.
 */
@Entity(tableName = "style_presets")
data class StylePresetEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val name: String,
    val isBuiltIn: Boolean = false,
    val styleSnapshot: WidgetStyleEntity
)

object BuiltInPresets {
    fun all(): List<StylePresetEntity> = listOf(
        StylePresetEntity(
            id = 1L,
            name = "Cyber Liquid Glass",
            isBuiltIn = true,
            styleSnapshot = WidgetStyleEntity(
                backgroundModeKey = BackgroundMode.GRADIENT_4.key,
                color1Argb = 0xFF0B1120L,
                color2Argb = 0xFF1E1B4BL,
                color3Argb = 0xFF0891B2L,
                color4Argb = 0xFF7C3AEDL,
                gradientAngle = 135f,
                stop1 = 0f,
                stop2 = 0.35f,
                stop3 = 0.72f,
                stop4 = 1f,
                glassEnabled = true,
                glassBgOpacity = 0.74f,
                glassFrostIntensity = 0.45f,
                glassBorderThicknessDp = 1.6f,
                glassBorderOpacity = 0.72f,
                glassSpecularIntensity = 0.62f,
                glassInnerGlowIntensity = 0.40f,
                glassNoiseGrain = 0.18f,
                fontFamilyKey = BundledFont.SPACE_GROTESK.key
            )
        ),
        StylePresetEntity(
            id = 2L,
            name = "Midnight Aurora",
            isBuiltIn = true,
            styleSnapshot = WidgetStyleEntity(
                backgroundModeKey = BackgroundMode.GRADIENT_4.key,
                color1Argb = 0xFF06121CL,
                color2Argb = 0xFF064E3BL,
                color3Argb = 0xFF0284C7L,
                color4Argb = 0xFF311042L,
                gradientAngle = 152f,
                stop1 = 0f,
                stop2 = 0.38f,
                stop3 = 0.76f,
                stop4 = 1f,
                glassEnabled = true,
                glassBgOpacity = 0.80f,
                glassFrostIntensity = 0.36f,
                glassBorderThicknessDp = 1.4f,
                glassBorderOpacity = 0.60f,
                glassSpecularIntensity = 0.50f,
                glassInnerGlowIntensity = 0.45f,
                glassNoiseGrain = 0.15f,
                fontFamilyKey = BundledFont.OUTFIT.key
            )
        ),
        StylePresetEntity(
            id = 3L,
            name = "Solar Flare Studio",
            isBuiltIn = true,
            styleSnapshot = WidgetStyleEntity(
                backgroundModeKey = BackgroundMode.GRADIENT_4.key,
                color1Argb = 0xFF1F0A1DL,
                color2Argb = 0xFF7C2D12L,
                color3Argb = 0xFFBE123CL,
                color4Argb = 0xFFD97706L,
                gradientAngle = 45f,
                stop1 = 0f,
                stop2 = 0.32f,
                stop3 = 0.68f,
                stop4 = 1f,
                glassEnabled = true,
                glassBgOpacity = 0.82f,
                glassFrostIntensity = 0.30f,
                glassBorderThicknessDp = 1.8f,
                glassBorderOpacity = 0.65f,
                glassSpecularIntensity = 0.58f,
                glassInnerGlowIntensity = 0.35f,
                glassNoiseGrain = 0.22f,
                fontFamilyKey = BundledFont.SYNE.key
            )
        ),
        StylePresetEntity(
            id = 4L,
            name = "Apple Frost Crystal",
            isBuiltIn = true,
            styleSnapshot = WidgetStyleEntity(
                backgroundModeKey = BackgroundMode.GRADIENT_2.key,
                color1Argb = 0xFF334155L,
                color2Argb = 0xFF0F172AL,
                gradientAngle = 120f,
                glassEnabled = true,
                glassBgOpacity = 0.56f,
                glassFrostIntensity = 0.68f,
                glassBorderThicknessDp = 2.0f,
                glassBorderOpacity = 0.85f,
                glassSpecularIntensity = 0.78f,
                glassInnerGlowIntensity = 0.55f,
                glassNoiseGrain = 0.24f,
                fontFamilyKey = BundledFont.PLUS_JAKARTA_SANS.key
            )
        ),
        StylePresetEntity(
            id = 5L,
            name = "Velvet Obsidian",
            isBuiltIn = true,
            styleSnapshot = WidgetStyleEntity(
                backgroundModeKey = BackgroundMode.GRADIENT_2.key,
                color1Argb = 0xFF090A0FL,
                color2Argb = 0xFF1E293BL,
                gradientAngle = 165f,
                glassEnabled = true,
                glassBgOpacity = 0.90f,
                glassFrostIntensity = 0.20f,
                glassBorderThicknessDp = 1.2f,
                glassBorderOpacity = 0.50f,
                glassSpecularIntensity = 0.35f,
                glassInnerGlowIntensity = 0.25f,
                glassNoiseGrain = 0.14f,
                fontFamilyKey = BundledFont.JETBRAINS_MONO.key
            )
        ),
        StylePresetEntity(
            id = 6L,
            name = "Neon Vaporwave",
            isBuiltIn = true,
            styleSnapshot = WidgetStyleEntity(
                backgroundModeKey = BackgroundMode.GRADIENT_4.key,
                color1Argb = 0xFF2E1065L,
                color2Argb = 0xFF701A75L,
                color3Argb = 0xFFDB2777L,
                color4Argb = 0xFF06B6D4L,
                gradientAngle = 115f,
                stop1 = 0f,
                stop2 = 0.30f,
                stop3 = 0.66f,
                stop4 = 1f,
                glassEnabled = true,
                glassBgOpacity = 0.78f,
                glassFrostIntensity = 0.38f,
                glassBorderThicknessDp = 1.7f,
                glassBorderOpacity = 0.75f,
                glassSpecularIntensity = 0.64f,
                glassInnerGlowIntensity = 0.48f,
                glassNoiseGrain = 0.20f,
                fontFamilyKey = BundledFont.SYNE.key
            )
        ),
        StylePresetEntity(
            id = 7L,
            name = "Editorial Ivory Noir",
            isBuiltIn = true,
            styleSnapshot = WidgetStyleEntity(
                backgroundModeKey = BackgroundMode.GRADIENT_2.key,
                color1Argb = 0xFF1C1917L,
                color2Argb = 0xFF44403CL,
                gradientAngle = 140f,
                textColorArgb = 0xFFFEF3C7L,
                glassEnabled = true,
                glassBgOpacity = 0.84f,
                glassFrostIntensity = 0.32f,
                glassBorderThicknessDp = 1.5f,
                glassBorderOpacity = 0.55f,
                glassSpecularIntensity = 0.42f,
                glassInnerGlowIntensity = 0.30f,
                glassNoiseGrain = 0.25f,
                fontFamilyKey = BundledFont.PLAYFAIR_DISPLAY.key
            )
        ),
        StylePresetEntity(
            id = 8L,
            name = "Emerald Bioglass",
            isBuiltIn = true,
            styleSnapshot = WidgetStyleEntity(
                backgroundModeKey = BackgroundMode.GRADIENT_4.key,
                color1Argb = 0xFF022C22L,
                color2Argb = 0xFF065F46L,
                color3Argb = 0xFF0D9488L,
                color4Argb = 0xFF115E59L,
                gradientAngle = 130f,
                stop1 = 0f,
                stop2 = 0.40f,
                stop3 = 0.75f,
                stop4 = 1f,
                glassEnabled = true,
                glassBgOpacity = 0.72f,
                glassFrostIntensity = 0.50f,
                glassBorderThicknessDp = 1.8f,
                glassBorderOpacity = 0.76f,
                glassSpecularIntensity = 0.66f,
                glassInnerGlowIntensity = 0.46f,
                glassNoiseGrain = 0.16f,
                fontFamilyKey = BundledFont.OUTFIT.key
            )
        )
    )
}
