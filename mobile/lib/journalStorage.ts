import AsyncStorage from '@react-native-async-storage/async-storage';
import * as SecureStore from 'expo-secure-store';
import * as LocalAuthentication from 'expo-local-authentication';

// Journal entry CONTENT still lives in plain AsyncStorage, not encrypted at the app
// level. That's a deliberate call, not an oversight: for a single-device personal app
// (not distributed, not multi-user), the real protection for data at rest is iOS's own
// file-level encryption tied to your device passcode (Data Protection), which already
// covers AsyncStorage's files once the phone is locked. Rolling custom encryption here
// would mean either hand-writing crypto (a real way to make things LESS safe if done
// wrong) or pulling in a full crypto library for marginal benefit over what the OS
// already provides in this specific threat model. Revisit if this app ever gets used by
// more than one person or on a device without a passcode set.
//
// What DID move to real secure storage: the PIN itself (Keychain-backed via
// SecureStore, not plain AsyncStorage), and the primary unlock path is now Face
// ID/Touch ID via expo-local-authentication, with the PIN as fallback — see
// requireReauth() below. This satisfies REQ-024 ("lock or require re-authentication
// before opening journals") with an actual OS-backed mechanism instead of a UI-only gate.

const JOURNAL_KEY = 'kemet_journal_entries';
const PIN_KEY = 'kemet-journal-pin'; // SecureStore keys: alphanumeric, ., -, _ only

export interface JournalEntry {
  id: string;
  createdAt: string;
  text: string;
}

export async function getJournalEntries(): Promise<JournalEntry[]> {
  const raw = await AsyncStorage.getItem(JOURNAL_KEY);
  return raw ? JSON.parse(raw) : [];
}

export async function addJournalEntry(text: string): Promise<void> {
  const entries = await getJournalEntries();
  entries.unshift({ id: String(Date.now()), createdAt: new Date().toISOString(), text });
  await AsyncStorage.setItem(JOURNAL_KEY, JSON.stringify(entries));
}

export async function getStoredPin(): Promise<string | null> {
  return SecureStore.getItemAsync(PIN_KEY);
}

export async function setStoredPin(pin: string): Promise<void> {
  await SecureStore.setItemAsync(PIN_KEY, pin);
}

export type ReauthResult = 'biometric-success' | 'biometric-unavailable' | 'biometric-failed';

/** Tries Face ID/Touch ID first. Returns 'biometric-unavailable' if the device has no
 * enrolled biometrics (simulator, no Face ID set up, etc.) so the caller can fall back
 * to the PIN screen — it does NOT fall back automatically, since "unavailable" and
 * "the user failed the scan" should be handled differently by the UI. */
export async function requireReauth(): Promise<ReauthResult> {
  const hasHardware = await LocalAuthentication.hasHardwareAsync();
  const isEnrolled = await LocalAuthentication.isEnrolledAsync();
  if (!hasHardware || !isEnrolled) {
    return 'biometric-unavailable';
  }

  const result = await LocalAuthentication.authenticateAsync({
    promptMessage: 'Unlock your journal',
    disableDeviceFallback: false, // let iOS itself offer the device passcode as a fallback too
  });

  return result.success ? 'biometric-success' : 'biometric-failed';
}
