export type Easing = 'linear' | 'easeIn' | 'easeOut' | 'easeInOut';

export type VerticalBlurConfig = {
  type: 'vertical';
  blurRadiusPx: number;
  /** Y where blur starts. Default: top of view. */
  startY?: number;
  /** Y where blur reaches maximum. Default: bottom of view. */
  endY?: number;
  /** Intensity at startY (0 = clear). Default: 0. */
  startIntensity?: number;
  /** Intensity at endY (1 = full blur). Default: 1. */
  endIntensity?: number;
  easing?: Easing;
  numStops?: number;
};

export type HorizontalBlurConfig = {
  type: 'horizontal';
  blurRadiusPx: number;
  /** X where blur starts. Default: left of view. */
  startX?: number;
  /** X where blur reaches maximum. Default: right of view. */
  endX?: number;
  startIntensity?: number;
  endIntensity?: number;
  easing?: Easing;
  numStops?: number;
};

export type RadialBlurConfig = {
  type: 'radial';
  blurRadiusPx: number;
  /** X of blur center. Default: view center. */
  centerX?: number;
  /** Y of blur center. Default: view center. */
  centerY?: number;
  /** Gradient radius in px. Default: half of view diagonal. */
  radius?: number;
  /** Intensity at center. Default: 1. */
  centerIntensity?: number;
  /** Intensity at edge. Default: 0. */
  edgeIntensity?: number;
  easing?: Easing;
  numStops?: number;
};

export type BlurConfig =
  VerticalBlurConfig | HorizontalBlurConfig | RadialBlurConfig;
