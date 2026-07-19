import React from 'react';
import {
  ActivityIndicator,
  Pressable,
  PressableProps,
  StyleProp,
  StyleSheet,
  Text,
  TextStyle,
  View,
  ViewStyle,
} from 'react-native';
import { colors, radii, shadows, spacing } from '../lib/theme';

type ButtonVariant = 'primary' | 'secondary' | 'ghost';

interface KemetButtonProps extends PressableProps {
  title: string;
  variant?: ButtonVariant;
  loading?: boolean;
  style?: StyleProp<ViewStyle>;
  textStyle?: StyleProp<TextStyle>;
}

export function KemetButton({
  title,
  variant = 'primary',
  loading = false,
  disabled,
  style,
  textStyle,
  ...pressableProps
}: KemetButtonProps) {
  const isDisabled = disabled || loading;
  return (
    <Pressable
      accessibilityRole="button"
      disabled={isDisabled}
      style={({ pressed }) => [
        styles.button,
        styles[`${variant}Button`],
        pressed && !isDisabled && styles.buttonPressed,
        isDisabled && styles.buttonDisabled,
        style,
      ]}
      {...pressableProps}
    >
      {loading ? (
        <ActivityIndicator color={variant === 'primary' ? colors.white : colors.goldDark} />
      ) : (
        <Text style={[styles.buttonText, styles[`${variant}ButtonText`], textStyle]}>{title}</Text>
      )}
    </Pressable>
  );
}

export function Card({ children, style }: { children: React.ReactNode; style?: StyleProp<ViewStyle> }) {
  return <View style={[styles.card, style]}>{children}</View>;
}

export function SectionTitle({ label, kicker }: { label: string; kicker?: string }) {
  return (
    <View style={styles.sectionTitleRow}>
      <View>
        {kicker && <Text style={styles.kicker}>{kicker}</Text>}
        <Text style={styles.sectionTitle}>{label}</Text>
      </View>
    </View>
  );
}

export function EmptyState({ title, body }: { title: string; body: string }) {
  return (
    <Card style={styles.emptyState}>
      <Text style={styles.emptyTitle}>{title}</Text>
      <Text style={styles.emptyBody}>{body}</Text>
    </Card>
  );
}

const styles = StyleSheet.create({
  button: {
    minHeight: 48,
    borderRadius: radii.sm,
    alignItems: 'center',
    justifyContent: 'center',
    paddingHorizontal: spacing.lg,
    paddingVertical: spacing.md,
  },
  primaryButton: { backgroundColor: colors.ink },
  secondaryButton: {
    backgroundColor: colors.surface,
    borderWidth: 1,
    borderColor: colors.border,
  },
  ghostButton: { backgroundColor: 'transparent' },
  buttonPressed: { opacity: 0.78, transform: [{ translateY: 1 }] },
  buttonDisabled: { opacity: 0.55 },
  buttonText: { fontSize: 15, fontWeight: '700' },
  primaryButtonText: { color: colors.white },
  secondaryButtonText: { color: colors.ink },
  ghostButtonText: { color: colors.goldDark },
  card: {
    backgroundColor: colors.surface,
    borderRadius: radii.md,
    borderWidth: 1,
    borderColor: colors.border,
    padding: spacing.lg,
    ...shadows.soft,
  },
  sectionTitleRow: { marginTop: spacing.xl, marginBottom: spacing.md },
  kicker: {
    color: colors.goldDark,
    fontSize: 11,
    fontWeight: '800',
    letterSpacing: 0,
    textTransform: 'uppercase',
    marginBottom: spacing.xs,
  },
  sectionTitle: { color: colors.ink, fontSize: 20, fontWeight: '800' },
  emptyState: { alignItems: 'center', gap: spacing.sm, marginTop: spacing.lg },
  emptyTitle: { color: colors.ink, fontSize: 18, fontWeight: '800', textAlign: 'center' },
  emptyBody: { color: colors.muted, fontSize: 14, lineHeight: 20, textAlign: 'center' },
});