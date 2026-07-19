import React from 'react';
import { Alert } from 'react-native';
import { fireEvent, render, screen, waitFor } from '@testing-library/react-native';
import FacultiesScreen from '../FacultiesScreen';
import { fetchFacultyList, fetchUserProfile, setActiveFaculty } from '../../lib/api';

jest.mock('../../lib/api', () => ({ fetchFacultyList: jest.fn(), fetchUserProfile: jest.fn(), setActiveFaculty: jest.fn() }));
const listMock = fetchFacultyList as jest.MockedFunction<typeof fetchFacultyList>;
const profileMock = fetchUserProfile as jest.MockedFunction<typeof fetchUserProfile>;
const selectMock = setActiveFaculty as jest.MockedFunction<typeof setActiveFaculty>;

beforeEach(() => {
  listMock.mockResolvedValue([
    { id: 'amen', displayName: 'Amen', role: 'Foundation', practiceOrder: 1 },
    { id: 'maat', displayName: "Ma'at", role: 'Truth', practiceOrder: 5 },
  ]);
  profileMock.mockResolvedValue({ id: 'u', displayName: 'Amina', activeFacultyId: 'amen' });
});

it('shows active faculty and switches to another', async () => {
  const changed = jest.fn();
  selectMock.mockResolvedValue({ id: 'u', displayName: 'Amina', activeFacultyId: 'maat' });
  await render(<FacultiesScreen onFacultyChanged={changed} />);
  expect(await screen.findByText('Active')).toBeOnTheScreen();
  await fireEvent.press(screen.getByText("Ma'at"));
  await waitFor(() => expect(selectMock).toHaveBeenCalledWith('maat'));
  expect(changed).toHaveBeenCalled();
});

it('does nothing when the active faculty is selected', async () => {
  await render(<FacultiesScreen onFacultyChanged={jest.fn()} />);
  await screen.findByText('Active');
  await fireEvent.press(screen.getByText('Amen'));
  expect(selectMock).not.toHaveBeenCalled();
});

it('reports initial and switching failures', async () => {
  const alert = jest.spyOn(Alert, 'alert').mockImplementation();
  listMock.mockRejectedValueOnce(new Error('load failed'));
  const view = await render(<FacultiesScreen onFacultyChanged={jest.fn()} />);
  await waitFor(() => expect(alert).toHaveBeenCalledWith('Error', 'load failed'));

  listMock.mockResolvedValue([{ id: 'maat', displayName: "Ma'at", role: 'Truth', practiceOrder: 5 }]);
  selectMock.mockRejectedValue(new Error('save failed'));
  await view.rerender(<FacultiesScreen key="again" onFacultyChanged={jest.fn()} />);
  await fireEvent.press(await screen.findByText("Ma'at"));
  await waitFor(() => expect(alert).toHaveBeenCalledWith('Error', 'save failed'));
});
