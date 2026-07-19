import React from 'react';
import { fireEvent, render, screen } from '@testing-library/react-native';
import { Card, EmptyState, KemetButton, SectionTitle } from '../ui';

it('invokes an enabled button and disables it while loading', async () => {
  const onPress = jest.fn();
  const view = await render(<KemetButton title="Continue" onPress={onPress} />);
  await fireEvent.press(screen.getByRole('button'));
  expect(onPress).toHaveBeenCalled();

  await view.rerender(<KemetButton title="Continue" onPress={onPress} loading />);
  expect(screen.getByRole('button')).toBeDisabled();
  expect(screen.queryByText('Continue')).toBeNull();
});

it('renders reusable content components', async () => {
  await render(<><Card><SectionTitle kicker="Read" label="Teaching" /></Card><EmptyState title="Empty" body="Nothing yet" /></>);
  expect(screen.getByText('Read')).toBeOnTheScreen();
  expect(screen.getByText('Teaching')).toBeOnTheScreen();
  expect(screen.getByText('Nothing yet')).toBeOnTheScreen();
});
