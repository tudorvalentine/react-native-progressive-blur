import { findNodeHandle } from 'react-native';
import type { RefObject } from 'react';
import NativeProgressiveBlur from './NativeProgressiveBlur';
import type { BlurConfig } from './types';

export function applyBlur(ref: RefObject<unknown>, config: BlurConfig): void {
  const tag = findNodeHandle(
    ref.current as Parameters<typeof findNodeHandle>[0]
  );
  if (tag == null) return;

  switch (config.type) {
    case 'vertical':
      NativeProgressiveBlur.applyVerticalBlur(
        tag,
        config.blurRadiusPx,
        config.startY ?? -1,
        config.endY ?? -1,
        config.startIntensity ?? 0,
        config.endIntensity ?? 1,
        config.easing ?? 'easeIn',
        config.numStops ?? 20
      );
      break;

    case 'horizontal':
      NativeProgressiveBlur.applyHorizontalBlur(
        tag,
        config.blurRadiusPx,
        config.startX ?? -1,
        config.endX ?? -1,
        config.startIntensity ?? 0,
        config.endIntensity ?? 1,
        config.easing ?? 'easeIn',
        config.numStops ?? 20
      );
      break;

    case 'radial':
      NativeProgressiveBlur.applyRadialBlur(
        tag,
        config.blurRadiusPx,
        config.centerX ?? -1,
        config.centerY ?? -1,
        config.radius ?? -1,
        config.centerIntensity ?? 1,
        config.edgeIntensity ?? 0,
        config.easing ?? 'easeIn',
        config.numStops ?? 20
      );
      break;
  }
}

export function clearBlur(ref: RefObject<unknown>): void {
  const tag = findNodeHandle(
    ref.current as Parameters<typeof findNodeHandle>[0]
  );
  if (tag == null) return;
  NativeProgressiveBlur.clearBlur(tag);
}
