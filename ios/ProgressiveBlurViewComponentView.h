#pragma once

#import <React/RCTViewComponentView.h>
#import <UIKit/UIKit.h>

/**
 * Fabric ComponentView for ProgressiveBlurView.
 *
 * Renders a UIVisualEffectView (UIBlurEffect) with a CAGradientLayer mask
 * applied to its blur layer so the blur fades progressively.  Children are
 * added to the contentView of the visual-effect view so they appear sharp,
 * on top of the frosted backdrop — matching the @react-native-community/blur
 * pattern.
 */
@interface ProgressiveBlurViewComponentView : RCTViewComponentView

@end
