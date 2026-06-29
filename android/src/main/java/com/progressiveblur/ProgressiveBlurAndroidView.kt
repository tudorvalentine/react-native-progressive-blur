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
    // -1 means "span the full view height"; any positive value caps the gradient.
    private var blurLength: Float = -1f

    init {
        setLayerType(LAYER_TYPE_HARDWARE, null)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        super.onLayout(changed, l, t, r, b)
        if (width > 0 && height > 0) applyBlur()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (width > 0 && height > 0) applyBlur()
    }

    fun setBlurRadius(radius: Float) { blurRadius = radius.coerceIn(0f, 150f); applyBlur() }
    fun setBlurType(type: String)    { blurType = type;                          applyBlur() }
    fun setStartIntensity(v: Float)  { startIntensity = v.coerceIn(0f, 1f);      applyBlur() }
    fun setEndIntensity(v: Float)    { endIntensity   = v.coerceIn(0f, 1f);      applyBlur() }
    fun setEasing(name: String)      { easing = name;                            applyBlur() }
    fun setNumStops(stops: Int)      { numStops = stops.coerceAtLeast(2);        applyBlur() }
    fun setBlurLength(length: Float) { blurLength = length;                      applyBlur() }

    private fun applyBlur() {
        if (width <= 0 || height <= 0) return
        if (blurRadius <= 0f) { ProgressiveBlurHelper.clear(this); return }
        ProgressiveBlurHelper.applyWithSize(this, buildConfig(), width.toFloat(), height.toFloat())
    }

    private fun resolvedEndY(): Float =
        if (blurLength > 0f) blurLength else Float.POSITIVE_INFINITY

    private fun buildConfig(): ProgressiveBlurConfig = when (blurType) {
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
        // "top-bottom": heaviest at top → swap intensities so the high value
        // lands at y=0 (startY), then fade toward endY.
        "top-bottom" -> ProgressiveBlurConfig.Vertical(
            blurRadiusPx = blurRadius,
            endY = resolvedEndY(),
            startIntensity = endIntensity,
            endIntensity = startIntensity,
            easing = Easing.fromString(easing),
            numStops = numStops,
        )
        // "bottom-top" / "vertical": heaviest at bottom, no intensity swap needed.
        else -> ProgressiveBlurConfig.Vertical(
            blurRadiusPx = blurRadius,
            endY = resolvedEndY(),
            startIntensity = startIntensity,
            endIntensity = endIntensity,
            easing = Easing.fromString(easing),
            numStops = numStops,
        )
    }
}
