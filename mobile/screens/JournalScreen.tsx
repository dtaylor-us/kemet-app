import React, { useEffect, useState } from 'react';
import { View, Text, TextInput, FlatList, StyleSheet, ActivityIndicator } from 'react-native';
import {
  getJournalEntries,
  addJournalEntry,
  getStoredPin,
  setStoredPin,
  requireReauth,
  JournalEntry,
} from '../lib/journalStorage';
import { Card, EmptyState, KemetButton } from '../components/ui';
import { colors, radii, spacing } from '../lib/theme';

/**
 * Local-only journaling (see lib/journalStorage.ts for the reasoning on what is and
 * isn't encrypted at rest). Unlock order: Face ID/Touch ID first if enrolled, PIN
 * (Keychain-backed via SecureStore) as fallback or first-time setup — satisfies
 * REQ-024 ("lock or require re-authentication before opening journals").
 */
export default function JournalScreen() {
  const [unlocked, setUnlocked] = useState(false);
  const [checkingBiometric, setCheckingBiometric] = useState(true);
  const [pinInput, setPinInput] = useState('');
  const [hasPin, setHasPin] = useState<boolean | null>(null);
  const [entries, setEntries] = useState<JournalEntry[]>([]);
  const [draft, setDraft] = useState('');

  useEffect(() => {
    (async () => {
      const storedPin = await getStoredPin();
      setHasPin(!!storedPin);

      if (storedPin) {
        // Only attempt biometrics once a PIN fallback already exists — otherwise a
        // failed/cancelled scan would leave the user stuck with no way in at all.
        const result = await requireReauth();
        if (result === 'biometric-success') {
          setUnlocked(true);
        }
        // 'biometric-unavailable' or 'biometric-failed' both just fall through to the
        // PIN screen below — no separate error state needed.
      }
      setCheckingBiometric(false);
    })();
  }, []);

  useEffect(() => {
    if (unlocked) {
      getJournalEntries().then(setEntries);
    }
  }, [unlocked]);

  async function handlePinSubmit() {
    if (hasPin === false) {
      await setStoredPin(pinInput);
      setUnlocked(true);
      return;
    }
    const storedPin = await getStoredPin();
    if (pinInput === storedPin) {
      setUnlocked(true);
    } else {
      setPinInput('');
    }
  }

  async function handleAddEntry() {
    if (!draft.trim()) return;
    await addJournalEntry(draft.trim());
    setDraft('');
    setEntries(await getJournalEntries());
  }

  if (hasPin === null || checkingBiometric) {
    return <ActivityIndicator style={styles.center} />;
  }

  if (!unlocked) {
    return (
      <View style={styles.lockContainer}>
        <Card style={styles.lockCard}>
          <Text style={styles.lockKicker}>Private journal</Text>
          <Text style={styles.lockTitle}>
            {hasPin ? 'Enter your journal PIN' : 'Set a PIN to protect your journal'}
          </Text>
          {hasPin && (
            <Text style={styles.lockSubtitle}>Face ID/Touch ID did not unlock it. Use your PIN instead.</Text>
          )}
          <TextInput
            style={styles.pinInput}
            value={pinInput}
            onChangeText={setPinInput}
            keyboardType="number-pad"
            secureTextEntry
            maxLength={6}
            placeholder="PIN"
            placeholderTextColor={colors.faint}
          />
          <KemetButton title={hasPin ? 'Unlock' : 'Set PIN'} onPress={handlePinSubmit} disabled={!pinInput.trim()} style={styles.lockButton} />
        </Card>
      </View>
    );
  }

  return (
    <View style={styles.container}>
      <FlatList
        data={entries}
        keyExtractor={(e) => e.id}
        contentContainerStyle={styles.list}
        ListEmptyComponent={<EmptyState title="No entries yet" body="Write the first note from today's practice when something is ready to be kept." />}
        renderItem={({ item }) => (
          <Card style={styles.entry}>
            <Text style={styles.entryDate}>{new Date(item.createdAt).toLocaleString()}</Text>
            <Text style={styles.entryText}>{item.text}</Text>
          </Card>
        )}
      />
      <View style={styles.composer}>
        <TextInput
          style={styles.draftInput}
          value={draft}
          onChangeText={setDraft}
          placeholder="Write today's reflection..."
          placeholderTextColor={colors.faint}
          multiline
        />
        <KemetButton title="Save entry" onPress={handleAddEntry} disabled={!draft.trim()} />
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: colors.background },
  center: { flex: 1, justifyContent: 'center' },
  lockContainer: { flex: 1, justifyContent: 'center', alignItems: 'center', padding: spacing.xl, backgroundColor: colors.background },
  lockCard: { width: '100%', alignItems: 'center' },
  lockKicker: { color: colors.goldDark, fontSize: 12, fontWeight: '900', textTransform: 'uppercase' },
  lockTitle: { color: colors.ink, fontSize: 22, fontWeight: '900', textAlign: 'center', marginTop: spacing.sm },
  lockSubtitle: { fontSize: 14, lineHeight: 20, color: colors.muted, textAlign: 'center', marginTop: spacing.sm },
  pinInput: {
    borderWidth: 1,
    borderColor: colors.border,
    borderRadius: radii.md,
    padding: spacing.md,
    width: 140,
    textAlign: 'center',
    fontSize: 22,
    letterSpacing: 0,
    color: colors.ink,
    backgroundColor: colors.white,
    marginTop: spacing.xl,
  },
  lockButton: { alignSelf: 'stretch', marginTop: spacing.xl },
  list: { padding: spacing.lg, gap: spacing.md, flexGrow: 1 },
  entry: { marginBottom: spacing.sm },
  entryDate: { fontSize: 12, color: colors.goldDark, marginBottom: spacing.sm, fontWeight: '800' },
  entryText: { color: colors.text, fontSize: 15, lineHeight: 22 },
  composer: { padding: spacing.md, borderTopWidth: 1, borderTopColor: colors.border, gap: spacing.sm, backgroundColor: colors.surface },
  draftInput: {
    borderWidth: 1,
    borderColor: colors.border,
    borderRadius: radii.md,
    padding: spacing.md,
    minHeight: 88,
    textAlignVertical: 'top',
    color: colors.ink,
    backgroundColor: colors.white,
    fontSize: 15,
    lineHeight: 21,
  },
});
