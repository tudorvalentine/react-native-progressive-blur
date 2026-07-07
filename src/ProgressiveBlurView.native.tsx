import React from 'react';
import type { StyleProp, ViewStyle } from 'react-native';
import NativeProgressiveBlurView from './ProgressiveBlurViewNativeComponent';
import type { Easing } from './types';

export type ProgressiveBlurViewProps = {
  /** Blur direction/mode. Default: 'vertical' */
  blurType?: 'vertical' | 'top-bottom' | 'bottom-top' | 'horizontal' | 'radial';
  /**
   * Maximum blur radius in pixels (0–150).
   * Set to 0 to clear the effect. Default: 60
   */
  blurRadius?: number;
  /**
   * Intensity at the start edge (0 = sharp).
   * For radial this is the center intensity. Default: 0
   */
  startIntensity?: number;
  /**
   * Intensity at the end edge (1 = fully blurred).
   * For radial this is the edge intensity. Default: 1
   */
  endIntensity?: number;
  /** Gradient acceleration curve. Default: 'easeIn' */
  easing?: Easing;
  /** Number of gradient stops — higher = smoother curve. Default: 20 */
  numStops?: number;
  /**
   * Limits the blur gradient to the first N dp of the view.
   * Content beyond this point is fully clear. Default: -1 (full view height).
   */
  blurLength?: number;
  /** Android: blur this view's own children (wrapper mode) instead of the backdrop. */
  selfBlur?: boolean;
  style?: StyleProp<ViewStyle>;
  children?: React.ReactNode;
};

export function ProgressiveBlurView({
  blurType = 'vertical',
  blurRadius = 60,
  startIntensity = 0,
  endIntensity = 1,
  easing = 'easeIn',
  numStops = 20,
  blurLength = -1,
  selfBlur = false,
  style,
  children,
}: ProgressiveBlurViewProps) {
  return (
    <NativeProgressiveBlurView
      blurType={blurType}
      blurRadius={blurRadius}
      startIntensity={startIntensity}
      endIntensity={endIntensity}
      easing={easing}
      numStops={numStops}
      blurLength={blurLength}
      selfBlur={selfBlur}
      style={style}
    >
      {children}
    </NativeProgressiveBlurView>
  );
}
