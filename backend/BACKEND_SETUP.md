# Backend setup: generate the project shell first, then drop these files in

I can't verify current Maven Central / Spring Boot version numbers from this sandbox
(no network access to start.spring.io or repo.maven.apache.org), so rather than hand you
a `pom.xml` with guessed version pins that might already be stale, generate the project
shell yourself at **https://start.spring.io** — it's a two-minute web form, no install
needed — with these exact settings:

- Project: **Maven**
- Language: **Java**
- Spring Boot: whatever is marked default/recommended (currently a 3.x line)
- Group: `com.kemet`
- Artifact: `core-service`
- Packaging: **Jar**
- Java: **17**
- Dependencies to add (search box on the right):
  - Spring Web
  - Spring Data JPA
  - PostgreSQL Driver
  - OAuth2 Resource Server
  - Validation
  - Lombok

Click **Generate**, unzip it, open the folder in VSCode. Then copy everything from this
`backend/` folder on top of it, matching the paths below (the `com/kemet/core/...` Java
package path will already exist from the generator — just add these files into it):

```
core-service/
├── src/main/java/com/kemet/core/
│   ├── CoreServiceApplication.java        (generator creates this already — replace it)
│   ├── domain/
│   │   ├── AppUser.java
│   │   ├── FacultyContent.java
│   │   ├── PracticeState.java
│   │   └── ChatMessage.java
│   ├── repository/
│   │   ├── AppUserRepository.java
│   │   ├── FacultyContentRepository.java
│   │   ├── PracticeStateRepository.java
│   │   └── ChatMessageRepository.java
│   ├── security/
│   │   └── SecurityConfig.java
│   ├── user/
│   │   ├── UserService.java               (find-or-create AppUser from the JWT — shared by 3 controllers)
│   │   ├── UserController.java            (GET /api/user/me, PATCH /api/user/active-faculty)
│   │   └── dto/
│   │       ├── UserProfile.java
│   │       └── SetActiveFacultyRequest.java
│   ├── companion/
│   │   ├── CompanionService.java
│   │   ├── CompanionController.java
│   │   └── dto/
│   │       ├── ChatRequest.java
│   │       └── ChatResponse.java
│   ├── faculty/
│   │   ├── FacultyController.java         (GET /api/faculty list, GET /api/faculty/{id} detail)
│   │   └── dto/
│   │       └── FacultySummary.java
│   ├── practice/
│   │   └── PracticeController.java
│   └── config/
│       └── SeedDataLoader.java
├── src/main/resources/
│   ├── application.yml                     (replace the generator's default)
│   ├── prompts/companion-system-prompt.md
│   └── seed/
│       ├── practice-framework.json         (shared breathing/protocol/script content — all 11 faculties)
│       └── faculties/
│           ├── 01-amen.json
│           ├── 02-sekher.json
│           ├── 03-ausar.json
│           ├── 04-tehuti.json
│           ├── 05-maat.json
│           ├── 06-heru-khuti.json
│           ├── 07-auset.json
│           ├── 08-heru.json
│           ├── 09-het-heru.json
│           ├── 10-sebek.json
│           └── 11-geb.json
├── docker-compose.yml
└── .env.example
```

## Why these dependency choices

- **OAuth2 Resource Server** (not the full Spring Security starter) — Core Service only
  needs to *validate* Auth0-issued JWTs on incoming requests, not manage its own login
  UI. Auth0 remains the identity provider, exactly as the architecture spec has it.
- **No Redis starter** — per the earlier architecture-spec discussion, Redis isn't load
  -bearing for this slice (one test user, no caching need yet). `docker-compose.yml`
  still includes it, commented out, so it's a one-line change to add back later without
  re-deriving the config.
- **No reactive/WebFlux dependency** — the OpenAI API call is a single outbound HTTP
  request per chat turn. `java.net.http.HttpClient` (built into the JDK since 11) handles
  that without pulling in a second HTTP stack alongside Spring MVC.

## Before running

1. `docker compose up -d` — starts PostgreSQL (and Redis if you uncomment it).
2. Copy `.env.example` to `.env` (or export the variables another way) and fill in:
   - `OPENAI_API_KEY` — from platform.openai.com (separate from any ChatGPT Plus/Pro
     subscription — this draws from your API credit balance, not your chat subscription)
   - `AUTH0_ISSUER_URI` / `AUTH0_AUDIENCE` — from your Auth0 tenant/API settings
   - `DB_PASSWORD` — match whatever you set in `docker-compose.yml`
   - `SERVER_PORT` / `DB_HOST_PORT` — defaults are `8090` and `55432` to avoid common
     local conflicts with other apps using `8080` and `5432`
3. Check `application.yml`'s `app.openai.model` value against
   https://platform.openai.com/docs/models before running — model names shift often
   enough that I didn't want to lock in a guess as if it were verified fact. Pick
   whatever's currently the cheapest general-purpose GPT-5-family model.
4. Run the app (VSCode's Spring Boot extension "Run", or `./mvnw spring-boot:run` in a
   terminal). On first boot, `SeedDataLoader` merges `seed/practice-framework.json` into
   each of the 11 files under `seed/faculties/` and inserts any that aren't already in
   the database (idempotent — safe to restart, won't duplicate or overwrite existing
   rows even if you've since edited one by hand).
5. Sanity check: `curl -H "Authorization: Bearer <token>" http://localhost:8090/api/faculty`
   should return a list of all 11 faculties; `.../api/faculty/amen` should return the
   full merged content for one of them.
