import { useCallback, useRef, useState } from 'react';
import {
  Dimensions,
  PanResponder,
  Pressable,
  SafeAreaView,
  ScrollView,
  StatusBar,
  StyleSheet,
  Text,
  View,
} from 'react-native';
import { ProgressiveBlurView } from 'react-native-progressive-blur';

const { width: SCREEN_WIDTH } = Dimensions.get('window');

// ─── Content ─────────────────────────────────────────────────────────────────

const CARDS = [
  { bg: '#FF6B6B', accent: '#C0392B', label: 'Tomato Red' },
  { bg: '#FF9F43', accent: '#E17055', label: 'Sunset Orange' },
  { bg: '#FFEAA7', accent: '#FDCB6E', label: 'Lemon Yellow' },
  { bg: '#A29BFE', accent: '#6C5CE7', label: 'Lavender' },
  { bg: '#74B9FF', accent: '#0984E3', label: 'Sky Blue' },
  { bg: '#55EFC4', accent: '#00B894', label: 'Mint Green' },
  { bg: '#FD79A8', accent: '#E84393', label: 'Hot Pink' },
  { bg: '#B2BEC3', accent: '#636E72', label: 'Steel Gray' },
  { bg: '#81ECEC', accent: '#00CEC9', label: 'Aqua' },
  { bg: '#FDCB6E', accent: '#E17055', label: 'Amber' },
  { bg: '#6C5CE7', accent: '#4A3FCB', label: 'Deep Purple' },
  { bg: '#00B894', accent: '#00695C', label: 'Emerald' },
];

// ─── Slider ──────────────────────────────────────────────────────────────────

const TRACK_WIDTH = SCREEN_WIDTH - 120;

function toPos(v: number) {
  return (v / 150) * TRACK_WIDTH;
}
function toVal(pos: number) {
  return Math.round((pos / TRACK_WIDTH) * 150);
}

function RadiusSlider({
  value,
  onValueChange,
}: {
  value: number;
  onValueChange: (v: number) => void;
}) {
  const startPos = useRef(toPos(value));
  const [thumbLeft, setThumbLeft] = useState(toPos(value));

  const pan = useRef(
    PanResponder.create({
      onStartShouldSetPanResponder: () => true,
      onPanResponderGrant: (evt) => {
        const x = Math.max(0, Math.min(TRACK_WIDTH, evt.nativeEvent.locationX));
        startPos.current = x;
        setThumbLeft(x);
        onValueChange(toVal(x));
      },
      onPanResponderMove: (_, { dx }) => {
        const x = Math.max(0, Math.min(TRACK_WIDTH, startPos.current + dx));
        setThumbLeft(x);
        onValueChange(toVal(x));
      },
      onPanResponderRelease: (_, { dx }) => {
        startPos.current = Math.max(
          0,
          Math.min(TRACK_WIDTH, startPos.current + dx)
        );
      },
    })
  ).current;

  return (
    <View style={s.sliderHitArea} {...pan.panHandlers}>
      <View style={s.track}>
        <View style={[s.fill, { width: thumbLeft }]} />
      </View>
      <View style={[s.thumb, { left: thumbLeft - 12 }]} />
    </View>
  );
}

// ─── App ─────────────────────────────────────────────────────────────────────

type BlurMode = 'vertical' | 'horizontal' | 'radial';

const MODES: { label: string; mode: BlurMode }[] = [
  { label: 'Vertical', mode: 'vertical' },
  { label: 'Horizontal', mode: 'horizontal' },
  { label: 'Radial', mode: 'radial' },
];

export default function App() {
  const [activeMode, setActiveMode] = useState<BlurMode | null>(null);
  const [radius, setRadius] = useState(60);

  const handleClear = useCallback(() => setActiveMode(null), []);

  const handleRadius = useCallback((r: number) => setRadius(r), []);

  // blurRadius=0 tells the native view to clear the RenderEffect
  const blurRadius = activeMode ? radius : 0;

  return (
    <SafeAreaView style={s.safe}>
      <StatusBar barStyle="dark-content" backgroundColor="#fff" />

      {/*
       * ProgressiveBlurView wraps the scrollable content.
       * The native FrameLayout applies setRenderEffect to itself,
       * which blurs everything rendered inside it.
       */}
      <ProgressiveBlurView
        blurType={activeMode ?? 'vertical'}
        blurRadius={blurRadius}
        startIntensity={0}
        endIntensity={1}
        easing="easeIn"
        style={s.content}
      >
        <ScrollView
          showsVerticalScrollIndicator={false}
          contentContainerStyle={s.scrollContent}
        >
          {CARDS.map((card, i) => (
            <View key={i} style={[s.card, { backgroundColor: card.bg }]}>
              <View style={[s.circle, { backgroundColor: card.accent }]} />
              <View style={s.cardInfo}>
                <Text style={s.cardTitle}>{card.label}</Text>
                <Text style={s.cardHex}>{card.bg}</Text>
              </View>
              <View style={[s.badge, { backgroundColor: card.accent }]}>
                <Text style={s.badgeText}>{i + 1}</Text>
              </View>
            </View>
          ))}
        </ScrollView>
      </ProgressiveBlurView>

      {/* Controls */}
      <View style={s.panel}>
        <View style={s.panelHeader}>
          <Text style={s.panelTitle}>Progressive Blur</Text>
          {activeMode ? (
            <Text style={s.modePill}>{activeMode}</Text>
          ) : (
            <Text style={s.modeNone}>none</Text>
          )}
        </View>

        <View style={s.buttonRow}>
          {MODES.map(({ label, mode }) => (
            <Pressable
              key={mode}
              style={[s.btn, activeMode === mode && s.btnOn]}
              onPress={() => setActiveMode(mode)}
            >
              <Text style={[s.btnLabel, activeMode === mode && s.btnLabelOn]}>
                {label}
              </Text>
            </Pressable>
          ))}
          <Pressable style={s.btnClear} onPress={handleClear}>
            <Text style={s.btnClearLabel}>Clear</Text>
          </Pressable>
        </View>

        <View style={s.sliderRow}>
          <Text style={s.sliderCaption}>Radius</Text>
          <RadiusSlider value={radius} onValueChange={handleRadius} />
          <Text style={s.sliderValue}>{radius}px</Text>
        </View>
      </View>
    </SafeAreaView>
  );
}

// ─── Styles ──────────────────────────────────────────────────────────────────

const s = StyleSheet.create({
  safe: { flex: 1, backgroundColor: '#fff' },

  content: { flex: 1 },
  scrollContent: { paddingHorizontal: 16, paddingTop: 12, paddingBottom: 8 },

  card: {
    borderRadius: 18,
    marginBottom: 10,
    height: 110,
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: 18,
    overflow: 'hidden',
  },
  circle: { width: 60, height: 60, borderRadius: 30, opacity: 0.85 },
  cardInfo: { flex: 1, paddingLeft: 14 },
  cardTitle: {
    fontSize: 18,
    fontWeight: '700',
    color: '#fff',
    textShadowColor: 'rgba(0,0,0,0.2)',
    textShadowRadius: 6,
    textShadowOffset: { width: 0, height: 1 },
  },
  cardHex: {
    fontSize: 12,
    color: 'rgba(255,255,255,0.7)',
    marginTop: 3,
    fontFamily: 'monospace',
    letterSpacing: 1,
  },
  badge: {
    width: 40,
    height: 40,
    borderRadius: 20,
    alignItems: 'center',
    justifyContent: 'center',
  },
  badgeText: { color: '#fff', fontWeight: '800', fontSize: 15 },

  panel: {
    backgroundColor: '#fff',
    borderTopWidth: StyleSheet.hairlineWidth,
    borderTopColor: '#dee2e6',
    paddingHorizontal: 20,
    paddingTop: 14,
    paddingBottom: 20,
    gap: 14,
  },
  panelHeader: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
  },
  panelTitle: { fontSize: 15, fontWeight: '700', color: '#212529' },
  modePill: {
    fontSize: 12,
    fontWeight: '600',
    color: '#fff',
    backgroundColor: '#212529',
    paddingHorizontal: 10,
    paddingVertical: 3,
    borderRadius: 10,
    overflow: 'hidden',
    textTransform: 'capitalize',
  },
  modeNone: { fontSize: 12, color: '#adb5bd' },

  buttonRow: { flexDirection: 'row', gap: 8 },
  btn: {
    flex: 1,
    height: 38,
    borderRadius: 19,
    borderWidth: 1.5,
    borderColor: '#ced4da',
    backgroundColor: '#f8f9fa',
    alignItems: 'center',
    justifyContent: 'center',
  },
  btnOn: { backgroundColor: '#212529', borderColor: '#212529' },
  btnLabel: { fontSize: 13, fontWeight: '600', color: '#495057' },
  btnLabelOn: { color: '#fff' },
  btnClear: {
    flex: 1,
    height: 38,
    borderRadius: 19,
    borderWidth: 1.5,
    borderColor: '#fa5252',
    backgroundColor: '#fff5f5',
    alignItems: 'center',
    justifyContent: 'center',
  },
  btnClearLabel: { fontSize: 13, fontWeight: '600', color: '#fa5252' },

  sliderRow: { flexDirection: 'row', alignItems: 'center', gap: 10 },
  sliderCaption: {
    fontSize: 12,
    fontWeight: '600',
    color: '#868e96',
    width: 44,
  },
  sliderHitArea: { flex: 1, height: 40, justifyContent: 'center' },
  track: {
    height: 4,
    backgroundColor: '#e9ecef',
    borderRadius: 2,
    overflow: 'hidden',
  },
  fill: { height: 4, backgroundColor: '#212529', borderRadius: 2 },
  thumb: {
    position: 'absolute',
    width: 24,
    height: 24,
    borderRadius: 12,
    backgroundColor: '#212529',
    top: 8,
    elevation: 3,
    shadowColor: '#000',
    shadowOpacity: 0.2,
    shadowRadius: 4,
    shadowOffset: { width: 0, height: 2 },
  },
  sliderValue: {
    fontSize: 12,
    fontWeight: '600',
    color: '#495057',
    width: 44,
    textAlign: 'right',
  },
});
