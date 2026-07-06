package com.progressiveblur

import android.content.Context
import android.graphics.Canvas
import android.graphics.RenderNode
import android.os.Build
import android.view.View
import android.view.ViewGroup
import com.facebook.react.views.view.ReactViewGroup

/**
 * Progressive backdrop-blur container — live per-frame recapture.
 *
 * The earlier approach applied a [android.graphics.RenderEffect] statically to the
 * sibling views behind this one. On the New Architecture (Fabric) that effect does
 * NOT composite over freshly-mounted content — it only "takes" after a real window
 * detach/reattach (i.e. navigating away and back), and every Fabric re-commit (data
 * load, pull-to-refresh) drops it again. No redraw / invalidate / layer toggle fixes
 * it (all verified on device).
 *
 * So instead we own the effect ourselves, like a classic backdrop blur but on the GPU:
 * every frame we re-record the sibling views that sit behind us into our own
 * [RenderNode], apply the progressive gradient-masked blur shader to that node, and
 * draw it as our background before our (sharp) header children. Because it re-records
 * each frame, cold start, scrolling and pull-to-refresh are all reflected automatically
 * — there is no frozen effect to go stale.
 *
 * Layout stays the same (blur view is a sibling drawn on top of the content):
 *
 *   <View>
 *     <ScrollView />            ← sibling behind — captured & blurred each frame
 *     <ProgressiveBlurView>
 *       <Header />              ← children drawn on top, always sharp
 *     </ProgressiveBlurView>
 *   </View>
 *
 * Requires API 31+ (RenderEffect / RuntimeShader). Below that no blur is drawn.
 */
class ProgressiveBlurAndroidView(context: Context) : ReactViewGroup(context) {

    private var blurRadius: Float = 0f
    private var blurType: String = "vertical"
    private var startIntensity: Float = 0f
    private var endIntensity: Float = 1f
    private var easing: String = "easeIn"
    private var numStops: Int = 10
    private var blurLength: Float = -1f

    private val supported = Build.VERSION.SDK_INT >= 31
    private val blurNode: RenderNode? =
        if (Build.VERSION.SDK_INT >= 29) RenderNode("progressiveBlur") else null

    /** Guards against re-entrancy while we draw the sibling views into [blurNode]. */
    private var capturing = false

    // No self-invalidation: when the content behind us changes (scroll, refresh, data
    // load) the system marks our on-screen region dirty and redraws us, and dispatchDraw
    // re-captures the fresh backdrop then. A permanent per-frame self-invalidate (an
    // earlier approach) pinned the UI thread at ~19ms/frame and made hard scrolling
    // judder, for no benefit — natural invalidation already covers every change.

    /**
     * Capture + blur at 1/N resolution. The result is upscaled when drawn; since it is
     * blurred anyway the quality loss is invisible, but the two-pass Gaussian costs
     * ~N² fewer pixels — the difference between smooth scrolling and jank.
     *
     * Kept at 2 (not higher): a larger factor snaps the captured backdrop to an N-px
     * grid, so while scrolling the blurred content steps in N-px jumps and visibly
     * judders against the sharp content. 2 halves that to sub-pixel-invisible while
     * still cutting the blur cost ~4×.
     */
    private val downsample = 2

    init {
        // We paint our own content (the backdrop) in dispatchDraw.
        setWillNotDraw(false)
    }

    // ── Prop setters ─────────────────────────────────────────────────────────

    fun setBlurRadius(r: Float)      { blurRadius = r.coerceIn(0f, 150f);  invalidate() }
    fun setBlurType(t: String)       { blurType = t;                        invalidate() }
    fun setStartIntensity(v: Float)  { startIntensity = v.coerceIn(0f, 1f); invalidate() }
    fun setEndIntensity(v: Float)    { endIntensity   = v.coerceIn(0f, 1f); invalidate() }
    fun setEasing(n: String)         { easing = n;                          invalidate() }
    fun setNumStops(s: Int)          { numStops = s.coerceAtLeast(2);       invalidate() }
    fun setBlurLength(l: Float)      { blurLength = if (l < 0f) l else l * resources.displayMetrics.density; invalidate() }

    // ── Backdrop capture + draw ────────────────────────────────────────────────

    override fun dispatchDraw(canvas: Canvas) {
        drawBackdrop(canvas)
        super.dispatchDraw(canvas)
    }

    private fun drawBackdrop(canvas: Canvas) {
        val node = blurNode ?: return
        if (!supported || !canvas.isHardwareAccelerated) return
        val w = width
        val h = height
        if (w <= 0 || h <= 0 || blurRadius <= 0f) return
        val parent = this.parent as? ViewGroup ?: return
        val myIndex = parent.indexOfChild(this)
        if (myIndex <= 0) return // nothing behind us to blur

        val f = downsample
        val nw = (w / f).coerceAtLeast(1)
        val nh = (h / f).coerceAtLeast(1)

        // Re-record the siblings behind us into our node at 1/f resolution, mapped so
        // the parent region directly behind this view lands at the node's (0,0) origin.
        node.setPosition(0, 0, nw, nh)
        val rc = node.beginRecording(nw, nh)
        capturing = true
        try {
            rc.scale(1f / f, 1f / f)
            // Only the strip behind the header is needed; clipping lets the recorder
            // skip the (tall) off-strip content of the ScrollView entirely.
            rc.clipRect(0f, 0f, w.toFloat(), h.toFloat())
            for (i in 0 until myIndex) {
                val child = parent.getChildAt(i)
                if (child.visibility != View.VISIBLE) continue
                rc.save()
                rc.translate((child.left - left).toFloat(), (child.top - top).toFloat())
                child.draw(rc)
                rc.restore()
            }
        } finally {
            capturing = false
            node.endRecording()
        }

        node.setRenderEffect(
            ProgressiveBlurHelper.buildEffect(buildConfig(f), nw.toFloat(), nh.toFloat())
        )

        canvas.save()
        canvas.scale(f.toFloat(), f.toFloat())
        canvas.drawRenderNode(node)
        canvas.restore()
    }

    // ── Config ────────────────────────────────────────────────────────────────

    private fun resolvedEndY(scale: Int): Float =
        if (blurLength > 0f) blurLength / scale else Float.POSITIVE_INFINITY

    /** Spatial params (radius, gradient extent) are divided by [scale] to match the
     *  down-sampled node; intensities/stops are resolution-independent. */
    private fun buildConfig(scale: Int): ProgressiveBlurConfig {
        val r = blurRadius / scale
        return when (blurType) {
            "horizontal" -> ProgressiveBlurConfig.Horizontal(
                blurRadiusPx = r,
                startIntensity = startIntensity,
                endIntensity = endIntensity,
                easing = Easing.fromString(easing),
                numStops = numStops,
            )
            "radial" -> ProgressiveBlurConfig.Radial(
                blurRadiusPx = r,
                centerIntensity = startIntensity,
                edgeIntensity = endIntensity,
                easing = Easing.fromString(easing),
                numStops = numStops,
            )
            "top-bottom" -> ProgressiveBlurConfig.Vertical(
                blurRadiusPx = r,
                endY = resolvedEndY(scale),
                startIntensity = endIntensity,   // swap: heaviest at y=0
                endIntensity = startIntensity,
                easing = Easing.fromString(easing),
                numStops = numStops,
            )
            else -> ProgressiveBlurConfig.Vertical(
                blurRadiusPx = r,
                endY = resolvedEndY(scale),
                startIntensity = startIntensity,
                endIntensity = endIntensity,
                easing = Easing.fromString(easing),
                numStops = numStops,
            )
        }
    }
}
