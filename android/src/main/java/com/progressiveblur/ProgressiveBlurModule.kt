package com.progressiveblur

import android.view.View
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.uimanager.UIManagerModule

class ProgressiveBlurModule(reactContext: ReactApplicationContext) :
  NativeProgressiveBlurSpec(reactContext) {

  companion object {
    const val NAME = NativeProgressiveBlurSpec.NAME
  }

  override fun applyVerticalBlur(
    viewTag: Double,
    blurRadiusPx: Double,
    startY: Double,
    endY: Double,
    startIntensity: Double,
    endIntensity: Double,
    easing: String,
    numStops: Double,
  ) {
    withView(viewTag.toInt()) { view ->
      ProgressiveBlurHelper.apply(
        view,
        ProgressiveBlurConfig.Vertical(
          blurRadiusPx = blurRadiusPx.toFloat(),
          startY = if (startY < 0) 0f else startY.toFloat(),
          endY = if (endY < 0) Float.POSITIVE_INFINITY else endY.toFloat(),
          startIntensity = startIntensity.toFloat(),
          endIntensity = endIntensity.toFloat(),
          easing = Easing.fromString(easing),
          numStops = numStops.toInt().coerceAtLeast(2),
        )
      )
    }
  }

  override fun applyHorizontalBlur(
    viewTag: Double,
    blurRadiusPx: Double,
    startX: Double,
    endX: Double,
    startIntensity: Double,
    endIntensity: Double,
    easing: String,
    numStops: Double,
  ) {
    withView(viewTag.toInt()) { view ->
      ProgressiveBlurHelper.apply(
        view,
        ProgressiveBlurConfig.Horizontal(
          blurRadiusPx = blurRadiusPx.toFloat(),
          startX = if (startX < 0) 0f else startX.toFloat(),
          endX = if (endX < 0) Float.POSITIVE_INFINITY else endX.toFloat(),
          startIntensity = startIntensity.toFloat(),
          endIntensity = endIntensity.toFloat(),
          easing = Easing.fromString(easing),
          numStops = numStops.toInt().coerceAtLeast(2),
        )
      )
    }
  }

  override fun applyRadialBlur(
    viewTag: Double,
    blurRadiusPx: Double,
    centerX: Double,
    centerY: Double,
    radius: Double,
    centerIntensity: Double,
    edgeIntensity: Double,
    easing: String,
    numStops: Double,
  ) {
    withView(viewTag.toInt()) { view ->
      ProgressiveBlurHelper.apply(
        view,
        ProgressiveBlurConfig.Radial(
          blurRadiusPx = blurRadiusPx.toFloat(),
          centerX = if (centerX < 0) Float.NaN else centerX.toFloat(),
          centerY = if (centerY < 0) Float.NaN else centerY.toFloat(),
          radius = if (radius < 0) Float.POSITIVE_INFINITY else radius.toFloat(),
          centerIntensity = centerIntensity.toFloat(),
          edgeIntensity = edgeIntensity.toFloat(),
          easing = Easing.fromString(easing),
          numStops = numStops.toInt().coerceAtLeast(2),
        )
      )
    }
  }

  override fun clearBlur(viewTag: Double) {
    withView(viewTag.toInt()) { view ->
      ProgressiveBlurHelper.clear(view)
    }
  }

  // Resolves a view by reactTag and runs [block] on the UI thread.
  private fun withView(reactTag: Int, block: (View) -> Unit) {
    val uiManager = reactApplicationContext
      .getNativeModule(UIManagerModule::class.java) ?: return
    uiManager.addUIBlock { manager ->
      try {
        val view = manager.resolveView(reactTag) ?: return@addUIBlock
        block(view)
      } catch (_: Exception) {
        // View not found — skip silently
      }
    }
  }
}
