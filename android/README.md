# Sudo Sleep - Timetable & Academic Planner 🎓📅

**Sudo Sleep** is a modern, offline-first Material Design 3 Android application built for university students, pupils, and educators. It provides seamless management of course subjects, weighted gradebook calculations, an interactive hourly timetable grid, and real-time scheduling conflict detection.

---

## ✨ Features

### 📖 Subject & Gradebook Management
- **Custom Color-Coded Subjects**: Create and manage subjects with custom color coding (palette of 10 vibrant/pastel colors).
- **Activity Requirements**: Define required components per subject (Lecture, Laboratory, Seminar, Practical Work, or Custom Types).
- **Weighted Gradebook**: Track grades with customizable weights and automatically calculate arithmetic and weighted grade averages in real time.
- **Semester GPA**: View overall semester average score across all enrolled subjects.

### 🗓️ Interactive Weekly Timetable Grid
- **08:00 – 22:00 Canvas Grid**: Displays an hourly canvas across all 7 days of the week (Monday – Sunday).
- **Proportional Activity Blocks**: Scheduled classes render as duration-scaled, color-coded event cards displaying course name, activity type badge, location/room, and instructor information.
- **Unscheduled Activity Detector**: Top warning banner alerts you of pending subject requirements with quick-action scheduling options.

### ⚡ Smart Overlap Conflict Detection
- **Conflict Validation**: When placing an activity onto the schedule, Sudo Sleep checks for time overlaps using the formula: `newStart < existingEnd && newEnd > existingStart`.
- **Warning Override**: Interactive alert warning dialog alerts you to time slot collisions while allowing you to force-schedule or cancel.

### 📱 Material Design 3 Polish
- Modern, clean UX with rounded containers, soft elevations, generous whitespace, and fluid touch interactions.
- Dynamic color themes (Light and Dark modes) with full Edge-to-Edge display support.

---

## 🛠️ Architecture & Tech Stack

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose with Material Design 3
- **Local Persistence**: Room Database with Kotlin Coroutines and StateFlow
- **Architecture**: Modern Android Architecture (`ViewModel`, `Repository`, DAOs, Entities, POJOs)
- **Min SDK**: 29 (Android 10+)
- **Compile SDK**: 37

---

## 🚀 Future Updates & Roadmap

- [ ] **Notifications & Class Reminders**: Configurable push notifications 10–15 minutes before scheduled classes begin.
- [ ] **Export & Import Schedule**: Backup and restore timetables as JSON or export to standard iCalendar (`.ics`) format.
- [ ] **Home Screen Widget**: Compact launcher widget showing today's remaining classes and upcoming lectures.
- [ ] **Assignment & Exam Countdown Tracker**: Dedicated section for tracking upcoming homework deadlines, project submissions, and exam dates.
- [ ] **Multi-Semester Support**: Archive past terms and manage multiple academic semesters simultaneously.

---

## 💻 Getting Started

1. Open Android Studio.
2. Open the project folder (`SudoSleep`).
3. Sync Gradle dependencies and build the project (`./gradlew assembleDebug`).
4. Run on an Android emulator or connected device running Android 10+ (API 29+).

---

## 📄 License
Created for students and teachers. Designed with ❤️ using Jetpack Compose and Material Design 3.
