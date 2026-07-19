import React from 'react';
import { Alert } from 'react-native';
import { fireEvent, render, screen, waitFor } from '@testing-library/react-native';
import LoginScreen from '../LoginScreen';
import { login } from '../../lib/auth0Client';

jest.mock('../../lib/auth0Client', () => ({ login: jest.fn() }));
const loginMock = login as jest.MockedFunction<typeof login>;

it('signs in successfully', async () => {
  const onSignedIn = jest.fn();
  loginMock.mockResolvedValue();
  await render(<LoginScreen onSignedIn={onSignedIn} />);
  await fireEvent.press(screen.getByRole('button'));
  await waitFor(() => expect(onSignedIn).toHaveBeenCalled());
  expect(screen.getByText('Log in')).toBeOnTheScreen();
});

it('reports login failures but ignores user cancellation', async () => {
  const alert = jest.spyOn(Alert, 'alert').mockImplementation();
  loginMock.mockRejectedValueOnce(new Error('network'));
  const view = await render(<LoginScreen onSignedIn={jest.fn()} />);
  await fireEvent.press(screen.getByRole('button'));
  await waitFor(() => expect(alert).toHaveBeenCalledWith('Login failed', 'network'));

  loginMock.mockRejectedValueOnce(new Error('a0.session.user_cancelled'));
  await view.rerender(<LoginScreen onSignedIn={jest.fn()} />);
  await fireEvent.press(screen.getByRole('button'));
  await waitFor(() => expect(loginMock).toHaveBeenCalledTimes(2));
  expect(alert).toHaveBeenCalledTimes(1);
});
