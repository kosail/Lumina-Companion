# PLAN.md — Lúmina Companion App (Android + Desktop)

> **Audience: AI agents.** This is the execution plan for building the companion client.
> It is **not** a source of truth. `INVARIANTS.md` (rank 1) and `AGENTS.md` (workflow) outrank it,
> and the wire contract `../Lumina-BETA-RPI-2W/docs/API_CONTRACT.md` is authoritative for protocol.
> If this plan conflicts with any of them, they win — stop and ask.

---

## 1. Mission and strategy

Build the FR-11 companion client for **Android + Desktop (JVM)** exactly to `INVARIANTS.md` and
`AGENTS.md`: status dashboard, volume/mute, people list, runtime start/stop, and person enrollment
from the Pi camera or from photos. The primary users are **blind and low-vision** people.

**Goal: fast development, extremely reliable code.** The reliability strategy is:

1. **Verify before writing.** Every API is checked against the pinned version's source contract or
   official docs before it is used (FE-INV-001). No assumed signatures.
2. **Test-first pure core.** All protocol/domain logic is pure and fully unit-tested before it is
   wired to the UI.
3. **Transport behind interfaces.** Sockets live only in `transport/`; everything else is testable
   with fakes and no device or network (FE-INV-022/050).
4. **Small, phase-gated changes.** One concern per phase; each phase compiles, its tests pass, and it
   gets a `CHG-FE` entry (FE-INV-061).
5. **A vertical slice early.** Telemetry → dashboard is proven end-to-end against the mock server in
   Phase 2, de-risking the riskiest integration (Ktor raw sockets) before breadth.

### Hard environment constraint

The agent container has **no JDK, Gradle, Node, Kotlin, or git**. The agent **cannot compile or run
tests**. Therefore:

- **The user runs every build/test and pastes the output.** The agent reports facts, not impressions
  (FE-INV-062).
- The agent must **minimize compile round-trips**: verify APIs first, keep diffs small, prefer pure
  functions, avoid experimental APIs.
- A phase is not started until the previous phase's user-run gate is green.

---

## 2. Locked decisions (from the review)

| Topic | Decision | Notes |
|-------|----------|-------|
| Token provisioning | **Manual entry + paste** in a MiuiX `TextField`, masked with a reveal toggle | No new permission/dependency. |
| Photo source | **System Photo Picker only** (`PickMultipleVisualMedia`) | No permission; no phone-camera capture. |
| Persistence | **`multiplatform-settings` 1.3.0** + **`multiplatform-settings-no-arg` 1.3.0** | User-added. Construct with the no-arg `Settings()` factory in `commonMain`; `MapSettings` is the test fake. |
| Test depth | **JVM `commonTest` unit tests + fakes only** | No Compose UI / instrumented test dependencies. |
| Target order | **Android-first**; Desktop is the fast mock-server dev loop | Both targets still build (FE-INV-021). |

### Governance gap this creates (must be closed in Phase 0)

`multiplatform-settings` and `multiplatform-settings-no-arg` are in the build but are **not** in the
FE-INV-020 stack table or AGENTS §3. Per FE-INV-020/061 this needs:

- an appended `CHG-FE` decision entry recording both artifacts at `1.3.0`, and
- an amendment to FE-INV-020 and AGENTS §3.

---

## 3. Requirements trace

| # | Requirement | Contract | Invariants | Delivered in |
|---|---|---|---|---|
| R1 | Live status dashboard (runtime/core/sensors/people/enroll) | §3.1, §4.1, §7 | FE-INV-030/031/034 | Phase 2 |
| R2 | Volume get/set/mute | §4.2, §5.1–5.3 | FE-INV-030/032 | Phase 3 |
| R3 | People list + refresh | §4.3, §5.4 | FE-INV-030/031 | Phase 4 |
| R4 | Runtime state/start/stop + "Start Lúmina" recovery | §4.4, §5.5–5.7, §6.5 | FE-INV-030/032 | Phase 3/4 |
| R5 | Enroll from Pi camera (streamed, cancel on 2nd conn) | §4.5–4.8, §5.8, §6.2–6.3 | FE-INV-032 | Phase 4 |
| R6 | Enroll from photos (resize/JPEG/base64, caps) | §4.10, §5.9, §6.4 | FE-INV-033 | Phase 4 |
| R7 | Configurable host/ports/token, token private | §2, §11 | FE-INV-034/053 | Phase 3 |
| R8 | Offline/local-only, reconnect/backoff, sentinels, ts jumps | §3.3, §9 | FE-INV-003/031 | Phase 2/5 |
| R9 | Accessibility + Spanish + MIUI | — | FE-INV-010/025/041 | Phase 2–5 |
| R10 | Commented code for a Java-dev maintainer | — | FE-INV-060 | every phase |

**Explicitly out of scope (FE-INV-040):** camera preview/streaming, currency recognition,
navigation, earbud battery, proximity distance, detected-object lists, face deletion,
accounts/subscriptions.

---

## 4. Architecture

```
shared/src/commonMain/kotlin/com/korealm/lumina/
  protocol/    Wire @Serializable models, Json config, command builders, status→domain mapper,
               sentinel handling, error mapping, liveness reducer          (pure, unit-tested)
  transport/   TelemetrySource + ControlClient (interfaces) and their Ktor implementations
               (ktor-network UDP/TCP), endpoints, backoff, cancellation
  data/        SettingsStore(iface) over multiplatform-settings, DeviceRepository,
               PeopleRepository, EnrollmentRepository, ImagePreparer (expect), fakes
  ui/          AppRoot (Scaffold + NavigationBar), dashboard/, people/, settings/,
               components/, theme/, composeResources (es-MX strings)
  di/          Koin modules (common) + platform module
androidApp/    MainActivity → startKoin + App()
desktopApp/    main.kt → startKoin + Window { App() }
```

### Interfaces (names stable; signatures evolve — AGENTS §5)

```kotlin
interface TelemetrySource { fun status(): Flow<TelemetryEvent> }   // Online(DeviceStatus) | Offline
interface ControlClient {
    suspend fun volumeGet(): ControlResult<VolumeState>
    suspend fun volumeSet(percent: Int): ControlResult<VolumeState>
    suspend fun setMuted(muted: Boolean): ControlResult<VolumeState>
    suspend fun people(): ControlResult<List<String>>
    suspend fun runtimeState(): ControlResult<RuntimeState>
    suspend fun runtimeStart(): ControlResult<RuntimeState>
    suspend fun runtimeStop(): ControlResult<RuntimeState>
    suspend fun enrollFromCamera(name: String, frames: Int): Flow<EnrollEvent>
    suspend fun enrollFromImages(name: String, images: List<ByteArray>): Flow<EnrollEvent>
    suspend fun cancelEnrollment(): ControlResult<EnrollEvent>
}
interface SettingsStore { var endpoint: Endpoint; var token: String }
interface ImagePreparer { fun prepare(raw: ByteArray): ByteArray }   // ≤1080px, JPEG q80
```

`ControlResult<T>` is a sealed type (`Ok`, `Unauthorized`, `Busy`, `BadRequest`, `Internal`,
`Io`) — expected failures are values, never thrown across coroutine boundaries (AGENTS §7).

### Data flow

```
UDP datagrams ─▶ KtorTelemetrySource ─▶ Flow<TelemetryEvent> ─▶ DeviceRepository ─┐
TCP replies   ─▶ KtorControlClient   ─▶ ControlResult<T>     ─▶ *Repository ──────┤
                                                                                  ▼
                                        ViewModel (StateFlow<ImmutableUiState>) ─▶ Composable
                                        events ▲────────────────────────────────────────┘
```

---

## 5. Key design decisions

1. **Connection-per-request TCP.** Each control command opens a short-lived connection, sends one
   line, reads one reply, closes. `enroll.camera.start` holds its own connection for the stream;
   `cancelEnrollment()` opens a **second** connection (contract §6.3, AGENTS §5). This matches the
   "sequential per socket" guarantee and avoids cross-command state.
2. **Wire vs domain.** `protocol/` decodes the wire exactly; a mapper converts to a domain
   `DeviceStatus` where sentinels become `null` and enums are tolerant strings (unknown ⇒ `Unknown`),
   never `0` (FE-INV-031). `Json { ignoreUnknownKeys = true; explicitNulls = false }` (FE-INV-030).
3. **Liveness reducer is pure.** Given (lastStatusArrivalMs, nowMs) it yields `Online`/`Offline`
   after 5 s; the clock is injected so tests are deterministic (contract §2, §9).
4. **No magic numbers.** `-1`/`null` are modeled as `null`; every sentinel has a test.
5. **Structured concurrency.** `viewModelScope`/injected scopes only; sockets and image work on
   `Dispatchers.IO`; leaving a screen cancels its enrollment work (FE-INV-052).
6. **Single Activity, 3-tab NavigationBar** (Dashboard / Personas / Ajustes), MiuiX `Scaffold`.

---

## 6. MiuiX component mapping (FE-INV-025)

| UI need | MiuiX component |
|---|---|
| App shell / popup host | `Scaffold` (required for Overlay/Window popups) |
| Top bar | `TopAppBar` |
| Tabs | `NavigationBar` (3 items; from the `miuix-ui` "basic" package, not a separate artifact) |
| Status cards | `Card` |
| Text / labels | `Text`, `SmallTitle` |
| Actions | `Button`, `IconButton`, `FloatingActionButton` |
| Volume | `Slider` |
| Mute | `Switch` |
| Inputs (name/host/port/token) | `TextField` |
| Enrollment progress | `ProgressIndicator` |
| Transient feedback | `Snackbar` |
| Confirmations | `OverlayDialog` / `WindowDialog` |
| Settings rows | `ArrowPreference`, `SwitchPreference`, `SliderPreference` |
| Icons / separators | `miuix-icons` + `Icon`, `Divider` |
| People refresh | `PullToRefresh` |

**Documented fallbacks (a default component is allowed only here, and must be noted in code + a
`CHG-FE` entry):** `LazyColumn` (MiuiX removed its own), the platform Photo Picker, Compose
`liveRegion` semantics, and `miuix-blur` (guard to API ≥ 31; the manifest already overrides it).

**Dependency note (`miuix-nav`).** The build depends on `top.yukonga.miuix.kmp:miuix-nav:0.9.4`
(a self-contained navigation runtime). It is **not** required for `NavigationBar` and is currently
unused; per user decision (CHG-FE-0007) it is retained provisionally because it may be useful, and
**must be removed in the final cleanup phase if still unused**.

---

## 7. Required Android platform changes (not new dependencies)

- Add **`android.permission.INTERNET`** to `androidApp/src/main/AndroidManifest.xml`; raw sockets
  fail without it.
- Evaluate binding traffic to the Wi-Fi hotspot network (the hotspot has no internet, so Android may
  prefer mobile data): a small Android `actual` using `ConnectivityManager` if needed. Verify in
  Phase 0; only add if the mock/device test shows misrouting.

---

## 8. Phases

Each phase ends with a **user-run gate** and an appended `CHG-FE` entry. Do not start a phase until
the previous gate is green.

### Phase 0 — API verification + governance (no app code) — ✅ COMPLETE (2026-09-21)
**Deliverables:** `docs/API_VERIFICATION.md` with `URL + access date` for each item below.
**Verify (FE-INV-001/024):**
- Ktor 3.6.0 `ktor-network`: `aSocket`/`SelectorManager`, `UdpSocket.send`/`receive`,
  TCP `connect`, `ByteReadChannel.readUTF8Line`.
- kotlinx-serialization-json 1.11.0: `ignoreUnknownKeys`, `explicitNulls`, tolerant enums.
- Koin 4.2.2: `koinViewModel()` in Compose, `startKoin` on Android/JVM.
- MiuiX 0.9.4: signatures for every component in §6; `miuix-blur` minimum API.
- `com.russhwolf:multiplatform-settings-no-arg:1.3.0`: `Settings()` signature and its **Android
  initialization requirement** (Startup/ContentProvider). If init is unreliable, fall back to
  explicit `SharedPreferencesSettings`/`PreferencesSettings` via `expect`/`actual`.
- Android `PickMultipleVisualMedia` + `BitmapFactory`/`Bitmap.compress` resize path.
- Compose Multiplatform `liveRegion` availability in 1.12.0.
**Governance:** append `CHG-FE-0004` (decision) for `multiplatform-settings` +
`multiplatform-settings-no-arg` 1.3.0; amend FE-INV-020 and AGENTS §3.
**Gate:** user confirms; any unresolved API ⇒ stop and ask.

### Phase 1 — Protocol core (pure) — ✅ COMPLETE (2026-09-21)
**Deliverables:** `protocol/` — wire models, Json, command builders, status mapper, sentinel
handling, error mapping, liveness reducer, image-cap validation.
**Tests:** decode every §4 example; unknown keys ignored; missing optionals tolerated; all sentinels;
exact command JSON strings (incl. token); error `code` → `ControlResult`; offline exactly after 5 s;
image caps (≤12, ≤8 MiB decoded, ≤16 MiB line).
**Gate:** `./gradlew :shared:allTests`.

### Phase 2 — Transport + DI + desktop vertical slice — ✅ COMPLETE (2026-09-21)
**Deliverables:** Ktor UDP/TCP implementations behind the interfaces; endpoint config; backoff
(1/2/5/10 s); cancellation; Koin modules; minimal Dashboard rendering live telemetry.
**Tests:** framing/parsing with fake channels; ViewModel state with fake `TelemetrySource`.
**Gate:** `./gradlew :shared:allTests`, then `:desktopApp:run` against `npm run dev` shows live
status and "Sin conexión" when the server stops.

### Phase 3 — Control + Settings — ✅ COMPLETE (2026-09-22)
**Deliverables:** volume set/mute, runtime start/stop, settings screen (host/ports/token) persisted
via `SettingsStore`, `unauthorized`/`busy`/`internal` UX, reconnect banner.
**Tests:** command builders already covered; ViewModel tests with fake `ControlClient`/`SettingsStore`.
**Gate:** `./gradlew :shared:allTests :androidApp:assembleDebug`.

### Phase 4 — People + enrollment — 🚧 IN PROGRESS (2026-09-22)
**Deliverables:** People screen + refresh; camera enroll with progress and cancel; photo enroll via
Photo Picker + `ImagePreparer` + `enroll.images`; "Start Lúmina" when `enroll.error.runtime !=
"started"`; live-region announcements.
**Tests:** enrollment state machine with fake `ControlClient`; image-prep unit tests (size/format).
**Gate:** `./gradlew :shared:allTests :androidApp:assembleDebug`, then the mock scenarios.

### Phase 5 — Hardening + accessibility
**Deliverables:** full resilience-matrix coverage, TalkBack audit, MIUI fallback audit, §13 DoD
checklist.
**Gate:** `./gradlew check`; manual TalkBack pass on a device/emulator.

### Phase 6 — Integration validation + docs
**Deliverables:** end-to-end vs `Testing_server` (desktop + emulator/device), README/CHANGELOG
updates.
**Gate:** user-confirmed end-to-end transcript.

---

## 9. Testing strategy

- **Pure logic** (protocol, sentinels, liveness, caps, image math): JVM `commonTest`, fully covered.
- **ViewModels:** fakes for `TelemetrySource`, `ControlClient`, `SettingsStore` (`MapSettings`).
- **Integration:** the mock agent in `../Testing_server/` is the oracle.
  `npm run verify` (12 checks) and the live scenario flags.
- **Accessibility:** manual TalkBack checklist (§10); no automated UI tests (no new deps).

### Scenario matrix (from the mock)

| Scenario | Expected UI |
|---|---|
| `runtime-down` | "Sin conexión"/runtime down; offer `runtime.start`; people empty |
| `volume-unknown` | volume shows "—", control still enabled |
| `camera-dark` | day/night = "unknown" |
| `earbuds-absent` | sink ≠ ready ⇒ "Audio no listo" |
| `enroll-fail` | Spanish error; "Start Lúmina" if `runtime != started` |
| `enroll-slow` | progress advances; cancel works |
| `busy` (2nd enroll) | "Ocupado"; wait for active enrollment |
| stop server | offline after 5 s, then auto-recover |

---

## 10. Accessibility and MIUI checklist (FE-INV-010/025)

- [ ] Every interactive element has a Spanish `contentDescription`/semantics label.
- [ ] Touch targets ≥ 48 dp.
- [ ] No meaning by color alone.
- [ ] Connection and enrollment state changes announced (`liveRegion`).
- [ ] Logical focus order; decorative nodes excluded.
- [ ] Large text, high contrast via theme colors.
- [ ] MiuiX component used everywhere; every fallback documented.

---

## 11. Risks and mitigations

| Risk | Mitigation |
|---|---|
| Bleeding-edge versions (AGP 9.1.1, Kotlin 2.4.20, CMP 1.12.0) API drift | Phase 0 verification against pinned docs/source; ask if unresolved |
| No local compiler ⇒ slow feedback | Verify first, small diffs, pure core tested on the user's JVM |
| Android routing to mobile data on a no-internet hotspot | §7 network-binding item, added only if observed |
| MiuiX missing a component | Documented fallback + `CHG-FE` entry |
| `multiplatform-settings-no-arg` Android init | Phase 0 check; fall back to explicit `expect`/`actual` |
| Enrollment blocking/ cancel races | Connection-per-request + dedicated cancel connection; fake-driven state-machine tests |

---

## 12. Changelog plan

- `CHG-FE-0004` — create this plan (docs).
- `CHG-FE-0005` — rewrite README for humans + fix the `Testing_server` relative path (docs).
- `CHG-FE-0006` — multiplatform-settings 1.3.0 decision + FE-INV-020/AGENTS amendment (Phase 0).
- One `CHG-FE` entry per phase thereafter (`impl`/`fix`/`test`).

---

## 13. References

- `INVARIANTS.md` (rank 1), `AGENTS.md`, `CHANGELOG.md`
- `../Lumina-BETA-RPI-2W/docs/API_CONTRACT.md` (proto 1, authoritative)
- `../Testing_server/` (mock agent; delete when the app ships)
- MiuiX components: `https://compose-miuix-ui.github.io/miuix/components/`
- Ktor sockets: `https://ktor.io/docs/` · Koin: `https://insert-koin.io/docs/`
- multiplatform-settings: `https://github.com/russhwolf/multiplatform-settings`
