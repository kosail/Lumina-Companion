# Lúmina — Companion App

The phone and desktop app for **Lúmina**, an edge-AI assistive device that narrates the world
through bone-conduction audio for **blind and low-vision people**.

This app is the **companion client**: it connects to the Lúmina device (a Raspberry Pi Zero 2 W
running as a local Wi-Fi hotspot) and lets you check on it, adjust it, and enroll people it should
recognize. Everything runs **on the local network** — no cloud, no accounts, no internet.

> **Accessibility is a functional requirement here, not a nice-to-have.** Our users cannot see the
> screen. Every screen and control must work with a screen reader (TalkBack), use large touch
> targets, and speak its state changes in Spanish.

---

## What it does

- **Status dashboard** — is the device reachable, is the runtime running, temperature, memory, FPS,
  battery-free sensor info, volume, day/night, and the list of enrolled people.
- **Volume** — read, set, and mute the device's audio.
- **People** — see who is enrolled, and add someone new.
- **Enrollment** — add a person using the **device camera** (live) or **photos from your gallery**.
- **Runtime control** — start or stop the Lúmina runtime, and recover it if it goes down.

**Out of scope:** camera preview/streaming, currency recognition, navigation, earbud battery,
detecting objects, deleting people, and anything account- or subscription-related.

---

## Tech at a glance

| Piece | Choice |
|---|---|
| Language | Kotlin Multiplatform |
| UI | Compose Multiplatform + **MiuiX** (MIUI design language) |
| Targets | Android and Desktop (JVM) |
| Sockets | Ktor `ktor-network` (raw UDP + TCP) |
| JSON | kotlinx-serialization |
| Dependency injection | Koin |
| Settings storage | multiplatform-settings |
| Build | Gradle wrapper (AGP) |

The exact pinned versions live in [`gradle/libs.versions.toml`](gradle/libs.versions.toml).

---

## Repository layout

```
Lumina-BETA-ANDROID/
  androidApp/     Android entry point (MainActivity)
  desktopApp/     Desktop entry point (main.kt)
  shared/         All shared logic and UI (the bulk of the app)
    src/commonMain/   protocol · transport · data · ui · di
    src/androidMain/  Android-specific pieces (expect/actual)
    src/jvmMain/      Desktop-specific pieces (expect/actual)
    src/commonTest/   Unit tests
  PLAN.md         How the app is being built, phase by phase
```

---

## Getting started

### Prerequisites

- **Android Studio** (recommended), or a JDK compatible with the Android Gradle Plugin.
- **Android SDK** with the platform matching `compileSdk` in `libs.versions.toml`.
- **Node 18+** — only for the mock device server (see below).
- The Android SDK location is read from `local.properties` (not committed).

### Build and run

```bash
# Android app (debug)
./gradlew :androidApp:assembleDebug

# Desktop app (fast development loop)
./gradlew :desktopApp:run
# or, with hot reload:
./gradlew :desktopApp:hotRun --auto
```

### Run the tests

```bash
# Unit tests for all shared logic (fast, no device)
./gradlew :shared:allTests

# Everything
./gradlew check
```

---

## Developing without the hardware

A mock Lúmina device lives one directory up, in [`../Testing_server`](../Testing_server). It speaks
the exact same protocol as the real device, so you can build and test the whole app on your laptop.

```bash
cd ../Testing_server
npm install
npm run dev        # starts the mock device; restarts when you edit files
```

Then point the app at it:

| Where the app runs | Address to use |
|---|---|
| Desktop | `127.0.0.1` |
| Android emulator | `10.0.2.2` |
| Physical Android device | The Pi's LAN IP (or `adb reverse tcp:47601 tcp:47601` for TCP only) |
| Real Lúmina device | `10.42.0.1` (the hotspot gateway, the default) |

The mock's default control token is `dev-token`. The real device's token lives on the Pi.

The mock can also simulate problems so you can test every state: `runtime-down`, `volume-unknown`,
`camera-dark`, `earbuds-absent`, `enroll-fail`, `enroll-slow`, and more. See its README for the full
list.

---

## Connecting to a real device

1. Join the Lúmina hotspot from your phone or laptop.
2. Open the app's **Settings** and enter the gateway address and the control token.
3. The app subscribes to live telemetry over UDP and sends commands over a token-gated TCP channel.

Host, ports, and token are all configurable. The token is kept in private app storage and is never
logged or committed.

---

## Project documents

This repo uses an explicit, spec-driven workflow. Read these before contributing:

| File | What it is |
|---|---|
| [`INVARIANTS.md`](INVARIANTS.md) | **Highest authority.** Non-negotiable facts and decisions. |
| [`AGENTS.md`](AGENTS.md) | The operating manual: workflow, style, tooling, rules. |
| [`PLAN.md`](PLAN.md) | The build plan, phase by phase. |
| [`CHANGELOG.md`](CHANGELOG.md) | Append-only record of every meaningful change. |
| [`../Lumina-BETA-RPI-2W/docs/API_CONTRACT.md`](../Lumina-BETA-RPI-2W/docs/API_CONTRACT.md) | The wire protocol (owned by the runtime repo; read-only here). |

The wire protocol is owned by the **runtime repository**. This repo only consumes it — never change
the protocol here; changes start on the runtime side.

---

## Contributing

- Keep the app's logic **out of the UI**: screens render state and emit events; view models own the
  state.
- Everything is **commented**, written for a maintainer coming from Java.
- Add a `CHG-FE-NNNN` entry to `CHANGELOG.md` for every meaningful change.
- Build and test with `./gradlew` only.
- Never hardcode the host, ports, or token.
- Every new user-facing string is **Spanish** and lives in string resources.
