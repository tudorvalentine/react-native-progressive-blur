import { useRef, useState } from 'react';
import {
  Dimensions,
  PanResponder,
  ScrollView,
  StatusBar,
  StyleSheet,
  Text,
  View,
} from 'react-native';
import { ProgressiveBlurView } from 'react-native-progressive-blur';

const { width: SCREEN_WIDTH } = Dimensions.get('window');

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

// ─── Header ──────────────────────────────────────────────────────────────────
// The background is blurred top→bottom with startIntensity=1 (fully blurred at
// the top) fading to endIntensity=0 (sharp at the bottom).  The title text sits
// outside the ProgressiveBlurView so it is always crisp.

const HEADER_HEIGHT = 90;

// The header is a transparent overlay — it carries no background of its own.
// The ProgressiveBlurView wrapping the ScrollView blurs the scroll content
// in the top HEADER_HEIGHT*1.5 dp zone, creating the frosted-glass effect.
function Header() {
  return (
    <View style={s.header} pointerEvents="none">
      <Text style={s.headerTitle}>Progressive Blur</Text>
      <Text style={s.headerSub}>React Native · Android demo</Text>
    </View>
  );
}

// ─── Cards ───────────────────────────────────────────────────────────────────
// Each card wraps its content in renderToHardwareTextureAndroid so that nested
// Fabric children are rasterised into a GPU texture before the parent's
// hardware layer composites them through the RenderEffect.

function SocialCard() {
  return (
    <View style={c.socialCard} renderToHardwareTextureAndroid>
      <View style={c.row}>
        <View style={c.socialAvatar} />
        <View>
          <Text style={c.name}>Sarah K.</Text>
          <Text style={c.meta}>2 min ago</Text>
        </View>
      </View>
      <Text style={c.body}>
        Just finished my morning run! Feeling amazing today 🏃‍♀️✨
      </Text>
      <View style={c.socialImage} />
      <View style={c.row}>
        <Text style={c.reactionLike}>❤️ 24</Text>
        <Text style={c.reactionComment}>💬 6</Text>
      </View>
    </View>
  );
}

function MusicCard() {
  return (
    <View style={c.musicCard} renderToHardwareTextureAndroid>
      <Text style={c.musicEyebrow}>Now Playing</Text>
      <View style={c.musicAlbum}>
        <Text style={c.musicNote}>♫</Text>
      </View>
      <Text style={c.trackTitle}>Midnight City</Text>
      <Text style={c.trackArtist}>M83</Text>
      <View style={c.progressTrack}>
        <View style={c.musicProgressFill} />
      </View>
      <View style={c.rowBetween}>
        <Text style={c.meta}>2:14</Text>
        <Text style={c.meta}>3:48</Text>
      </View>
    </View>
  );
}

function WeatherCard() {
  return (
    <View style={c.weatherCard} renderToHardwareTextureAndroid>
      <Text style={c.weatherEyebrow}>San Francisco</Text>
      <View style={c.weatherCenter}>
        <Text style={c.weatherEmoji}>⛅</Text>
        <Text style={c.temp}>24°</Text>
        <Text style={c.conditions}>Partly Cloudy</Text>
      </View>
      <View style={c.rowAround}>
        <View style={c.statBlock}>
          <Text style={c.statVal}>62%</Text>
          <Text style={c.statLbl}>Humidity</Text>
        </View>
        <View style={c.weatherDivider} />
        <View style={c.statBlock}>
          <Text style={c.statVal}>12</Text>
          <Text style={c.statLbl}>km/h</Text>
        </View>
      </View>
    </View>
  );
}

function ActivityCard() {
  return (
    <View style={c.activityCard} renderToHardwareTextureAndroid>
      <Text style={c.activityEyebrow}>Activity</Text>
      <View style={c.ringWrap}>
        <View style={c.activityRing}>
          <Text style={c.activityPct}>82%</Text>
        </View>
        <Text style={c.activityGoal}>Goal reached</Text>
      </View>
      <View style={c.rowAround}>
        <View style={c.statBlock}>
          <Text style={c.activityStatVal}>8.2k</Text>
          <Text style={c.statLbl}>Steps</Text>
        </View>
        <View style={c.activityDivider} />
        <View style={c.statBlock}>
          <Text style={c.activityStatVal}>420</Text>
          <Text style={c.statLbl}>kcal</Text>
        </View>
      </View>
    </View>
  );
}

// ─── Grid config ─────────────────────────────────────────────────────────────

type BlurType = 'top-bottom' | 'bottom-top' | 'horizontal' | 'radial';

const ITEMS: { blurType: BlurType; label: string; content: React.ReactNode }[] =
  [
    {
      blurType: 'top-bottom',
      label: '↓ Top → Bottom',
      content: <SocialCard />,
    },
    { blurType: 'bottom-top', label: '↑ Bottom → Top', content: <MusicCard /> },
    { blurType: 'horizontal', label: '→ Horizontal', content: <WeatherCard /> },
    { blurType: 'radial', label: '◎ Radial', content: <ActivityCard /> },
  ];

// ─── App ──────────────────────────────────────────────────────────────────────

export default function App() {
  const [radius, setRadius] = useState(20);

  return (
    <View style={s.safe}>
      <StatusBar
        barStyle="dark-content"
        backgroundColor="transparent"
        translucent
      />

      {/* Blur wrapper — blurs only the top HEADER_HEIGHT*1.5 dp so scroll
          content entering the header zone fades into frosted glass */}
      <ProgressiveBlurView
        blurType="top-bottom"
        blurRadius={radius}
        startIntensity={0}
        endIntensity={1}
        easing="easeOut"
        blurLength={HEADER_HEIGHT * 1.5}
        style={s.blurWrapper}
      >
        <ScrollView
          style={s.scroll}
          contentContainerStyle={s.scrollContent}
          showsVerticalScrollIndicator={false}
        >
          <View style={s.grid}>
            {[0, 1].map((row) => (
              <View key={row} style={s.gridRow}>
                {[0, 1].map((col) => {
                  const item = ITEMS[row * 2 + col]!;
                  return (
                    <View key={item.blurType} style={s.cellWrapper}>
                      <ProgressiveBlurView
                        blurType={item.blurType}
                        blurRadius={radius}
                        startIntensity={0}
                        endIntensity={1}
                        easing="easeIn"
                        style={s.cell}
                      >
                        {item.content}
                      </ProgressiveBlurView>
                      <Text style={s.cellLabel}>{item.label}</Text>
                    </View>
                  );
                })}
              </View>
            ))}
            {[0, 1].map((row) => (
              <View key={row} style={s.gridRow}>
                {[0, 1].map((col) => {
                  const item = ITEMS[row * 2 + col]!;
                  return (
                    <View key={item.blurType} style={s.cellWrapper}>
                      <ProgressiveBlurView
                        blurType={item.blurType}
                        blurRadius={radius}
                        startIntensity={0}
                        endIntensity={1}
                        easing="easeIn"
                        style={s.cell}
                      >
                        {item.content}
                      </ProgressiveBlurView>
                      <Text style={s.cellLabel}>{item.label}</Text>
                    </View>
                  );
                })}
              </View>
            ))}
          </View>
        </ScrollView>
      </ProgressiveBlurView>

      <Header />

      <View style={s.panel}>
        <Text style={s.panelTitle}>Blur radius</Text>
        <View style={s.sliderRow}>
          <Text style={s.sliderCaption}>Radius</Text>
          <RadiusSlider value={radius} onValueChange={setRadius} />
          <Text style={s.sliderValue}>{radius}px</Text>
        </View>
      </View>
    </View>
  );
}

// ─── Card styles ─────────────────────────────────────────────────────────────

const c = StyleSheet.create({
  // ── SocialCard ──
  socialCard: { flex: 1, padding: 12, gap: 8, backgroundColor: '#fff5f5' },
  socialAvatar: {
    width: 30,
    height: 30,
    borderRadius: 15,
    backgroundColor: '#FF6B6B',
  },
  socialImage: {
    flex: 1,
    borderRadius: 8,
    minHeight: 40,
    backgroundColor: '#FFAAAA',
  },
  reactionLike: { fontSize: 11, fontWeight: '600', color: '#FF6B6B' },
  reactionComment: { fontSize: 11, fontWeight: '600', color: '#adb5bd' },

  // ── MusicCard ──
  musicCard: { flex: 1, padding: 12, gap: 8, backgroundColor: '#f0f7ff' },
  musicEyebrow: {
    fontSize: 11,
    fontWeight: '700',
    letterSpacing: 0.3,
    color: '#0984E3',
  },
  musicAlbum: {
    alignSelf: 'center',
    width: 52,
    height: 52,
    borderRadius: 10,
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: '#74B9FF',
  },
  musicProgressFill: {
    width: '60%',
    height: 3,
    borderRadius: 2,
    backgroundColor: '#74B9FF',
  },
  rowBetween: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
    justifyContent: 'space-between',
  },

  // ── WeatherCard ──
  weatherCard: { flex: 1, padding: 12, gap: 8, backgroundColor: '#f0fff8' },
  weatherEyebrow: {
    fontSize: 11,
    fontWeight: '700',
    letterSpacing: 0.3,
    color: '#00B894',
  },
  weatherDivider: {
    width: 1,
    height: 28,
    borderRadius: 1,
    backgroundColor: '#b2f0dc',
  },
  rowAround: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
    justifyContent: 'space-around',
  },

  // ── ActivityCard ──
  activityCard: { flex: 1, padding: 12, gap: 8, backgroundColor: '#f8f5ff' },
  activityEyebrow: {
    fontSize: 11,
    fontWeight: '700',
    letterSpacing: 0.3,
    color: '#6C5CE7',
  },
  activityRing: {
    width: 64,
    height: 64,
    borderRadius: 32,
    borderWidth: 6,
    alignItems: 'center',
    justifyContent: 'center',
    borderColor: '#A29BFE',
  },
  activityPct: { fontSize: 16, fontWeight: '800', color: '#6C5CE7' },
  activityGoal: { fontSize: 11, color: '#6C5CE7', textAlign: 'center' },
  activityStatVal: { fontSize: 13, fontWeight: '700', color: '#6C5CE7' },
  activityDivider: {
    width: 1,
    height: 28,
    borderRadius: 1,
    backgroundColor: '#c5b8fd',
  },

  // ── Shared ──
  row: { flexDirection: 'row', alignItems: 'center', gap: 8 },
  name: { fontSize: 12, fontWeight: '700', color: '#2d3436' },
  meta: { fontSize: 10, color: '#adb5bd' },
  body: { fontSize: 11, color: '#555', lineHeight: 16 },
  musicNote: { fontSize: 22, color: '#fff' },
  trackTitle: {
    fontSize: 13,
    fontWeight: '700',
    color: '#2d3436',
    textAlign: 'center',
  },
  trackArtist: { fontSize: 11, color: '#636e72', textAlign: 'center' },
  progressTrack: {
    height: 3,
    backgroundColor: '#dfe6e9',
    borderRadius: 2,
    overflow: 'hidden',
  },
  weatherCenter: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
    gap: 2,
  },
  weatherEmoji: { fontSize: 28 },
  temp: { fontSize: 32, fontWeight: '800', color: '#2d3436' },
  conditions: { fontSize: 11, color: '#636e72', textAlign: 'center' },
  ringWrap: { flex: 1, alignItems: 'center', justifyContent: 'center', gap: 4 },
  statBlock: { alignItems: 'center', gap: 2 },
  statVal: { fontSize: 13, fontWeight: '700', color: '#2d3436' },
  statLbl: {
    fontSize: 9,
    color: '#adb5bd',
    textTransform: 'uppercase',
    letterSpacing: 0.3,
  },
});

// ─── Layout styles ────────────────────────────────────────────────────────────

const s = StyleSheet.create({
  // StatusBar.currentHeight is a synchronous static value on Android —
  // unlike SafeAreaView's async inset measurement it is correct on frame 0,
  // which prevents the header from overlapping the status bar on cold start.
  safe: {
    flex: 1,
    backgroundColor: '#fff',
    paddingTop: StatusBar.currentHeight ?? 0,
  },

  // Header — transparent overlay, title only; blur is on the content behind it
  header: {
    position: 'absolute',
    top: 0,
    left: 0,
    right: 0,
    height: HEADER_HEIGHT,
    zIndex: 10,
    justifyContent: 'flex-end',
    paddingHorizontal: 18,
    paddingBottom: 14,
  },
  headerTitle: { fontSize: 20, fontWeight: '800', color: '#1a1a2e' },
  headerSub: { fontSize: 12, color: 'rgba(0,0,0,0.5)', marginTop: 2 },

  // ProgressiveBlurView wraps the ScrollView so the top blurLength dp are
  // blurred — scroll content passing into that zone gets the frosted-glass look.
  blurWrapper: { flex: 1 },
  scroll: { flex: 1 },
  scrollContent: { paddingTop: HEADER_HEIGHT },
  grid: { padding: 12, gap: 12, paddingBottom: 24 },
  gridRow: { flexDirection: 'row', gap: 12 },
  cellWrapper: { flex: 1 },
  cell: { height: 210, borderRadius: 16, overflow: 'hidden' },
  cellLabel: {
    fontSize: 10,
    fontWeight: '600',
    color: '#868e96',
    textAlign: 'center',
    marginTop: 5,
  },

  // Panel
  panel: {
    backgroundColor: '#fff',
    borderTopWidth: StyleSheet.hairlineWidth,
    borderTopColor: '#dee2e6',
    paddingHorizontal: 20,
    paddingTop: 14,
    paddingBottom: 20,
    gap: 10,
  },
  panelTitle: { fontSize: 13, fontWeight: '700', color: '#212529' },

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
