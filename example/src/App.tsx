import { useState } from 'react';
import {
  type NativeScrollEvent,
  type NativeSyntheticEvent,
  ScrollView,
  StatusBar,
  StyleSheet,
  Text,
  TouchableOpacity,
  View,
} from 'react-native';
import {
  SafeAreaProvider,
  useSafeAreaInsets,
} from 'react-native-safe-area-context';
import { ProgressiveBlurView } from 'react-native-progressive-blur';

const HEADER_HEIGHT = 64;
const FADE_ZONE = 24;
const MAX_BLUR_RADIUS = 22;
const SCROLL_THRESHOLD = 80;

type ActivityCard = {
  id: number;
  emoji: string;
  title: string;
  subtitle: string;
  value: string;
  unit: string;
  accent: string;
};

const ACTIVITIES: ActivityCard[] = [
  {
    id: 1,
    emoji: '🏃',
    title: 'Morning Run',
    subtitle: 'Today · 7:12 AM',
    value: '5.4',
    unit: 'km',
    accent: '#FF6B6B',
  },
  {
    id: 2,
    emoji: '❤️',
    title: 'Heart Rate',
    subtitle: 'Avg over last hour',
    value: '72',
    unit: 'bpm',
    accent: '#FF8C69',
  },
  {
    id: 3,
    emoji: '🔥',
    title: 'Calories',
    subtitle: 'Burned today',
    value: '1,840',
    unit: 'kcal',
    accent: '#FFA940',
  },
  {
    id: 4,
    emoji: '💧',
    title: 'Hydration',
    subtitle: 'Goal: 2.5 L',
    value: '1.8',
    unit: 'L',
    accent: '#4ECDC4',
  },
  {
    id: 5,
    emoji: '😴',
    title: 'Sleep',
    subtitle: 'Last night',
    value: '7h 23m',
    unit: '',
    accent: '#A78BFA',
  },
  {
    id: 6,
    emoji: '🧘',
    title: 'Mindfulness',
    subtitle: 'Streak: 5 days',
    value: '20',
    unit: 'min',
    accent: '#34D399',
  },
  {
    id: 7,
    emoji: '🚴',
    title: 'Cycling',
    subtitle: 'Yesterday',
    value: '18.2',
    unit: 'km',
    accent: '#60A5FA',
  },
  {
    id: 8,
    emoji: '⚡',
    title: 'Energy',
    subtitle: 'Recovery score',
    value: '87',
    unit: '%',
    accent: '#F59E0B',
  },
  {
    id: 9,
    emoji: '🎯',
    title: 'Weekly Goal',
    subtitle: 'Steps progress',
    value: '78%',
    unit: '',
    accent: '#EC4899',
  },
  {
    id: 10,
    emoji: '🏋️',
    title: 'Strength',
    subtitle: 'Last session',
    value: '45',
    unit: 'min',
    accent: '#8B5CF6',
  },
];

function Screen() {
  const insets = useSafeAreaInsets();
  const headerH = HEADER_HEIGHT + insets.top;
  const overlayH = headerH + FADE_ZONE;
  const [blurRadius, setBlurRadius] = useState(0);

  const handleScroll = (e: NativeSyntheticEvent<NativeScrollEvent>) => {
    const y = e.nativeEvent.contentOffset.y;
    setBlurRadius(
      Math.max(
        0,
        Math.min(MAX_BLUR_RADIUS, (y / SCROLL_THRESHOLD) * MAX_BLUR_RADIUS)
      )
    );
  };

  return (
    <View style={styles.root}>
      <StatusBar
        translucent
        barStyle="light-content"
        backgroundColor="transparent"
      />

      {/* ── Scrollable content ──────────────────────────────────────────── */}
      <ScrollView
        onScroll={handleScroll}
        scrollEventThrottle={16}
        showsVerticalScrollIndicator={false}
        contentContainerStyle={[
          styles.contentContainer,
          { paddingTop: overlayH + 12, paddingBottom: insets.bottom + 32 },
        ]}
      >
        {/* Hero banner */}
        <View style={styles.heroBanner}>
          <View style={styles.heroTop}>
            <View>
              <Text style={styles.heroLabel}>TODAY'S STEPS</Text>
              <Text style={styles.heroValue}>12,340</Text>
              <Text style={styles.heroGoal}>Goal: 15,000 · 82% complete</Text>
            </View>
            <View style={styles.heroRing}>
              <Text style={styles.heroRingPct}>82%</Text>
            </View>
          </View>
          <View style={styles.heroStats}>
            {[
              { label: 'Distance', val: '8.7 km' },
              { label: 'Active', val: '1h 12m' },
              { label: 'Floors', val: '14' },
            ].map((s) => (
              <View key={s.label} style={styles.heroStat}>
                <Text style={styles.heroStatVal}>{s.val}</Text>
                <Text style={styles.heroStatLbl}>{s.label}</Text>
              </View>
            ))}
          </View>
        </View>

        <Text style={styles.sectionTitle}>Activity</Text>

        {ACTIVITIES.map((item) => (
          <TouchableOpacity
            key={item.id}
            activeOpacity={0.72}
            style={styles.cardWrap}
          >
            <View style={[styles.card, { borderLeftColor: item.accent }]}>
              <View
                style={[
                  styles.cardIcon,
                  { backgroundColor: `${item.accent}22` },
                ]}
              >
                <Text style={styles.cardEmoji}>{item.emoji}</Text>
              </View>
              <View style={styles.cardBody}>
                <Text style={styles.cardTitle}>{item.title}</Text>
                <Text style={styles.cardSub}>{item.subtitle}</Text>
              </View>
              <View style={styles.cardRight}>
                <Text style={[styles.cardVal, { color: item.accent }]}>
                  {item.value}
                </Text>
                {item.unit ? (
                  <Text style={styles.cardUnit}>{item.unit}</Text>
                ) : null}
              </View>
            </View>
          </TouchableOpacity>
        ))}
      </ScrollView>

      {/*
       * ── Backdrop blur overlay ─────────────────────────────────────────────
       *
       * ProgressiveBlurView now works like @react-native-community/blur:
       *
       *   • Android: captures a bitmap of everything behind this view in
       *     z-order (the ScrollView cards), then blurs it progressively
       *     via a GPU RenderNode.  Children inside remain fully sharp.
       *
       *   • iOS: UIVisualEffectView with a CAGradientLayer mask gives the
       *     system frosted-glass blur; intensity is driven by blurRadius.
       *
       * blurType="bottom-top": strongest blur at the top (header zone),
       *                        fades clear over the FADE_ZONE below.
       */}
      <ProgressiveBlurView
        blurType="top-bottom"
        blurRadius={blurRadius}
        startIntensity={0}
        endIntensity={1}
        easing="easeInOut"
        style={[styles.blurOverlay, { height: overlayH }]}
      >
        {/* Header UI — children of the blur view, drawn ON TOP, always sharp */}
        <View
          style={[
            {
              height: headerH,
              paddingTop: insets.top,
            },
          ]}
        >
          <View style={styles.headerRow}>
            {/* ← Back button */}
            <TouchableOpacity style={styles.navBtn} activeOpacity={0.7}>
              <View style={styles.navCircle}>
                <Text style={styles.navChevron}>‹</Text>
              </View>
            </TouchableOpacity>

            {/* Center island pill */}
            <View style={styles.island}>
              <Text style={styles.islandName} numberOfLines={1}>
                Dashboard
              </Text>
              <Text style={styles.islandGreeting} numberOfLines={1}>
                Good morning
              </Text>
            </View>

            {/* Avatar */}
            <TouchableOpacity style={styles.avatarBtn} activeOpacity={0.8}>
              <View style={styles.avatar}>
                <Text style={styles.avatarLetters}>TD</Text>
              </View>
              <View style={styles.onlineDot} />
            </TouchableOpacity>
          </View>
        </View>
        {/* FADE_ZONE — empty; the blur gradient fades to clear here */}
      </ProgressiveBlurView>
    </View>
  );
}

export default function App() {
  return (
    <SafeAreaProvider>
      <Screen />
    </SafeAreaProvider>
  );
}

// ─── Styles ──────────────────────────────────────────────────────────────────

const styles = StyleSheet.create({
  root: {
    flex: 1,
    backgroundColor: '#0C0C14',
  },

  contentContainer: {
    paddingHorizontal: 16,
  },

  blurOverlay: {
    position: 'absolute',
    top: 0,
    left: 0,
    right: 0,
  },

  headerRow: {
    flex: 1,
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: 14,
  },

  navBtn: {
    width: 40,
    height: 40,
    alignItems: 'center',
    justifyContent: 'center',
  },

  navCircle: {
    width: 36,
    height: 36,
    borderRadius: 18,
    backgroundColor: 'rgba(255,255,255,0.16)',
    borderWidth: StyleSheet.hairlineWidth,
    borderColor: 'rgba(255,255,255,0.28)',
    alignItems: 'center',
    justifyContent: 'center',
  },

  navChevron: {
    fontSize: 26,
    color: '#FFFFFF',
    lineHeight: 30,
    marginTop: -1,
  },

  island: {
    flex: 1,
    marginHorizontal: 8,
    alignItems: 'center',
    backgroundColor: 'rgba(255,255,255,0.12)',
    borderWidth: StyleSheet.hairlineWidth,
    borderColor: 'rgba(255,255,255,0.22)',
    borderRadius: 24,
    paddingVertical: 7,
    paddingHorizontal: 18,
  },

  islandName: {
    color: '#FFFFFF',
    fontSize: 15,
    fontWeight: '600',
    letterSpacing: 0.1,
  },

  islandGreeting: {
    color: 'rgba(255,255,255,0.60)',
    fontSize: 11,
    marginTop: 1,
  },

  avatarBtn: {
    width: 40,
    height: 40,
    alignItems: 'center',
    justifyContent: 'center',
  },

  avatar: {
    width: 36,
    height: 36,
    borderRadius: 18,
    backgroundColor: '#6C63FF',
    alignItems: 'center',
    justifyContent: 'center',
    borderWidth: 2,
    borderColor: 'rgba(255,255,255,0.22)',
  },

  avatarLetters: {
    color: '#FFFFFF',
    fontSize: 13,
    fontWeight: '700',
    letterSpacing: 0.5,
  },

  onlineDot: {
    position: 'absolute',
    bottom: 2,
    right: 2,
    width: 9,
    height: 9,
    borderRadius: 5,
    backgroundColor: '#34D399',
    borderWidth: 1.5,
    borderColor: '#0C0C14',
  },

  heroBanner: {
    backgroundColor: '#13131E',
    borderRadius: 20,
    padding: 20,
    marginBottom: 24,
    borderWidth: StyleSheet.hairlineWidth,
    borderColor: '#1E1E30',
  },

  heroTop: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 20,
  },

  heroLabel: {
    color: '#5555AA',
    fontSize: 11,
    fontWeight: '700',
    letterSpacing: 1.3,
    marginBottom: 4,
  },

  heroValue: {
    color: '#FFFFFF',
    fontSize: 38,
    fontWeight: '800',
    letterSpacing: -0.5,
  },

  heroGoal: {
    color: '#5555AA',
    fontSize: 13,
    marginTop: 3,
  },

  heroRing: {
    width: 74,
    height: 74,
    borderRadius: 37,
    backgroundColor: 'rgba(108,99,255,0.12)',
    borderWidth: 3,
    borderColor: '#6C63FF',
    alignItems: 'center',
    justifyContent: 'center',
  },

  heroRingPct: {
    color: '#8B84FF',
    fontSize: 18,
    fontWeight: '700',
  },

  heroStats: {
    flexDirection: 'row',
    gap: 8,
  },

  heroStat: {
    flex: 1,
    alignItems: 'center',
    paddingVertical: 12,
    backgroundColor: '#0F0F1C',
    borderRadius: 14,
  },

  heroStatVal: {
    color: '#EEEEFF',
    fontSize: 15,
    fontWeight: '700',
  },

  heroStatLbl: {
    color: '#5555AA',
    fontSize: 11,
    marginTop: 3,
  },

  sectionTitle: {
    color: '#EEEEFF',
    fontSize: 20,
    fontWeight: '700',
    letterSpacing: -0.3,
    marginBottom: 12,
  },

  cardWrap: {
    marginBottom: 10,
  },

  card: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: '#13131E',
    borderRadius: 16,
    padding: 14,
    borderWidth: StyleSheet.hairlineWidth,
    borderColor: '#1E1E30',
    borderLeftWidth: 3,
  },

  cardIcon: {
    width: 44,
    height: 44,
    borderRadius: 12,
    alignItems: 'center',
    justifyContent: 'center',
    marginRight: 12,
  },

  cardEmoji: {
    fontSize: 22,
  },

  cardBody: {
    flex: 1,
  },

  cardTitle: {
    color: '#EEEEFF',
    fontSize: 15,
    fontWeight: '600',
  },

  cardSub: {
    color: '#55558A',
    fontSize: 12,
    marginTop: 2,
  },

  cardRight: {
    alignItems: 'flex-end',
    marginLeft: 8,
  },

  cardVal: {
    fontSize: 18,
    fontWeight: '700',
    letterSpacing: -0.3,
  },

  cardUnit: {
    color: '#55558A',
    fontSize: 11,
    marginTop: 1,
  },
});
