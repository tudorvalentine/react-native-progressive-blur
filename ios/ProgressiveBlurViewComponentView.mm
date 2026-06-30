#import "ProgressiveBlurViewComponentView.h"

#import <React/RCTConversions.h>
#import <React/RCTFabricComponentsPlugins.h>

#import <react/renderer/components/ProgressiveBlurSpec/ComponentDescriptors.h>
#import <react/renderer/components/ProgressiveBlurSpec/EventEmitters.h>
#import <react/renderer/components/ProgressiveBlurSpec/Props.h>
#import <react/renderer/components/ProgressiveBlurSpec/RCTComponentViewHelpers.h>

using namespace facebook::react;

// ─── Easing helpers ──────────────────────────────────────────────────────────

static float easeIn(float t)    { return t * t * t; }
static float easeOut(float t)   { float u = 1.f - t; return 1.f - u * u * u; }
static float easeInOut(float t) {
    return t < 0.5f ? 4.f * t * t * t : 1.f - powf(-2.f * t + 2.f, 3.f) / 2.f;
}
static float applyEasing(float t, NSString *name) {
    if ([name isEqualToString:@"easeOut"])   return easeOut(t);
    if ([name isEqualToString:@"easeInOut"]) return easeInOut(t);
    if ([name isEqualToString:@"linear"])    return t;
    return easeIn(t); // default
}

// ─── Component view ───────────────────────────────────────────────────────────

@interface ProgressiveBlurViewComponentView () <RCTProgressiveBlurViewViewProtocol>
@end

@implementation ProgressiveBlurViewComponentView {
    UIVisualEffectView *_blurView;
    UIViewPropertyAnimator *_animator; // controls blur intensity 0-1
    CAGradientLayer *_gradientMask;

    // Current props (cached to reapply when bounds change)
    float _blurRadius;
    NSString *_blurType;
    float _startIntensity;
    float _endIntensity;
    NSString *_easing;
    int _numStops;
    float _blurLength;
}

// ── Init ─────────────────────────────────────────────────────────────────────

- (instancetype)initWithFrame:(CGRect)frame
{
    if (self = [super initWithFrame:frame]) {
        _blurRadius    = 60.f;
        _blurType      = @"vertical";
        _startIntensity = 0.f;
        _endIntensity   = 1.f;
        _easing        = @"easeIn";
        _numStops      = 20;
        _blurLength    = -1.f;

        [self _setupBlurView];
    }
    return self;
}

- (void)_setupBlurView
{
    UIBlurEffect *effect = [UIBlurEffect effectWithStyle:UIBlurEffectStyleSystemUltraThinMaterial];

    _blurView = [[UIVisualEffectView alloc] initWithEffect:nil];
    _blurView.frame = self.bounds;
    _blurView.autoresizingMask = UIViewAutoresizingFlexibleWidth | UIViewAutoresizingFlexibleHeight;
    // Insert behind React children so children appear sharp on top.
    [self insertSubview:_blurView atIndex:0];

    // UIViewPropertyAnimator trick: set fractionComplete to control intensity.
    _animator = [[UIViewPropertyAnimator alloc] initWithDuration:1
                                                           curve:UIViewAnimationCurveLinear
                                                      animations:^{
        self->_blurView.effect = effect;
    }];
    _animator.pausesOnCompletion = YES;
    [_animator startAnimation];
    [_animator pauseAnimation];

    _gradientMask = [CAGradientLayer layer];
    _gradientMask.frame = _blurView.bounds;
    _blurView.layer.mask = _gradientMask;
}

// ── Layout ───────────────────────────────────────────────────────────────────

- (void)layoutSubviews
{
    [super layoutSubviews];
    _blurView.frame = self.bounds;
    _gradientMask.frame = _blurView.bounds;
    [self _applyBlur];
}

// ── Codegen / Fabric ─────────────────────────────────────────────────────────

+ (ComponentDescriptorProvider)componentDescriptorProvider
{
    return concreteComponentDescriptorProvider<RNProgressiveBlurViewComponentDescriptor>();
}

- (void)updateProps:(const Props::Shared &)props
           oldProps:(const Props::Shared &)oldProps
{
    const auto &newProps = *std::static_pointer_cast<const RNProgressiveBlurViewProps>(props);

    _blurRadius     = newProps.blurRadius;
    _blurType       = RCTNSStringFromString(newProps.blurType);
    _startIntensity = newProps.startIntensity;
    _endIntensity   = newProps.endIntensity;
    _easing         = RCTNSStringFromString(newProps.easing);
    _numStops       = newProps.numStops;
    _blurLength     = newProps.blurLength;

    [self _applyBlur];
    [super updateProps:props oldProps:oldProps];
}

// ── Blur application ─────────────────────────────────────────────────────────

- (void)_applyBlur
{
    if (CGRectIsEmpty(self.bounds)) return;

    // Blur intensity: map blurRadius [0, 150] → animator fractionComplete [0, 1].
    float intensity = MIN(_blurRadius / 150.f, 1.f);
    _animator.fractionComplete = intensity;

    // Build gradient mask for the progressive direction.
    [self _updateGradientMask];
}

/**
 * Builds a CAGradientLayer that controls how much of the blur is visible
 * at each point in the view.  The gradient goes from black (blur shows)
 * to clear (blur hidden), matching the JS startIntensity / endIntensity.
 */
- (void)_updateGradientMask
{
    CGFloat h = CGRectGetHeight(self.bounds);
    if (h <= 0) return;

    // Effective range for the gradient (honour blurLength)
    CGFloat effectiveLength = (_blurLength > 0) ? MIN(_blurLength, h) : h;
    CGFloat endFraction = effectiveLength / h;

    int stops = MAX(_numStops, 2);
    NSMutableArray<id> *colors    = [NSMutableArray arrayWithCapacity:stops];
    NSMutableArray<NSNumber *> *positions = [NSMutableArray arrayWithCapacity:stops];

    float si = _startIntensity;
    float ei = _endIntensity;
    NSString *easingName = _easing;

    // For "top-bottom" the internal convention has heavy blur at the top,
    // matching the Android Kotlin swap: intensities are inverted.
    BOOL isTopBottom = [_blurType isEqualToString:@"top-bottom"];
    if (isTopBottom) { float tmp = si; si = ei; ei = tmp; }

    for (int i = 0; i < stops; i++) {
        float t = (float)i / (float)(stops - 1);
        float mapped = applyEasing(t, easingName);
        float intensity = si + (ei - si) * mapped;
        intensity = MAX(0.f, MIN(1.f, intensity));

        // Map i to position within [0, endFraction]
        float pos = (endFraction == 1.f) ? t : t * endFraction;
        [colors addObject:(id)[[UIColor colorWithWhite:0 alpha:intensity] CGColor]];
        [positions addObject:@(pos)];
    }

    // Clamp: beyond endFraction the blur is fully clear.
    if (endFraction < 1.f) {
        [colors addObject:(id)[[UIColor clearColor] CGColor]];
        [positions addObject:@(1.f)];
    }

    BOOL isHorizontal = [_blurType isEqualToString:@"horizontal"];
    if (isHorizontal) {
        _gradientMask.startPoint = CGPointMake(0, 0.5);
        _gradientMask.endPoint   = CGPointMake(1, 0.5);
    } else {
        _gradientMask.startPoint = CGPointMake(0.5, 0);
        _gradientMask.endPoint   = CGPointMake(0.5, 1);
    }

    [CATransaction begin];
    [CATransaction setDisableActions:YES];
    _gradientMask.colors    = colors;
    _gradientMask.locations = positions;
    _gradientMask.frame     = _blurView.bounds;
    [CATransaction commit];
}

// ── React child mounting ──────────────────────────────────────────────────────
//
// IMPORTANT: do NOT re-parent React children into _blurView.contentView.
// Doing so detaches them from the coordinate space Fabric lays them out in
// and collapses flex layout (children shrink-wrap at the origin).
//
// UIBlurEffect already blurs the BACKDROP — i.e. whatever is rendered behind
// this view (the ScrollView).  So children only need to sit *in front of*
// _blurView to appear sharp on top of the frosted backdrop.  We therefore
// mount them directly into `self`, exactly like RCTViewComponentView does,
// but offset by 1 because _blurView permanently occupies subview index 0.

- (void)mountChildComponentView:(UIView<RCTComponentViewProtocol> *)childComponentView
                          index:(NSInteger)index
{
    // +1 keeps _blurView at the back; React children stack at 1...N on top.
    [self insertSubview:childComponentView atIndex:index + 1];
}

- (void)unmountChildComponentView:(UIView<RCTComponentViewProtocol> *)childComponentView
                            index:(NSInteger)index
{
    [childComponentView removeFromSuperview];
}

@end

// Required for the Fabric component registry.
Class<RCTComponentViewProtocol> RNProgressiveBlurViewCls(void)
{
    return ProgressiveBlurViewComponentView.class;
}
