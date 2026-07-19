import React, { useEffect, useState } from 'react';
import { View, Text, ScrollView, StyleSheet, ActivityIndicator, Alert } from 'react-native';
import { fetchFaculty, fetchUserProfile, markPracticeComplete, FacultyContent } from '../lib/api';
import { Card, KemetButton, SectionTitle } from '../components/ui';
import { colors, radii, spacing } from '../lib/theme';

export default function PracticeScreen() {
  const [faculty, setFaculty] = useState<FacultyContent | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    // Always show whatever faculty the user currently has active (set via the
    // Faculties tab) rather than a hardcoded one — this screen used to be
    // Amen-only before all 11 faculties were seeded.
    fetchUserProfile()
      .then((profile) => fetchFaculty(profile.activeFacultyId))
      .then(setFaculty)
      .catch((e) => setError(String(e.message ?? e)))
      .finally(() => setLoading(false));
  }, []);

  async function handleComplete() {
    try {
      const result = await markPracticeComplete();
      Alert.alert('Logged', `Completed days: ${result.completedDays}`);
    } catch (e: any) {
      Alert.alert('Error', String(e.message ?? e));
    }
  }

  if (loading) return <ActivityIndicator style={styles.center} />;
  if (error) return <Text style={styles.error}>{error}</Text>;
  if (!faculty) return null;

  return (
    <ScrollView style={styles.container} contentContainerStyle={styles.content}>
      <Card style={styles.heroCard}>
        <Text style={styles.orderLabel}>Faculty {faculty.practiceOrder}</Text>
        <Text style={styles.title}>{faculty.displayName}</Text>
        <Text style={styles.subtitle}>{faculty.role}</Text>
        <View style={styles.metaRow}>
          <Text style={styles.metaLabel}>Tree position</Text>
          <Text style={styles.metaValue}>{faculty.treePosition}</Text>
        </View>
      </Card>

      <SectionTitle kicker="Read" label="Teaching notes" />
      <Card style={styles.stackCard}>
        {faculty.teachingNotes.map((note, i) => (
          <Text key={i} style={styles.paragraph}>{note}</Text>
        ))}
      </Card>

      <SectionTitle kicker="Begin" label="Breathing technique" />
      <Card>
        <Text style={styles.paragraph}>{faculty.meditationInstructions.breathingTechnique.summary}</Text>
        <View style={styles.breathGrid}>
          <View style={styles.breathCell}>
            <Text style={styles.cellLabel}>Inhale</Text>
            <Text style={styles.cellText}>{faculty.meditationInstructions.breathingTechnique.inbreath}</Text>
          </View>
          <View style={styles.breathCell}>
            <Text style={styles.cellLabel}>Exhale</Text>
            <Text style={styles.cellText}>{faculty.meditationInstructions.breathingTechnique.outbreath}</Text>
          </View>
        </View>
      </Card>

      <SectionTitle kicker="Practice" label="Meditation script" />
      <Card style={styles.stackCard}>
        {faculty.meditationInstructions.scriptSteps.map((step) => (
          <View key={step.step} style={styles.stepRow}>
            <Text style={styles.stepNumber}>{step.step}</Text>
            <View style={styles.stepText}>
              <Text style={styles.stepName}>{step.name}</Text>
              <Text style={styles.paragraph}>{step.instruction}</Text>
            </View>
          </View>
        ))}
      </Card>

      <SectionTitle kicker="Speak" label="Hekau / affirmations" />
      <Card style={styles.stackCard}>
        {faculty.hekau.map((h) => (
          <Text key={h.id} style={styles.hekau}>"{h.text}"</Text>
        ))}
      </Card>

      <SectionTitle kicker="Carry" label="Suggested daily actions" />
      <Card style={styles.stackCard}>
        {faculty.suggestedDailyActions.map((action, i) => (
          <View key={i} style={styles.actionRow}>
            <View style={styles.actionBullet} />
            <Text style={styles.actionText}>{action}</Text>
          </View>
        ))}
      </Card>

      <SectionTitle kicker="Reflect" label="Journal prompts" />
      <Card style={styles.stackCard}>
        {faculty.journalPrompts.map((prompt, i) => (
          <Text key={i} style={styles.prompt}>{prompt}</Text>
        ))}
      </Card>

      <KemetButton title="Mark today's practice complete" onPress={handleComplete} style={styles.completeButton} />
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: colors.background },
  content: { padding: spacing.lg, paddingBottom: spacing.xxl },
  center: { flex: 1, justifyContent: 'center' },
  error: { color: colors.red, padding: spacing.lg },
  heroCard: { backgroundColor: colors.ink },
  orderLabel: { color: colors.gold, fontSize: 12, fontWeight: '900', letterSpacing: 0, textTransform: 'uppercase' },
  title: { color: colors.white, fontSize: 34, fontWeight: '900', marginTop: spacing.sm },
  subtitle: { fontSize: 16, color: colors.surfaceMuted, lineHeight: 22, marginTop: spacing.xs },
  metaRow: {
    marginTop: spacing.lg,
    borderRadius: radii.sm,
    backgroundColor: 'rgba(255, 253, 248, 0.09)',
    padding: spacing.md,
  },
  metaLabel: { color: colors.faint, fontSize: 11, fontWeight: '800', textTransform: 'uppercase' },
  metaValue: { color: colors.white, fontSize: 15, fontWeight: '700', marginTop: spacing.xs },
  stackCard: { gap: spacing.sm },
  paragraph: { fontSize: 15, lineHeight: 23, color: colors.text },
  breathGrid: { flexDirection: 'row', gap: spacing.md, marginTop: spacing.md },
  breathCell: { flex: 1, borderRadius: radii.sm, backgroundColor: colors.blueMuted, padding: spacing.md },
  cellLabel: { color: colors.blue, fontSize: 11, fontWeight: '900', textTransform: 'uppercase' },
  cellText: { color: colors.ink, fontSize: 14, lineHeight: 19, marginTop: spacing.xs, fontWeight: '700' },
  stepRow: { flexDirection: 'row', gap: spacing.md },
  stepNumber: {
    width: 30,
    height: 30,
    borderRadius: 15,
    textAlign: 'center',
    lineHeight: 30,
    overflow: 'hidden',
    backgroundColor: colors.surfaceMuted,
    color: colors.goldDark,
    fontWeight: '900',
  },
  stepText: { flex: 1 },
  stepName: { color: colors.ink, fontSize: 16, fontWeight: '800', marginBottom: spacing.xs },
  hekau: { fontSize: 17, fontStyle: 'italic', lineHeight: 24, color: colors.ink },
  actionRow: { flexDirection: 'row', gap: spacing.md, alignItems: 'flex-start' },
  actionBullet: { width: 8, height: 8, borderRadius: 4, backgroundColor: colors.gold, marginTop: 7 },
  actionText: { flex: 1, color: colors.text, fontSize: 15, lineHeight: 22 },
  prompt: { color: colors.blue, fontSize: 15, lineHeight: 22, fontWeight: '700' },
  completeButton: { marginTop: spacing.xl },
});
