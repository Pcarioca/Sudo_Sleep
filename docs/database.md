# Sudo Sleep — Database design (PostgreSQL)

Target stack: Java, Spring Boot, Spring Data JPA / Hibernate, PostgreSQL.
Table and column names are `snake_case` (Spring Boot maps `subjectId` → `subject_id` by default).

```mermaid
erDiagram
    users ||--o{ semesters : "has"
    semesters ||--o{ subjects : "contains"
    subjects ||--o{ activity_requirements : "needs"
    subjects ||--o{ grades : "has"
    activity_requirements ||--o{ timetable_entries : "scheduled as"

    users {
        bigint id PK "identity"
        varchar(255) email UK "NOT NULL"
        varchar(100) display_name "NOT NULL"
        timestamptz created_at "NOT NULL, default now()"
    }

    semesters {
        bigint id PK "identity"
        bigint user_id FK "NOT NULL, ON DELETE CASCADE"
        varchar(100) name "NOT NULL, e.g. 2026/27 Sem 1"
        date start_date "NOT NULL, Monday of week 1"
        smallint weeks_count "NOT NULL, > 0, default 14"
    }

    subjects {
        bigint id PK "identity"
        bigint semester_id FK "NOT NULL, ON DELETE CASCADE"
        varchar(150) name "NOT NULL, UNIQUE (semester_id, name)"
        char(7) color_hex "NOT NULL, e.g. #0288D1"
    }

    activity_requirements {
        bigint id PK "identity"
        bigint subject_id FK "NOT NULL, ON DELETE CASCADE"
        varchar(20) activity_type "NOT NULL: LECTURE, LABORATORY, SEMINAR, PRACTICAL_WORK, CUSTOM"
        varchar(100) custom_type_name "required only when CUSTOM"
    }

    timetable_entries {
        bigint id PK "identity"
        bigint requirement_id FK "NOT NULL, ON DELETE CASCADE"
        smallint day_of_week "NOT NULL, 1=Mon .. 7=Sun"
        time start_time "NOT NULL"
        time end_time "NOT NULL, must be > start_time"
        smallint week_from "NOT NULL, >= 1"
        smallint week_to "NOT NULL, >= week_from"
        varchar(4) week_parity "NOT NULL, ALL, ODD or EVEN, default ALL"
        varchar(100) room "nullable"
        varchar(150) teacher "nullable"
    }

    grades {
        bigint id PK "identity"
        bigint subject_id FK "NOT NULL, ON DELETE CASCADE"
        numeric(4,2) score "NOT NULL, 1..10"
        numeric(5,2) weight "NOT NULL, > 0, default 1.00"
    }
```

## Constraints not visible in the diagram

| Table | Constraint |
|---|---|
| `semesters` | `CHECK (weeks_count > 0)` |
| `subjects` | `CHECK (color_hex ~ '^#[0-9A-Fa-f]{6}$')` |
| `activity_requirements` | `CHECK (activity_type IN ('LECTURE','LABORATORY','SEMINAR','PRACTICAL_WORK','CUSTOM'))` |
| `activity_requirements` | `CHECK ((activity_type = 'CUSTOM') = (custom_type_name IS NOT NULL))` |
| `timetable_entries` | `CHECK (day_of_week BETWEEN 1 AND 7)` and `CHECK (end_time > start_time)` |
| `timetable_entries` | `CHECK (week_from >= 1 AND week_to >= week_from)` |
| `timetable_entries` | `CHECK (week_parity IN ('ALL','ODD','EVEN'))` |
| `grades` | `CHECK (score BETWEEN 1 AND 10)` and `CHECK (weight > 0)` |

Rules that span tables are checked in the service layer (Spring Validation), not in the DB:

- `week_to <= semesters.weeks_count` for the entry's semester.
- `start_date` is a Monday.
- The chosen `week_parity` has at least one week inside `[week_from, week_to]` (e.g. `ODD` with weeks 2–2 is invalid).
- The 08:00–22:00 grid and the 1–4 h duration limit.

## Indexes

PostgreSQL does not index foreign keys automatically, so add:

- `semesters (user_id)`
- `subjects (semester_id)`
- `activity_requirements (subject_id)`
- `grades (subject_id)`
- `timetable_entries (requirement_id)`
- `timetable_entries (day_of_week, start_time)` — used by the overlap check

## Design notes

- **Weeks.** An entry runs in weeks `week_from..week_to` of its semester, optionally only on odd or even weeks. Week 1 is the week starting on `semesters.start_date`.
- **Current week** = `floor((today - start_date) / 7) + 1`. An entry is shown in week `w` when `week_from <= w <= week_to` and the parity matches (`ALL` always, `ODD` if `w` is odd, `EVEN` if `w` is even).
- **Overlap detection is not a DB constraint.** The user may force-schedule over a conflict, so an exclusion constraint would block a valid action. Two entries conflict only if all three hold:
  1. same `day_of_week`;
  2. time overlap: `newStart < existingEnd AND newEnd > existingStart`;
  3. they share at least one week (same range *and* compatible parity).

  Without rule 3, a lab on odd weeks and another on even weeks in the same slot would be reported as a false conflict.
- **A requirement can have several entries (1:N)**, e.g. a lecture held twice a week, or one laboratory in room A on odd weeks and room B on even weeks.
- **"Unscheduled" is derived**: a requirement with no rows in `timetable_entries`. There is no `is_scheduled` flag to keep in sync.
- **Averages and the semester GPA are computed**, never stored. Semester GPA = average over the subjects of one `semesters` row.

## Mapping from the original Room schema

| Room (Android, SQLite) | PostgreSQL |
|---|---|
| `SubjectEntity.colorHex` | `subjects.color_hex` |
| `ActivityRequirementEntity.activityType: String` | `activity_type` with CHECK / Java enum |
| `ActivityRequirementEntity.isScheduled` | removed (derived) |
| `TimetableEntryEntity.subjectId` | removed (reachable via `requirement_id`) |
| `TimetableEntryEntity.day` | `day_of_week` (matches `java.time.DayOfWeek`) |
| `TimetableEntryEntity.startHour` + `duration` | `start_time` + `end_time` |
| `GradeEntity.value: Double` | `score numeric(4,2)` (`BigDecimal`) |
| `GradeEntity.weight: Double` | `weight numeric(5,2)` (`BigDecimal`) |
| *(none)* | `users`, `semesters`, `subjects.semester_id` |
| *(none)* | `timetable_entries.week_from`, `week_to`, `week_parity` |
