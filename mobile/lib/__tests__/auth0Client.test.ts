import { auth0, getAccessToken, hasValidCredentials, login, logout } from '../auth0Client';

const mockAuthorize = jest.fn();
const mockSaveCredentials = jest.fn();
const mockClearSession = jest.fn();
const mockClearCredentials = jest.fn();
const mockHasValidCredentials = jest.fn();
const mockGetCredentials = jest.fn();

beforeEach(() => {
  Object.assign(auth0.webAuth, { authorize: mockAuthorize, clearSession: mockClearSession });
  Object.assign(auth0.credentialsManager, {
    saveCredentials: mockSaveCredentials,
    clearCredentials: mockClearCredentials,
    hasValidCredentials: mockHasValidCredentials,
    getCredentials: mockGetCredentials,
  });
});

it('authorizes with refresh-token scope and saves credentials', async () => {
  const credentials = { accessToken: 'token' };
  mockAuthorize.mockResolvedValue(credentials);
  await login();
  expect(mockAuthorize).toHaveBeenCalledWith(expect.objectContaining({
    scope: expect.stringContaining('offline_access'), audience: 'https://api.kemet.local',
  }), { customScheme: 'kemet' });
  expect(mockSaveCredentials).toHaveBeenCalledWith(credentials);
});

it('clears both Auth0 session and credentials on logout', async () => {
  await logout();
  expect(mockClearSession).toHaveBeenCalledWith({}, { customScheme: 'kemet' });
  expect(mockClearCredentials).toHaveBeenCalled();
});

it('delegates credential validation and token refresh', async () => {
  mockHasValidCredentials.mockResolvedValue(true);
  mockGetCredentials.mockResolvedValue({ accessToken: 'fresh' });
  await expect(hasValidCredentials()).resolves.toBe(true);
  await expect(getAccessToken()).resolves.toBe('fresh');
});
