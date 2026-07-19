import React, { useEffect, useState } from 'react';
import { View, Text, Pressable, StyleSheet, SafeAreaView } from 'react-native';
import { StatusBar } from 'expo-status-bar';
import { hasValidCredentials, logout } from './lib/auth0Client';
import LoginScreen from './screens/LoginScreen';
import PracticeScreen from './screens/PracticeScreen';
import FacultiesScreen from './screens/FacultiesScreen';
import CompanionScreen from './screens/CompanionScreen';
import JournalScreen from './screens/JournalScreen';
import { colors, radii, spacing } from './lib/theme';

// Deliberately no navigation library (react-navigation etc.) for this app — four
// screens and a login gate don't need one, and it's one less native dependency to link
// via CocoaPods. Revisit once the screen count grows past what a simple switcher can
// handle cleanly.

type Tab = 'practice' | 'faculties' | 'companion' | 'journal';

const tabs: { key: Tab; label: string; description: string }[] = [
  { key: 'practice', label: 'Practice', description: 'Daily sequence' },
  { key: 'faculties', label: 'Faculties', description: 'Workbook path' },
  { key: 'companion', label: 'Companion', description: 'Guided Q&A' },
  { key: 'journal', label: 'Journal', description: 'Private notes' },
];

export default function App() {
  const [signedIn, setSignedIn] = useState<boolean | null>(null);
  const [tab, setTab] = useState<Tab>('practice');
  // Bumping this forces PracticeScreen to refetch after switching faculties in the
  // Faculties tab — simplest thing that works without pulling in a state library.
  const [facultyRefreshKey, setFacultyRefreshKey] = useState(0);

  useEffect(() => {
    hasValidCredentials().then(setSignedIn).catch(() => setSignedIn(false));
  }, []);

  async function handleLogout() {
    await logout();
    setSignedIn(false);
  }

  if (signedIn === null) return null;

  if (!signedIn) {
    return (
      <SafeAreaView style={styles.safeArea}>
        <LoginScreen onSignedIn={() => setSignedIn(true)} />
        <StatusBar style="auto" />
      </SafeAreaView>
    );
  }

  return (
    <SafeAreaView style={styles.safeArea}>
      <View style={styles.header}>
        <View>
          <Text style={styles.headerEyebrow}>Kemet practice</Text>
          <Text style={styles.headerTitle}>{tabs.find((item) => item.key === tab)?.description}</Text>
        </View>
        <Pressable style={styles.logoutButton} onPress={handleLogout}>
          <Text style={styles.logoutLabel}>Log out</Text>
        </Pressable>
      </View>
      <View style={styles.screen}>
        {tab === 'practice' && <PracticeScreen key={facultyRefreshKey} />}
        {tab === 'faculties' && (
          <FacultiesScreen
            onFacultyChanged={() => {
              setFacultyRefreshKey((k) => k + 1);
              setTab('practice');
            }}
          />
        )}
        {tab === 'companion' && <CompanionScreen />}
        {tab === 'journal' && <JournalScreen />}
      </View>
      <View style={styles.tabBar}>
        {tabs.map((item) => (
          <TabButton key={item.key} label={item.label} active={tab === item.key} onPress={() => setTab(item.key)} />
        ))}
      </View>
      <StatusBar style="dark" />
    </SafeAreaView>
  );
}

function TabButton({ label, active, onPress }: { label: string; active: boolean; onPress: () => void }) {
  return (
    <Pressable style={[styles.tabButton, active && styles.tabButtonActive]} onPress={onPress}>
      <View style={[styles.tabDot, active && styles.tabDotActive]} />
      <Text style={[styles.tabLabel, active && styles.tabLabelActive]}>{label}</Text>
    </Pressable>
  );
}

const styles = StyleSheet.create({
  safeArea: { flex: 1, backgroundColor: colors.background },
  header: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingHorizontal: spacing.lg,
    paddingTop: spacing.md,
    paddingBottom: spacing.sm,
  },
  headerEyebrow: { color: colors.goldDark, fontSize: 12, fontWeight: '800', letterSpacing: 0, textTransform: 'uppercase' },
  headerTitle: { color: colors.ink, fontSize: 22, fontWeight: '900', marginTop: 2 },
  logoutButton: {
    minHeight: 36,
    justifyContent: 'center',
    borderRadius: radii.sm,
    paddingHorizontal: spacing.md,
    backgroundColor: colors.surface,
    borderWidth: 1,
    borderColor: colors.border,
  },
  logoutLabel: { fontSize: 13, color: colors.muted, fontWeight: '700' },
  screen: { flex: 1 },
  tabBar: {
    flexDirection: 'row',
    marginHorizontal: spacing.lg,
    marginBottom: spacing.md,
    padding: spacing.xs,
    borderRadius: radii.lg,
    backgroundColor: colors.surfaceMuted,
  },
  tabButton: { flex: 1, alignItems: 'center', gap: spacing.xs, paddingVertical: spacing.sm, borderRadius: radii.md },
  tabButtonActive: { backgroundColor: colors.surface },
  tabDot: { width: 5, height: 5, borderRadius: 3, backgroundColor: 'transparent' },
  tabDotActive: { backgroundColor: colors.gold },
  tabLabel: { fontSize: 12, color: colors.muted, fontWeight: '700' },
  tabLabelActive: { color: colors.ink, fontWeight: '900' },
});
