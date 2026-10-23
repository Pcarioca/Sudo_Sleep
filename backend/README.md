# Sudo Sleep — Backend

Java 21 · Spring Boot 4 · Spring Data JPA (Hibernate) · Spring Validation · PostgreSQL · Swagger (springdoc).

The Android app in `../android` is the client; this folder is the REST API it will talk to.
The database design is in [`docs/database.md`](../docs/database.md) (repo root).

## Run

1. Start PostgreSQL: `docker compose up -d`
2. Start the API: `./gradlew bootRun` (Windows: `gradlew.bat bootRun`)
3. Swagger UI: <http://localhost:8080/swagger-ui.html>

Database credentials default to the values in `docker-compose.yml`. Override them with the
`DB_URL`, `DB_USER` and `DB_PASSWORD` environment variables.

## Package layout (`com.sudosleep.backend`)

| Package | Role |
|---|---|
| `controller` | REST endpoints |
| `service` | business logic |
| `repository` | Spring Data JPA repositories |
| `entity` | JPA entities (tables) |
| `dto` | request/response objects |
| `mapper` | entity ↔ DTO conversion |
| `exception` | custom exceptions + global error handling |
| `config` | Spring/Swagger configuration |
