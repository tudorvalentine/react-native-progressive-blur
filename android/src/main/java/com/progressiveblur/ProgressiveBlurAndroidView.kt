package com.progressiveblur

import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RenderNode
import android.graphics.Shader
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
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

    /** Guards against re-entrancy while we draw the backdrop into [blurNode]. */
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

    /**
     * A scrolling backdrop (e.g. the chat's inverted list) may update on the render
     * thread without dirtying our on-screen region, which would freeze the blur
     * mid-scroll. This fires on any scroll in the tree and re-captures. Cheap — only
     * during actual scrolling, not every idle frame.
     */
    private val scrollListener = ViewTreeObserver.OnScrollChangedListener {
        if (supported && blurRadius > 0f) invalidate()
    }

    init {
        // We paint our own content (the backdrop) in dispatchDraw.
        setWillNotDraw(false)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        viewTreeObserver.addOnScrollChangedListener(scrollListener)
    }

    override fun onDetachedFromWindow() {
        viewTreeObserver.removeOnScrollChangedListener(scrollListener)
        super.onDetachedFromWindow()
    }

    /**
     * Self-blur mode: instead of capturing siblings behind us, this view wraps the
     * content and applies the progressive [android.graphics.RenderEffect] to ITSELF
     * (so its children are blurred). RenderEffect on a view is GPU-live — scrolling and
     * transforms (inverted lists) are reflected without capture — the trade-off being
     * the possible Fabric cold-start compositing quirk. Used for chat where the content
     * is an inverted list that the capture path can't handle.
     */
    private var selfBlur = false

    // ── Prop setters ─────────────────────────────────────────────────────────

    fun setBlurRadius(r: Float)      { blurRadius = r.coerceIn(0f, 150f);  refresh() }
    fun setBlurType(t: String)       { blurType = t;                        refresh() }
    fun setStartIntensity(v: Float)  { startIntensity = v.coerceIn(0f, 1f); refresh() }
    fun setEndIntensity(v: Float)    { endIntensity   = v.coerceIn(0f, 1f); refresh() }
    fun setEasing(n: String)         { easing = n;                          refresh() }
    fun setNumStops(s: Int)          { numStops = s.coerceAtLeast(2);       refresh() }
    fun setBlurLength(l: Float)      { blurLength = if (l < 0f) l else l * resources.displayMetrics.density; refresh() }
    fun setSelfBlur(v: Boolean)      { selfBlur = v; if (!v && supported) setRenderEffect(null); refresh() }

    private fun refresh() {
        if (selfBlur) applySelfEffect() else invalidate()
    }

    /** Applies the progressive effect to this view itself (self-blur mode). */
    private fun applySelfEffect() {
        if (!selfBlur || !supported) return
        val w = width
        val h = height
        if (w <= 0 || h <= 0) return
        val effectH = if (blurLength > h) blurLength else h.toFloat()
        val effect = if (blurRadius > 0f)
            ProgressiveBlurHelper.buildEffect(buildConfig(1), w.toFloat(), effectH) else null
        setRenderEffect(effect)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (selfBlur) applySelfEffect()
    }

    // ── Backdrop capture + draw ────────────────────────────────────────────────

    override fun dispatchDraw(canvas: Canvas) {
        // API < 31 has no RenderEffect — fall back to a plain dark→transparent gradient
        // scrim so the header still has a readable backing (drawn behind the header in
        // overlay mode, over the content in self-blur mode).
        if (!supported) {
            if (selfBlur) {
                super.dispatchDraw(canvas)
                drawFallbackScrim(canvas)
            } else {
                drawFallbackScrim(canvas)
                super.dispatchDraw(canvas)
            }
            return
        }
        if (!selfBlur) drawBackdrop(canvas)
        super.dispatchDraw(canvas)
    }

    private val fallbackPaint = Paint()

    /** Dark→transparent gradient shown on API < 31 where GPU blur is unavailable. */
    private fun drawFallbackScrim(canvas: Canvas) {
        if (blurRadius <= 0f) return
        val w = width
        val h = height
        if (w <= 0 || h <= 0) return
        val end = if (blurLength > 0f) blurLength else h.toFloat()
        // Matches the app's legacy fallback: rgba(12,15,18) opaque at top → clear.
        fallbackPaint.shader = LinearGradient(
            0f, 0f, 0f, end,
            0xE60C0F12.toInt(), 0x000C0F12, Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, 0f, w.toFloat(), end, fallbackPaint)
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
                // View.draw(canvas) skips the view's OWN transform (normally applied by
                // the parent's drawChild). Apply it here so e.g. an inverted chat list's
                // scaleY(-1) is honoured and the capture isn't mirrored.
                rc.concat(child.matrix)
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
