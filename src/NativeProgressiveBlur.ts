import { TurboModuleRegistry, type TurboModule } from 'react-native';

export interface Spec extends TurboModule {
  // -1 sentinels: startY/endY use view bounds; startX/endX use view bounds; centerX/centerY use view center; radius auto-computes
  applyVerticalBlur(
    viewTag: number,
    blurRadiusPx: number,
    startY: number,
    endY: number,
    startIntensity: number,
    endIntensity: number,
    easing: string,
    numStops: number
  ): void;

  applyHorizontalBlur(
    viewTag: number,
    blurRadiusPx: number,
    startX: number,
    endX: number,
    startIntensity: number,
    endIntensity: number,
    easing: string,
    numStops: number
  ): void;

  applyRadialBlur(
    viewTag: number,
    blurRadiusPx: number,
    centerX: number,
    centerY: number,
    radius: number,
    centerIntensity: number,
    edgeIntensity: number,
    easing: string,
    numStops: number
  ): void;

  clearBlur(viewTag: number): void;
}

export default TurboModuleRegistry.getEnforcing<Spec>('ProgressiveBlur');
