import React from 'react';
import { fireEvent, render, screen, waitFor } from '@testing-library/react-native';
import App from '../App';
import { hasValidCredentials, logout } from '../lib/auth0Client';

jest.mock('../lib/auth0Client', () => ({ hasValidCredentials: jest.fn(), logout: jest.fn() }));
jest.mock('../screens/LoginScreen', () => {
  const React = require('react');
  const { Pressable, Text } = require('react-native');
  return ({ onSignedIn }: any) => <Pressable accessibilityRole="button" onPress={onSignedIn}><Text>Mock login</Text></Pressable>;
});
jest.mock('../screens/PracticeScreen', () => {
  const { Text } = require('react-native');
  return () => <Text>Practice screen</Text>;
});
jest.mock('../screens/FacultiesScreen', () => {
  const React = require('react');
  const { Pressable, Text } = require('react-native');
  return ({ onFacultyChanged }: any) => <Pressable accessibilityRole="button" onPress={onFacultyChanged}><Text>Faculty screen</Text></Pressable>;
});
jest.mock('../screens/CompanionScreen', () => {
  const { Text } = require('react-native');
  return () => <Text>Companion screen</Text>;
});
jest.mock('../screens/JournalScreen', () => {
  const { Text } = require('react-native');
  return () => <Text>Journal screen</Text>;
});

const credentialsMock = hasValidCredentials as jest.MockedFunction<typeof hasValidCredentials>;
const logoutMock = logout as jest.MockedFunction<typeof logout>;

it('shows login and enters the authenticated application', async () => {
  credentialsMock.mockResolvedValue(false);
  await render(<App />);
  await fireEvent.press(await screen.findByText('Mock login'));
  expect(await screen.findByText('Practice screen')).toBeOnTheScreen();
});

it('handles credential errors as signed out', async () => {
  credentialsMock.mockRejectedValue(new Error('invalid'));
  await render(<App />);
  expect(await screen.findByText('Mock login')).toBeOnTheScreen();
});

it('navigates all tabs, applies a faculty change, and logs out', async () => {
  credentialsMock.mockResolvedValue(true);
  logoutMock.mockResolvedValue();
  await render(<App />);
  expect(await screen.findByText('Practice screen')).toBeOnTheScreen();

  await fireEvent.press(screen.getByText('Faculties'));
  await fireEvent.press(await screen.findByText('Faculty screen'));
  expect(await screen.findByText('Practice screen')).toBeOnTheScreen();

  await fireEvent.press(screen.getByText('Companion'));
  expect(screen.getByText('Companion screen')).toBeOnTheScreen();
  await fireEvent.press(screen.getByText('Journal'));
  expect(screen.getByText('Journal screen')).toBeOnTheScreen();

  await fireEvent.press(screen.getByText('Log out'));
  await waitFor(() => expect(logoutMock).toHaveBeenCalled());
  expect(await screen.findByText('Mock login')).toBeOnTheScreen();
});
