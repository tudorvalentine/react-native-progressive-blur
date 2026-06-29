package com.progressiveblur

import android.content.Context
import android.widget.FrameLayout

class ProgressiveBlurAndroidView(context: Context) : FrameLayout(context) {

    private var blurRadius: Float = 60f
    private var blurType: String = "vertical"
    private var startIntensity: Float = 0f
    private var endIntensity: Float = 1f
    private var easing: String = "easeIn"
    private var numStops: Int = 20

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        super.onLayout(changed, l, t, r, b)
        if (changed && width > 0 && height > 0) {
            applyBlur()
        }
    }

    fun setBlurRadius(radius: Float) {
        blurRadius = radius.coerceIn(0f, 150f)
        applyBlur()
    }

    fun setBlurType(type: String) {
        blurType = type
        applyBlur()
    }

    fun setStartIntensity(intensity: Float) {
        startIntensity = intensity.coerceIn(0f, 1f)
        applyBlur()
    }

    fun setEndIntensity(intensity: Float) {
        endIntensity = intensity.coerceIn(0f, 1f)
        applyBlur()
    }

    fun setEasing(easingName: String) {
        easing = easingName
        applyBlur()
    }

    fun setNumStops(stops: Int) {
        numStops = stops.coerceAtLeast(2)
        applyBlur()
    }

    private fun applyBlur() {
        if (width <= 0 || height <= 0) return

        if (blurRadius <= 0f) {
            ProgressiveBlurHelper.clear(this)
            return
        }

        val config: ProgressiveBlurConfig = when (blurType) {
            "horizontal" -> ProgressiveBlurConfig.Horizontal(
                blurRadiusPx = blurRadius,
                startIntensity = startIntensity,
                endIntensity = endIntensity,
                easing = Easing.fromString(easing),
                numStops = numStops,
            )
            "radial" -> ProgressiveBlurConfig.Radial(
                blurRadiusPx = blurRadius,
                centerIntensity = startIntensity,
                edgeIntensity = endIntensity,
                easing = Easing.fromString(easing),
                numStops = numStops,
            )
            else -> ProgressiveBlurConfig.Vertical(
                blurRadiusPx = blurRadius,
                startIntensity = startIntensity,
                endIntensity = endIntensity,
                easing = Easing.fromString(easing),
                numStops = numStops,
            )
        }

        ProgressiveBlurHelper.apply(this, config)
    }
}
