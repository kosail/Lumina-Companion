# AGENTS.md — Lúmina Companion App (Operating Manual for AI Agents)

> This file is the **operating manual** for AI agents and contributors working on the Lúmina
> companion app. It defines **how** we work (style, workflow, tooling, rules).
>
> It does **not** define the app's core decisions. Those live in `INVARIANTS.md` and are ranked
> above this file. The wire protocol is owned by the runtime repository and is **read-only** here.

---

## 1. Project identity

- **Project:** Lúmina — an edge-AI assistive device that narrates the environment through
  bone-conduction audio for blind and low-vision users. Spanish-first, fully on-device.
- **This module:** the **companion client** for **Android and Desktop (JVM)**, built with Kotlin
  Multiplatform + Compose Multiplatform. It pairs with the Raspberry Pi runtime over the device's
  local hotspot.
- **Primary users:** **blind and low-vision people.** Accessibility is a functional requirement,
  not a polish item (see §6 and `INVARIANTS.md` FE-INV-010).
- **What the app does:** shows device status, controls volume/mute, lists and enrolls people,
  starts/stops the runtime, and enrolls faces from the camera or photos. Nothing else (FE-INV-040).
- **Division of work:** the **user** owns the app; the transport/DI/JSON dependencies are already in
  the build (§3). AI agents assist with code, docs, and tests. The C++ runtime is a **separate
  repository** the app must never modify (FE-INV-004).

### Related documents (do not duplicate; link instead)

| File | Role |
|------|------|
| `INVARIANTS.md` | Highest source of truth — non-negotiable facts/decisions |
| `CHANGELOG.md` | Append-only machine-readable history (`CHG-FE-NNNN`) |
| `../Lumina-BETA-RPI-2W/docs/API_CONTRACT.md` | **External, read-only** wire contract (proto 1) |
| `../Lumina-BETA-RPI-2W/docs/COMPANION.md` | Runtime-side companion architecture (context only) |
| `../Lumina/Testing_server/` | Mock agent server used to develop/test the app |

---

## 2. Source-of-truth hierarchy (READ THIS FIRST)

```
INVARIANTS.md            ← highest; never violate or silently change
  > AGENTS.md            ← this file (workflow/style rules)
    > code comments / any other file

../Lumina-BETA-RPI-2W/docs/API_CONTRACT.md   ← external, read-only; authoritative for the wire
CHANGELOG.md                                 ← record of every meaningful change to the above
```

**Conflict rule.** If anything conflicts with an invariant, the invariant wins. If a task seems to
require changing or violating an invariant, **STOP and ask the user**. Never resolve the conflict
yourself. For protocol questions, the contract wins — and the contract can only be changed in the
runtime repository (FE-INV-004).

**FE-INV-001 applies to you at all times: NEVER ASSUME.** No API contract may be assumed. Verify
every API against the **pinned version's source contract or the official documentation** before
using it. If neither can be verified, or your confidence is below `0.80`, stop and ask the user.

---

## 3. Stack and platforms

- **Language:** Kotlin Multiplatform **2.4.20**. Official code style (`kotlin.code.style=official`).
- **UI:** Compose Multiplatform **1.12.0** + Material3 **1.12.0-alpha03**; design kit **MiuiX KMP
  0.9.4** (`top.yukonga.miuix.kmp`).
- **Build:** Android Gradle Plugin **9.1.1**; build with the **Gradle wrapper** only (`./gradlew`,
  FE-INV-023). Versions live in `gradle/libs.versions.toml`.
- **Targets:** `androidApp` (Android, `com.korealm.lumina`, minSdk **24**, compile/targetSdk **37**)
  and `desktopApp` (JVM). JVM target **11**.
- **Transport:** **Ktor 3.6.0** — raw sockets via `io.ktor:ktor-network` (`connect()` for TCP,
  `UdpSocket` for UDP); JSON via **kotlinx-serialization-json 1.11.0**. The `ktor-client-*` HTTP
  artifacts are kept for later but are **not** the raw transport (FE-INV-020/022/024).
- **DI:** **Koin 4.2.2** (`koin-core`, `koin-compose`, `koin-compose-viewmodel`, `koin-android`).
- **Design language:** **MIUI**, via MiuiX KMP (`top.yukonga.miuix.kmp`, all modules). Every UI
  element uses a MiuiX component; default Compose/Material components are allowed **only** where
  MiuiX has no equivalent (FE-INV-025).
- **Networking scope:** UDP `47600` (subscribe + 1 Hz status) and TCP `47601` (token-gated control)
  against the Pi gateway on the hotspot. No cloud, no relay (FE-INV-003/034).
- **Host for builds/tests:** the developer machine; unit tests run on the JVM with no device.

---

## 4. Repository layout

```
Lumina-BETA-ANDROID/
  AGENTS.md                  # this file
  INVARIANTS.md              # foundation (rank 1)
  CHANGELOG.md               # append-only history (AI-oriented)
  settings.gradle.kts        # includes :androidApp, :desktopApp, :shared
  build.gradle.kts
  gradle/libs.versions.toml  # version catalog (single source of pinned versions)
  androidApp/                # Android entry point (MainActivity)
  desktopApp/                # JVM/desktop entry point (main.kt)
  shared/
    src/commonMain/kotlin/com/korealm/lumina/   # all shared logic + UI
    src/androidMain/                            # expect/actual: Android
    src/jvmMain/                                # expect/actual: JVM
    src/commonTest/                             # kotlin-test unit tests
```

Intended package layout under `commonMain` (create as needed; keep new code inside the matching
package and **do not** add new top-level modules without asking):

| Package | Responsibility |
|---------|----------------|
| `protocol/` | Wire types + JSON encode/decode, command builders, sentinel handling (pure) |
| `transport/` | Ktor UDP/TCP sockets behind interfaces (`TelemetrySource`, `ControlClient`) |
| `data/` | Repositories + settings/config storage, fakes for tests |
| `ui/` | Screens, components, theme, view models + immutable UI state |
| `di/` | Koin module wiring |

---

## 5. Architecture rules (non-negotiable, see INVARIANTS)

1. **Interface-first + dependency injection.** Transport and repositories are reached only through
   interfaces; concrete implementations are wired with **Koin** (FE-INV-050). Logic must be
   testable with fakes and **no device or network**.
2. **Unidirectional data flow.** UI state is an immutable `data class` exposed by a `ViewModel` as
   `StateFlow`; events go up, state comes down (FE-INV-051).
3. **No business logic in composables.** Composables render state and emit events; they may hold
   only local UI state (e.g. an expanded flag).
4. **No global mutable state.** Pass dependencies explicitly through the DI graph.
5. **Structured concurrency.** `viewModelScope`/injected scopes; no `GlobalScope`; sockets and IO on
   `Dispatchers.IO`; never block the main thread (FE-INV-052).
6. **Pure logic is separated and tested.** Protocol encode/decode, base64, sentinel handling, and
   status reduction live in plain functions/classes with unit tests.
7. **Single responsibility.** One primary type per file where practical; keep files focused.

### Core interfaces (sketch — exact signatures evolve, names are stable)

```kotlin
// Telemetry is push-based: the transport parses datagrams and emits typed status.
interface TelemetrySource {
    fun status(): Flow<DeviceStatus>       // 1 Hz; emits and lets the UI mark staleness
}

// Control is request/response over the token-gated TCP channel.
interface ControlClient {
    suspend fun volumeGet(): VolumeState
    suspend fun volumeSet(percent: Int): VolumeState
    suspend fun setMuted(muted: Boolean): VolumeState
    suspend fun people(): List<String>
    suspend fun runtimeState(): RuntimeState
    suspend fun runtimeStart(): RuntimeState
    suspend fun runtimeStop(): RuntimeState
    suspend fun enrollFromCamera(name: String, frames: Int): Flow<EnrollEvent>
    suspend fun enrollFromImages(name: String, images: List<ByteArray>): Flow<EnrollEvent>
    suspend fun cancelEnrollment(): EnrollEvent
}
```

---

## 6. Accessibility rules (mandatory)

The users of this app cannot see the screen. Treat the following as defects if missing:

1. Every interactive element has a Spanish `contentDescription`/`semantics` label; decorative
   elements are excluded from semantics.
2. Touch targets are **≥ 48 dp**; do not shrink them for visual density.
3. Never encode meaning in color alone — pair color with text, shape, or a spoken label.
4. Announce state changes (connection lost/restored, enrollment progress and completion) via a
   `liveRegion`/semantics announcement.
5. Maintain a logical focus order; avoid focus traps and duplicate focus.
6. Prefer large text and high contrast; use theme colors, not ad-hoc low-contrast values.

---

## 7. Coding style

Official Kotlin style is the baseline. Project conventions:

| Element | Convention | Notes |
|---------|------------|-------|
| Types (class/interface/enum) | `PascalCase` | `data class` for state |
| Functions / properties | `camelCase` | |
| Constants | `UPPER_SNAKE_CASE` | `const val` in a companion/object |
| Packages | `com.korealm.lumina.<layer>` | see §4 |
| Files | `PascalCase.kt` matching the primary type | one primary type per file |

- **Immutability first:** `val` over `var`; immutable `data class` for UI state and wire models.
- **Nullability:** model "unknown" explicitly (`null`/sealed types), never with a magic number
  (FE-INV-031).
- **Errors:** use `Result`/sealed types for expected failures; do not throw across coroutine
  boundaries. Surface a user-readable Spanish message, not a raw exception.
- **Coroutines:** `suspend` functions for async work; `Flow` for streams; honor cancellation.
- **Compose:** keep composables small and stateless; hoist state; use `key` in lists; avoid heavy
  work in composition; use `remember` for expensive derived values.
- **Strings:** all user-facing text in `composeResources`/string resources, Spanish (`es_MX`).
- **UI kit:** use **MiuiX** components; fall back to a default Compose/Material component **only**
  when MiuiX has no equivalent, and document the gap (FE-INV-025).
- **No wildcard imports**; include only what you use.

---

## 8. Commenting policy (mandatory)

> The base agent tooling defaults to "do not add comments". **That default is overridden here.**
> In this repository, comments are **required** and reviewers will reject uncommented code.

Rules:

1. **Every file** starts with a short block explaining its responsibility and where it sits in the
   app (protocol / transport / data / ui).
2. **Every public type and function** is documented (KDoc): what it does, parameters, return value,
   and any threading/`suspend` assumptions.
3. **Every non-obvious block** explains *why*, not just *what*.
4. **Add a "Kotlin note"** when a construct differs from Java in a way the maintainer would not
   expect: `expect`/`actual`, Compose recomposition/state hoisting, coroutines/`Flow`, Koin scopes.
5. Do **not** restate the code (`i++ // increment i`).
6. TODOs must include an owner tag and reference an invariant or changelog id, e.g.
   `// TODO(ui): announce progress — FE-INV-010 / CHG-FE-0002`.

---

## 9. Documentation-fetching rules

FE-INV-001 requires verifying every API against the pinned source contract or the official
documentation. When you need an API or behavior:

1. **Check the pinned version's source contract, or fetch the official source** for the **pinned
   version** in `libs.versions.toml`.
2. **Cite URL + access date** in the code comment or the changelog entry.
3. Prefer these canonical sources (not blog aggregators):

| Topic | Canonical source |
|-------|------------------|
| Android platform | `https://developer.android.com/` |
| Kotlin / coroutines | `https://kotlinlang.org/docs/` · `https://kotlinlang.org/api/kotlinx.coroutines/` |
| Compose Multiplatform | `https://www.jetbrains.com/help/kotlin-multiplatform-dev/compose-multiplatform.html` |
| Compose (Android docs) | `https://developer.android.com/develop/ui/compose` |
| Ktor (client sockets) | `https://ktor.io/docs/` |
| Koin | `https://insert-koin.io/docs/` |
| MiuiX KMP (repo) | `https://github.com/Yukonga/miuix-kmp` |
| MiuiX components | `https://compose-miuix-ui.github.io/miuix/components/` |
| Accessibility | `https://developer.android.com/guide/topics/ui/accessibility` |

4. If a lookup fails or confidence < `0.80`, **ask the user** — do not guess.

---

## 10. Protocol integration rules

The wire contract is `../Lumina-BETA-RPI-2W/docs/API_CONTRACT.md` (**proto 1**). Summary for quick
orientation — the contract is authoritative if they ever disagree:

- **UDP `47600`:** send `{"t":"subscribe"}`; receive a 1 Hz `status` datagram (runtime, core,
  sensors, people, enrollment). Offline after **5 s** of silence.
- **TCP `47601`:** newline-delimited JSON; **every** request carries the shared token.
  - Commands: `volume.get/set/mute`, `people.list`, `runtime.state/start/stop`,
    `enroll.camera.start/cancel`, `enroll.images`.
  - Connections are processed **sequentially**; enrollment blocks its socket and is cancelled by a
    **second** connection. Handle `busy`, `bad_request`, `unauthorized`, `ok:false`, `enroll.error`.
  - Sentinels: `volume:-1`, `luma:-1`, `tempC/load1/memAvailableKb:null`, `people:[]` when down.
- **Images:** resize to ≤ 1080 px height, JPEG ~q80, ≤ 500 KB each, 3–5 photos, raw base64; total
  ≤ 8 MiB decoded, one request ≤ 16 MiB.
- **Endpoints:** gateway `10.42.0.1` by default; desktop `127.0.0.1`; emulator `10.0.2.2`; physical
  device the Pi's LAN IP. All configurable (FE-INV-034/053).

Do not invent fields or commands. If the app needs something the contract lacks, **stop and ask**;
it must be added in the runtime repo first (FE-INV-004/030).

---

## 11. Testing and commands

- **Framework:** `kotlin-test` in `shared/src/commonTest` (JVM-run, no device).
- **Pure logic is fully tested:** protocol encode/decode, sentinel handling, base64, status
  reduction, command builders, error mapping.
- **Fakes over mocks:** every interface has a fake in `commonTest`/`data/`.
- **Integration** is exercised against the mock agent server in `../Lumina/Testing_server/`.
- A change is not "done" until it compiles and its tests pass.

```bash
# Unit tests for shared logic (fast, no device)
./gradlew :shared:allTests

# Android debug build
./gradlew :androidApp:assembleDebug

# Desktop client (dev loop)
./gradlew :desktopApp:run

# Everything
./gradlew check

# Mock agent server (separate terminal; Node + tsx)
cd ../Lumina/Testing_server && npm run verify   # or: npm run dev
```

---

## 12. Agent workflow rules

1. **Read `INVARIANTS.md` before starting any task.** Reject/ask if the task conflicts with it.
2. **Never assume (FE-INV-001).** Fetch docs or ask.
3. **Never touch the runtime repository** (FE-INV-004). Read its contract; do not edit it.
4. **Small, reviewable changes.** One concern per change; keep diffs focused.
5. **Update `CHANGELOG.md` for every meaningful change or decision** (FE-INV-061), using the exact
   YAML-block format documented at the top of that file. Append only.
6. **Never silently change an invariant.** Stop and ask first.
7. **Never commit secrets/tokens.** No credentials in the repo; tokens live in private storage.
8. **Do not add a dependency** without approval (FE-INV-020). Ktor, Koin, and kotlinx-serialization
   are already wired; the user owns further dependency changes.
9. **Report facts, not impressions** (FE-INV-062): paste build/test output, cite the command.
10. **Ask when confidence < 0.80.** Asking is cheap; a wrong assumption costs days.

---

## 13. Definition of Done (checklist)

A task is done when **all** of the following hold:

- [ ] It does not violate any invariant (`INVARIANTS.md`).
- [ ] It builds (`:androidApp:assembleDebug` and/or `:desktopApp`) and `:shared:allTests` passes.
- [ ] Pure logic has unit tests; fakes used instead of real network/device.
- [ ] Accessibility is satisfied (semantics labels, ≥48 dp targets, announcements) — §6.
- [ ] UI uses **MiuiX** components; any default-component fallback is justified and documented
      (FE-INV-025).
- [ ] State is immutable and managed by a `ViewModel`; no logic in composables; no global state.
- [ ] Every new type/function/block is commented per §8, including Kotlin notes where useful.
- [ ] User-facing text is Spanish and lives in string resources.
- [ ] External facts are cited (URL + access date) per §9.
- [ ] `CHANGELOG.md` has an appended `CHG-FE-NNNN` entry.
- [ ] No new dependency was introduced without approval; no runtime-repo file was touched.

---

## 14. Prohibited actions

- ❌ Assuming anything instead of consulting docs or asking (FE-INV-001).
- ❌ Changing/violating an invariant without explicit user approval.
- ❌ Creating, editing, or deleting anything under `../Lumina-BETA-RPI-2W/` (FE-INV-004).
- ❌ Inventing protocol fields/commands or diverging from `API_CONTRACT.md` (FE-INV-030).
- ❌ Cloud calls, analytics, telemetry, or any internet dependency (FE-INV-003).
- ❌ Opening sockets outside the transport layer, or adding a second network library
  (FE-INV-022).
- ❌ `GlobalScope`, blocking the main thread, or leaking coroutine scopes (FE-INV-052).
- ❌ Business logic inside composables; mutable global state (FE-INV-051).
- ❌ Hardcoding the host, ports, or token; logging/committing the token (FE-INV-053).
- ❌ Hardcoded user-facing strings instead of Spanish string resources (FE-INV-041).
- ❌ Using a default Compose/Material component where a MiuiX equivalent exists (FE-INV-025).
- ❌ Adding dependencies or bumping pinned versions without approval (FE-INV-020/023).
- ❌ Writing code without comments (FE-INV-060).
