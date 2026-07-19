import AsyncStorage from '@react-native-async-storage/async-storage';
import * as SecureStore from 'expo-secure-store';
import * as LocalAuthentication from 'expo-local-authentication';
import {
  addJournalEntry, getJournalEntries, getStoredPin, requireReauth, setStoredPin,
} from '../journalStorage';

jest.mock('@react-native-async-storage/async-storage', () => ({ getItem: jest.fn(), setItem: jest.fn() }));
jest.mock('expo-secure-store', () => ({ getItemAsync: jest.fn(), setItemAsync: jest.fn() }));
jest.mock('expo-local-authentication', () => ({
  hasHardwareAsync: jest.fn(), isEnrolledAsync: jest.fn(), authenticateAsync: jest.fn(),
}));

const storage = AsyncStorage as jest.Mocked<typeof AsyncStorage>;
const secure = SecureStore as jest.Mocked<typeof SecureStore>;
const biometrics = LocalAuthentication as jest.Mocked<typeof LocalAuthentication>;

it('returns an empty journal when nothing is stored', async () => {
  storage.getItem.mockResolvedValue(null);
  await expect(getJournalEntries()).resolves.toEqual([]);
});

it('reads entries and prepends a new timestamped entry', async () => {
  jest.spyOn(Date, 'now').mockReturnValue(123);
  storage.getItem.mockResolvedValue(JSON.stringify([{ id: 'old', createdAt: 'then', text: 'Earlier' }]));
  await expect(getJournalEntries()).resolves.toHaveLength(1);
  await addJournalEntry('Today');
  const saved = JSON.parse(storage.setItem.mock.calls[0][1]);
  expect(saved).toEqual([
    expect.objectContaining({ id: '123', text: 'Today' }),
    expect.objectContaining({ id: 'old' }),
  ]);
});

it('stores and retrieves the PIN through secure storage', async () => {
  secure.getItemAsync.mockResolvedValue('2468');
  await expect(getStoredPin()).resolves.toBe('2468');
  await setStoredPin('1357');
  expect(secure.setItemAsync).toHaveBeenCalledWith('kemet-journal-pin', '1357');
});

it.each([
  [false, true],
  [true, false],
])('reports unavailable without hardware and enrollment', async (hardware, enrolled) => {
  biometrics.hasHardwareAsync.mockResolvedValue(hardware);
  biometrics.isEnrolledAsync.mockResolvedValue(enrolled);
  await expect(requireReauth()).resolves.toBe('biometric-unavailable');
  expect(biometrics.authenticateAsync).not.toHaveBeenCalled();
});

it.each([[true, 'biometric-success'], [false, 'biometric-failed']] as const)
('maps biometric result %s', async (success, expected) => {
  biometrics.hasHardwareAsync.mockResolvedValue(true);
  biometrics.isEnrolledAsync.mockResolvedValue(true);
  biometrics.authenticateAsync.mockResolvedValue({ success } as any);
  await expect(requireReauth()).resolves.toBe(expected);
  expect(biometrics.authenticateAsync).toHaveBeenCalledWith(expect.objectContaining({
    promptMessage: 'Unlock your journal', disableDeviceFallback: false,
  }));
});
