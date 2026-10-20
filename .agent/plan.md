# Project Plan

Sudo Sleep - A modern Material 3 Timetable, Activity Scheduling, and Gradebook Android application for students and teachers.

## Project Brief

# Project Brief: Sudo Sleep

Sudo Sleep is a modern Material 3 Android application designed for students and teachers to manage subjects, track grades with dynamic average calculations, schedule course activities, and view interactive weekly timetables with conflict detection.

---

## Features

1. **Subject & Gradebook Management**:
   - Create and manage color-coded subjects with component requirements (Lecture, Seminar, Laboratory, Custom).
   - Track grades with configurable weights and automatic calculation of arithmetic/weighted averages.

2. **Interactive Timetable & Hourly Grid**:
   - Display a weekly schedule (08:00–22:00) broken down by day tabs (Monday–Sunday).
   - Color-coded activity blocks scaled by duration, showing subject name, type, room, and teacher information.

3. **Activity Scheduling & Overlap Conflict Detection**:
   - Assign unscheduled subject requirements into timetable slots with start time and duration selection.
   - Real-time time overlap validation standard calculation (`newStart < existingEnd && newEnd > existingStart`) with interactive conflict warning override.

4. **Unscheduled Requirement Detector & Quick-Action Banner**:
   - Track pending activity requirements across all subjects.
   - Interactive top warning banner highlighting unassigned requirements with direct navigation to scheduling.

---

## High-Level Tech Stack

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose with Material Design 3 (M3) styling, Edge-to-Edge layout support, and dynamic color scheme
- **Navigation & Adaptive Strategy**: State-driven Navigation and Compose Material Adaptive for adaptive UI layouts
- **Asynchrony & State Management**: Kotlin Coroutines, Flow, `ViewModel`, and `StateFlow`
- **Local Persistence**: Room Database for offline-first local CRUD operations across Subjects, Activity Requirements, Grades, and Timetable Entries

## Implementation Steps

### Task_1_DataLayerAndRoomDatabase: Design and implement Room entities (Subject, Requirement, TimetableEntry, Grade), DAOs, Database, Repository, and ViewModels supporting subject tracking, grade average calculations, and timetable slot validation.
- **Status:** IN_PROGRESS
- **Acceptance Criteria:**
  - Room entities, DAOs, and Database created for Subjects, Requirements, Grades, and TimetableEntries
  - Repository and ViewModel setup with Coroutines and Flow
  - Overlap conflict detection algorithm implemented
- **StartTime:** 2026-10-20 16:38:40 EEST

### Task_2_SubjectAndGradebookUI: Implement Compose UI screens for Subject management and Gradebook tracking with dynamic weighted average calculations.
- **Status:** PENDING
- **Acceptance Criteria:**
  - Subject list & detail screens supporting color-coding and activity components
  - Gradebook UI for adding grades with weights and displaying calculated averages
  - Seamless navigation between screens

### Task_3_TimetableAndSchedulingUI: Implement interactive Weekly Timetable hourly grid (Mon-Sun, 08:00-22:00), activity scheduling UI with real-time overlap conflict warnings, and unscheduled requirement banner.
- **Status:** PENDING
- **Acceptance Criteria:**
  - Timetable screen displaying day tabs and hourly grid activity blocks
  - Scheduling dialog allowing assignment of requirements to time slots with conflict warning override
  - Unscheduled requirement banner with direct scheduling quick-actions

### Task_4_AppIconThemeAndPolish: Apply Material 3 color theme (light/dark mode), enable full Edge-to-Edge display, and create an adaptive app icon for Sudo Sleep.
- **Status:** PENDING
- **Acceptance Criteria:**
  - Material Design 3 energetic theme and color schemes integrated
  - Full Edge-to-Edge layout enabled
  - Adaptive app icon implemented

### Task_5_RunAndVerify: Compile the app, run all unit/instrumentation tests, verify app stability on emulator/device, ensure no crashes, and check alignment with user requirements.
- **Status:** PENDING
- **Acceptance Criteria:**
  - build pass
  - make sure all existing tests pass
  - app does not crash
  - critic_agent verifies application stability, confirms alignment with user requirements, and reports critical UI issues

