# Kemet

Kemet is a personal-use Neteru practice app with a Spring Boot backend and an Expo React Native mobile client. The app seeds all 11 faculties from the workbook, uses Auth0 for mobile login, calls an OpenAI-backed companion through the backend, and protects the local journal behind device biometrics with a Keychain-backed PIN fallback.

This repository is intentionally scoped for one user running the backend locally on a Mac and the mobile app on a physical device or simulator. Teacher authoring, multi-role access, offline sync, audio, notifications, hosted backend deployment, and automated companion guardrail testing are not part of the current implementation.

## Repository Layout

```text
kemet/
├── backend/     Spring Boot Core Service, PostgreSQL integration, Auth0 resource server, OpenAI companion
├── mobile/      Expo React Native app with Auth0, faculties, companion, practice, and journal screens
├── SETUP.md     Project setup notes and product-scope context
├── RUNNING.md   End-to-end local runbook
└── architecture-specification.md
```

## Tech Stack

- Backend: Java 21, Spring Boot 4.1, Spring MVC, Spring Security OAuth2 Resource Server, Spring Data JPA, PostgreSQL, Maven, JaCoCo
- Mobile: Expo 57, React 19, React Native 0.86, TypeScript 6, Jest, React Native Testing Library
- Identity and integrations: Auth0, OpenAI API, Docker Compose for local PostgreSQL

## Prerequisites

Install or verify these before running the app:

- Java 21
- Docker Desktop
- Node.js and npm
- Xcode for iOS builds or Android Studio for Android builds
- An Auth0 tenant with an API and a Native application
- An OpenAI API key

See [RUNNING.md](RUNNING.md) for the full local configuration flow.

## Quick Start

1. Configure backend environment variables:

   ```sh
   cd backend
   cp .env.example .env
   ```

   Fill in `OPENAI_API_KEY`, `AUTH0_ISSUER_URI`, `AUTH0_AUDIENCE`, `DB_PASSWORD`, `SERVER_PORT`, and `DB_HOST_PORT` in `backend/.env`.

2. Start local PostgreSQL:

   ```sh
   docker compose up -d
   ```

3. Start the backend:

   ```sh
   ./mvnw spring-boot:run
   ```

4. Install mobile dependencies:

   ```sh
   cd ../mobile
   npm install
   ```

5. Configure the mobile app:

   - Set Auth0 values in `mobile/lib/authConfig.ts`.
   - Set the Auth0 plugin domain in `mobile/app.json`.
   - Copy `mobile/.env.example` to `mobile/.env` and set `EXPO_PUBLIC_API_BASE_URL` to your Mac's LAN IP and backend port (e.g. `http://192.168.1.x:8090`). Run `ipconfig getifaddr en0` to find the IP. Physical phones cannot reach `localhost`.

6. Build and run the native mobile app:

   ```sh
   npx expo prebuild --clean
   npx expo run:ios
   ```

   Use `npx expo run:android` for Android.

Because the app uses `react-native-auth0`, it requires a development build and cannot run in Expo Go.

## Configuration Notes

- `AUTH0_AUDIENCE` must be the Auth0 API Identifier, not the Native app Client ID.
- `AUTH0_ISSUER_URI` usually ends with a trailing slash.
- Auth0 callback and logout URLs must match the scheme and bundle identifier configured in `mobile/app.json`.
- Physical phones must call the backend through the Mac's LAN IP, not `localhost`.
- Confirm `app.openai.model` in `backend/src/main/resources/application.yml` is available for your OpenAI account before first boot.

## Testing

Run backend tests from `backend/`:

```sh
./mvnw test
```

Run mobile tests from `mobile/`:

```sh
npm test
```

Run the mobile TypeScript check from `mobile/`:

```sh
npx tsc --noEmit
```

The backend JaCoCo configuration enforces 80% line coverage during Maven verification. The mobile Jest configuration enforces 80% global coverage across branches, functions, lines, and statements.

## Operational Runbook

Use [RUNNING.md](RUNNING.md) as the source of truth for local operation. It covers:

- Auth0 setup
- Backend `.env` configuration
- PostgreSQL lifecycle commands
- Backend startup and tests
- Mobile Auth0 and API configuration
- Native iOS and Android builds
- First-launch checks
- Troubleshooting for network, Auth0, API, and PostgreSQL issues

## Security And Privacy

- Do not commit `.env` files, OpenAI keys, Auth0 secrets, or personal journal data.
- The journal PIN is stored through `expo-secure-store`; journal content is stored locally on the device.
- Companion responses should remain grounded in approved seeded content. Use the manual guardrail prompts in [SETUP.md](SETUP.md) before relying on companion behavior for a faculty.

## Development Guidelines

- Keep backend behavior covered by focused Spring tests.
- Keep mobile behavior covered by Jest and React Native Testing Library tests.
- Prefer updating [RUNNING.md](RUNNING.md) when local setup or operation changes.
- Prefer updating [architecture-specification.md](architecture-specification.md) when service boundaries, data ownership, authentication, or companion behavior materially change.