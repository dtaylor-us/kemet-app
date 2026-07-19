import {
  API_BASE_URL,
  fetchFaculty,
  fetchFacultyList,
  fetchUserProfile,
  markPracticeComplete,
  sendCompanionMessage,
  setActiveFaculty,
} from '../api';
import { getAccessToken } from '../auth0Client';

jest.mock('../auth0Client', () => ({ getAccessToken: jest.fn() }));

const token = getAccessToken as jest.MockedFunction<typeof getAccessToken>;
const fetchMock = jest.fn();

beforeEach(() => {
  globalThis.fetch = fetchMock;
  token.mockResolvedValue('access-token');
});

function response(body: unknown, ok = true, status = 200) {
  return {
    ok,
    status,
    json: jest.fn().mockResolvedValue(body),
    text: jest.fn().mockResolvedValue(String(body)),
  } as unknown as Response;
}

test.each([
  ['faculty list', fetchFacultyList, '/api/faculty', undefined, [{ id: 'amen' }]],
  ['faculty detail', () => fetchFaculty('maat'), '/api/faculty/maat', undefined, { id: 'maat' }],
  ['profile', fetchUserProfile, '/api/user/me', undefined, { activeFacultyId: 'amen' }],
  ['practice completion', markPracticeComplete, '/api/practice/complete', { method: 'POST' }, { completedDays: 2 }],
] as const)('fetches %s with authorization', async (_name, call, path, options, body) => {
  fetchMock.mockResolvedValue(response(body));
  await expect(call()).resolves.toEqual(body);
  expect(fetchMock).toHaveBeenCalledWith(`${API_BASE_URL}${path}`, expect.objectContaining({
    ...(options ?? {}),
    headers: expect.objectContaining({ Authorization: 'Bearer access-token', 'Content-Type': 'application/json' }),
  }));
});

it('sends faculty and companion JSON payloads', async () => {
  fetchMock.mockResolvedValueOnce(response({ activeFacultyId: 'maat' }))
    .mockResolvedValueOnce(response({ reply: 'Be still.', aiGenerated: true }));

  await setActiveFaculty('maat');
  await sendCompanionMessage('How?');

  expect(fetchMock).toHaveBeenNthCalledWith(1, `${API_BASE_URL}/api/user/active-faculty`, expect.objectContaining({
    method: 'PATCH', body: JSON.stringify({ facultyId: 'maat' }),
  }));
  expect(fetchMock).toHaveBeenNthCalledWith(2, `${API_BASE_URL}/api/companion/chat`, expect.objectContaining({
    method: 'POST', body: JSON.stringify({ message: 'How?' }),
  }));
});

it('preserves caller headers', async () => {
  fetchMock.mockResolvedValue(response([]));
  await fetchFacultyList();
  expect(fetchMock.mock.calls[0][1].headers).toEqual(expect.objectContaining({ Authorization: 'Bearer access-token' }));
});

it('throws a diagnostic error for non-success responses', async () => {
  fetchMock.mockResolvedValue(response('denied', false, 403));
  await expect(fetchFacultyList()).rejects.toThrow('/api/faculty -> 403: denied');
});

it('reads API_BASE_URL from EXPO_PUBLIC_API_BASE_URL', () => {
  expect(API_BASE_URL).toBe('http://test-host:8090');
});

it('throws when EXPO_PUBLIC_API_BASE_URL is not set', () => {
  const saved = process.env.EXPO_PUBLIC_API_BASE_URL;
  try {
    delete process.env.EXPO_PUBLIC_API_BASE_URL;
    expect(() => {
      jest.isolateModules(() => {
        require('../api');
      });
    }).toThrow('EXPO_PUBLIC_API_BASE_URL is not set');
  } finally {
    process.env.EXPO_PUBLIC_API_BASE_URL = saved;
  }
});

it('handles an unreadable error body', async () => {
  fetchMock.mockResolvedValue({ ok: false, status: 500, text: jest.fn().mockRejectedValue(new Error('broken')) });
  await expect(fetchUserProfile()).rejects.toThrow('/api/user/me -> 500: ');
});
