import React from 'react';
import { Alert } from 'react-native';
import { fireEvent, render, screen, waitFor } from '@testing-library/react-native';
import PracticeScreen from '../PracticeScreen';
import { fetchFaculty, fetchUserProfile, markPracticeComplete } from '../../lib/api';

jest.mock('../../lib/api', () => ({ fetchFaculty: jest.fn(), fetchUserProfile: jest.fn(), markPracticeComplete: jest.fn() }));
const profileMock = fetchUserProfile as jest.MockedFunction<typeof fetchUserProfile>;
const facultyMock = fetchFaculty as jest.MockedFunction<typeof fetchFaculty>;
const completeMock = markPracticeComplete as jest.MockedFunction<typeof markPracticeComplete>;

const faculty: any = {
  id: 'amen', displayName: 'Amen', role: 'Hidden foundation', treePosition: 'Crown', practiceOrder: 1,
  teachingNotes: ['Be still'], hekau: [{ id: 'h1', text: 'I am still' }],
  meditationInstructions: {
    breathingTechnique: { summary: 'Breathe gently', inbreath: 'Four', outbreath: 'Six', principle: 'Balance' },
    environmentProtocols: [], expectedSensations: '', durationGuidance: '',
    scriptSteps: [{ step: 1, name: 'Settle', instruction: 'Become quiet' }],
  },
  journalPrompts: ['What emerged?'], suggestedDailyActions: ['Pause before speaking'],
};

beforeEach(() => {
  profileMock.mockResolvedValue({ id: 'u', displayName: 'Amina', activeFacultyId: 'amen' });
  facultyMock.mockResolvedValue(faculty);
});

it('loads and renders the active faculty practice', async () => {
  await render(<PracticeScreen />);
  await waitFor(() => expect(screen.getByText('Amen')).toBeOnTheScreen());
  expect(facultyMock).toHaveBeenCalledWith('amen');
  expect(screen.getByText('Be still')).toBeOnTheScreen();
  expect(screen.getByText('I am still', { exact: false })).toBeOnTheScreen();
  expect(screen.getByText('Pause before speaking')).toBeOnTheScreen();
});

it('logs completion and reports completion failures', async () => {
  const alert = jest.spyOn(Alert, 'alert').mockImplementation();
  completeMock.mockResolvedValueOnce({ completedDays: 3 }).mockRejectedValueOnce(new Error('offline'));
  await render(<PracticeScreen />);
  const button = await screen.findByRole('button');
  await fireEvent.press(button);
  await waitFor(() => expect(alert).toHaveBeenCalledWith('Logged', 'Completed days: 3'));
  await fireEvent.press(button);
  await waitFor(() => expect(alert).toHaveBeenCalledWith('Error', 'offline'));
});

it('renders load failures', async () => {
  profileMock.mockRejectedValue(new Error('unauthorized'));
  await render(<PracticeScreen />);
  expect(await screen.findByText('unauthorized')).toBeOnTheScreen();
});
