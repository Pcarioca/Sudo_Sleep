# Sudo Sleep — Database design (PostgreSQL)

Target stack: Java 21, Spring Boot 4, Spring Data JPA / Hibernate, PostgreSQL 17.
Table and column names are `snake_case` (Spring Boot maps `subjectId` → `subject_id` by default).

```mermaid
erDiagram
    users ||--o{ semesters : "has"
    users ||--o{ password_reset_tokens : "requests"
    semesters ||--o{ subjects : "contains"
    semesters ||--o{ academic_events : "schedules"
    subjects ||--o{ activity_requirements : "needs"
    subjects ||--o{ grades : "has"
    subjects ||--o{ academic_events : "linked to"
    activity_requirements ||--o{ timetable_entries : "scheduled as"
    timetable_entries ||--o{ timetable_entry_cancellations : "canceled in weeks"

    users {
        bigint id PK "identity"
        varchar(255) email UK "NOT NULL"
        varchar(100) display_name "NOT NULL"
        varchar(255) password_hash "nullable (social-only accounts)"
        varchar(20) auth_provider "NOT NULL: LOCAL, GOOGLE, FACEBOOK, LINKED"
        varchar(255) google_sub UK "nullable"
        varchar(255) facebook_id UK "nullable"
        boolean has_completed_onboarding "NOT NULL, default false"
        varchar(64) default_reminder_offsets_csv "NOT NULL, default 15,60"
        timestamptz created_at "NOT NULL, default now()"
    }

    password_reset_tokens {
        bigint id PK "identity"
        bigint user_id FK "NOT NULL, ON DELETE CASCADE"
        varchar(64) token_hash "NOT NULL (SHA-256 hex of 6-digit code)"
        timestamptz expires_at "NOT NULL"
        boolean used "NOT NULL, default false"
        timestamptz created_at "NOT NULL"
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
        varchar(500) notes "nullable"
        boolean notifications_enabled "NOT NULL, default true"
        varchar(64) reminder_offsets_csv "NOT NULL, default 15"
    }

    timetable_entry_cancellations {
        bigint id PK "identity"
        bigint entry_id FK "NOT NULL, ON DELETE CASCADE"
        int iso_year_week "NOT NULL, YYYYWW e.g. 202648, UNIQUE (entry_id, iso_year_week)"
    }

    grades {
        bigint id PK "identity"
        bigint subject_id FK "NOT NULL, ON DELETE CASCADE"
        numeric(4,2) score "NOT NULL, 1..10"
        numeric(5,2) weight "NOT NULL, > 0, default 1.00"
    }

    academic_events {
        bigint id PK "identity"
        bigint semester_id FK "NOT NULL, ON DELETE CASCADE"
        bigint subject_id FK "nullable, ON DELETE SET NULL"
        varchar(150) title "NOT NULL"
        varchar(30) event_type "NOT NULL: EXAM, MIDTERM, PROJECT_DEADLINE, QUIZ, CUSTOM_EVENT"
        date event_date "NOT NULL"
        time start_time "NOT NULL"
        time end_time "NOT NULL, must be > start_time"
        varchar(100) room "nullable"
        varchar(500) notes "nullable"
        char(7) color_hex "NOT NULL, e.g. #D81B60"
        boolean notifications_enabled "NOT NULL, default true"
        varchar(64) reminder_offsets_csv "NOT NULL, default 15,60,1440"
    }
```

## Constraints not visible in the diagram

| Table | Constraint |
|---|---|
| `users` | `CHECK (auth_provider IN ('LOCAL','GOOGLE','FACEBOOK','LINKED'))` |
| `semesters` | `CHECK (weeks_count > 0)` |
| `subjects` | `CHECK (color_hex ~ '^#[0-9A-Fa-f]{6}$')` |
| `activity_requirements` | `CHECK (activity_type IN ('LECTURE','LABORATORY','SEMINAR','PRACTICAL_WORK','CUSTOM'))` |
| `activity_requirements` | `CHECK ((activity_type = 'CUSTOM') = (custom_type_name IS NOT NULL))` |
| `timetable_entries` | `CHECK (day_of_week BETWEEN 1 AND 7)` and `CHECK (end_time > start_time)` |
| `timetable_entries` | `CHECK (week_from >= 1 AND week_to >= week_from)` |
| `timetable_entries` | `CHECK (week_parity IN ('ALL','ODD','EVEN'))` |
| `timetable_entry_cancellations` | `UNIQUE (entry_id, iso_year_week)` |
| `grades` | `CHECK (score BETWEEN 1 AND 10)` and `CHECK (weight > 0)` |
| `academic_events` | `CHECK (end_time > start_time)` and `CHECK (color_hex ~ '^#[0-9A-Fa-f]{6}$')` |
| `academic_events` | `CHECK (event_type IN ('EXAM','MIDTERM','PROJECT_DEADLINE','QUIZ','CUSTOM_EVENT'))` |

Rules that span tables are checked in the service layer (Spring Validation), not in the DB:

- `week_to <= semesters.weeks_count` for the entry's semester.
- `start_date` is a Monday.
- The chosen `week_parity` has at least one week inside `[week_from, week_to]` (e.g. `ODD` with weeks 2–2 is invalid).
- The 08:00–22:00 grid and the 1–4 h duration limit for recurring `timetable_entries`.
- `academic_events.subject_id` (when non-null) belongs to the same `semester_id`.

## Indexes

PostgreSQL does not index foreign keys automatically, so we index:

- `password_reset_tokens (user_id)`
- `semesters (user_id)`
- `subjects (semester_id)`
- `activity_requirements (subject_id)`
- `grades (subject_id)`
- `timetable_entries (requirement_id)`
- `timetable_entries (day_of_week, start_time)` — used by the overlap check
- `timetable_entry_cancellations (entry_id)`
- `academic_events (semester_id)`, `academic_events (subject_id)`, `academic_events (semester_id, event_date)`

## Design notes

- **Weeks.** An entry runs in weeks `week_from..week_to` of its semester, optionally only on odd or even weeks. Week 1 is the week starting on `semesters.start_date`.
- **Current week** = `floor((today - start_date) / 7) + 1`. An entry is shown in week `w` when `week_from <= w <= week_to` and the parity matches (`ALL` always, `ODD` if `w` is odd, `EVEN` if `w` is even).
- **Per-week cancellations.** Single-week cancellations ("Canceled this week only") are stored in `timetable_entry_cancellations` keyed by `(entry_id, iso_year_week)` where `iso_year_week = isoWeekBasedYear * 100 + isoWeekNumber` (matching Android's `computeWeekEpochId()`).
- **Overlap detection is not a DB constraint.** The user may force-schedule over a conflict, so an exclusion constraint would block a valid action. Two entries conflict only if all three hold:
  1. same `day_of_week`;
  2. time overlap: `newStart < existingEnd AND newEnd > existingStart`;
  3. they share at least one week (same range *and* compatible parity).
- **A requirement can have several entries (1:N)**, e.g. a lecture held twice a week, or one laboratory in room A on odd weeks and room B on even weeks.
- **"Unscheduled" is derived**: a requirement with no rows in `timetable_entries`. There is no `is_scheduled` flag to keep in sync.
- **Averages and the semester GPA are computed**, never stored. Semester GPA = average over the subjects of one `semesters` row.

## Mapping from the Room schema (`android/`)

| Room (Android, SQLite) | PostgreSQL |
|---|---|
| `SubjectEntity.colorHex` | `subjects.color_hex` |
| `ActivityRequirementEntity.activityType: String` | `activity_type` with CHECK / Java enum |
| `ActivityRequirementEntity.isScheduled` | removed (derived) |
| `TimetableEntryEntity.subjectId` | removed (reachable via `requirement_id`) |
| `TimetableEntryEntity.day` | `day_of_week` (matches `java.time.DayOfWeek`) |
| `TimetableEntryEntity.startHour` + `duration` | `start_time` + `end_time` |
| `TimetableEntryEntity.notes`, `notificationsEnabled`, `reminderOffsetsCsv` | `timetable_entries.notes`, `notifications_enabled`, `reminder_offsets_csv` |
| `TimetableEntryEntity.canceledWeekEpochId` | `timetable_entry_cancellations.iso_year_week` |
| `GradeEntity.value: Double` | `score numeric(4,2)` (`BigDecimal`) |
| `GradeEntity.weight: Double` | `weight numeric(5,2)` (`BigDecimal`) |
| `AcademicEventEntity` | `academic_events` (`event_date` ↔ `dateEpochDay`, `start_time`/`end_time` ↔ `startHour`/`duration`) |
| `UserPreferencesManager` (auth/profile/defaults) | `users` (`email`, `display_name`, `auth_provider`, `has_completed_onboarding`, `default_reminder_offsets_csv`) |
