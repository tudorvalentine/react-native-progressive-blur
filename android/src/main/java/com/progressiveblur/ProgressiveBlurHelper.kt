package com.progressiveblur

import android.graphics.LinearGradient
import android.graphics.RadialGradient
import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.graphics.Shader
import android.os.Build
import android.view.View
import androidx.annotation.RequiresApi
import kotlin.math.hypot

// ─── Easing ──────────────────────────────────────────────────────────────────

fun interface Easing {
    fun transform(t: Float): Float

    companion object {
        val Linear = Easing { t -> t }
        val EaseIn = Easing { t -> t * t * t }
        val EaseOut = Easing { t -> 1f - (1f - t) * (1f - t) * (1f - t) }
        val EaseInOut = Easing { t ->
            if (t < 0.5f) 4f * t * t * t
            else 1f - (-2f * t + 2f).let { it * it * it } / 2f
        }

        fun fromString(name: String): Easing = when (name) {
            "linear" -> Linear
            "easeOut" -> EaseOut
            "easeInOut" -> EaseInOut
            else -> EaseIn
        }
    }
}

// ─── Config ──────────────────────────────────────────────────────────────────

sealed class ProgressiveBlurConfig {

    data class Vertical(
        val blurRadiusPx: Float,
        val startY: Float = 0f,
        val endY: Float = Float.POSITIVE_INFINITY,
        val startIntensity: Float = 0f,
        val endIntensity: Float = 1f,
        val easing: Easing = Easing.EaseIn,
        val numStops: Int = 20,
    ) : ProgressiveBlurConfig()

    data class Horizontal(
        val blurRadiusPx: Float,
        val startX: Float = 0f,
        val endX: Float = Float.POSITIVE_INFINITY,
        val startIntensity: Float = 0f,
        val endIntensity: Float = 1f,
        val easing: Easing = Easing.EaseIn,
        val numStops: Int = 20,
    ) : ProgressiveBlurConfig()

    data class Radial(
        val blurRadiusPx: Float,
        val centerX: Float = Float.NaN,
        val centerY: Float = Float.NaN,
        val radius: Float = Float.POSITIVE_INFINITY,
        val centerIntensity: Float = 1f,
        val edgeIntensity: Float = 0f,
        val easing: Easing = Easing.EaseIn,
        val numStops: Int = 20,
    ) : ProgressiveBlurConfig()
}

// ─── Helper ───────────────────────────────────────────────────────────────────

object ProgressiveBlurHelper {

    fun apply(view: View, config: ProgressiveBlurConfig) {
        if (Build.VERSION.SDK_INT >= 33) {
            applyWithRuntimeShader(view, config)
        } else if (Build.VERSION.SDK_INT >= 31) {
            applyFallbackBlur(view, config)
        }
    }

    fun clear(view: View) {
        if (Build.VERSION.SDK_INT >= 31) {
            view.setRenderEffect(null)
        }
    }

    // ─── API 33+: two-pass separated Gaussian via RuntimeShader ──────────────

    @RequiresApi(33)
    private fun applyWithRuntimeShader(view: View, config: ProgressiveBlurConfig) {
        val w = view.width.toFloat()
        val h = view.height.toFloat()
        if (w <= 0f || h <= 0f) return

        val (blurRadiusPx, maskShader) = resolveParams(view, config, w, h)
        view.setRenderEffect(buildProgressiveRenderEffect(blurRadiusPx, w, h, maskShader))
    }

    @RequiresApi(33)
    private fun buildProgressiveRenderEffect(
        blurRadiusPx: Float,
        width: Float,
        height: Float,
        maskShader: Shader,
    ): RenderEffect {
        fun makePass(sksl: String): RenderEffect {
            val shader = RuntimeShader(sksl)
            shader.setFloatUniform("blurRadius", blurRadiusPx)
            shader.setFloatUniform("crop", 0f, 0f, width, height)
            shader.setInputShader("mask", maskShader)
            return RenderEffect.createRuntimeShaderEffect(shader, "content")
        }
        val horizontal = makePass(ProgressiveBlurShaders.HORIZONTAL_BLUR_SKSL)
        val vertical = makePass(ProgressiveBlurShaders.VERTICAL_BLUR_SKSL)
        return RenderEffect.createChainEffect(vertical, horizontal)
    }

    // ─── Mask creation ────────────────────────────────────────────────────────

    private fun resolveParams(
        view: View,
        config: ProgressiveBlurConfig,
        w: Float,
        h: Float,
    ): Pair<Float, Shader> = when (config) {

        is ProgressiveBlurConfig.Vertical -> {
            val endY = if (config.endY.isInfinite()) h else config.endY
            val mask = buildLinearGradientShader(
                x0 = 0f, y0 = config.startY,
                x1 = 0f, y1 = endY,
                startIntensity = config.startIntensity,
                endIntensity = config.endIntensity,
                easing = config.easing,
                numStops = config.numStops,
            )
            config.blurRadiusPx to mask
        }

        is ProgressiveBlurConfig.Horizontal -> {
            val endX = if (config.endX.isInfinite()) w else config.endX
            val mask = buildLinearGradientShader(
                x0 = config.startX, y0 = 0f,
                x1 = endX, y1 = 0f,
                startIntensity = config.startIntensity,
                endIntensity = config.endIntensity,
                easing = config.easing,
                numStops = config.numStops,
            )
            config.blurRadiusPx to mask
        }

        is ProgressiveBlurConfig.Radial -> {
            val cx = if (config.centerX.isNaN()) w / 2f else config.centerX
            val cy = if (config.centerY.isNaN()) h / 2f else config.centerY
            val r = if (config.radius.isInfinite()) hypot(w, h) / 2f else config.radius
            val mask = buildRadialGradientShader(
                cx = cx, cy = cy, radius = r,
                centerIntensity = config.centerIntensity,
                edgeIntensity = config.edgeIntensity,
                easing = config.easing,
                numStops = config.numStops,
            )
            config.blurRadiusPx to mask
        }
    }

    private fun buildLinearGradientShader(
        x0: Float, y0: Float,
        x1: Float, y1: Float,
        startIntensity: Float,
        endIntensity: Float,
        easing: Easing,
        numStops: Int,
    ): LinearGradient {
        val colors = IntArray(numStops)
        val positions = FloatArray(numStops)
        for (i in 0 until numStops) {
            val t = i.toFloat() / (numStops - 1)
            val intensity = lerp(startIntensity, endIntensity, easing.transform(t))
            val alpha = (intensity.coerceIn(0f, 1f) * 255f).toInt()
            colors[i] = android.graphics.Color.argb(alpha, 0, 0, 0)
            positions[i] = t
        }
        return LinearGradient(x0, y0, x1, y1, colors, positions, Shader.TileMode.CLAMP)
    }

    private fun buildRadialGradientShader(
        cx: Float, cy: Float, radius: Float,
        centerIntensity: Float,
        edgeIntensity: Float,
        easing: Easing,
        numStops: Int,
    ): RadialGradient {
        val colors = IntArray(numStops)
        val positions = FloatArray(numStops)
        for (i in 0 until numStops) {
            val t = i.toFloat() / (numStops - 1)
            val intensity = lerp(centerIntensity, edgeIntensity, easing.transform(t))
            val alpha = (intensity.coerceIn(0f, 1f) * 255f).toInt()
            colors[i] = android.graphics.Color.argb(alpha, 0, 0, 0)
            positions[i] = t
        }
        return RadialGradient(cx, cy, radius, colors, positions, Shader.TileMode.CLAMP)
    }

    // ─── API 31-32 fallback ───────────────────────────────────────────────────

    @RequiresApi(31)
    private fun applyFallbackBlur(view: View, config: ProgressiveBlurConfig) {
        val blurRadius = when (config) {
            is ProgressiveBlurConfig.Vertical -> config.blurRadiusPx
            is ProgressiveBlurConfig.Horizontal -> config.blurRadiusPx
            is ProgressiveBlurConfig.Radial -> config.blurRadiusPx
        }
        view.setRenderEffect(
            RenderEffect.createBlurEffect(blurRadius, blurRadius, Shader.TileMode.CLAMP)
        )
    }

    // ─── Utility ──────────────────────────────────────────────────────────────

    private fun lerp(start: Float, stop: Float, fraction: Float): Float =
        start + fraction * (stop - start)
}
