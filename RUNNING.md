# Running Kemet Locally

This guide starts from an already-cloned workspace and gets the backend, database, and
mobile app running together on your Mac and phone.

## What Runs Where

- `backend/` is the Spring Boot Core Service. It runs on your Mac at
  `http://localhost:8090` and talks to local PostgreSQL.
- `backend/docker-compose.yml` starts PostgreSQL on host port `55432`.
- `mobile/` is the Expo React Native app. Because it uses `react-native-auth0`, it needs
  a native iOS/Android build and cannot run in Expo Go.
- When testing on a physical phone, the phone must call your Mac by LAN IP, not
  `localhost`.

## Prerequisites

Install or verify these first:

- Java 21: `java -version`
- Docker Desktop: `docker --version`
- Node.js and npm: `node --version` and `npm --version`
- Expo CLI through `npx`; no global install is required
- Xcode for iOS device/simulator builds, or Android Studio for Android builds
- An OpenAI API key
- An Auth0 tenant with:
  - one Auth0 API for the backend audience
  - one Auth0 Native application for the mobile app

## 1. Configure Auth0

In Auth0, create or confirm these resources:

1. Applications > APIs > your Kemet API. Its Identifier becomes `AUTH0_AUDIENCE`.
2. Applications > Applications > your Native application. Its Domain and Client ID go
   into the mobile app.
3. In the Native application's settings, set both Allowed Callback URLs and Allowed
   Logout URLs to:

   ```text
   kemet://YOUR_AUTH0_DOMAIN/ios/com.kemet.mobile/callback
   ```

   Replace `YOUR_AUTH0_DOMAIN` with the real tenant domain, for example
   `dev-example.us.auth0.com`.

Keep these values aligned across:

- `backend/.env`
- `mobile/lib/authConfig.ts`
- `mobile/app.json`

## 2. Configure The Backend

From the repo root:

```sh
cd backend
cp .env.example .env
```

Edit `backend/.env`:

```sh
OPENAI_API_KEY=your-openai-api-key
AUTH0_ISSUER_URI=https://YOUR_AUTH0_DOMAIN/
AUTH0_AUDIENCE=your-auth0-api-identifier
DB_PASSWORD=kemet_local_dev
SERVER_PORT=8090
DB_HOST_PORT=55432
```

Notes:

- `AUTH0_ISSUER_URI` usually ends with a trailing slash.
- `AUTH0_AUDIENCE` must be the Auth0 API Identifier, not the Native app Client ID.
- `DB_PASSWORD` must match the password Docker gives PostgreSQL. The default in
  `docker-compose.yml` is `kemet_local_dev`.
- `SERVER_PORT` and `DB_HOST_PORT` are offset from common defaults so Kemet can run
  alongside other local apps that use ports `8080` and `5432`.

Check `backend/src/main/resources/application.yml` before first boot and confirm
`app.openai.model` is a currently available model in your OpenAI account.

## 3. Start PostgreSQL

From `backend/`:

```sh
docker compose up -d
docker compose ps
```

Expected result: `kemet-postgres` is running and host port `55432` is published.

To stop the database without deleting data:

```sh
docker compose down
```

To delete the local database volume and start fresh:

```sh
docker compose down -v
```

## 4. Start The Backend

From `backend/`:

```sh
./mvnw spring-boot:run
```

Keep this terminal open. On first successful boot, the app seeds the 11 faculties into
PostgreSQL. The seed loader is idempotent, so restarting should not duplicate rows.

A healthy backend should show Spring Boot listening on port `8090`. Authenticated API
requests are required for the app endpoints, so a plain browser visit to most API paths
is expected to return `401`.

Run backend tests when you want to verify the service without starting the mobile app:

```sh
./mvnw test
```

## 5. Configure The Mobile App

In another terminal, from the repo root:

```sh
cd mobile
npm install
```

If Expo reports native package version mismatches, align the Expo-managed packages:

```sh
npx expo install react-native-auth0 expo-secure-store expo-local-authentication @react-native-async-storage/async-storage
```

Edit `mobile/lib/authConfig.ts`:

```ts
export const AUTH0_DOMAIN = 'YOUR_AUTH0_DOMAIN';
export const AUTH0_CLIENT_ID = 'YOUR_NATIVE_APP_CLIENT_ID';
export const AUTH0_AUDIENCE = 'YOUR_AUTH0_API_IDENTIFIER';
export const AUTH0_CUSTOM_SCHEME = 'kemet';
```

Edit `mobile/app.json` and set the `react-native-auth0` plugin `domain` to the same
Auth0 domain.

Find your Mac's Wi-Fi IP address:

```sh
ipconfig getifaddr en0
```

Copy `mobile/.env.example` to `mobile/.env` and set `EXPO_PUBLIC_API_BASE_URL`:

```sh
cp .env.example .env
```

Edit `mobile/.env`:

```sh
EXPO_PUBLIC_API_BASE_URL=http://YOUR_LAN_IP:8090
```

Replace `YOUR_LAN_IP` with the IP printed by `ipconfig`. Your phone and Mac must be on
the same network, and macOS firewall settings must allow inbound connections to the
backend.

## 6. Run The Mobile App

Because this app uses Auth0 native code, use a development build instead of Expo Go.

For iOS:

```sh
cd mobile
npx expo prebuild --clean
npx expo run:ios
```

For Android:

```sh
cd mobile
npx expo prebuild --clean
npx expo run:android
```

When prompted, choose your physical device or simulator. A physical iPhone may require
trusting your developer certificate in iOS Settings > General > VPN & Device
Management.

If you change `mobile/app.json`, native plugins, bundle identifiers, Auth0 domain, or
permissions, run `npx expo prebuild --clean` again before rebuilding.

## 7. First Launch Checklist

1. Log in through Auth0.
2. Confirm the Faculties tab loads all seeded faculties.
3. Open a faculty detail screen and verify content appears.
4. Send a Companion message and confirm the backend returns an AI response.
5. Open the Journal tab, set the PIN, and verify Face ID/Touch ID or PIN unlock works.

## Troubleshooting

### Mobile App Cannot Reach The Backend

- Confirm the backend is still running on port `8090`.
- Confirm `EXPO_PUBLIC_API_BASE_URL` in `mobile/.env` uses your Mac's current LAN IP.
- Re-run `ipconfig getifaddr en0` if you changed networks.
- Make sure phone and Mac are on the same Wi-Fi.
- Check that macOS firewall is not blocking Java.

### API Requests Return 401

- Confirm `AUTH0_AUDIENCE` is the Auth0 API Identifier.
- Confirm `AUTH0_ISSUER_URI` matches the Auth0 tenant and includes the expected scheme,
  for example `https://YOUR_AUTH0_DOMAIN/`.
- Confirm `mobile/lib/authConfig.ts` uses the same audience.
- Log out and log in again after changing Auth0 settings.

### Auth0 Callback Fails

- Confirm Auth0 Allowed Callback URLs and Allowed Logout URLs exactly match:

  ```text
  kemet://YOUR_AUTH0_DOMAIN/ios/com.kemet.mobile/callback
  ```

- Confirm `mobile/app.json` uses `scheme: "kemet"` and iOS bundle identifier
  `com.kemet.mobile`.
- Re-run `npx expo prebuild --clean` after changing native config.

### PostgreSQL Fails To Start

- Check whether something else is using port `55432`.
- Run `docker compose ps` from `backend/`.
- If the local data can be discarded, reset with `docker compose down -v` and start
  again.

### Companion Does Not Respond

- Confirm `OPENAI_API_KEY` is set in `backend/.env`.
- Confirm `app.openai.model` in `application.yml` is available to your OpenAI account.
- Check the backend terminal for the actual upstream error.

## Daily Run Sequence

Use this after everything has been configured once:

Terminal 1:

```sh
cd backend
docker compose up -d
./mvnw spring-boot:run
```

Terminal 2:

```sh
cd mobile
npx expo run:ios
```

If your Mac's LAN IP changed, update `EXPO_PUBLIC_API_BASE_URL` in `mobile/.env` before launching the mobile
app.