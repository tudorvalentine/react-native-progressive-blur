import type { RefObject } from 'react';
import type { BlurConfig } from './types';

// Web/non-native fallback — progressive blur requires GPU on Android/iOS.
export function applyBlur(
  _ref: RefObject<unknown>,
  _config: BlurConfig
): void {}

export function clearBlur(_ref: RefObject<unknown>): void {}
