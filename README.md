# Sudo Sleep - Cozy Timetable & Academic Planner 🎓📅💤

**Sudo Sleep** is a modern, offline-first Material Design 3 Android application built for university students, pupils, and educators. It combines an interactive multi-view timetable (Day, Week, Month), weighted gradebook tracking, exam & deadline countdowns, smart multi-reminder notifications, and one-tap phone wallpaper / PDF schedule exports in a smooth, "cute", and pleasurable UI.

---

## ✨ Key Features

### 🗓️ Interactive Multi-View Timetable (Day, Week & Month)
- **Direct Tap-to-Add on Schedule Grid**: Tap any empty slot on the `08:00–22:00` canvas in Day or Week view to immediately schedule a class, quick activity, or exam pre-filled with that exact day and hour.
- **Pinch-to-Zoom & Fluid Swipes**: Pinch vertically with two fingers to dynamically scale the hourly grid (fit the whole day on one screen or zoom in for full room/instructor details) and swipe left/right between days.
- **Day, Week & Month Switcher**:
  - **Day View**: Proportional color-coded cards, live current-time indicator line, and a *"Next up today"* countdown banner.
  - **Week View**: Compact 7-day bird's-eye grid with week navigation and direct tap-to-add.
  - **Month View**: Interactive monthly calendar highlighting exam dates (`🎯`) and daily recurring class dots with a selected-date agenda.
- **"Canceled This Week Only"**: Pause a class for the current week without deleting it from your recurring schedule—renders dimmed with a strikethrough badge and automatically mutes reminders until next week.

### 🎯 Exams, Midterms & Deadlines Tracker
- **Date-Specific Academic Events**: Schedule Exams, Midterms, Project Deadlines, Quizzes, and Custom Events linked to subjects or standalone.
- **Countdown Radar**: Live *"Days left"* badges on the Exam Radar banner, Month View calendar, and Exam cards.

### 🔔 Smart Local Notifications & Multiple Reminders
- **Multiple Reminders per Activity/Exam**: Attach multiple reminder chips (`5m`, `10m`, `15m`, `30m`, `1h`, `2h`, `1 day before`) to any class or exam.
- **Global & Per-Item Controls**: Toggle notifications globally or per individual class/exam, plus a **Send Test Notification Now** button in Settings.

### 🖼️ Phone Wallpaper (PNG) & Print-Ready PDF Export
- **Lock-Screen Ready PNG Wallpaper**: Generates crisp `1080×2400` HD schedule wallpapers with customizable aesthetic styles (*Cozy Pastel*, *Midnight Dark*, *Matcha Cream*), optional lock-screen clock clearance, gallery save (`MediaStore.Images`), and one-tap **Set as Phone Wallpaper**.
- **A4 Landscape PDF Export**: Native `PdfDocument` exporter saves print-ready weekly timetables directly to `Downloads/SudoSleep` with instant share/print actions.

### 📖 Subject & Weighted Gradebook Management
- **Custom Color-Coded Subjects**: Choose from 12 vibrant & pastel subject colors and define required course components (Lecture, Laboratory, Seminar, Practical Work, or Custom).
- **Real-Time Weighted Averages & Semester GPA**: Automatically calculates subject averages and overall semester GPA.
- **Safe Deletion UX**: Delete actions for classes, exams, subjects, and grades are tucked inside Edit / 3-dot overflow menus with confirmation dialogs to prevent accidental deletions.

### 🌸 Cute Themes, Frontend Auth & Interactive Tutorial
- **6 Aesthetic Color Themes**: *Pastel Dream*, *Matcha Mint*, *Sakura Blossom*, *Ocean Breeze*, *Sunset Peach*, and *Dynamic Material You* (with System, Light, and Cozy Dark modes).
- **Welcome Login Screen**: Clean Email/Username & Password login + *Continue with Google* (decoupled via `AuthManager` for future backend integration).
- **Interactive Coach-Mark Tutorial**: Skippable 4-step spotlight walkthrough on first install (and replayable anytime from the Hamburger menu).
- **Polished Navigation & Back-Stack**: Slide-out Hamburger Navigation Drawer (`ModalNavigationDrawer`) and smart system back-gesture handling (`BackHandler`).

---

## 🛠️ Architecture & Tech Stack

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose (Material Design 3, `HorizontalPager`, `ModalNavigationDrawer`, `ModalBottomSheet`, multi-touch `pointerInput`)
- **Local Persistence**: Room Database (v2) + `SharedPreferences` state flows
- **Notifications & Export**: `AlarmManager`, `NotificationChannel`, Android `Canvas` & `PdfDocument`, `WallpaperManager`, `MediaStore`
- **Architecture**: MVVM (`ViewModel`, `SudoSleepRepository`, DAOs, Entities, relational POJOs)
- **Min SDK**: 29 (Android 10+) • **Compile SDK**: 37

---

## 💻 Getting Started

1. Open the project folder (`SudoSleep`) in **Android Studio**.
2. Sync Gradle dependencies and run `./gradlew :app:assembleDebug`.
3. Launch on an Android emulator or physical device (API 29+).

---

## 📄 License
Designed and built with ❤️ using Jetpack Compose and Material Design 3.
