import React, { useState } from 'react';
import { View, Text, TextInput, FlatList, StyleSheet, KeyboardAvoidingView, Platform } from 'react-native';
import { sendCompanionMessage } from '../lib/api';
import { EmptyState, KemetButton } from '../components/ui';
import { colors, radii, spacing } from '../lib/theme';

interface DisplayMessage {
  id: string;
  role: 'user' | 'assistant';
  text: string;
}

export default function CompanionScreen() {
  const [messages, setMessages] = useState<DisplayMessage[]>([]);
  const [input, setInput] = useState('');
  const [sending, setSending] = useState(false);

  async function handleSend() {
    const text = input.trim();
    if (!text || sending) return;

    const userMessage: DisplayMessage = { id: String(Date.now()), role: 'user', text };
    setMessages((prev) => [...prev, userMessage]);
    setInput('');
    setSending(true);

    try {
      const response = await sendCompanionMessage(text);
      setMessages((prev) => [
        ...prev,
        { id: String(Date.now() + 1), role: 'assistant', text: response.reply },
      ]);
    } catch (e: any) {
      setMessages((prev) => [
        ...prev,
        { id: String(Date.now() + 2), role: 'assistant', text: `Error: ${e.message ?? e}` },
      ]);
    } finally {
      setSending(false);
    }
  }

  return (
    <KeyboardAvoidingView
      style={styles.container}
      behavior={Platform.OS === 'ios' ? 'padding' : undefined}
    >
      <FlatList
        data={messages}
        keyExtractor={(m) => m.id}
        contentContainerStyle={styles.list}
        ListEmptyComponent={
          <EmptyState
            title="Ask from the current practice"
            body="Use the companion for clarification, reflection prompts, or help staying grounded in the active faculty content."
          />
        }
        renderItem={({ item }) => (
          <View style={[styles.bubble, item.role === 'user' ? styles.userBubble : styles.assistantBubble]}>
            {item.role === 'assistant' && (
              // REQ-038/066 — every companion response must be visibly labeled as
              // AI-generated, never presented as if it were official teaching.
              <Text style={styles.aiLabel}>AI Companion — AI-generated, not official teaching</Text>
            )}
            <Text style={[styles.bubbleText, item.role === 'user' && styles.userBubbleText]}>{item.text}</Text>
          </View>
        )}
      />
      <View style={styles.inputRow}>
        <TextInput
          style={styles.input}
          value={input}
          onChangeText={setInput}
          placeholder="Ask about your active practice..."
          placeholderTextColor={colors.faint}
          editable={!sending}
          multiline
        />
        <KemetButton title="Send" onPress={handleSend} disabled={sending || !input.trim()} loading={sending} style={styles.sendButton} />
      </View>
    </KeyboardAvoidingView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: colors.background },
  list: { padding: spacing.lg, gap: spacing.md, flexGrow: 1 },
  bubble: { padding: spacing.md, borderRadius: radii.md, maxWidth: '88%' },
  userBubble: { backgroundColor: colors.blue, alignSelf: 'flex-end', borderBottomRightRadius: radii.sm },
  assistantBubble: { backgroundColor: colors.surface, alignSelf: 'flex-start', borderBottomLeftRadius: radii.sm, borderWidth: 1, borderColor: colors.border },
  aiLabel: { fontSize: 10, color: colors.goldDark, marginBottom: spacing.xs, textTransform: 'uppercase', fontWeight: '900' },
  bubbleText: { color: colors.text, fontSize: 15, lineHeight: 21 },
  userBubbleText: { color: colors.white },
  inputRow: {
    flexDirection: 'row',
    alignItems: 'flex-end',
    padding: spacing.md,
    gap: spacing.sm,
    borderTopWidth: 1,
    borderTopColor: colors.border,
    backgroundColor: colors.surface,
  },
  input: {
    flex: 1,
    maxHeight: 108,
    borderWidth: 1,
    borderColor: colors.border,
    borderRadius: radii.md,
    paddingHorizontal: spacing.md,
    paddingVertical: spacing.md,
    color: colors.ink,
    backgroundColor: colors.white,
    fontSize: 15,
    lineHeight: 20,
  },
  sendButton: { minWidth: 84 },
});
