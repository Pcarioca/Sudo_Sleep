# Sudo Sleep — Full-Stack Integration Plan (`master` Frontend × `main` Backend)

> **Status:**
> - **Phase 1 (Repository Integration + Backend Gaps): COMPLETE** on branch `integrate/frontend-on-main` (see **Section 6**).
> - **Phase 2 (Android Backend Authentication, Secure Token Store & Profile Sync): COMPLETE** (see **Section 7**).
> - **Phase 3 (Full Academic Data Offline-First Sync): COMPLETE** (see **Section 8**).
> - **Phase 4 (Social Login — Google Credential Manager & Facebook Login SDK): COMPLETE** (see **Section 9**).

---

## 0. Current State Summary (Branch Audit)

### A. `main` Branch (Current Working Tree)
- **Monorepo Layout Already Established (`04949fe`, `2d65b6c`):**
  - `backend/` — Standalone **Java 21 + Spring Boot 4.1.1** Gradle project (`sudosleep-backend`).
  - `android/` — Standalone Android Gradle project containing an **older snapshot (`9a53644`)** of the mobile app prior to the `master` UI/UX overhaul.
  - `docs/database.md` — PostgreSQL ER diagram, constraints, and Room-to-PostgreSQL design notes.
  - *Working-tree note:* Untracked build leftovers (`app/`, `build/`, `.gradle/`, `local.properties`, and root `.idea/` Android module files) currently sit in the repo root from a previous `master` checkout and should be cleaned up during integration so Android Studio opens `android/` cleanly.
- **Backend Tech Stack:**
  - **Runtime & Framework:** Java 21 (Foojay Gradle toolchain), Spring Boot `4.1.1` (`spring-boot-starter-webmvc`, `spring-boot-starter-data-jpa`, `spring-boot-starter-validation`).
  - **Database:** PostgreSQL 17 via `backend/docker-compose.yml` (`container_name: sudosleep-postgres`, port `5432:5432`, DB/user/password defaults: `sudosleep`), Hibernate `ddl-auto=update`.
  - **API Documentation & Error Format:** `springdoc-openapi-starter-webmvc-ui:3.1.1` (`/swagger-ui.html`) + `GlobalExceptionHandler` returning RFC 9457 `application/problem+json` (`400`, `404`, `409` including colliding `conflicts` array on schedule overlap).
  - **Authentication Mechanism on `main`:** **None.** There is no Spring Security, no password column, no JWT filter, and no OAuth2/OIDC verifier yet; `{userId}` is passed directly in URL paths (`/api/users/{userId}/semesters`).
  - **How Backend Runs Locally:**
    1. `cd backend && docker compose up -d` (starts PostgreSQL 17 on `localhost:5432`).
    2. `cd backend && ./gradlew bootRun` (`gradlew.bat bootRun` on Windows) — starts Spring Boot on `http://localhost:8080` (reachable from Android Emulator via `http://10.0.2.2:8080/`).
    3. Unit tests (no DB required): `cd backend && ./gradlew test`.

### B. `master` Branch (`21a8839` — Overhauled Android Frontend)
- **Language & Stack Clarification:** Although located under standard `app/src/main/java/com/example/sudosleep/...` source directories, the entire Android frontend on `master` is written in **Kotlin (2.2.10) + Jetpack Compose (Material 3)**, compiled with **AGP 9.5.1**, `minSdk = 29`, `targetSdk = 36`, `compileSdk = 37`.
- **Networking Libraries Already in `gradle/libs.versions.toml` & `app/build.gradle.kts`:** `Retrofit 2.12.0`, `Moshi 1.15.2` (`converter-moshi` + KSP `moshi-kotlin-codegen`), and `OkHttp 4.10.0` (`logging-interceptor`) are already declared as dependencies, though no Retrofit API interfaces exist yet.
- **Persistence & Architecture Today:** 100% offline-first local storage using **Room v2** (`AppDatabase`: 5 tables) + **`SharedPreferences`** (`UserPreferencesManager`) + MVVM (`SudoSleepRepository`, `AuthManager` stub, `SubjectViewModel`, `TimetableViewModel`).

---

## 1. Target Folder Layout on `main`

To keep the Android app and Spring Boot API cleanly decoupled while coexisting in one repository, all frontend files from `master:<root>` map into `main:android/` (replacing the stale `9a53644` snapshot in `android/`), while `backend/` and `docs/` remain at the repository root.

```text
SudoSleep/                                      # Repository root
├── .gitignore                                  # Root ignore rules (OS junk, stray root .gradle/build/.env)
├── README.md                                   # Monorepo overview & quick-start links
├── INTEGRATION_PLAN.md                         # This architectural integration plan
├── docs/
│   └── database.md                             # PostgreSQL ER diagram & schema documentation (update w/ Auth, Exams, Cancellations)
│
├── backend/                                    # Standalone Spring Boot 4 / Java 21 Gradle project (open in IntelliJ IDEA)
│   ├── .gitignore
│   ├── .env.example                            # Template of required env vars (committed; real .env gitignored)
│   ├── README.md
│   ├── build.gradle                            # Add spring-security, jjwt (or spring-oauth2-resource-server), google-api-client, mail
│   ├── settings.gradle
│   ├── docker-compose.yml                      # PostgreSQL 17 local container (port 5432)
│   ├── gradlew / gradlew.bat / gradle/
│   ├── requests.http                           # HTTP smoke-test suite
│   └── src/
│       ├── main/
│       │   ├── resources/
│       │   │   └── application.properties      # DB, JWT, Google/Facebook OAuth, SMTP env-backed properties
│       │   └── java/com/sudosleep/backend/
│       │       ├── BackendApplication.java
│       │       ├── config/                     # TimeConfig, OpenApiConfig, + SecurityConfig, CorsConfig
│       │       ├── security/                   # [NEW] JwtService, JwtAuthFilter, CurrentUser principal, Google/Facebook token verifiers
│       │       ├── controller/                 # User, Semester, Subject, ActivityRequirement, Timetable, Grade
│       │       │                               # + [NEW] AuthController, AcademicEventController
│       │       ├── service/                    # User, Semester, Subject, ActivityRequirement, Timetable, Grade, GradeMath,
│       │       │                               # ScheduleRules, RequirementRules + [NEW] AuthService, PasswordResetService, AcademicEventService
│       │       ├── repository/                 # User, Semester, Subject, ActivityRequirement, TimetableEntry, Grade
│       │       │                               # + [NEW] PasswordResetTokenRepository, TimetableCancellationRepository, AcademicEventRepository
│       │       ├── entity/                     # User (extended), Semester, Subject, ActivityRequirement, ActivityType,
│       │       │                               # TimetableEntry (extended), WeekParity, Grade
│       │       │                               # + [NEW] AuthProvider, PasswordResetToken, TimetableCancellation, AcademicEvent, AcademicEventType
│       │       ├── dto/                        # Existing request/response records + [NEW] Auth*, Profile*, AcademicEvent*, Cancellation* DTOs
│       │       ├── mapper/                     # Existing mappers + [NEW] AcademicEventMapper
│       │       └── exception/                  # GlobalExceptionHandler (+ 401/403 handlers), BadRequest, Conflict, ResourceNotFound, ScheduleConflict
│       └── test/java/com/sudosleep/backend/
│           └── service/                        # GradeMathTest, ScheduleRulesTest, + Auth & AcademicEvent unit tests
│
└── android/                                    # Standalone Android Gradle project (open `SudoSleep/android` in Android Studio)
    ├── .gitignore                              # Ignores build/, .gradle/, local.properties, *.jks
    ├── README.md                               # Ported from master:README.md + backend connection instructions
    ├── build.gradle.kts                        # Ported from master:build.gradle.kts
    ├── settings.gradle.kts                     # rootProject.name = "Sudo Sleep", include(":app")
    ├── gradle.properties
    ├── gradlew / gradlew.bat / gradle/
    │   └── libs.versions.toml                  # Ported from master + Credential Manager / Google ID / Facebook Login / Security-Crypto
    └── app/
        ├── .gitignore
        ├── build.gradle.kts                    # Ported from master + BuildConfig API_BASE_URL + social auth SDKs
        └── src/
            ├── main/
            │   ├── AndroidManifest.xml         # Permissions, ReminderReceiver, FacebookActivity/meta-data, debug networkSecurityConfig
            │   ├── keepRules/rules.keep
            │   ├── res/                        # Drawables, mipmaps, values (strings.xml, colors.xml, themes.xml), xml/
            │   └── java/com/example/sudosleep/
            │       ├── MainActivity.kt
            │       ├── data/
            │       │   ├── auth/
            │       │   │   ├── AuthManager.kt              # Upgraded from stub -> real API + Google CredentialManager + Facebook LoginManager
            │       │   │   └── TokenStore.kt               # [NEW] Encrypted JWT + userId + activeSemesterId storage
            │       │   ├── remote/                         # [NEW] Retrofit + OkHttp + Moshi networking layer
            │       │   │   ├── ApiClient.kt                # OkHttpClient w/ AuthInterceptor & TokenAuthenticator
            │       │   │   ├── AuthApi.kt                  # /api/auth/* & /api/users/me endpoints
            │       │   │   ├── AcademicApi.kt              # Semesters, Subjects, Requirements, Timetable, Cancellations, Grades, AcademicEvents
            │       │   │   └── dto/                        # Network DTOs & Room <-> API mappers
            │       │   ├── local/                          # Room v3 offline cache (synced with backend IDs)
            │       │   │   ├── AppDatabase.kt
            │       │   │   ├── entity/                     # SubjectEntity, ActivityRequirementEntity, TimetableEntryEntity, GradeEntity, AcademicEventEntity
            │       │   │   ├── dao/                        # SubjectDao, RequirementDao, TimetableDao, GradeDao, AcademicEventDao
            │       │   │   └── pojo/                       # SubjectWithDetails, TimetableEntryWithSubject, UnscheduledRequirementWithSubject, AcademicEventWithSubject
            │       │   ├── preferences/
            │       │   │   └── UserPreferencesManager.kt   # Theme, dark mode, onboarding, notification defaults, zoom scale
            │       │   └── repository/
            │       │       └── SudoSleepRepository.kt      # Offline-first sync orchestrating Remote API + Room cache
            │       ├── notifications/
            │       │   ├── NotificationHelper.kt           # AlarmManager & NotificationChannels
            │       │   └── ReminderReceiver.kt             # BroadcastReceiver firing class & exam alerts
            │       ├── util/
            │       │   └── TimetableExporter.kt            # 1080x2400 PNG Wallpaper & A4 Landscape PDF generator
            │       └── ui/
            │           ├── MainNavigationScreen.kt         # Auth gate, Navigation Drawer (User Profile), Bottom Bar, Onboarding trigger
            │           ├── auth/
            │           │   └── LoginScreen.kt              # Extended w/ Sign Up (Name+Email+Password), Forgot Password, Google & Facebook buttons
            │           ├── exams/
            │           │   └── ExamsAndDeadlinesScreen.kt  # Exam radar, upcoming/past lists, AddOrEditExamBottomSheet
            │           ├── export/
            │           │   └── TimetableExportScreen.kt    # Wallpaper preview/apply/save/share & PDF export
            │           ├── onboarding/
            │           │   └── OnboardingOverlay.kt        # 4-step OnboardingCoachMarkOverlay
            │           ├── settings/
            │           │   └── SettingsAndNotificationsScreen.kt
            │           ├── subject/
            │           │   ├── SubjectListScreen.kt
            │           │   ├── SubjectDetailScreen.kt
            │           │   ├── AddSubjectBottomSheet.kt
            │           │   ├── GradeDialog.kt
            │           │   └── ColorPalette.kt
            │           ├── timetable/
            │           │   ├── TimetableMainScreen.kt
            │           │   ├── TimetableGridCanvas.kt
            │           │   ├── TimetableWeekAndMonthViews.kt
            │           │   ├── DaySelector.kt
            │           │   ├── UnscheduledWarningBanner.kt
            │           │   ├── ScheduleEntryBottomSheet.kt
            │           │   ├── TimetableEntryDetailDialog.kt
            │           │   └── ConflictWarningDialog.kt
            │           ├── theme/                          # Color.kt, Theme.kt, Type.kt
            │           └── viewmodel/
            │               ├── SubjectViewModel.kt
            │               └── TimetableViewModel.kt
            ├── test/java/com/example/sudosleep/            # ConflictDetectionTest, GradeCalculationTest, ScheduleUpgradesTest
            └── androidTest/java/com/example/sudosleep/     # ExampleInstrumentedTest
```

> **Workspace Cleanup Note:** When migrating `master` into `android/`, remove the untracked root `app/`, `build/`, `.gradle/`, and root Android `.idea/modules` leftovers, and open **`SudoSleep/android`** as the Android Studio project root (matching `main`'s `README.md`).

---

## 2. Frontend Inventory (`master` Branch Mapping)

### Models, Entities & Relational POJOs
| Frontend Class (`com.example.sudosleep`) | Layer | Fields & Role on `master` |
|---|---|---|
| `SubjectEntity` | Room `subjects` | `id`, `name`, `colorHex` |
| `ActivityRequirementEntity` | Room `activity_requirements` | `id`, `subjectId` (FK), `activityType` (`String`), `customTypeName`, `isScheduled` (`Boolean`) |
| `TimetableEntryEntity` | Room `timetable_entries` | `id`, `subjectId` (FK), `requirementId` (FK), `day` (`1..7`), `startHour` (`8..21`), `duration` (`1..4`), `room`, `teacher`, `notes`, `canceledWeekEpochId` (`Int?`, e.g. `202648`), `notificationsEnabled`, `reminderOffsetsCsv` |
| `GradeEntity` | Room `grades` | `id`, `subjectId` (FK), `value` (`Double`), `weight` (`Double = 1.0`) |
| `AcademicEventEntity` | Room `academic_events` | `id`, `subjectId?` (nullable FK), `title`, `eventType` (`Exam`, `Midterm`, `Project Deadline`, `Quiz`, `Custom Event`), `dateEpochDay`, `startHour`, `duration`, `room`, `notes`, `colorHex`, `notificationsEnabled`, `reminderOffsetsCsv` |
| `SubjectWithDetails` | Room POJO | `SubjectEntity` + `List<ActivityRequirementEntity>` + `List<GradeEntity>` |
| `TimetableEntryWithSubject` | Room POJO | `TimetableEntryEntity` + `SubjectEntity` + `ActivityRequirementEntity` |
| `UnscheduledRequirementWithSubject` | Room POJO | `ActivityRequirementEntity` + `SubjectEntity` |
| `AcademicEventWithSubject` | Room POJO | `AcademicEventEntity` + `SubjectEntity?` |
| `UserPreferencesState` / `UserPreferencesManager` | SharedPreferences | `isLoggedIn`, `userDisplayName`, `userEmail`, `authProvider`, `hasCompletedOnboarding`, `globalNotificationsEnabled`, `defaultReminderOffsetsCsv` (`"15,60"`), `colorTheme` (6 themes), `darkModeConfig`, `hourHeightDp` (`38f..115f`) |
| `AuthManager` | Auth Stub | `loginWithEmailOrUsername(identifier, password)`, `continueWithGoogle(mockAccountName, mockEmail)`, `logout()`. *(Currently accepts any non-blank credentials locally; lacks Sign Up with Name, Forgot Password, and Facebook Login methods.)* |
| `SudoSleepRepository` | Local Repository | Central data layer (`DataRepository` equivalent) exposing `Flow`s for subjects, timetable entries, unscheduled requirements, grades, and academic events, plus conflict checking (`checkOverlapConflict`), quick tap-to-add (`scheduleQuickActivity`), week cancellation (`setEntryCanceledThisWeek`), GPA math, and demo seeding (`seedInitialDataIfEmpty`). |

### Screens & Feature Flows on `master`
1. **`LoginScreen` (`ui/auth/LoginScreen.kt`):** Email/Username + Password login form and *Continue with Google* button. *(Frontend UI gap to add during implementation: Sign Up mode with Display Name field, Forgot Password sheet/screen, and Continue with Facebook button.)*
2. **`MainNavigationScreen` (`ui/MainNavigationScreen.kt`):** Auth gate, slide-out Hamburger `ModalNavigationDrawer` (displays User Profile `displayName`, `email`, Semester GPA pill, active theme pill, Tutorial replay, Log Out), 4-tab `NavigationBar` (`Timetable`, `Subjects`, `Exams`, `Export`), reactive `AlarmManager` sync, and predictive `BackHandler`.
3. **`TimetableMainScreen`, `TimetableGridCanvas`, `TimetableWeekAndMonthViews` (`ui/timetable/`):** Day/Week/Month switcher, pinch-to-zoom `08:00–22:00` canvas, horizontal day pager, *"Next up today"* & *"Exam Radar"* banners, `UnscheduledWarningBanner`, direct tap-to-add (`ScheduleEntryBottomSheet` supporting Quick Class, Unscheduled Queue, and Exam creation), `ConflictWarningDialog` (`force` override), and `TimetableEntryDetailDialog` (quick view, *"Canceled this week only"* switch, edit mode, safe deletion).
4. **`SubjectListScreen`, `SubjectDetailScreen`, `AddSubjectBottomSheet`, `GradeDialog` (`ui/subject/`):** Semester GPA hero card, 12-color subject picker, standard + custom requirement chips, weighted gradebook, safe subject & grade deletion.
5. **`ExamsAndDeadlinesScreen` (`ui/exams/ExamsAndDeadlinesScreen.kt`):** Countdown radar card, upcoming vs completed/past events lists, `AddOrEditExamBottomSheet` with Material 3 `DatePickerDialog`, optional linked subject, and multi-reminder chips.
6. **`TimetableExportScreen` & `TimetableExporter` (`ui/export/`, `util/`):** Client-side `Canvas` & `PdfDocument` rendering of `1080×2400` lock-screen PNG wallpapers (3 styles, `WallpaperManager` apply, `MediaStore.Images` save/share) and A4 Landscape PDFs (`MediaStore.Downloads`).
7. **`SettingsAndNotificationsScreen`, `NotificationHelper`, `ReminderReceiver` (`ui/settings/`, `notifications/`):** Global reminder switch, default multi-offset chips (`5m..1440m`), instant test notification button, 6 cute color themes, Light/Dark/System mode, and tutorial replay.
8. **`OnboardingCoachMarkOverlay` (`ui/onboarding/OnboardingOverlay.kt`):** 4-step spotlight walkthrough stored via `hasCompletedOnboarding`.

---

## 3. Data & Endpoint Mapping Table

> **Important Structural Bridge (`Semester` Scope):**
> On `main`, the backend organizes academic data under `users -> semesters -> subjects -> {activity_requirements -> timetable_entries, grades}`. On `master`, the frontend has no `Semester` selector UI yet and queries `subjects` globally.
> **Integration Strategy:** Automatically create/fetch a default active `Semester` (e.g., current academic term starting on the nearest Monday, `weeksCount = 14`) upon Sign Up / Login, store `activeSemesterId` locally in `TokenStore`, and route all semester-scoped calls through `/api/semesters/{activeSemesterId}/...` (while optionally exposing a semester switcher in the Drawer later).

| Frontend Feature / Screen | Frontend Model & Action (`master`) | Existing Backend Endpoint(s) on `main` | Backend Table(s) on `main` | Mapping Notes & Required Transformations |
|---|---|---|---|---|
| **Sign Up with Name** | `LoginScreen` *(add Sign Up tab)* -> `AuthManager.signUp(displayName, email, password)` | ⚠️ *Partial:* `POST /api/users` (`UserRequest { email, displayName }`) — **no password or JWT** | `users` (`id`, `email`, `display_name`, `created_at`) | **GAP:** Replace open `POST /api/users` with `POST /api/auth/register` accepting `{ displayName, email, password }`, hashing password with BCrypt, auto-creating default `Semester`, and returning `{ accessToken, refreshToken, user, activeSemesterId }`. |
| **Login (Email + Password)** | `LoginScreen` -> `AuthManager.loginWithEmailOrUsername(email, password)` | ❌ **MISSING** | `users` *(missing `password_hash`)* | **GAP:** Add `POST /api/auth/login` (`{ email, password }`) returning JWT tokens + `UserResponse` + `activeSemesterId`. Update frontend label/validation from arbitrary username to email (or add optional `username` column). |
| **Google Sign-In** | `LoginScreen` -> `AuthManager.continueWithGoogle(...)` | ❌ **MISSING** | `users` *(missing provider / `google_sub`)* | **GAP:** Use Android `CredentialManager` (`GetGoogleIdOption`) to obtain a Google `idToken`, send to `POST /api/auth/oauth/google` (`{ idToken }`), verify signature/audience on backend, upsert `User`, return backend JWT. |
| **Facebook Login** | `LoginScreen` *(add Facebook button)* -> `AuthManager.continueWithFacebook(...)` | ❌ **MISSING** | `users` *(missing `facebook_id`)* | **GAP:** Use Facebook Android SDK `LoginManager` (`email`, `public_profile`) to get `accessToken`, send to `POST /api/auth/oauth/facebook` (`{ accessToken }`), verify via Meta Graph `debug_token` + `/me?fields=id,name,email`, upsert `User`, return backend JWT. |
| **Forgot / Reset Password** | `LoginScreen` *(add Forgot Password flow)* -> `AuthManager.requestPasswordReset(email)` & `confirmPasswordReset(token, newPassword)` | ❌ **MISSING** | ❌ **MISSING** (`password_reset_tokens`) | **GAP:** Add `POST /api/auth/forgot-password` (sends 6-digit OTP or signed reset token via SMTP) and `POST /api/auth/reset-password` (`{ email, codeOrToken, newPassword }`). |
| **User Profile (Drawer Header & Account Management)** | `MainNavigationScreen` drawer header (`userDisplayName`, `userEmail`, Logout) | `GET /api/users/{id}`<br>`PUT /api/users/{id}`<br>`DELETE /api/users/{id}` | `users` | Exists! Add `/api/users/me` (`GET`/`PUT`/`DELETE`) resolved from JWT `@AuthenticationPrincipal` so clients cannot spoof `{id}` in URLs. |
| **Active Semester Context & Semester GPA** | `SubjectListScreen` & Drawer GPA badge (`calculateOverallGpa()`) | `GET /api/users/{userId}/semesters`<br>`POST /api/users/{userId}/semesters`<br>`GET /api/semesters/{id}`<br>`PUT /api/semesters/{id}`<br>`DELETE /api/semesters/{id}`<br>`GET /api/semesters/{id}/gpa` | `semesters` | Exists! Note: `SemesterService` enforces `startDate.getDayOfWeek() == DayOfWeek.MONDAY` and `weeksCount` in `1..52` (default `14`). `SemesterGpaResponse.gpa` matches `SudoSleepRepository.calculateOverallGpa` (mean of weighted subject averages, 2 decimals). |
| **Subjects (List, Create w/ Components, Rename/Color, Delete)** | `SubjectListScreen`, `AddSubjectBottomSheet`, `SubjectDetailScreen` (`SubjectEntity`) | `GET /api/semesters/{semesterId}/subjects`<br>`POST /api/semesters/{semesterId}/subjects`<br>`GET /api/subjects/{id}`<br>`PUT /api/subjects/{id}`<br>`DELETE /api/subjects/{id}` | `subjects`, `activity_requirements` | Exists! `SubjectRequest` already accepts nested `requirements`. **Enum mapping needed:** Frontend `"Lecture"`, `"Laboratory"`, `"Seminar"`, `"Practical Work"` -> `LECTURE`, `LABORATORY`, `SEMINAR`, `PRACTICAL_WORK`; any other label (e.g. `"Custom"`, `"Study Session"`, `"Project Studio"`) -> `activityType = CUSTOM` + `customTypeName = <label>`. |
| **Activity Requirements & Unscheduled Banner** | `UnscheduledWarningBanner`, `SubjectDetailScreen` (`ActivityRequirementEntity`) | `GET /api/subjects/{subjectId}/requirements`<br>`POST /api/subjects/{subjectId}/requirements`<br>`PUT /api/requirements/{id}`<br>`DELETE /api/requirements/{id}`<br>`GET /api/semesters/{semesterId}/requirements/unscheduled` | `activity_requirements` | Exists! Backend derives `scheduled` dynamically (`entries.existsByRequirementId`) instead of storing `isScheduled`. Frontend Room cache updates `isScheduled` from `ActivityRequirementResponse.scheduled`. |
| **Timetable Entries & Overlap Conflict Detection** | `TimetableMainScreen`, `ScheduleEntryBottomSheet` (Modes 0 & 1), `TimetableEntryDetailDialog`, `ConflictWarningDialog` (`TimetableEntryEntity`) | `GET /api/semesters/{semesterId}/timetable?week=N`<br>`POST /api/semesters/{semesterId}/timetable/conflicts`<br>`POST /api/requirements/{requirementId}/timetable-entries?force=`<br>`GET /api/timetable-entries/{id}`<br>`PUT /api/timetable-entries/{id}?force=`<br>`DELETE /api/timetable-entries/{id}` | `timetable_entries` | **Core CRUD & 409 Conflict flow exist!**<br>**Transformations:** Frontend `day` (`1..7`) <-> `dayOfWeek`; `startHour` (`8..21`) + `duration` (`1..4`) <-> `startTime` (`"08:00"`) + `endTime` (`"10:00"`). Quick Tap-to-Add (`scheduleQuickActivity`) chains `POST /subjects` (if new) -> `POST /requirements` (if existing subject) -> `POST /timetable-entries`.<br>**GAP on `timetable_entries`:** Backend lacks `notes`, `notificationsEnabled`, and `reminderOffsetsCsv` columns. |
| **"Canceled This Week Only" Toggle** | `TimetableEntryDetailDialog` switch -> `setEntryCanceledThisWeek(entryId, canceled)` (`canceledWeekEpochId`) | ❌ **MISSING** | ❌ **MISSING** *(no cancellation column/table on `main`)* | **GAP:** Add `timetable_cancellations` table (`entry_id`, `iso_year_week` or `semester_week` / `canceled_date`) + `PUT` & `DELETE /api/timetable-entries/{id}/cancellations/{isoYearWeek}` (and include `canceledIsoWeeks` / `canceledThisWeek` in `TimetableEntryResponse` and conflict checks). |
| **Grades & Subject Averages** | `SubjectDetailScreen`, `AddGradeDialog` (`GradeEntity(value, weight)`) | `GET /api/subjects/{subjectId}/grades`<br>`GET /api/subjects/{subjectId}/grades/summary`<br>`POST /api/subjects/{subjectId}/grades`<br>`PUT /api/grades/{id}`<br>`DELETE /api/grades/{id}` | `grades` | Exists! `GradeEntity.value` <-> `GradeRequest.score` (`BigDecimal`), `weight` <-> `weight`. **Validation mismatch:** Frontend `AddGradeDialog` allows `0.0..10.0`, while backend enforces `@DecimalMin("1.00") @DecimalMax("10.00")` and DB `CHECK (score between 1 and 10)` — align frontend validation to `1.0..10.0`. |
| **Exams, Midterms & Deadlines** | `ExamsAndDeadlinesScreen`, `AddOrEditExamBottomSheet`, `ScheduleEntryBottomSheet` (Mode 2), `TimetableMonthView` (`AcademicEventEntity`) | ❌ **MISSING** | ❌ **MISSING** | **GAP:** Backend has zero support for date-specific academic events. Add `academic_events` table + CRUD endpoints under `/api/semesters/{semesterId}/events` and `/api/events/{id}`. |
| **Smart Local Notifications & Multi-Reminders** | `SettingsAndNotificationsScreen`, `NotificationHelper`, `ReminderReceiver` | ⚠️ *Client-scheduled (`AlarmManager`), metadata unsynced* | *(Needs columns on `timetable_entries`, `academic_events`, `users`)* | Alarm firing stays native on Android (`AlarmManager` + `BroadcastReceiver`), **but** `notificationsEnabled` and `reminderOffsetsCsv` on entries/events (and user default offsets) must be persisted on backend so reminders restore after login on a new device. |
| **Wallpaper (PNG) & Print PDF Export** | `TimetableExportScreen`, `TimetableExporter` | *None required (Client-side rendering)* | Reads synced `timetable_entries` + `users.display_name` | Purely on-device Android `Canvas` / `PdfDocument` / `WallpaperManager` / `MediaStore` feature using the synced timetable state and authenticated user's `displayName`. |
| **Onboarding Tutorial & Cute Themes** | `OnboardingCoachMarkOverlay`, `SettingsAndNotificationsScreen` (`UserPreferencesManager`) | *Optional user-preferences sync (`PUT /api/users/me/preferences`)* | Local `SharedPreferences` *(optional columns on `users`)* | Works locally via `UserPreferencesManager`; optionally store `hasCompletedOnboarding` on `users` so returning users on a fresh install are not shown onboarding twice. |

---

## 4. Backend Gaps List (Missing on `main`)

Below is every backend addition and modification required on `main` (`backend/`) to support 100% of `master`'s frontend features plus real authentication (Email/Password, Forgot Password, Google Sign-In, and Facebook Login):

### Gap 1: Spring Security & Stateless JWT Authentication
- **Missing Dependencies (`backend/build.gradle`):**
  - `org.springframework.boot:spring-boot-starter-security`
  - JWT library (`io.jsonwebtoken:jjwt-api` / `jjwt-impl` / `jjwt-jackson:0.12.6` or `org.springframework.boot:spring-boot-starter-oauth2-resource-server` with Nimbus `JwtEncoder`/`JwtDecoder`).
- **Missing Infrastructure:**
  - `SecurityConfig`: Stateless session policy (`SessionCreationPolicy.STATELESS`), CSRF disabled for Bearer API, permit `/api/auth/**`, `/v3/api-docs/**`, `/swagger-ui/**`, `/swagger-ui.html`, require `Authenticated` on `/api/**`.
  - `JwtService` & `JwtAuthenticationFilter`: Issue short-lived Access Tokens (e.g., 15–60 min) + Refresh Tokens (e.g., 30 days) encoding `userId` and `email`.
  - **Ownership / Authorization Guard:** Today, any caller knowing `{semesterId}`, `{subjectId}`, `{requirementId}`, `{entryId}`, or `{gradeId}` can read/modify another user's rows! Every service method must verify `resource.user.id == authenticatedUserId` (returning `404` or `403` on mismatch) and expose `/api/users/me` + `/api/users/me/semesters`.
  - `GlobalExceptionHandler`: Add handlers for `AuthenticationException` (`401 Unauthorized`) and `AccessDeniedException` (`403 Forbidden`) using RFC 9457 `ProblemDetail`.

### Gap 2: User Credentials, Social Identity Linking & Password Reset
- **`users` Table Schema Extensions (`User.java`):**
  - `password_hash VARCHAR(255) NULL` *(nullable so Google/Facebook-only accounts don't require a local password)*.
  - `google_sub VARCHAR(255) UNIQUE NULL` *(immutable Google `sub` claim)*.
  - `facebook_id VARCHAR(255) UNIQUE NULL` *(app-scoped Facebook user `id`)*.
  - `auth_provider VARCHAR(20) NOT NULL DEFAULT 'LOCAL'` (`LOCAL`, `GOOGLE`, `FACEBOOK`, `LINKED`).
- **New Auth Endpoints (`AuthController` under `/api/auth`):**
  1. `POST /api/auth/register` — `{ displayName, email, password }` -> validates password strength (min 8 chars), hashes with `BCryptPasswordEncoder`, creates `User`, **auto-provisions default `Semester`** (Monday of current week, 14 weeks), returns `AuthResponse { accessToken, refreshToken, user, activeSemesterId }`.
  2. `POST /api/auth/login` — `{ email, password }` -> verifies BCrypt hash, ensures user has at least one `Semester`, returns `AuthResponse`.
  3. `POST /api/auth/refresh` — `{ refreshToken }` -> rotates/re-issues `accessToken`.
  4. `POST /api/auth/forgot-password` — `{ email }` -> generates a cryptographically random 6-digit OTP / token (hashed in a new `password_reset_tokens` table with `expires_at = now() + 15m` and `used = false`) and emails it via `spring-boot-starter-mail` (always returns `200 OK` to prevent email enumeration).
  5. `POST /api/auth/reset-password` — `{ email, token, newPassword }` -> validates non-expired unused token, updates `users.password_hash`, marks token used.

### Gap 3: Google Sign-In & Facebook Login Server-Side Token Verification
- **Google ID Token Verification (`POST /api/auth/oauth/google`):**
  - Dependency: `com.google.api-client:google-api-client` (`GoogleIdTokenVerifier`) or Spring's `NimbusJwtDecoder` against Google's JWKS (`https://www.googleapis.com/oauth2/v3/certs`).
  - Verification rules: Verify signature, `iss` in `["https://accounts.google.com", "accounts.google.com"]`, `exp`, and **`aud` == Backend's Google Web Client ID (`GOOGLE_WEB_CLIENT_ID`)**.
  - Account linking: Match by `google_sub` first; if not found, match verified `email` (`email_verified == true`) and link `google_sub` to the existing `User`; otherwise create new `User` (`displayName = name`, `email = email`) + default `Semester`.
- **Facebook Access Token Verification (`POST /api/auth/oauth/facebook`):**
  - Verification via Meta Graph API using Spring `RestClient`:
    1. Call `GET https://graph.facebook.com/debug_token?input_token={userAccessToken}&access_token={FACEBOOK_APP_ID}|{FACEBOOK_APP_SECRET}` and verify `data.is_valid == true` and `data.app_id == FACEBOOK_APP_ID`.
    2. Call `GET https://graph.facebook.com/me?fields=id,name,email&access_token={userAccessToken}` (with `appsecret_proof` HMAC-SHA256).
  - Account linking: Match by `facebook_id` first; else match by `email` (if provided by Facebook; handle edge case where a Facebook account has no email by returning `400` asking for email or generating a Completing-Sign-Up step) -> upsert `User` + default `Semester` -> return backend JWT `AuthResponse`.

### Gap 4: Exams, Midterms & Deadlines (`academic_events` Table & CRUD API)
- **Completely missing on `main`.** Needed by `ExamsAndDeadlinesScreen`, `ScheduleEntryBottomSheet` (Mode 2), `TimetableWeekView`, `TimetableMonthView`, and `NotificationHelper`.
- **New Entity & Table `academic_events`:**
  - `id` (`BIGINT IDENTITY PK`)
  - `semester_id` (`BIGINT NOT NULL FK -> semesters(id) ON DELETE CASCADE`)
  - `subject_id` (`BIGINT NULL FK -> subjects(id) ON DELETE SET NULL` or `CASCADE` — nullable for *"Standalone / General Event"*)
  - `title` (`VARCHAR(150) NOT NULL`)
  - `event_type` (`VARCHAR(30) NOT NULL` — enum `EXAM`, `MIDTERM`, `PROJECT_DEADLINE`, `QUIZ`, `CUSTOM_EVENT`)
  - `event_date` (`LocalDate NOT NULL` — maps cleanly to/from frontend `dateEpochDay`)
  - `start_time` (`LocalTime NOT NULL`, `08:00..21:00`) & `end_time` (`LocalTime NOT NULL`, `<= 22:00`, max 4h)
  - `room` (`VARCHAR(100) NULL`), `notes` (`VARCHAR(500) NULL`), `color_hex` (`CHAR(7) NOT NULL`, `^#[0-9A-Fa-f]{6}$`)
  - `notifications_enabled` (`BOOLEAN NOT NULL DEFAULT TRUE`), `reminder_offsets_csv` (`VARCHAR(64) NOT NULL DEFAULT '15,60,1440'`)
- **New Endpoints (`AcademicEventController`):**
  - `GET /api/semesters/{semesterId}/events?from=YYYY-MM-DD&to=YYYY-MM-DD`
  - `POST /api/semesters/{semesterId}/events`
  - `GET /api/events/{id}`
  - `PUT /api/events/{id}`
  - `DELETE /api/events/{id}`

### Gap 5: Timetable Entry Missing Fields & "Canceled This Week" Persistence
- **Missing Columns on `timetable_entries` (`TimetableEntry.java`, `TimetableEntryRequest.java`, `TimetableEntryResponse.java`):**
  - `notes VARCHAR(500) NULL` (used in `ScheduleEntryBottomSheet` and `TimetableEntryDetailDialog`).
  - `notifications_enabled BOOLEAN NOT NULL DEFAULT TRUE`.
  - `reminder_offsets_csv VARCHAR(64) NOT NULL DEFAULT '15'`.
- **Missing "Canceled This Week Only" Persistence:**
  - Frontend toggles cancellation per ISO year-week (`canceledWeekEpochId`, e.g., `202648`), and `checkOverlapConflict` skips entries canceled for the current week.
  - **Recommended Backend Design:** Create table `timetable_entry_cancellations (id BIGINT PK, entry_id BIGINT FK -> timetable_entries(id) ON DELETE CASCADE, canceled_date LocalDate / iso_year_week INT, UNIQUE(entry_id, iso_year_week))` so multiple single-week cancellations across a semester are preserved cleanly without overwriting history.
  - **New Endpoints:**
    - `PUT /api/timetable-entries/{id}/cancellations/{isoYearWeek}` (mark canceled for that week)
    - `DELETE /api/timetable-entries/{id}/cancellations/{isoYearWeek}` (restore class for that week)
    - Include `Set<Integer> canceledIsoWeeks` in `TimetableEntryResponse`, and ignore canceled weeks when checking single-week conflicts if applicable.

### Gap 6: Minor Validation & Domain Alignment Fixes
1. **Grade Minimum Score (`0..10` vs `1..10`):** Backend `Grade` has `@DecimalMin("1.00")` and PostgreSQL `CHECK (score between 1 and 10)`, whereas frontend `GradeDialog.kt` currently checks `value < 0.0 || value > 10.0`. Align frontend `GradeDialog.kt` to `1.0..10.0` (standard 1–10 grading scale).
2. **ActivityType Mapping:** Frontend Quick Class offers `"Study Session"` in addition to `"Lecture"`, `"Laboratory"`, `"Seminar"`, `"Practical Work"`, `"Custom"`, and demo seeding uses `"Project Studio"`. Ensure the Android mapper sends any non-standard type as `activityType = "CUSTOM"` with `customTypeName = <type>` so backend `RequirementRules` (`(activity_type = 'CUSTOM') = (custom_type_name is not null)`) never rejects it with `400`.
3. **Android Cleartext Localhost Traffic (Debug Only):** Android 10+ blocks `http://10.0.2.2:8080` by default unless a debug `network_security_config.xml` permits cleartext to `10.0.2.2` / `localhost` / LAN dev IP.

---

## 5. Manual Prerequisites Checklist (Secrets, Console Configs & Local Env)

Do **not** commit real secrets to Git. Before or during the implementation phase, you must manually provision and supply the following items:

### A. Local Infrastructure & Build Tools
- [ ] **Docker Desktop** installed and running locally (for `cd backend && docker compose up -d` PostgreSQL 17 on port `5432`).
- [ ] **JDK 21** installed (or internet access on first Gradle run so Foojay downloads Java 21 for `backend/`) and **Android SDK 36/37** configured in `android/local.properties` (`sdk.dir=...`).

### B. Google Sign-In Prerequisites (Google Cloud Console)
1. [ ] Create/select a project in **Google Cloud Console** -> **APIs & Services -> OAuth consent screen** (configure App name `Sudo Sleep`, support email, scopes `openid`, `email`, `profile`, and add your test Gmail accounts while in *Testing* mode).
2. [ ] Create **OAuth 2.0 Client ID #1 — Type: Android**:
   - Package name: `com.example.sudosleep`
   - **Debug SHA-1 fingerprint**: obtain locally via `cd android && ./gradlew signingReport` (from `Variant: debug`) and paste into the Android OAuth client. *(Also add Release keystore SHA-1 later for production.)*
3. [ ] Create **OAuth 2.0 Client ID #2 — Type: Web application**:
   - Copy the generated **Web Client ID** (`xxxx-xxxxxxxxxxxxxxxx.apps.googleusercontent.com`).
   - *Why both?* Android `CredentialManager` (`GetGoogleIdOption.Builder().setServerClientId(...)`) **requires the Web Client ID**, and the Spring Boot backend verifies `idToken` audience (`aud`) against this exact **Web Client ID**.

### C. Facebook Login Prerequisites (Meta for Developers Console)
1. [ ] Create an app in **Meta for Developers** (*Authenticate and request data from users with Facebook Login*) and enable `email` and `public_profile` permissions.
2. [ ] Under **Facebook Login -> Settings / Quickstart -> Android**:
   - Package Name: `com.example.sudosleep`
   - Default Activity Class Name: `com.example.sudosleep.MainActivity`
   - **Debug Key Hash (Base64 SHA-1)**: generate from your `~/.android/debug.keystore` and register it in the Meta console.
3. [ ] From **App Settings -> Basic** & **App Settings -> Advanced**, collect:
   - **Facebook App ID** (numeric string, needed on both Android & Backend)
   - **Facebook Client Token** (needed on Android in `strings.xml` / Manifest meta-data)
   - **Facebook App Secret** (**Backend ONLY** — never place in the Android app!)

### D. Email / Forgot-Password SMTP Credentials (Backend)
- [ ] Provide an SMTP sender account for password-reset emails (e.g., Gmail App Password, Mailtrap for local dev, Resend, or SendGrid):
  - `MAIL_HOST` (e.g., `sandbox.smtp.mailtrap.io` or `smtp.gmail.com`)
  - `MAIL_PORT` (e.g., `587`)
  - `MAIL_USERNAME`
  - `MAIL_PASSWORD`
  - `MAIL_FROM` (e.g., `no-reply@sudosleep.app`)

### E. Exact `.env` / Local Config Files You Will Populate

#### 1. `backend/.env` (Gitignored — loaded by Spring Boot or IDE Run Configuration)
```dotenv
# Database (matches docker-compose.yml defaults if omitted locally)
DB_URL=jdbc:postgresql://localhost:5432/sudosleep
DB_USER=sudosleep
DB_PASSWORD=<your-db-password-or-sudosleep-for-local-docker>

# Stateless JWT Signing Key (generate >= 256-bit random base64/hex secret, e.g. `openssl rand -base64 48`)
JWT_SECRET=<provide-strong-256-bit-secret>
JWT_ACCESS_EXPIRATION_SECONDS=3600
JWT_REFRESH_EXPIRATION_SECONDS=2592000

# OAuth Token Verification
GOOGLE_WEB_CLIENT_ID=<your-google-web-client-id>.apps.googleusercontent.com
FACEBOOK_APP_ID=<your-facebook-app-id>
FACEBOOK_APP_SECRET=<your-facebook-app-secret>

# Forgot-Password SMTP
MAIL_HOST=<your-smtp-host>
MAIL_PORT=587
MAIL_USERNAME=<your-smtp-username>
MAIL_PASSWORD=<your-smtp-password>
MAIL_FROM=<your-verified-sender-email>
```

#### 2. `android/local.properties` (Gitignored — read by `android/app/build.gradle.kts` into `BuildConfig` / Manifest placeholders)
```properties
sdk.dir=C\:\\Users\\<you>\\AppData\\Local\\Android\\Sdk

# Backend Base URL:
# - Use http://10.0.2.2:8080/ for Android Emulator
# - Use http://<your-pc-lan-ip>:8080/ for a physical phone on the same Wi-Fi
API_BASE_URL=http://10.0.2.2:8080/

# OAuth Client Identifiers (public client IDs, injected into BuildConfig / strings resValues)
GOOGLE_WEB_CLIENT_ID=<your-google-web-client-id>.apps.googleusercontent.com
FACEBOOK_APP_ID=<your-facebook-app-id>
FACEBOOK_CLIENT_TOKEN=<your-facebook-client-token>
```

---

## 6. Phase 1 Execution Log — Completed

**Branch:** `integrate/frontend-on-main`

### Completed Deliverables
1. **Frontend Migration (`android/`):**
   - Imported the full Kotlin/Compose Android frontend from `master` (`21a8839`) into `android/` without overwriting or disrupting `backend/` or `docs/`.
   - Added `android/local.properties.example` and configured root `.gitignore` & `backend/.gitignore` so real `.env` and `local.properties` files remain untracked while templates (`.env.example`, `local.properties.example`) are version-controlled.
2. **Stateless JWT Auth, Social OAuth Verification & Ownership Security (`backend/`):**
   - Added Spring Security (`SecurityConfig`), HS256 JWT issuance/validation (`JwtService`, `JwtAuthenticationFilter`), and RFC 9457 `401`/`403` error responses (`GlobalExceptionHandler`, `UnauthorizedException`, `ForbiddenException`).
   - Implemented `AuthController` & `AuthService`:
     - `POST /api/auth/register` (BCrypt password hashing + automatic default Monday semester creation), `POST /api/auth/login`, `POST /api/auth/refresh`.
     - `POST /api/auth/forgot-password` & `POST /api/auth/reset-password` backed by SHA-256 hashed 6-digit codes in `password_reset_tokens` and `PasswordResetMailService` (SMTP when `MAIL_HOST` is set; dev log fallback otherwise).
     - `POST /api/auth/oauth/google` (JWKS signature, issuer, expiration, and `GOOGLE_WEB_CLIENT_ID` audience verification via `GoogleIdTokenVerifierImpl`) and `POST /api/auth/oauth/facebook` (Meta Graph `/debug_token` + `/me` with HMAC-SHA256 `appsecret_proof` via `FacebookGraphTokenVerifierImpl`), with automatic account creation and email-based linking (`AuthProvider.LINKED`).
   - Added `/api/users/me` (`GET`/`PUT`/`DELETE`), `/api/semesters` (`GET`/`POST`), and enforced strict per-user ownership (`OwnershipGuard`) across Users, Semesters, Subjects, Activity Requirements, Timetable Entries, Grades, and Academic Events.
3. **Timetable Extensions, Per-Week Cancellations & Academic Events (`backend/`):**
   - Extended `timetable_entries` with `notes`, `notifications_enabled`, and `reminder_offsets_csv`.
   - Added `timetable_entry_cancellations` (`PUT` & `DELETE /api/timetable-entries/{id}/cancellations/{isoYearWeek}`) and week-aware conflict filtering (`?isoYearWeek=` on `POST /api/semesters/{semesterId}/timetable/conflicts`).
   - Added `academic_events` entity, repository, mapper, service, and controller (`GET/POST /api/semesters/{semesterId}/events`, `GET/PUT/DELETE /api/events/{id}`) for Exams, Midterms, Project Deadlines, Quizzes, and Custom Events.
4. **Configuration Templates & Documentation:**
   - Added `backend/.env.example` and configured `spring.config.import=optional:file:.env[.properties]` in `backend/src/main/resources/application.properties`.
   - Updated `docs/database.md`, `backend/README.md`, `backend/requests.http`, and Swagger OpenAPI Bearer auth (`OpenApiConfig`).

### Verification Results
- **Backend (`backend/`):** `.\gradlew.bat clean test bootJar` — **BUILD SUCCESSFUL** (all 45 unit, security filter, and Spring Security WebMvc wiring tests passed; executable Spring Boot JAR packaged cleanly).
- **Android (`android/`):** `.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug` — **BUILD SUCCESSFUL** (unit tests passed and debug APK assembled cleanly inside `android/`).
- *Note:* Live PostgreSQL `bootRun` smoke test requires starting Docker (`cd backend && docker compose up -d` followed by `./gradlew bootRun`); PostgreSQL was not running locally during verification.

---

## 7. Phase 2 Execution Log — Completed

### Completed Deliverables (`android/`)
1. **Network Client, BuildConfig & Secure Token Storage:**
   - Enabled `BuildConfig` in `android/app/build.gradle.kts`, reading `API_BASE_URL` (defaults to `http://10.0.2.2:8080/` for emulator, overrideable in `android/local.properties` for physical devices) plus OAuth placeholder fields.
   - Added debug-only `android/app/src/debug/AndroidManifest.xml` and `android/app/src/debug/res/xml/network_security_config.xml` to permit cleartext HTTP to local dev servers in debug builds while preserving HTTPS enforcement in release builds.
   - Implemented `EncryptedTokenStore` (`data/auth/TokenStore.kt`) backed by Android Keystore AES-256-GCM encryption on private `SharedPreferences`.
   - Implemented `AuthDtos.kt`, `AuthApi.kt`, `ApiErrorParser.kt` (parses RFC 9457 `ProblemDetail` and validation maps), and `ApiClient.kt` (OkHttp `AuthInterceptor` + thread-safe `TokenRefreshAuthenticator` for automatic 401 refresh & retry).
2. **Backend-Connected `AuthManager` (`data/auth/AuthManager.kt`):**
   - Replaced the local stub with real backend calls: `register`, `login`, `requestPasswordReset`, `confirmPasswordReset`, `updateDisplayName`, `syncProfilePreferences`, `restoreSessionIfAvailable`, `handleSessionExpired`, and `logout` (plus ready-to-wire `loginWithGoogleIdToken` & `loginWithFacebookAccessToken`).
3. **Polished Material 3 Auth & Profile UI:**
   - Upgraded `LoginScreen.kt` with animated **Log In** / **Create Account** switcher (Full Name, Email, Password, Confirm Password), inline validation, loading spinners, RFC 9457 error banners, and session-expired notices.
   - Added `ForgotPasswordDialog.kt` (2-step code request -> 6-digit code + new password confirmation with resend support).
   - Updated `MainNavigationScreen.kt` with startup session validation/auto-entry, interactive **Edit Display Name** dialog in the Navigation Drawer header synced to `PUT /api/users/me`, onboarding sync, and full **Log Out** cleanup (cancels all scheduled `AlarmManager` reminders & notifications via `NotificationHelper.cancelAllAlarms`, clears Room cached user tables via `SudoSleepRepository.clearAllUserData()`, clears encrypted tokens & user preferences, and returns to `LoginScreen`).

### Verification Results
- **Android (`android/`):** `.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug` — **BUILD SUCCESSFUL** (all 13 unit tests passed, including `AuthErrorAndValidationTest`, and debug APK built cleanly).

---

## 8. Phase 3 Execution Log — Completed

### Completed Deliverables (`android/`)
1. **Remote Academic API, DTOs & Bidirectional Mappers:**
   - Added `data/remote/dto/AcademicDtos.kt` (Moshi `@JsonClass(generateAdapter = true)` DTOs for Semesters, Subjects, Activity Requirements, Grades, Timetable Entries with `canceledIsoWeeks`, and Academic Events).
   - Added `data/remote/AcademicApi.kt` covering all authenticated academic CRUD and per-week cancellation endpoints (`PUT`/`DELETE api/timetable-entries/{id}/cancellations/{isoYearWeek}`), exposed via `ApiClient.academicApi`.
   - Added `data/remote/AcademicMappers.kt` for bidirectional Room entity <-> DTO mapping (standard + custom `ActivityType`, `AcademicEventType`, `startHour`/`duration` <-> `HH:00`, `dateEpochDay` <-> ISO `YYYY-MM-DD`, current-week cancellation resolution, and hex color normalization).
   - Added `updateActiveSemesterId(semesterId: Long)` to `SessionTokenStore` / `EncryptedTokenStore`.
2. **Offline-First Repository & Atomic Cache Sync (`SudoSleepRepository.kt`):**
   - Refactored `SudoSleepRepository` to keep Room as the reactive single source of truth for instant, lag-free UI rendering while syncing with the Spring Boot backend on `Dispatchers.IO`.
   - Implemented `syncFromBackend(showSuccessMessage)`: resolves or self-heals `activeSemesterId` (falling back to `GET /api/semesters` or creating a Monday semester if absent), fetches Subjects/Requirements, Timetable Entries (with cancellation weeks), Academic Events, and per-subject Grades in parallel, and atomically replaces Room tables inside `database.withTransaction { ... }` (using new `insertAll` + `deleteAll` DAO helpers to avoid empty-screen flicker).
   - Wired all create, update, cancel-this-week, and delete actions across Subjects, Activity Requirements, Timetable Entries, Grades, and Academic Events to persist asynchronously to the backend with graceful offline fallback (`IOException`) and user-facing snackbar feedback (`syncMessages`).
   - Unified Room primary keys with server PostgreSQL IDs so edits, deletes, and foreign keys stay consistent across devices; aligned `GradeDialog` validation (`1.0..10.0`, `weight > 0.0`, comma decimal support) with backend rules.
3. **App Lifecycle Sync, Alarm Rescheduling & UI Preservation:**
   - Wired `MainActivity`, `TimetableViewModel` (`isSyncing`, `syncMessages`, `refreshFromBackend`), and `MainNavigationScreen` so fresh login and cold app launch automatically validate the session, pull remote data, refresh all Compose views, and reschedule `AlarmManager` reminders via the existing `NotificationHelper.syncAllAlarms` reactive effect.
   - Added a non-intrusive top `LinearProgressIndicator` during active sync and a **"Sync Schedule Now"** action in the Navigation Drawer while preserving all existing Material 3 features (Day/Week/Month views, tap-to-add, pinch-to-zoom, conflict detection, missing-activity banner, PNG/PDF export, onboarding, and back-button handling).

### Verification Results
- **Android (`android/`):** `.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug` — **BUILD SUCCESSFUL** (all 18 unit tests passed, including the 5 new `AcademicMappersTest` cases, and `:app:assembleDebug` succeeded with zero errors).

---

## 9. Phase 4 Execution Log — Completed

### Completed Deliverables (`android/`)
1. **Dependencies & Manifest / Resource Configuration:**
   - Added `androidx.credentials:credentials:1.6.0`, `androidx.credentials:credentials-play-services-auth:1.6.0`, `com.google.android.libraries.identity.googleid:googleid:1.1.1` (Kotlin 2.2 metadata-compatible), and `com.facebook.android:facebook-login:19.0.1` to `android/gradle/libs.versions.toml` and `android/app/build.gradle.kts`.
   - Added placeholder strings (`default_web_client_id`, `facebook_app_id`, `facebook_client_token`, `fb_login_protocol_scheme`) in `android/app/src/main/res/values/strings.xml`.
   - Added Facebook `<queries>`, `<meta-data>` (`ApplicationId`, `ClientToken`), `FacebookActivity`, and `CustomTabActivity` intent-filter in `android/app/src/main/AndroidManifest.xml`.
2. **Modern Google Credential Manager & Facebook Login Helpers (`data/auth/SocialAuthProviders.kt`):**
   - Implemented `GoogleCredentialAuthHelper` using `CredentialManager` + `GetGoogleIdOption` (authorized-accounts first, automatic fallback to all Google accounts on `NoCredentialException`, `MutableContextWrapper`, random URL-safe nonce, `GoogleIdTokenCredential.createFrom`), plus `clearCredentialState()` on logout.
   - Implemented `FacebookLoginAuthHelper` using modern `ActivityResultRegistryOwner` (`LoginManager.logInWithReadPermissions(activity, callbackManager, listOf("email", "public_profile"))`) with `suspendCancellableCoroutine`, automatic callback unregistration, denied-email handling, and `LoginManager.getInstance().logOut()` on logout.
   - Added `SocialAuthConfig` placeholder/setup validation so unconfigured IDs show clean guidance instead of crashing.
3. **Material 3 Social Login UI & Full End-to-End Flow (`LoginScreen.kt`, `AuthManager.kt`, `MainActivity.kt`, `MainNavigationScreen.kt`):**
   - Added styled **Continue with Google** (Material 3 `OutlinedButton` + multi-color Google badge) and **Continue with Facebook** (`#1877F2` button + crisp `"f"` badge) below an `"or continue with"` divider on `LoginScreen`, with per-provider loading indicators, silent cancellation handling, RFC 9457 error reporting, and account-linking welcome notices (`AuthProvider.LINKED`).
   - Wired `onClearSocialSessions` into `AuthManager` so Logout and session expiry clear Credential Manager state, Facebook Login SDK session, `AlarmManager` notifications, Room cache, and encrypted tokens.
   - Added `SocialAuthConfigTest.kt` unit tests covering placeholder validation, Google Web Client ID precedence, Facebook protocol scheme matching, and nonce generation.

### Verification Results
- **Android (`android/`):** `.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug` — **BUILD SUCCESSFUL** (all 22 unit tests passed and `:app:assembleDebug` succeeded with zero errors).


