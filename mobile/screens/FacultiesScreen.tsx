import React, { useEffect, useState } from 'react';
import { View, Text, FlatList, Pressable, StyleSheet, ActivityIndicator, Alert } from 'react-native';
import { fetchFacultyList, fetchUserProfile, setActiveFaculty, FacultySummary } from '../lib/api';
import { Card } from '../components/ui';
import { colors, radii, spacing } from '../lib/theme';

export default function FacultiesScreen({ onFacultyChanged }: { onFacultyChanged: () => void }) {
  const [faculties, setFaculties] = useState<FacultySummary[]>([]);
  const [activeFacultyId, setActiveFacultyId] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [switching, setSwitching] = useState<string | null>(null);

  useEffect(() => {
    Promise.all([fetchFacultyList(), fetchUserProfile()])
      .then(([list, profile]) => {
        setFaculties(list);
        setActiveFacultyId(profile.activeFacultyId);
      })
      .catch((e) => Alert.alert('Error', String(e.message ?? e)))
      .finally(() => setLoading(false));
  }, []);

  async function handleSelect(facultyId: string) {
    if (facultyId === activeFacultyId) return;
    setSwitching(facultyId);
    try {
      const profile = await setActiveFaculty(facultyId);
      setActiveFacultyId(profile.activeFacultyId);
      onFacultyChanged();
    } catch (e: any) {
      Alert.alert('Error', String(e.message ?? e));
    } finally {
      setSwitching(null);
    }
  }

  if (loading) return <ActivityIndicator style={styles.center} />;

  return (
    <View style={styles.container}>
      <Card style={styles.introCard}>
        <Text style={styles.introTitle}>Choose the active faculty</Text>
        <Text style={styles.intro}>
          The workbook sequence starts with Amen and ends with Geb, working one to two faculties per month.
          Switching here changes what the Practice tab and Companion are grounded in.
        </Text>
      </Card>
      <FlatList
        data={faculties}
        keyExtractor={(f) => f.id}
        contentContainerStyle={styles.list}
        renderItem={({ item }) => {
          const isActive = item.id === activeFacultyId;
          return (
            <Pressable
              style={[styles.row, isActive && styles.rowActive]}
              onPress={() => handleSelect(item.id)}
              disabled={switching !== null}
            >
              <View style={[styles.orderBadge, isActive && styles.orderBadgeActive]}>
                <Text style={[styles.orderText, isActive && styles.orderTextActive]}>{item.practiceOrder}</Text>
              </View>
              <View style={styles.rowText}>
                <Text style={[styles.rowTitle, isActive && styles.rowTitleActive]}>{item.displayName}</Text>
                <Text style={[styles.rowSubtitle, isActive && styles.rowSubtitleActive]}>{item.role}</Text>
              </View>
              {switching === item.id ? (
                <ActivityIndicator />
              ) : isActive ? (
                <Text style={styles.activeLabel}>Active</Text>
              ) : null}
            </Pressable>
          );
        }}
      />
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: colors.background },
  center: { flex: 1, justifyContent: 'center' },
  introCard: { margin: spacing.lg, marginBottom: spacing.sm },
  introTitle: { color: colors.ink, fontSize: 20, fontWeight: '900', marginBottom: spacing.sm },
  intro: { fontSize: 14, color: colors.muted, lineHeight: 21 },
  list: { padding: spacing.lg, paddingTop: spacing.sm, gap: spacing.md },
  row: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing.md,
    padding: spacing.lg,
    borderRadius: radii.md,
    borderWidth: 1,
    borderColor: colors.border,
    backgroundColor: colors.surface,
  },
  rowActive: { backgroundColor: colors.ink, borderColor: colors.ink },
  orderBadge: {
    width: 42,
    height: 42,
    borderRadius: 21,
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: colors.surfaceMuted,
  },
  orderBadgeActive: { backgroundColor: colors.gold },
  orderText: { color: colors.goldDark, fontSize: 15, fontWeight: '900' },
  orderTextActive: { color: colors.ink },
  rowText: { flex: 1, paddingRight: 12 },
  rowTitle: { color: colors.ink, fontSize: 17, fontWeight: '900' },
  rowTitleActive: { color: colors.white },
  rowSubtitle: { fontSize: 13, color: colors.muted, marginTop: 3, lineHeight: 18 },
  rowSubtitleActive: { color: colors.surfaceMuted },
  activeLabel: {
    overflow: 'hidden',
    borderRadius: radii.sm,
    backgroundColor: colors.gold,
    color: colors.ink,
    fontSize: 12,
    fontWeight: '900',
    paddingHorizontal: spacing.sm,
    paddingVertical: spacing.xs,
  },
});
