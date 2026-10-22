<p align="center">
  <a href="https://play.google.com/apps/testing/dev.ihorshevchuk.piper">
    <img src="app/src/main/res/mipmap-xxxhdpi/ic_launcher.png" width="130"><br>
    <img src="https://play.google.com/intl/en_us/badges/static/images/badges/en_badge_web_generic.png" width="180" alt="Get it on Google Play">
  </a>
</p>

<h3 align="center">
  Piper is now in closed testing on Google Play! 🎉<br>
  Experience high-quality <b>offline neural text-to-speech</b> directly on your device.<br><br>
  Want to join the test?<br>
  First join the <a href="https://groups.google.com/g/piper-android-testers">testers group</a>,
  then <a href="https://play.google.com/apps/testing/dev.ihorshevchuk.piper">opt in and install</a> 🚀<br><br>
  Or grab the latest build from the
  <a href="https://github.com/piper-tts-apps/piper-app-android/releases/tag/latest">GitHub Releases page</a>
</h3>

# ⚠️ Powerful hardware required

Piper runs large neural speech models entirely on your device - there is no cloud server to do the work for you. **This app requires powerful hardware to run well.** On a recent flagship or high-end device, voices are fast and smooth. On entry-level or older phones, synthesis can be very slow, with long pauses, and may not keep up with TalkBack or fast reading - please do not expect a usable experience on basic devices. If your device is lighter, prefer Compact (INT8) voices, but a powerful processor and plenty of memory is what Piper is designed for.

[![CI](https://github.com/piper-tts-apps/piper-app-android/actions/workflows/ci.yml/badge.svg?branch=main)](https://github.com/piper-tts-apps/piper-app-android/actions/workflows/ci.yml)

# 📥 Cloning & Building Piper

## System Requirements

* Android 8.0+ (API 26) to run the app
* JDK 17
* Android SDK 35 + build-tools 35.0.0
* Android NDK 27.0.12077973 (the version pinned in piper-kotlin's `gradle.properties`)
* CMake (SDK package `cmake;3.22.1` or a system install)

---

## Getting the Sources

The app builds against the [piper-kotlin](https://github.com/piper-tts-apps/piper-kotlin)
engine library through a Gradle composite build, so check out both repos
side by side:

```bash
git clone https://github.com/piper-tts-apps/piper-app-android.git
git clone --recurse-submodules https://github.com/piper-tts-apps/piper-kotlin.git
```

The layout must be:

```
piper-kotlin        - :piper-engine, :piper-utils, :piper-player
piper-app-android   - this repo (:app, :tts-service, :voice-download)
```

---

## Prepare Dependencies

Fetch the native dependencies (ONNX Runtime + the vendored sources) once,
from the library repo:

```bash
cd ../piper-kotlin
./scripts/fetch-native-deps.sh
cd ../piper-app-android
```

Then compile the espeak-ng runtime data into the app's assets
(git-ignored, ~31 MB):

```bash
./scripts/stage-espeak-data.sh
```

---

## Build

```bash
./gradlew :app:assembleDebug
```

The APK lands at `app/build/outputs/apk/debug/app-debug.apk` (~93 MB with
all assets).

### Available Gradle Tasks

| Task | Description |
|------|-------------|
| `./gradlew :app:assembleDebug` | Build the debug APK |
| `./gradlew testDebugUnitTest` | Run all modules' JVM unit tests |
| `./gradlew :app:installDebug` | Build and install on a connected device/emulator |

---

# 📱 Running the App

## Emulator

1. Create/start an Android emulator (API 26+)
2. `./gradlew :app:installDebug`

---

## Physical Device

1. Enable USB debugging on the device
2. Install the APK:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

On first preview the app stages espeak-ng-data from assets into its
private filesDir; voices download into `<files>/voices/` (shared with
the TTS service).

To use Piper as the system voice, select it as the preferred engine in
the system's **Text-to-speech** settings (the app's Settings screen has
a shortcut).

---

# Project Structure

* `:app` – The app shell: the unified voice list (installed languages,
  all languages, flat search), voice detail (preview / set active /
  delete), Settings, Help, and the reader.
* `:tts-service` – Android `TextToSpeechService` backed by the piper-kotlin
  engine, so any app on the device can use Piper voices.
* `:voice-download` – Voice catalog browser with resumable downloads,
  MD5 verification, per-voice preview, and listen-before-install samples.
* `piper-kotlin` (sibling repo) – `:piper-engine` (JNI over libpiper),
  `:piper-utils` (speech-rate curve, sentence splitting, markers),
  `:piper-player` (streaming playback).

## Notes

- Speech-rate mapping: Android rates are physical multipliers
  (100 = 1x), clamped to 2.2x so `length_scale` never drops below 0.45
  (PT-BR sibilant protection).
- All user-facing strings live in `res/values/strings.xml`.

# License

GPL-3.0, matching piper1-gpl.
