import React from 'react';
import { fireEvent, render, screen, waitFor } from '@testing-library/react-native';
import JournalScreen from '../JournalScreen';
import { addJournalEntry, getJournalEntries, getStoredPin, requireReauth, setStoredPin } from '../../lib/journalStorage';

jest.mock('../../lib/journalStorage', () => ({
  addJournalEntry: jest.fn(), getJournalEntries: jest.fn(), getStoredPin: jest.fn(),
  requireReauth: jest.fn(), setStoredPin: jest.fn(),
}));
const pinMock = getStoredPin as jest.MockedFunction<typeof getStoredPin>;
const authMock = requireReauth as jest.MockedFunction<typeof requireReauth>;
const entriesMock = getJournalEntries as jest.MockedFunction<typeof getJournalEntries>;
const setPinMock = setStoredPin as jest.MockedFunction<typeof setStoredPin>;
const addMock = addJournalEntry as jest.MockedFunction<typeof addJournalEntry>;

beforeEach(() => entriesMock.mockResolvedValue([]));

it('sets a first PIN and saves a trimmed entry', async () => {
  pinMock.mockResolvedValue(null);
  await render(<JournalScreen />);
  const pin = await screen.findByPlaceholderText('PIN');
  await fireEvent.changeText(pin, '1234');
  await fireEvent.press(screen.getByRole('button'));
  await waitFor(() => expect(setPinMock).toHaveBeenCalledWith('1234'));
  expect(await screen.findByText('No entries yet')).toBeOnTheScreen();

  entriesMock.mockResolvedValue([{ id: '1', createdAt: '2026-01-01T00:00:00Z', text: 'Saved reflection' }]);
  await fireEvent.changeText(screen.getByPlaceholderText("Write today's reflection..."), '  reflection  ');
  await fireEvent.press(screen.getByRole('button'));
  await waitFor(() => expect(addMock).toHaveBeenCalledWith('reflection'));
  expect(await screen.findByText('Saved reflection')).toBeOnTheScreen();
});

it('unlocks through biometrics when a PIN exists', async () => {
  pinMock.mockResolvedValue('1234');
  authMock.mockResolvedValue('biometric-success');
  entriesMock.mockResolvedValue([{ id: '1', createdAt: '2026-01-01T00:00:00Z', text: 'Private' }]);
  await render(<JournalScreen />);
  expect(await screen.findByText('Private')).toBeOnTheScreen();
});

it('falls back to PIN and clears an incorrect PIN', async () => {
  pinMock.mockResolvedValue('1234');
  authMock.mockResolvedValue('biometric-failed');
  await render(<JournalScreen />);
  const input = await screen.findByPlaceholderText('PIN');
  await fireEvent.changeText(input, '9999');
  await fireEvent.press(screen.getByRole('button'));
  expect(screen.getByPlaceholderText('PIN')).toHaveDisplayValue('');
  await fireEvent.changeText(input, '1234');
  await fireEvent.press(screen.getByRole('button'));
  expect(await screen.findByText('No entries yet')).toBeOnTheScreen();
});
