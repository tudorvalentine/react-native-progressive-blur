import type { HostComponent, ViewProps } from 'react-native';
// eslint-disable-next-line @react-native/no-deep-imports
import codegenNativeComponent from 'react-native/Libraries/Utilities/codegenNativeComponent';
import type {
  Float,
  Int32,
  WithDefault,
} from 'react-native/Libraries/Types/CodegenTypesNamespace';

export interface NativeProps extends ViewProps {
  blurType?: WithDefault<string, 'vertical'>;
  blurRadius?: Float;
  startIntensity?: Float;
  endIntensity?: Float;
  easing?: WithDefault<string, 'easeIn'>;
  numStops?: Int32;
  /** Limits the blur gradient to the first N dp. -1 = full view height (default). */
  blurLength?: Float;
}

export default codegenNativeComponent<NativeProps>(
  'RNProgressiveBlurView'
) as HostComponent<NativeProps>;
