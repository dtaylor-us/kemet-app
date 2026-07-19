jest.mock('expo-status-bar', () => ({ StatusBar: () => null }));

jest.mock('react-native-auth0', () => ({
  __esModule: true,
  default: jest.fn().mockImplementation(() => ({
    webAuth: { authorize: jest.fn(), clearSession: jest.fn() },
    credentialsManager: {
      saveCredentials: jest.fn(),
      clearCredentials: jest.fn(),
      hasValidCredentials: jest.fn(),
      getCredentials: jest.fn(),
    },
  })),
}));
