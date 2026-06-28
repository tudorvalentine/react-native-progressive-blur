package com.progressiveblur

import com.facebook.react.bridge.ReactApplicationContext

class ProgressiveBlurModule(reactContext: ReactApplicationContext) :
  NativeProgressiveBlurSpec(reactContext) {

  override fun multiply(a: Double, b: Double): Double {
    return a * b
  }

  companion object {
    const val NAME = NativeProgressiveBlurSpec.NAME
  }
}
