import { View } from 'react-native';
import type { ProgressiveBlurViewProps } from './ProgressiveBlurView.native';

export type { ProgressiveBlurViewProps };

// Web fallback — progressive blur requires GPU/RenderEffect (Android/iOS only).
export function ProgressiveBlurView({
  style,
  children,
}: ProgressiveBlurViewProps) {
  return <View style={style}>{children}</View>;
}
