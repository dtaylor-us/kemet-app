import React from 'react';
import { fireEvent, render, screen, waitFor } from '@testing-library/react-native';
import CompanionScreen from '../CompanionScreen';
import { sendCompanionMessage } from '../../lib/api';

jest.mock('../../lib/api', () => ({ sendCompanionMessage: jest.fn() }));
const sendMock = sendCompanionMessage as jest.MockedFunction<typeof sendCompanionMessage>;

it('starts empty and prevents blank messages', async () => {
  await render(<CompanionScreen />);
  expect(screen.getByText('Ask from the current practice')).toBeOnTheScreen();
  expect(screen.getByRole('button')).toBeDisabled();
});

it('sends trimmed text and labels the AI response', async () => {
  sendMock.mockResolvedValue({ reply: 'Practice stillness.', aiGenerated: true });
  await render(<CompanionScreen />);
  await fireEvent.changeText(screen.getByPlaceholderText('Ask about your active practice...'), '  How?  ');
  await fireEvent.press(screen.getByRole('button'));
  expect(screen.getByText('How?')).toBeOnTheScreen();
  await waitFor(() => expect(screen.getByText('Practice stillness.')).toBeOnTheScreen());
  expect(sendMock).toHaveBeenCalledWith('How?');
  expect(screen.getByText('AI Companion — AI-generated, not official teaching')).toBeOnTheScreen();
});

it('renders upstream errors as companion messages', async () => {
  sendMock.mockRejectedValue(new Error('offline'));
  await render(<CompanionScreen />);
  await fireEvent.changeText(screen.getByPlaceholderText('Ask about your active practice...'), 'Help');
  await fireEvent.press(screen.getByRole('button'));
  await waitFor(() => expect(screen.getByText('Error: offline')).toBeOnTheScreen());
});
