# Sudo Sleep — Timetable & Academic Planner

Subjects, weighted gradebook, weekly timetable with week-aware conflict detection.

```
Sudo_Sleep/
├── android/   Android client (Kotlin, Jetpack Compose, Material 3)
├── backend/   REST API (Java 21, Spring Boot, Spring Data JPA, PostgreSQL, Swagger)
└── docs/      Database design (Mermaid ER diagram, constraints)
```

The two apps are independent projects with their own Gradle build. They only talk to each other over HTTP/JSON:

```
Android app  ──HTTP/JSON──▶  Spring Boot API  ──JPA/Hibernate──▶  PostgreSQL
```

| Folder | Open in | Details |
|---|---|---|
| `android/` | Android Studio | [android/README.md](android/README.md) |
| `backend/` | IntelliJ IDEA | [backend/README.md](backend/README.md) |

Database design: [docs/database.md](docs/database.md)
