import React, { useState } from 'react';
import { View, Text, StyleSheet, Alert } from 'react-native';
import { login } from '../lib/auth0Client';
import { KemetButton } from '../components/ui';
import { colors, radii, shadows, spacing } from '../lib/theme';

export default function LoginScreen({ onSignedIn }: { onSignedIn: () => void }) {
  const [loading, setLoading] = useState(false);

  async function handleLogin() {
    setLoading(true);
    try {
      await login();
      onSignedIn();
    } catch (e: any) {
      if (e?.message === 'a0.session.user_cancelled') {
        // User closed the login screen — not an error worth surfacing.
      } else {
        Alert.alert('Login failed', String(e?.message ?? e));
      }
    } finally {
      setLoading(false);
    }
  }

  return (
    <View style={styles.container}>
      <View style={styles.mark}>
        <Text style={styles.markText}>K</Text>
      </View>
      <View style={styles.panel}>
        <Text style={styles.eyebrow}>Personal practice companion</Text>
        <Text style={styles.title}>Kemet</Text>
        <Text style={styles.subtitle}>Continue your faculty practice, companion check-ins, and private journal.</Text>
        <KemetButton title="Log in" onPress={handleLogin} loading={loading} style={styles.button} />
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    padding: spacing.xl,
    backgroundColor: colors.background,
    gap: spacing.xl,
  },
  mark: {
    width: 72,
    height: 72,
    borderRadius: 36,
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: colors.ink,
  },
  markText: { color: colors.gold, fontSize: 34, fontWeight: '900' },
  panel: {
    width: '100%',
    borderRadius: radii.lg,
    backgroundColor: colors.surface,
    borderWidth: 1,
    borderColor: colors.border,
    padding: spacing.xl,
    alignItems: 'center',
    ...shadows.soft,
  },
  eyebrow: { color: colors.goldDark, fontSize: 12, fontWeight: '800', letterSpacing: 0, textTransform: 'uppercase' },
  title: { color: colors.ink, fontSize: 42, fontWeight: '900', marginTop: spacing.sm },
  subtitle: { fontSize: 16, lineHeight: 23, color: colors.muted, marginTop: spacing.sm, textAlign: 'center' },
  button: { alignSelf: 'stretch', marginTop: spacing.xl },
});
