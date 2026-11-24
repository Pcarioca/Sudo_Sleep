# Sudo Sleep — Backend

Java 21 · Spring Boot 4 · Spring Security (Stateless JWT + Google/Facebook OAuth verification) · Spring Data JPA (Hibernate) · Spring Validation · PostgreSQL · Swagger (springdoc).

The Android app in `../android` is the client; this folder is the REST API it talks to.
The database design is in [`../docs/database.md`](../docs/database.md).

## Configuration

Copy `.env.example` to `.env` (gitignored) and fill in your local values:

```bash
cp .env.example .env
```

Spring Boot automatically imports `backend/.env` (`spring.config.import=optional:file:.env[.properties]`).
Required variables are documented in [`.env.example`](.env.example):

- **Database:** `DB_URL`, `DB_USER`, `DB_PASSWORD` (default to `docker-compose.yml`).
- **JWT:** `JWT_SECRET` (at least 32 bytes), `JWT_ACCESS_EXPIRATION_SECONDS`, `JWT_REFRESH_EXPIRATION_SECONDS`.
- **Social OAuth verification:** `GOOGLE_WEB_CLIENT_ID`, `FACEBOOK_APP_ID`, `FACEBOOK_APP_SECRET`.
- **Forgot-password SMTP:** `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_FROM` (when `MAIL_HOST` is blank in local dev, reset codes are logged to the console instead of sent over SMTP).

## Run

1. Start PostgreSQL: `docker compose up -d`
2. Start the API: `./gradlew bootRun` (Windows: `gradlew.bat bootRun`)
3. Swagger UI: <http://localhost:8080/swagger-ui.html> (click **Authorize** and paste your Bearer `accessToken` from `/api/auth/register` or `/api/auth/login`)
4. Try the end-to-end flow in [`requests.http`](requests.http) (IntelliJ HTTP client).

Tables are created by Hibernate (`ddl-auto=update`), including CHECK constraints and `ON DELETE CASCADE`.

Tests (no database needed): `./gradlew test`

## Authentication & Authorization

- All `/api/auth/**` endpoints are public; every other `/api/**` endpoint requires `Authorization: Bearer <accessToken>`.
- Every resource (user profile, semesters, subjects, requirements, timetable entries, cancellations, grades, and academic events) is strictly scoped to the authenticated user via `OwnershipGuard` (returns `403 Forbidden` on cross-user access).
- Registration and first-time social login automatically provision a default 14-week semester starting on the Monday of the current week and return `activeSemesterId` inside `AuthResponse`.

## Endpoints

| Resource | Endpoints |
|---|---|
| Authentication (public) | `POST /api/auth/register` · `POST /api/auth/login` · `POST /api/auth/refresh` · `POST /api/auth/oauth/google` · `POST /api/auth/oauth/facebook` · `POST /api/auth/forgot-password` · `POST /api/auth/reset-password` |
| Users | `GET/PUT/DELETE /api/users/me` · `GET/PUT/DELETE /api/users/{id}` |
| Semesters | `GET/POST /api/semesters` · `GET/POST /api/users/{userId}/semesters` · `GET/PUT/DELETE /api/semesters/{id}` · `GET /api/semesters/{id}/gpa` |
| Subjects | `GET/POST /api/semesters/{semesterId}/subjects` · `GET/PUT/DELETE /api/subjects/{id}` |
| Activity requirements | `GET/POST /api/subjects/{subjectId}/requirements` · `PUT/DELETE /api/requirements/{id}` · `GET /api/semesters/{semesterId}/requirements/unscheduled` |
| Grades | `GET/POST /api/subjects/{subjectId}/grades` · `GET /api/subjects/{subjectId}/grades/summary` · `PUT/DELETE /api/grades/{id}` |
| Timetable & cancellations | `GET /api/semesters/{semesterId}/timetable?week=N` · `POST /api/semesters/{semesterId}/timetable/conflicts?excludeEntryId=&isoYearWeek=` · `POST /api/requirements/{requirementId}/timetable-entries?force=` · `GET/PUT/DELETE /api/timetable-entries/{id}` · `PUT/DELETE /api/timetable-entries/{id}/cancellations/{isoYearWeek}` |
| Academic events (Exams & Deadlines) | `GET /api/semesters/{semesterId}/events?from=&to=` · `POST /api/semesters/{semesterId}/events` · `GET/PUT/DELETE /api/events/{id}` |

### Conflicts

Two timetable entries conflict when they are on the same day, their times overlap
(`newStart < existingEnd && newEnd > existingStart`) **and** they share at least one week
(week ranges intersect and the odd/even parities allow a common week).
Creating or moving an entry that conflicts answers `409` with the colliding entries in `conflicts`;
repeat the request with `force=true` to schedule anyway.
Passing `isoYearWeek` (e.g. `202648`) to `POST /api/semesters/{semesterId}/timetable/conflicts` ignores entries canceled for that ISO week.

Errors use RFC 9457 problem JSON (`application/problem+json`): `400` validation, `401` unauthenticated, `403` forbidden, `404` missing, `409` duplicates or schedule conflicts.

## Package layout (`com.sudosleep.backend`)

`controller` → `service` → `repository` → `entity`; `dto` and `mapper` sit at the API boundary;
`security` holds JWT signing/filtering and Google/Facebook token verifiers;
`exception` holds RFC 9457 error handling and `config` holds Security/Swagger/Clock beans.
Scheduling and grade rules are plain classes (`ScheduleRules`, `GradeMath`) with unit tests.
