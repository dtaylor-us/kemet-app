// Fill these in from your Auth0 Dashboard (Applications -> your Native app -> Settings)
// and keep them in sync with app.json's react-native-auth0 plugin config and your
// backend's AUTH0_ISSUER_URI / AUTH0_AUDIENCE (backend/.env).
//
// AUTH0_AUDIENCE must be the identifier of the Auth0 "API" you created for Core Service
// (Applications -> APIs -> your API -> Identifier) — NOT the Native application's
// Client ID. Without a matching audience, Auth0 issues an opaque token instead of a
// JWT, and the backend's JWT decoder will reject every request with a 401.

export const AUTH0_DOMAIN = 'kemet-app.us.auth0.com'; // e.g. dev-abc123.us.auth0.com
export const AUTH0_CLIENT_ID = 'qPgYWcwcl3ED2P8RkOGggatrmB4f7txa';
export const AUTH0_AUDIENCE = 'https://api.kemet.local';

// Must match app.json's plugins -> react-native-auth0 -> customScheme exactly.
export const AUTH0_CUSTOM_SCHEME = 'kemet';
