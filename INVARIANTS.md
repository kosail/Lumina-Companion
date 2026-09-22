# INVARIANTS.md — Lúmina Companion App (Android + Desktop)

> **Rank: 1 (HIGHEST).** This file outranks `AGENTS.md`, any code comment, and any suggestion made
> by an AI agent or a human contributor.
>
> The entries below are facts and decisions that **MUST NOT be violated or silently changed**.
> They are the foundation of the companion app. The wire contract itself is owned by the runtime
> repository (`../Lumina-BETA-RPI-2W/docs/API_CONTRACT.md`) and is **read-only from here**.

---

## 0. How to read this file

Each invariant has:

- **ID** — stable identifier, never reused. The `FE-` prefix means "frontend" and keeps these IDs
  from colliding with the runtime's `INV-*` IDs.
- **Severity** — one of `CORE`, `HARD`, `SCOPE`, `PROCESS` (see legend).
- **Statement** — the rule itself.
- **Rationale** — *why* it exists (so a future agent does not "optimize it away").
- **Source** — where it came from (user instruction, runtime invariant, contract, research).
- **Changeability** — what it would take to change it.

### Severity legend

| Severity  | Meaning |
|-----------|---------|
| `CORE`    | Meta-rule that governs **all** work and every other invariant. |
| `HARD`    | Platform/stack fact. Changing it means changing the app stack or its targets. |
| `SCOPE`   | Deliverable decision. Changing it changes what the app ships. |
| `PROCESS` | Workflow rule for agents. |

### Change protocol (mandatory)

1. **Never silently change an invariant.** If a task appears to require it, **STOP and ask the user**.
2. Any accepted change requires: explicit human approval **and** an append-only entry in
   `CHANGELOG.md`.
3. Changed invariants are never rewritten in place without leaving a trace — supersede with a new
   ID or an explicit "amended by CHG-FE-XXXX" note.
4. If two invariants appear to conflict, **stop and ask the user**; do not pick one.

---

## 1. CORE invariants (meta-rules)

### FE-INV-001 — NEVER ASSUME  🟥 `CORE`

**Statement.** Never assume anything. **No API contract may be assumed** — not from memory, not from
training data, and not from an example or tutorial written for a different version. Kotlin is a
young language and Compose Multiplatform's API changes frequently, so an API you "remember" may not
exist in the pinned version, may have a different signature, or may behave differently.

Before using **any** API (a function, a type, a property, a parameter, a default, a lifecycle
guarantee), verify it against **one of**:

1. **The source-code contract of the exact pinned version** — the resolved dependency's declared
   signatures/types (read the actual source, the published API, or the IDE's resolved declaration),
   **or**
2. **The official documentation online** for that exact pinned version.

If neither can be verified, or confidence in the answer is **below `0.80`**, **STOP and ask the
user**. Do not guess, do not "try it and see", do not proceed on memory.

**Rationale.** This app integrates with a bespoke, non-standard protocol and with fast-moving
Kotlin Multiplatform / Compose libraries. A confidently wrong assumption about the wire format or a
Compose API is the single largest source of wasted time and silent breakage. Examples are not
contracts; only the pinned source or the official docs are. Asking is cheap.

**Operationalization.**
- Prefer the **source contract** over examples, blog posts, or snippets.
- Verify against the **pinned** version in `gradle/libs.versions.toml`, not "latest".
- External facts must be backed by a source: **URL + access date**, or the dependency
  coordinate/source file, recorded in comments/docs.
- If a signature does not match what you assumed, **stop and re-verify** before coding around it.
- Self-assessed confidence must be explicit when it is not obvious from a cited source.
- Asking is always allowed and never counts as failure.

**Source.** Runtime `INV-001`; direct user instruction (2026-09-21) stressing Kotlin / Compose
Multiplatform API volatility.

**Changeability.** Not changeable without the user explicitly revoking the instruction.

---

### FE-INV-002 — Invariants outrank everything  🟥 `CORE`

**Statement.** This file is the highest source of truth for the companion app. No agent, plan,
requirement, or code may contradict it. Where sources conflict, this file wins. For anything on the
wire, `API_CONTRACT.md` (runtime repo) is authoritative and this app must conform to it.

**Rationale.** Without a single immutable foundation, architecture drifts silently between agents
and across the two repositories.

**Source.** Runtime `INV-002`; user instruction defining this file's role.

**Changeability.** Only via the change protocol above.

---

### FE-INV-003 — Offline and local-only by default  🟥 `CORE`

**Statement.** The app communicates **only** with the Lúmina device over the local hotspot. It makes
**no** cloud calls, uses **no** external services, and emits **no** analytics or telemetry. There is
no internet dependency in any shipped path.

**Rationale.** Core product promise (edge AI, privacy, works without connectivity). The runtime is
network-free by design (runtime `INV-003`/`INV-034`); the app must not reintroduce a network
dependency or leak user data.

**Source.** Runtime `INV-003`, `INV-034`; `SPECS.md` FR-11.

**Changeability.** Requires user approval; contradicts the product's core value proposition.

---

### FE-INV-004 — Never modify the runtime repository  🟥 `CORE`

**Statement.** The app repository **must never** create, edit, move, or delete any file under the
runtime repository `../Lumina-BETA-RPI-2W/`, including `docs/API_CONTRACT.md`. The contract is an
external, **read-only** dependency. If the app needs a protocol change, **STOP and ask the user**;
the change is made in the runtime repository, logged there, and only then adopted here.

**Rationale.** The two repositories have separate histories and separate governance. Cross-repo
writes destroy traceability and can silently break the runtime.

**Source.** Direct user instruction (2026-09-21): "never pollute the C++ Lúmina runtime folder".

**Changeability.** Not changeable without the user explicitly revoking the instruction.

---

## 2. ACCESSIBILITY invariants

### FE-INV-010 — Accessibility is a hard requirement  🟥 `CORE`

**Statement.** The primary users are **blind and low-vision** people. Every screen and control must
be fully operable with a screen reader (TalkBack) and must not rely on vision:

1. Every interactive element has a meaningful `contentDescription` / `semantics` label in Spanish.
2. Touch targets are **≥ 48 dp**.
3. Information is **never conveyed by color alone** (pair with text, shape, or a spoken label).
4. Text meets high-contrast guidance; do not hardcode low-contrast colors outside the theme.
5. A logical, predictable focus order; no focus traps; decorative elements are marked as such.
6. Important state changes (connection lost, enrollment progress/finished) are announced
   (e.g. `liveRegion`) — not only drawn.

**Rationale.** An inaccessible companion app defeats the product's inclusion mission and is a
functional defect, not a polish item.

**Source.** Product identity (blind/low-vision users); runtime `INV-040`; direct user instruction.

**Changeability.** Not changeable; accessibility fixes are always in scope.

---

## 3. UI / UX invariants

### FE-INV-025 — MIUI is the app's design language  🟥 `HARD`

**Statement.** The app is intentionally styled with **MIUI UI/UX**, delivered through **MiuiX KMP**
(`top.yukonga.miuix.kmp`; all modules are in the build).

1. Use **MiuiX components** for every UI element.
2. Only when MiuiX does **not** provide a required component may a default Compose/Material
   component be used. That fallback must be **documented** (a code comment and a changelog entry
   naming the missing component) and styled to match MIUI as closely as practical.
3. Do **not** mix default Material components where a MiuiX equivalent exists.
4. Consult the official components documentation before using any component (per FE-INV-001):
   `https://compose-miuix-ui.github.io/miuix/components/`.

**Rationale.** The user deliberately added every MiuiX package, so a consistent MIUI look and feel is
a product requirement, not a preference. Mixing default components fragments the design, duplicates
styling work, and makes the app feel inconsistent across screens.

**Source.** User instruction (2026-09-21); MiuiX packages in `shared/build.gradle.kts`.

**Changeability.** By user approval.

---

### FE-INV-026 — The UI is modern, simple, spacious and highly accessible  🟥 `HARD`

**Statement.** Every screen must let a first-time user — including elderly and low-vision people —
find and operate each element without training:

1. **Modern, simple, uncluttered.** One primary purpose per screen; remove anything that does not
   serve it.
2. **Generous negative space.** Use dead/empty space deliberately to separate and group elements so
   each control is easy to locate; never crowd the screen.
3. **Big text and big icons.** Prefer large typography and large icons; keep visible text labels
   (no icon-only controls where a label fits).
4. **High contrast + color-coded regions.** Use color to help identify parts, always paired with
   text/shape — never color alone (FE-INV-010.3), and meeting high-contrast guidance.
5. **MIUI + modern guidelines.** Delivered through MiuiX (FE-INV-025), consistent with modern UI
   and MIUI design.
6. Touch targets stay ≥ 48 dp (FE-INV-010.2).

**Rationale.** Primary users are blind/low-vision and older adults; size, space and color are
functional aids, not polish. A dense or subtle UI is a usability defect for this audience.

**Source.** Direct user instruction (2026-09-21).

**Changeability.** By user approval.

---

## 4. PLATFORM / BUILD invariants

### FE-INV-020 — Fixed app stack  🟥 `HARD`

**Statement.** The app uses, and does not casually replace:

| Concern | Choice | Version (from `gradle/libs.versions.toml`) |
|---------|--------|--------------------------------------------|
| Language | Kotlin Multiplatform | 2.4.20 |
| UI | Compose Multiplatform + Material3 | 1.12.0 / 1.12.0-alpha03 |
| Design kit | MiuiX KMP (all modules) — see FE-INV-025 | 0.9.4 |
| Build | Android Gradle Plugin | 9.1.1 |
| Android SDK | minSdk 24 · compile/targetSdk 37 | — |
| JVM target | 11 | — |
| Transport | Ktor — raw sockets via `ktor-network` (HTTP `ktor-client-*` kept for later) | 3.6.0 |
| JSON | kotlinx-serialization-json | 1.11.0 |
| DI | Koin (`koin-core`, `koin-compose`, `koin-compose-viewmodel`, `koin-android`) | 4.2.2 |
| Persistence | `multiplatform-settings` + `multiplatform-settings-no-arg` (key-value config store) | 1.3.0 |

Adding any **other** dependency requires explicit user approval. Do not bump pinned versions
without approval.

**Rationale.** A small, frozen dependency set keeps builds reproducible and review cheap; matches
the runtime's fixed-stack rule.

**Source.** Runtime `INV-022`; user decision (2026-09-21) approving Ktor + Koin; `libs.versions.toml`.

**Changeability.** By user approval.

**Amendment (CHG-FE-0006, 2026-09-21).** The user added `multiplatform-settings` **1.3.0** and
`multiplatform-settings-no-arg` **1.3.0** (user-owned dependency change; both already in
`shared/build.gradle.kts` `commonMain`). They are the pinned persistence layer; verification is in
`docs/API_VERIFICATION.md` §3.5.

---

### FE-INV-021 — Targets are Android and Desktop (JVM)  🟥 `HARD`

**Statement.** Shared logic lives in `shared/src/commonMain`. Platform-specific code lives in
`shared/src/androidMain` / `shared/src/jvmMain` and is reached through `expect`/`actual`
declarations. No iOS/Web target is in scope.

**Rationale.** The beta needs one shared client for the phone and a desktop test/dev client; the
runtime provides no other client target.

**Source.** Repository layout; `SPECS.md` FR-11 (Android and desktop clients).

**Changeability.** By user approval.

---

### FE-INV-022 — All transport goes through Ktor  🟥 `HARD`

**Statement.** UDP (47600) and TCP (47601) access happens **only** inside the transport layer, using
Ktor's raw socket API (`io.ktor:ktor-network`: `connect()` for TCP, `UdpSocket` for UDP). JSON is
handled with **kotlinx-serialization**. The `ktor-client-*` HTTP artifacts are kept for future use
but are **not** the raw transport. UI, view models, and repositories never open sockets directly,
and no second network library (raw `java.net`, a direct OkHttp dependency, etc.) is introduced.

**Rationale.** One transport implementation keeps the protocol logic testable and swappable, and
keeps the dependency surface small.

**Source.** User decision (2026-09-21); runtime `INV-030` (interface-first).

**Changeability.** By user approval.

---

### FE-INV-023 — Build with the Gradle wrapper only  🟥 `HARD`

**Statement.** Build and test with `./gradlew`. Do not introduce a second build system, do not
require a locally installed Gradle, and do not edit `libs.versions.toml` versions without approval.

**Rationale.** Reproducible builds for the user and for CI; the wrapper pins the Gradle version.

**Source.** Repository tooling (`gradlew`, `gradle/wrapper`).

**Changeability.** By user approval.

---

### FE-INV-024 — Verify a dependency's API before adopting it  🟥 `HARD`

**Statement.** Before a dependency, or one of its APIs, is adopted, confirm against **official
documentation or the pinned source contract** (FE-INV-001) the exact module, the version to pin, and
that it provides the required capability (for Ktor: the raw **UDP** and **TCP** socket API). Record
the source (URL + access date) in the changelog entry that adopts it.

**Rationale.** Capability is version-sensitive; pinning the wrong module or version would block a
core feature.

**Source.** Runtime `INV-001`; user decision to use Ktor + Koin.

**Changeability.** Informational.

**Amendment (CHG-FE-0002, 2026-09-21).** Ktor **3.6.0** (`ktor-network` + `ktor-client-*`), Koin
**4.2.2**, and kotlinx-serialization-json **1.11.0** are now pinned and adopted. The ongoing duty to
verify each API used against the pinned version is governed by FE-INV-001.

**Amendment (CHG-FE-0006, 2026-09-21).** `multiplatform-settings` **1.3.0** +
`multiplatform-settings-no-arg` **1.3.0** are pinned and adopted. The Phase 0 verification of every
pinned API (Ktor 3.6.0 sockets, serialization, Koin, MiuiX, settings, Photo Picker, `liveRegion`) is
recorded with URL + access date in `docs/API_VERIFICATION.md`.

---

## 5. PROTOCOL invariants

### FE-INV-030 — `API_CONTRACT.md` (proto 1) is authoritative  🟥 `HARD`

**Statement.** The app implements **exactly** the commands, fields, enums, and error codes defined in
`../Lumina-BETA-RPI-2W/docs/API_CONTRACT.md`. It must not invent fields or commands, and must not
rely on behavior the contract does not guarantee. Protocol version is **1**.

**Rationale.** The runtime is the source of truth for the wire; guessing produces silent failures
against the real device.

**Source.** `docs/API_CONTRACT.md`; `SPECS.md` FR-11.

**Changeability.** Only by adopting an approved contract change from the runtime repo.

---

### FE-INV-031 — Handle sentinels and liveness correctly  🟥 `HARD`

**Statement.** Treat the contract's "unknown" sentinels as unknown, never as zero:
`volume == -1`, `luma == -1`, `tempC/load1/memAvailableKb == null`, `people == []` when the runtime
is down. Consider the device **offline** after **5 s** without a `status` datagram, and clear or
mark stale values accordingly.

**Rationale.** Rendering `-1` as a real value misleads the user; a stale dashboard is worse than an
honest "sin conexión".

**Source.** `docs/API_CONTRACT.md` §3–§4.

**Changeability.** Follows the contract.

---

### FE-INV-032 — Control requests are token-gated and sequential  🟥 `HARD`

**Statement.** Every TCP control request carries the shared token. Connections are processed
**sequentially per socket**: a long-running command (enrollment) blocks its connection until it
finishes, and is **cancelled on a second connection**. The app must handle `busy`, `bad_request`,
`unauthorized`, `ok:false`, and all `enroll.error` variants without crashing.

**Rationale.** These behaviors are contract guarantees; ignoring them causes hangs and confusing
failures during live demos.

**Source.** `docs/API_CONTRACT.md` §4–§6.

**Changeability.** Follows the contract.

---

### FE-INV-033 — Enrollment image rules  🟥 `HARD`

**Statement.** Before sending, resize on the device to **≤ 1080 px height**, encode **JPEG**
(~quality 80), target **≤ 500 KB per image**, send **3–5 photos** per person, as **raw base64**
(no `data:` prefix). Keep the batch under **8 MiB** decoded; a single request must fit the **16 MiB**
control line.

**Rationale.** The agent caps image enrollment at 8 MiB decoded and 16 MiB per line; oversized
payloads are rejected or dropped.

**Source.** `docs/API_CONTRACT.md` §4.10/§5.9; runtime CHG-0088.

**Changeability.** Follows the contract.

---

### FE-INV-034 — No relay server; connect to the gateway  🟥 `HARD`

**Statement.** The app connects **directly** to the Lúmina gateway on the hotspot (default
`10.42.0.1`); there is no laptop relay. The gateway address, ports, and token are **configurable**,
never hardcoded. Host mapping for development: desktop `127.0.0.1`, Android emulator `10.0.2.2`,
physical device the Pi's LAN IP (`adb reverse` forwards TCP only, not UDP).

**Rationale.** `SPECS.md` OOS-08 explicitly removed the relay; hardcoded endpoints break on other
networks and leak environment details.

**Source.** Runtime OOS-08, FR-11; `docs/API_CONTRACT.md`.

**Changeability.** Follows the contract / by user approval.

---

## 6. FEATURE SCOPE invariants

### FE-INV-040 — App scope is the FR-11 surface only  🟥 `SCOPE`

**Statement.** The app delivers: status dashboard, volume get/set/mute, enrolled-people list,
runtime start/stop, and person enrollment from camera or photos. **Out of scope:** camera preview or
streaming, currency recognition, navigation, earbud battery, proximity distance, detected-object
lists, face deletion, and any account/subscription feature.

**Rationale.** Scope control; several of these are not exposed by the contract at all, and others
were explicitly dropped by the runtime (e.g. earbud battery).

**Source.** Runtime `INV-040`/`INV-041`; `SPECS.md` FR-11; `docs/API_CONTRACT.md`.

**Changeability.** By user approval.

---

### FE-INV-041 — Spanish-first UI  🟥 `SCOPE`

**Statement.** All user-facing text is **Spanish (`es_MX`)** and lives in string resources. No
hardcoded user-facing string literals in composables.

**Rationale.** The product is Spanish-first (runtime `INV-042`); resources also make the text
reviewable and screen-reader friendly.

**Source.** Runtime `INV-042`; product identity.

**Changeability.** By user approval.

---

## 7. ARCHITECTURE invariants

### FE-INV-050 — Interface-first with dependency injection  🟥 `HARD`

**Statement.** Transport, repositories, and any external system are accessed through **interfaces**
(e.g. `TelemetrySource`, `ControlClient`, `EnrollmentRepository`). Concrete implementations are
wired with **Koin**. Pure logic must be unit-testable with fakes and **no device or network**.

**Rationale.** Testability and decoupling; matches the runtime's architecture rule and lets the
desktop client and tests run without a Pi.

**Source.** Runtime `INV-030`; user decision to use Koin.

**Changeability.** By user approval.

---

### FE-INV-051 — Unidirectional data flow; no logic in composables  🟥 `HARD`

**Statement.** UI state is an **immutable `data class`** exposed by a `ViewModel` as a `StateFlow`.
Events flow up, state flows down. Composables are pure renderers (plus local UI-only state) and
contain **no** business logic. No global mutable state.

**Rationale.** Predictable UI, testable state, fewer Compose recomposition bugs.

**Source.** Runtime `INV-030`/`INV-072`; user requirement (modular/testable).

**Changeability.** By user approval.

---

### FE-INV-052 — Structured concurrency  🟥 `HARD`

**Statement.** Use structured coroutine scopes (`viewModelScope`, an injected application scope).
**No `GlobalScope`.** Sockets and disk/IO work run on `Dispatchers.IO`; never block the main thread.
Cancellation must be honored (e.g. leaving the enrollment screen cancels its work cleanly).

**Rationale.** Leaked scopes and main-thread blocking cause freezes and battery drain; clean
cancellation is required for the cancel-enrollment flow.

**Source.** Runtime `INV-031` (never block the hot path); user requirement.

**Changeability.** By user approval.

---

### FE-INV-053 — Configuration is user-settable and secrets stay private  🟥 `HARD`

**Statement.** Host, ports, and token are settings, not constants. The token is stored in private
app storage and is **never** logged, displayed in full, or committed to version control.

**Rationale.** Different networks and devices need different endpoints; the token is the only access
control on the control channel.

**Source.** Runtime workflow rule (never commit secrets); `docs/API_CONTRACT.md` §2.

**Changeability.** By user approval.

---

## 8. PROCESS invariants

### FE-INV-060 — Everything is commented  🟥 `PROCESS`

**Statement.** Code is modular and **fully commented**. The maintainer is a Java developer, so
comments must also explain Kotlin/Compose/KMP specifics that differ from Java: `expect`/`actual`,
Compose recomposition and state, coroutines/`Flow`, and Koin wiring. Do not comment obvious lines.

**Rationale.** Explicit user requirement; maintainability by a small team on a deadline.

**Source.** Runtime `INV-072`; user instruction.

**Changeability.** By user approval.

---

### FE-INV-061 — Every meaningful change is logged  🟥 `PROCESS`

**Statement.** Every design decision and meaningful code change appends an entry to `CHANGELOG.md`
in the mandated YAML-block format (`CHG-FE-NNNN`). The log is **append-only**; history is never
rewritten, only superseded.

**Rationale.** Lets a future agent reconstruct *why* without re-reading the repo.

**Source.** Runtime `INV-070`; user instruction defining this repository's changelog.

**Changeability.** By user approval.

---

### FE-INV-062 — Report facts, not impressions  🟥 `PROCESS`

**Statement.** Report measurements and observed behavior (build result, test output, message
counts, timings), and cite the command used. Ask when confidence is below `0.80`.

**Rationale.** Runtime workflow rule; prevents guesswork from hardening into "fact".

**Source.** Runtime `AGENTS.md` §10.

**Changeability.** By user approval.

---

## 9. Index

| ID        | Severity | Title |
|-----------|----------|-------|
| FE-INV-001 | CORE     | NEVER ASSUME |
| FE-INV-002 | CORE     | Invariants outrank everything |
| FE-INV-003 | CORE     | Offline and local-only by default |
| FE-INV-004 | CORE     | Never modify the runtime repository |
| FE-INV-010 | CORE     | Accessibility is a hard requirement |
| FE-INV-020 | HARD     | Fixed app stack (Ktor + Koin approved) |
| FE-INV-021 | HARD     | Targets are Android and Desktop (JVM) |
| FE-INV-022 | HARD     | All transport goes through Ktor |
| FE-INV-023 | HARD     | Build with the Gradle wrapper only |
| FE-INV-024 | HARD     | Verify a dependency's API before adopting it |
| FE-INV-025 | HARD     | MIUI is the app's design language |
| FE-INV-026 | HARD     | The UI is modern, simple, spacious and highly accessible |
| FE-INV-030 | HARD     | `API_CONTRACT.md` (proto 1) is authoritative |
| FE-INV-031 | HARD     | Handle sentinels and liveness correctly |
| FE-INV-032 | HARD     | Control requests are token-gated and sequential |
| FE-INV-033 | HARD     | Enrollment image rules |
| FE-INV-034 | HARD     | No relay server; connect to the gateway |
| FE-INV-040 | SCOPE    | App scope is the FR-11 surface only |
| FE-INV-041 | SCOPE    | Spanish-first UI |
| FE-INV-050 | HARD     | Interface-first with dependency injection |
| FE-INV-051 | HARD     | Unidirectional data flow; no logic in composables |
| FE-INV-052 | HARD     | Structured concurrency |
| FE-INV-053 | HARD     | Configuration is user-settable and secrets stay private |
| FE-INV-060 | PROCESS  | Everything is commented |
| FE-INV-061 | PROCESS  | Every meaningful change is logged |
| FE-INV-062 | PROCESS  | Report facts, not impressions |
