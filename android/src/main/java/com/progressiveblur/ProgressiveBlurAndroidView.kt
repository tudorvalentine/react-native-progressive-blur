package com.progressiveblur

import android.content.Context
import android.graphics.RenderEffect
import android.os.Build
import android.view.View
import android.view.ViewGroup
import com.facebook.react.views.view.ReactViewGroup

/**
 * Backdrop-blur container — GPU-native, zero CPU capture.
 *
 * Instead of capturing a bitmap of what's behind this view (which requires a
 * full software re-render of the view tree on every scroll frame), this view
 * applies a [RenderEffect] directly to each sibling that sits behind it in the
 * parent's z-order.
 *
 * Why this works for scrolling:
 *   RenderEffect is applied in the target view's LOCAL coordinate space.
 *   For a ScrollView, local y=0 is always the top of the *visible* area —
 *   independent of scroll position.  So the blur gradient (y=0 → full blur,
 *   y=height → clear) always covers the region behind the header, and the GPU
 *   re-applies it automatically every frame at zero CPU cost.
 *
 * Requires API 33 (Android 13) for the RuntimeShader / gradient-mask path.
 * On API 31–32 a uniform-radius blur is applied as a degraded fallback.
 * Below API 31 no blur is rendered.
 *
 * Usage (same as before — no JS-side change needed):
 *
 *   <View>
 *     <ScrollView />            ← sibling behind — gets the RenderEffect
 *     <ProgressiveBlurView>
 *       <Header />              ← children drawn on top, always sharp
 *     </ProgressiveBlurView>
 *   </View>
 */
class ProgressiveBlurAndroidView(context: Context) : ReactViewGroup(context) {

    private var blurRadius: Float = 0f
    private var blurType: String = "vertical"
    private var startIntensity: Float = 0f
    private var endIntensity: Float = 1f
    private var easing: String = "easeIn"
    private var numStops: Int = 10
    private var blurLength: Float = -1f

    /** Sibling views that sit behind us — we own their RenderEffect while attached. */
    private val blurTargets = mutableListOf<View>()

    /**
     * Named runnable so we can cancel it in onDetachedFromWindow.
     * Posted (not called directly) from onAttachedToWindow so that Fabric finishes
     * inserting all siblings from the same commit before we scan.
     */
    private val findTargetsRunnable = Runnable { findTargets() }

    /**
     * Watches the parent for sibling add/remove events so we can re-apply the
     * effect when a blur target is recreated (e.g. pull-to-refresh rebuilds the
     * ScrollView native view).
     */
    private val parentHierarchyListener = object : ViewGroup.OnHierarchyChangeListener {
        override fun onChildViewAdded(parent: View, child: View) {
            if (child !== this@ProgressiveBlurAndroidView) {
                // A new sibling appeared — re-discover all targets and re-apply.
                findTargets()
            }
        }
        override fun onChildViewRemoved(parent: View, child: View) {
            if (blurTargets.remove(child)) {
                // Clear effect on the departing view before it is detached.
                if (Build.VERSION.SDK_INT >= 31) child.setRenderEffect(null)
            }
        }
    }

    init {
        setLayerType(LAYER_TYPE_HARDWARE, null)
    }

    // ── Prop setters ─────────────────────────────────────────────────────────

    fun setBlurRadius(r: Float)      { blurRadius = r.coerceIn(0f, 150f);  applyEffect() }
    fun setBlurType(t: String)       { blurType = t;                        applyEffect() }
    fun setStartIntensity(v: Float)  { startIntensity = v.coerceIn(0f, 1f); applyEffect() }
    fun setEndIntensity(v: Float)    { endIntensity   = v.coerceIn(0f, 1f); applyEffect() }
    fun setEasing(n: String)         { easing = n;                           applyEffect() }
    fun setNumStops(s: Int)          { numStops = s.coerceAtLeast(2);        applyEffect() }
    fun setBlurLength(l: Float)      { blurLength = if (l < 0f) l else l * resources.displayMetrics.density; applyEffect() }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        (parent as? ViewGroup)?.setOnHierarchyChangeListener(parentHierarchyListener)
        // Defer to next frame: Fabric may still be inserting other siblings from the
        // same commit batch when this fires, so a direct findTargets() call would see
        // an incomplete child list and leave blurTargets empty on cold start.
        post(findTargetsRunnable)
    }

    override fun onDetachedFromWindow() {
        (parent as? ViewGroup)?.setOnHierarchyChangeListener(null)
        removeCallbacks(findTargetsRunnable)
        clearEffect()
        blurTargets.clear()
        super.onDetachedFromWindow()
    }

    // onSizeChanged: bounds are now known — safe to build and apply the effect.
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w > 0 && h > 0) {
            // If the posted findTargets() hasn't run yet (or found nothing), retry
            // here so the effect is applied as soon as dimensions are available.
            if (blurTargets.isEmpty()) findTargets() else applyEffect()
        }
    }

    // ── Target discovery ─────────────────────────────────────────────────────

    /** Collects sibling views that are behind this view in the parent's z-order. */
    private fun findTargets() {
        blurTargets.clear()
        val parent = this.parent as? ViewGroup ?: return
        val myIndex = parent.indexOfChild(this)
        for (i in 0 until myIndex) {
            blurTargets.add(parent.getChildAt(i))
        }
        applyEffect()
    }

    // ── Effect application ───────────────────────────────────────────────────

    private fun applyEffect() {
        if (Build.VERSION.SDK_INT < 31 || blurTargets.isEmpty() || width <= 0 || height <= 0) return

        // When blurLength extends beyond the view's own height the Gaussian kernel
        // needs the larger crop so it can sample symmetrically past the view edge.
        val effectH = if (blurLength > height) blurLength else height.toFloat()
        val effect: RenderEffect? = if (blurRadius > 0f) {
            ProgressiveBlurHelper.buildEffect(buildConfig(), width.toFloat(), effectH)
        } else {
            null
        }
        blurTargets.forEach { it.setRenderEffect(effect) }
    }

    private fun clearEffect() {
        if (Build.VERSION.SDK_INT < 31) return
        blurTargets.forEach { it.setRenderEffect(null) }
    }

    // ── Config ────────────────────────────────────────────────────────────────

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
        "top-bottom" -> ProgressiveBlurConfig.Vertical(
            blurRadiusPx = blurRadius,
            endY = resolvedEndY(),
            startIntensity = endIntensity,   // swap: heaviest at y=0
            endIntensity = startIntensity,
            easing = Easing.fromString(easing),
            numStops = numStops,
        )
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
