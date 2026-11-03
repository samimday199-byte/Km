package com.example.widget

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import com.example.data.BackgroundMode
import com.example.data.WidgetStyleEntity
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

object WidgetBitmapRenderer {

    // Keep bitmap dimensions safely under RemoteViews Binder buffer limits (~500KB max per bitmap)
    private const val MAX_DIMENSION_PX = 560

    /**
     * Renders the widget background (Solid / 2-Color / 4-Color Gradient + Apple Liquid Glass simulation)
     * into a downscaled ARGB_8888 Bitmap suitable for both RemoteViews ImageView and Compose Live Preview.
     */
    fun renderBackgroundBitmap(
        style: WidgetStyleEntity,
        targetWidthPx: Int,
        targetHeightPx: Int,
        density: Float
    ): Bitmap {
        val rawW = targetWidthPx.coerceAtLeast(64)
        val rawH = targetHeightPx.coerceAtLeast(48)

        // Downscale proportionally if needed to stay well within RemoteViews memory limits
        val longest = max(rawW, rawH)
        val scaleFactor = if (longest > MAX_DIMENSION_PX) {
            MAX_DIMENSION_PX.toFloat() / longest.toFloat()
        } else {
            1f
        }

        val width = (rawW * scaleFactor).toInt().coerceAtLeast(48)
        val height = (rawH * scaleFactor).toInt().coerceAtLeast(36)
        val effectiveDensity = density * scaleFactor

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val wF = width.toFloat()
        val hF = height.toFloat()
        val maxRadius = min(wF, hF) / 2f

        val tl: Float
        val tr: Float
        val br: Float
        val bl: Float
        if (style.useIndependentCorners) {
            tl = (style.topLeftRadiusDp * effectiveDensity).coerceIn(0f, maxRadius)
            tr = (style.topRightRadiusDp * effectiveDensity).coerceIn(0f, maxRadius)
            br = (style.bottomRightRadiusDp * effectiveDensity).coerceIn(0f, maxRadius)
            bl = (style.bottomLeftRadiusDp * effectiveDensity).coerceIn(0f, maxRadius)
        } else {
            val r = (style.cornerRadiusDp * effectiveDensity).coerceIn(0f, maxRadius)
            tl = r
            tr = r
            br = r
            bl = r
        }

        val radii = floatArrayOf(tl, tl, tr, tr, br, br, bl, bl)
        val borderStrokePx = if (style.glassEnabled) {
            (style.glassBorderThicknessDp * effectiveDensity).coerceIn(0f, 16f)
        } else {
            0f
        }
        val inset = borderStrokePx / 2f + 0.5f
        val rect = RectF(inset, inset, wF - inset, hF - inset)

        val clipPath = Path().apply {
            addRoundRect(rect, radii, Path.Direction.CW)
        }

        val globalAlpha = style.globalOpacity.coerceIn(0.05f, 1f)
        val baseOpacity = if (style.glassEnabled) {
            (style.glassBgOpacity.coerceIn(0.05f, 1f) * globalAlpha).coerceIn(0.05f, 1f)
        } else {
            globalAlpha
        }

        canvas.save()
        canvas.clipPath(clipPath)

        // 1. Base Fill (Solid / 2-Color Gradient / 4-Color Gradient)
        val basePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style.let {
                val mode = BackgroundMode.fromKey(it.backgroundModeKey)
                val angleRad = Math.toRadians(it.gradientAngle.toDouble())
                val cx = wF / 2f
                val cy = hF / 2f
                val radius = hypot(wF.toDouble(), hF.toDouble()).toFloat() / 2f
                val dx = (cos(angleRad) * radius).toFloat()
                val dy = (sin(angleRad) * radius).toFloat()
                val x0 = cx - dx
                val y0 = cy - dy
                val x1 = cx + dx
                val y1 = cy + dy

                when (mode) {
                    BackgroundMode.SOLID -> {
                        color = applyOpacity(it.color1Argb.toInt(), baseOpacity)
                    }
                    BackgroundMode.GRADIENT_2 -> {
                        val c1 = applyOpacity(it.color1Argb.toInt(), baseOpacity)
                        val c2 = applyOpacity(it.color2Argb.toInt(), baseOpacity)
                        shader = LinearGradient(
                            x0, y0, x1, y1,
                            intArrayOf(c1, c2),
                            floatArrayOf(0f, 1f),
                            Shader.TileMode.CLAMP
                        )
                    }
                    BackgroundMode.GRADIENT_4 -> {
                        val c1 = applyOpacity(it.color1Argb.toInt(), baseOpacity)
                        val c2 = applyOpacity(it.color2Argb.toInt(), baseOpacity)
                        val c3 = applyOpacity(it.color3Argb.toInt(), baseOpacity)
                        val c4 = applyOpacity(it.color4Argb.toInt(), baseOpacity)
                        // Ensure strictly ascending stop positions in 0..1
                        val s1 = it.stop1.coerceIn(0f, 0.94f)
                        val s2 = it.stop2.coerceIn(s1 + 0.02f, 0.96f)
                        val s3 = it.stop3.coerceIn(s2 + 0.02f, 0.98f)
                        val s4 = it.stop4.coerceIn(s3 + 0.02f, 1.0f)
                        shader = LinearGradient(
                            x0, y0, x1, y1,
                            intArrayOf(c1, c2, c3, c4),
                            floatArrayOf(s1, s2, s3, s4),
                            Shader.TileMode.CLAMP
                        )
                    }
                }
            }
        }
        canvas.drawRect(rect, basePaint)

        // 2. Apple "Liquid Glass" Optical Layers
        if (style.glassEnabled) {
            // 2a. Frost Diffusion Layer (soft milky glass luminance across the card)
            val frost = (style.glassFrostIntensity.coerceIn(0f, 1f) * globalAlpha)
            if (frost > 0.01f) {
                val frostTopAlpha = (frost * 95f).toInt().coerceIn(0, 255)
                val frostMidAlpha = (frost * 42f).toInt().coerceIn(0, 255)
                val frostBotAlpha = (frost * 68f).toInt().coerceIn(0, 255)
                val frostPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    shader = LinearGradient(
                        0f, 0f, wF, hF,
                        intArrayOf(
                            Color.argb(frostTopAlpha, 240, 248, 255),
                            Color.argb(frostMidAlpha, 200, 225, 255),
                            Color.argb(frostBotAlpha, 255, 255, 255)
                        ),
                        floatArrayOf(0f, 0.55f, 1f),
                        Shader.TileMode.CLAMP
                    )
                }
                canvas.drawRect(rect, frostPaint)
            }

            // 2b. Soft Inner Glow (luminous edge diffusion from edges inward)
            val innerGlow = (style.glassInnerGlowIntensity.coerceIn(0f, 1f) * globalAlpha)
            if (innerGlow > 0.01f) {
                val glowRadius = max(wF, hF) * 0.72f
                val edgeAlpha = (innerGlow * 115f).toInt().coerceIn(0, 255)
                val innerGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    shader = RadialGradient(
                        wF * 0.5f,
                        hF * 0.5f,
                        glowRadius,
                        intArrayOf(
                            Color.argb(0, 255, 255, 255),
                            Color.argb((edgeAlpha * 0.35f).toInt(), 210, 240, 255),
                            Color.argb(edgeAlpha, 255, 255, 255)
                        ),
                        floatArrayOf(0f, 0.68f, 1f),
                        Shader.TileMode.CLAMP
                    )
                }
                canvas.drawRect(rect, innerGlowPaint)
            }

            // 2c. Top-Left Specular Highlight (curved liquid reflection sheen)
            val specular = (style.glassSpecularIntensity.coerceIn(0f, 1f) * globalAlpha)
            if (specular > 0.01f) {
                val specAlphaTop = (specular * 155f).toInt().coerceIn(0, 255)
                val specAlphaMid = (specular * 45f).toInt().coerceIn(0, 255)
                val specPath = Path().apply {
                    moveTo(0f, 0f)
                    lineTo(wF * 0.92f, 0f)
                    quadTo(wF * 0.42f, hF * 0.36f, 0f, hF * 0.78f)
                    close()
                }
                val specPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    shader = LinearGradient(
                        0f, 0f, wF * 0.55f, hF * 0.65f,
                        intArrayOf(
                            Color.argb(specAlphaTop, 255, 255, 255),
                            Color.argb(specAlphaMid, 235, 250, 255),
                            Color.argb(0, 255, 255, 255)
                        ),
                        floatArrayOf(0f, 0.48f, 1f),
                        Shader.TileMode.CLAMP
                    )
                }
                canvas.drawPath(specPath, specPaint)
            }

            // 2d. Subtle Procedural Noise Grain (frosted surface micro-texture)
            val grain = (style.glassNoiseGrain.coerceIn(0f, 1f) * globalAlpha)
            if (grain > 0.01f) {
                val noisePaint = Paint()
                val step = 3
                val rng = Random(421337L)
                val maxGrainAlpha = (grain * 48f).toInt().coerceIn(1, 90)
                var y = 0
                while (y < height) {
                    var x = 0
                    while (x < width) {
                        val roll = rng.nextInt(100)
                        if (roll < 38) {
                            val isLight = (roll and 1) == 0
                            val a = rng.nextInt(maxGrainAlpha + 1)
                            noisePaint.color = if (isLight) {
                                Color.argb(a, 255, 255, 255)
                            } else {
                                Color.argb(a, 10, 14, 26)
                            }
                            canvas.drawRect(
                                x.toFloat(),
                                y.toFloat(),
                                (x + step).toFloat(),
                                (y + step).toFloat(),
                                noisePaint
                            )
                        }
                        x += step
                    }
                    y += step
                }
            }
        }

        canvas.restore()

        // 3. White-to-Transparent Liquid Glass Border Highlight
        if (style.glassEnabled && borderStrokePx > 0.1f && style.glassBorderOpacity > 0.01f) {
            val borderOp = (style.glassBorderOpacity.coerceIn(0f, 1f) * globalAlpha)
            val rimHigh = (borderOp * 235f).toInt().coerceIn(0, 255)
            val rimMid = (borderOp * 65f).toInt().coerceIn(0, 255)
            val rimLow = (borderOp * 145f).toInt().coerceIn(0, 255)

            val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                this.style = Paint.Style.STROKE
                strokeWidth = borderStrokePx
                shader = LinearGradient(
                    0f, 0f, wF, hF,
                    intArrayOf(
                        Color.argb(rimHigh, 255, 255, 255),
                        Color.argb(rimMid, 180, 225, 255),
                        Color.argb((rimMid * 0.4f).toInt(), 255, 255, 255),
                        Color.argb(rimLow, 225, 240, 255)
                    ),
                    floatArrayOf(0f, 0.38f, 0.72f, 1f),
                    Shader.TileMode.CLAMP
                )
            }
            canvas.drawPath(clipPath, borderPaint)
        }

        return bitmap
    }

    private fun applyOpacity(argb: Int, opacityMultiplier: Float): Int {
        val origAlpha = (argb ushr 24) and 0xFF
        val newAlpha = (origAlpha * opacityMultiplier).toInt().coerceIn(0, 255)
        return (argb and 0x00FFFFFF) or (newAlpha shl 24)
    }
}
