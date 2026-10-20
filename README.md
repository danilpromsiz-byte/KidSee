# 🧿 KidSee — Android Parental Control & Media Guidance

> **Smart & caring screen supervision for Android phones, tablets, and Android TV.**

**KidSee** allows parents to monitor in real-time what their children are watching, control media playback (play/pause/skip/volume), hear internal audio, speak to the child via Push-to-Talk, block the screen on demand or by schedule, and watch multiple devices simultaneously with Picture-in-Picture (PiP) support.

---

## ✨ Key Features

- 📱 **Universal Device Support**: Operates in both **Parent** and **Child** modes across **Android Phones**, **Tablets**, and **Android TV**.
- 📺 **Android TV Ready**: Native Leanback launcher, D-pad navigation with remote control mapping (OK, Play/Pause, Next/Prev, Volume, Red button lock).
- 🖼️ **Picture-in-Picture (PiP)**: Keep an eye on your child’s device in a floating window over any app on your phone, tablet, or TV.
- 👁️ **Real-Time Screen & Audio Streaming**: WebRTC peer-to-peer streaming with ultra-low latency (<500ms).
- 🔊 **Live Audio Monitoring**: Powered by `AudioPlaybackCapture API` (Android 10+), hear video, game, and app sound directly.
- 🎤 **Live Intercom (Push-to-Talk)**: Hold to speak directly into your child's speaker in real-time (with automatic background media ducking).
- 🎙️ **Voice Notes**: Record and send voice messages that play with parent notifications.
- ⏯️ **Remote Media Control**: Pause, play, skip, or change video rolls across YouTube, Netflix, browsers, and media players.
- 🔒 **Screen Lock**: Instantly block the screen on demand or when non-video apps are detected, plus full-screen friendly Waze-style lock overlay.
- ⏰ **Bedtime & School Schedules**: Automated lock rules with weekday/weekend time slots.
- 🎨 **Waze-Inspired Design**: Friendly, playful visual language featuring rounded 24dp cards, pill buttons, and the animated **KidSee Eye** mascot.
- 🌐 **Multilingual Support**: Fully localized into 5 languages:
  - English (`en`)
  - Français (`fr`)
  - Español (`es`)
  - Русский (`ru`)
  - Українська (`uk`)

---

## 🏗️ Architecture & Modules

The project is built with **Clean Architecture** and Jetpack Compose:

```
KidSee/
├── app/                  # Main Android Application (Activities, Services, Screens, StateManager)
├── core/
│   ├── common/           # Coroutines, serialization, Timber logging, extensions
│   ├── domain/           # Pure Kotlin domain models (ChildDevice, DeviceCommand, ScheduleRule)
│   ├── data/             # KidSeePreferences (DataStore) and KidSeeRepository
│   ├── network/          # Firebase Realtime Database & WebRTC PeerConnection manager
│   └── ui/               # Waze Design System (Theme, Colors, Mascot, PillButton, TvCard, Strings)
```

---

## 🛠️ Android Services (Child Device)

1. **`KidSeeAccessibilityService`**: Detects active applications and inspects UI nodes to verify video playback vs. non-video apps.
2. **`KidSeeMediaMonitorService`**: Extends `NotificationListenerService` and interfaces with `MediaSessionManager` to control third-party media players (pause, play, skip, volume).
3. **`ScreenCaptureService`**: Foreground service leveraging `MediaProjection` and `AudioPlaybackCapture` to encode screen and system sound into WebRTC tracks.
4. **`KidSeeDeviceAdmin`**: `DeviceAdminReceiver` enforcing instant device lock via `DevicePolicyManager.lockNow()`.
5. **`KidSeeOverlayLockService`**: Fullscreen non-bypassable overlay using `SYSTEM_ALERT_WINDOW` displaying a friendly break reminder.
6. **`VoiceMessageManager`**: Handles audio recording on parent devices and speech playback with audio-focus ducking on child devices.

---

## 🚀 Building the Project

### Prerequisites
- JDK 17
- Android SDK (API 35/36 installed)

### Build Commands
```powershell
# Compile Kotlin for all modules
.\gradlew.bat compileDebugKotlin

# Run automated tests
.\gradlew.bat test

# Assemble Debug APK
.\gradlew.bat assembleDebug
```

The compiled APK will be generated at:
`app/build/outputs/apk/debug/app-debug.apk`

---

## 📄 License
Designed for family safety and parental supervision.
