package com.progressiveblur

import com.facebook.react.module.annotations.ReactModule
import com.facebook.react.uimanager.ThemedReactContext
import com.facebook.react.uimanager.ViewGroupManager
import com.facebook.react.uimanager.annotations.ReactProp

@ReactModule(name = ProgressiveBlurViewManager.NAME)
class ProgressiveBlurViewManager : ViewGroupManager<ProgressiveBlurAndroidView>() {

    companion object {
        const val NAME = "RNProgressiveBlurView"
    }

    override fun getName() = NAME

    override fun createViewInstance(context: ThemedReactContext) =
        ProgressiveBlurAndroidView(context)

    @ReactProp(name = "blurRadius", defaultFloat = 60f)
    fun setBlurRadius(view: ProgressiveBlurAndroidView, radius: Float) {
        view.setBlurRadius(radius)
    }

    @ReactProp(name = "blurType")
    fun setBlurType(view: ProgressiveBlurAndroidView, type: String?) {
        view.setBlurType(type ?: "vertical")
    }

    @ReactProp(name = "startIntensity", defaultFloat = 0f)
    fun setStartIntensity(view: ProgressiveBlurAndroidView, intensity: Float) {
        view.setStartIntensity(intensity)
    }

    @ReactProp(name = "endIntensity", defaultFloat = 1f)
    fun setEndIntensity(view: ProgressiveBlurAndroidView, intensity: Float) {
        view.setEndIntensity(intensity)
    }

    @ReactProp(name = "easing")
    fun setEasing(view: ProgressiveBlurAndroidView, easing: String?) {
        view.setEasing(easing ?: "easeIn")
    }

    @ReactProp(name = "numStops", defaultInt = 20)
    fun setNumStops(view: ProgressiveBlurAndroidView, stops: Int) {
        view.setNumStops(stops)
    }

    @ReactProp(name = "blurLength", defaultFloat = -1f)
    fun setBlurLength(view: ProgressiveBlurAndroidView, length: Float) {
        view.setBlurLength(length)
    }
}
