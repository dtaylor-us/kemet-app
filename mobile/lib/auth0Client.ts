import Auth0 from 'react-native-auth0';
import { AUTH0_DOMAIN, AUTH0_CLIENT_ID, AUTH0_AUDIENCE, AUTH0_CUSTOM_SCHEME } from './authConfig';

// Using the class-based Auth0 client (rather than the useAuth0() hook + Auth0Provider)
// so the same instance can be called from lib/api.ts, which is a plain module, not a
// React component — hooks can't be called there. This is one of react-native-auth0's
// two officially documented usage patterns, not a workaround.
export const auth0 = new Auth0({
  domain: AUTH0_DOMAIN,
  clientId: AUTH0_CLIENT_ID,
});

export async function login(): Promise<void> {
  const credentials = await auth0.webAuth.authorize(
    {
      // offline_access gets us a refresh token so getAccessToken() below can silently
      // renew instead of forcing a re-login every time the access token expires.
      scope: 'openid profile email offline_access',
      // Without this, Auth0 issues an opaque token instead of a JWT, and the backend's
      // Spring Security JWT decoder will reject every request with a 401. Must match
      // the Auth0 "API" identifier configured in backend/.env's AUTH0_AUDIENCE.
      audience: AUTH0_AUDIENCE,
    },
    { customScheme: AUTH0_CUSTOM_SCHEME }
  );
  await auth0.credentialsManager.saveCredentials(credentials);
}

export async function logout(): Promise<void> {
  await auth0.webAuth.clearSession({}, { customScheme: AUTH0_CUSTOM_SCHEME });
  await auth0.credentialsManager.clearCredentials();
}

export async function hasValidCredentials(): Promise<boolean> {
  return auth0.credentialsManager.hasValidCredentials();
}

/** Returns a currently-valid access token, transparently refreshing via the stored
 * refresh token if the previous one expired. Throws if the user isn't logged in. */
export async function getAccessToken(): Promise<string> {
  const credentials = await auth0.credentialsManager.getCredentials();
  return credentials.accessToken;
}
