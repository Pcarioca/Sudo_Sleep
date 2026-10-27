# Sudo Sleep — Backend

Java 21 · Spring Boot 4 · Spring Data JPA (Hibernate) · Spring Validation · PostgreSQL · Swagger (springdoc).

The Android app in `../android` is the client; this folder is the REST API it talks to.
The database design is in [`../docs/database.md`](../docs/database.md).

## Run

1. Start PostgreSQL: `docker compose up -d`
2. Start the API: `./gradlew bootRun` (Windows: `gradlew.bat bootRun`)
3. Swagger UI: <http://localhost:8080/swagger-ui.html>
4. Try the flow in `requests.http` (IntelliJ HTTP client) or in Swagger.

Tables are created by Hibernate (`ddl-auto=update`), including the CHECK constraints and `ON DELETE CASCADE`.
Database credentials default to `docker-compose.yml`; override with `DB_URL`, `DB_USER`, `DB_PASSWORD`.

Tests (no database needed): `./gradlew test`

## Endpoints

| Resource | Endpoints |
|---|---|
| Users | `POST /api/users` · `GET/PUT/DELETE /api/users/{id}` |
| Semesters | `GET/POST /api/users/{userId}/semesters` · `GET/PUT/DELETE /api/semesters/{id}` · `GET /api/semesters/{id}/gpa` |
| Subjects | `GET/POST /api/semesters/{semesterId}/subjects` · `GET/PUT/DELETE /api/subjects/{id}` |
| Activity requirements | `GET/POST /api/subjects/{subjectId}/requirements` · `PUT/DELETE /api/requirements/{id}` · `GET /api/semesters/{semesterId}/requirements/unscheduled` |
| Grades | `GET/POST /api/subjects/{subjectId}/grades` · `GET /api/subjects/{subjectId}/grades/summary` · `PUT/DELETE /api/grades/{id}` |
| Timetable | `GET /api/semesters/{semesterId}/timetable?week=N` · `POST /api/semesters/{semesterId}/timetable/conflicts` · `POST /api/requirements/{requirementId}/timetable-entries?force=` · `GET/PUT/DELETE /api/timetable-entries/{id}` (`PUT` also takes `?force=`) |

### Conflicts

Two timetable entries conflict when they are on the same day, their times overlap
(`newStart < existingEnd && newEnd > existingStart`) **and** they share at least one week
(week ranges intersect and the odd/even parities allow a common week).
Creating or moving an entry that conflicts answers `409` with the colliding entries in `conflicts`;
repeat the request with `force=true` to schedule anyway.

Errors use RFC 9457 problem JSON: `400` validation, `404` missing, `409` duplicates or schedule conflicts.

## Not included yet

- Authentication: the user id is part of the URL. Add Spring Security before exposing this publicly.
- Integration tests against PostgreSQL (Testcontainers) and notifications / `.ics` export from the roadmap.

## Package layout (`com.sudosleep.backend`)

`controller` → `service` → `repository` → `entity`; `dto` and `mapper` sit at the API boundary;
`exception` holds the error handling and `config` the Swagger/Clock beans.
Scheduling and grade rules are plain classes (`ScheduleRules`, `GradeMath`) with unit tests.
