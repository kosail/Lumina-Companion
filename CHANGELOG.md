# CHANGELOG.md — Lúmina Companion App (Android + Desktop)

> **AUDIENCE: AI AGENTS (machine-oriented). Not written for humans.**
>
> This file is an **append-only** record of every meaningful change and design decision made while
> building the companion app. Its purpose is to let a future agent reconstruct the *why* behind the
> current state **without re-reading the whole repository or re-deriving decisions from scratch**.
>
> IDs are prefixed **`CHG-FE-`** to distinguish them from the runtime repository's `CHG-NNNN`
> entries. The two logs are independent; an entry here never edits a runtime entry.

---

## Format specification (do not change without a changelog entry)

- Entries are **YAML mappings** in a single top-level sequence.
- **Append only.** Never edit or delete a past entry. To reverse/supersede a decision, add a new
  entry and set `supersedes`.
- Newest entries are appended at the **end** of the list.
- Required fields:

```yaml
- id: CHG-FE-NNNN           # zero-padded, strictly increasing, never reused
  date: YYYY-MM-DD
  agent: <model-or-tool>/<variant>   # e.g. opencode/deepseek-v4-flash
  type: docs|decision|impl|fix|refactor|test|chore|revert
  status: proposed|applied|superseded|reverted
  invariants: [FE-INV-xxx, ...]  # invariants affected/referenced; [] if none
  supersedes: CHG-FE-NNNN | null # the entry this one replaces, if any
  summary: >-                # one or two sentences, what changed
    ...
  rationale: >-              # WHY; the reasoning a future agent must not re-derive
    ...
  files: [path, ...]         # files added/modified/removed
  approvals: [user]          # who authorized it; [user] unless purely mechanical
  follow_up: >-              # nullable; what remains to do / what to watch
    ...
```

- `invariants` must list every invariant the entry touches. If a change **would** alter an
  invariant, it may only be applied after explicit user approval (see `INVARIANTS.md` change
  protocol).
- Keep `summary`/`rationale` factual and terse. No marketing language.

---

## Entries

```yaml
# ---------------------------------------------------------------------------
# CHG-FE-0001 — Establish the AI spec-driven environment for the companion app
# ---------------------------------------------------------------------------
- id: CHG-FE-0001
  date: 2026-09-21
  agent: opencode/deepseek-v4-flash
  type: docs
  status: applied
  invariants:
    - FE-INV-001
    - FE-INV-002
    - FE-INV-003
    - FE-INV-004
    - FE-INV-010
    - FE-INV-020
    - FE-INV-021
    - FE-INV-022
    - FE-INV-023
    - FE-INV-024
    - FE-INV-030
    - FE-INV-031
    - FE-INV-032
    - FE-INV-033
    - FE-INV-034
    - FE-INV-040
    - FE-INV-041
    - FE-INV-050
    - FE-INV-051
    - FE-INV-052
    - FE-INV-053
    - FE-INV-060
    - FE-INV-061
    - FE-INV-062
  supersedes: null
  summary: >-
    Created the companion app's governance: INVARIANTS.md (FE-INV-001..062, rank 1), AGENTS.md
    (operating manual distilled from the C++ runtime's AGENTS.md/INVARIANTS.md), and this
    append-only CHANGELOG.md. No build files or dependencies were changed.
  rationale: >-
    The app had only stub code and no shared rules, while the runtime already runs on a strict
    AI-spec-driven model. Distilling that model for the frontend keeps both repositories
    consistent and gives future agents a single source of truth. The app-specific invariants were
    adapted rather than copied: accessibility is promoted to CORE because the users are blind and
    low-vision; the wire contract (runtime API_CONTRACT.md proto 1) is treated as an external
    read-only dependency; and the runtime's offline/no-cloud stance becomes an app rule so the
    client cannot reintroduce a network dependency or leak user data. Per user decision, the
    transport is Ktor (ktor-network) and DI is Koin; the user wires those dependencies into the
    build, so this entry intentionally changes no Gradle files. The version pins for Ktor and Koin
    are deferred to the entry that adds them, after verifying official docs (FE-INV-024).
  files:
    - Lumina-BETA-ANDROID/INVARIANTS.md
    - Lumina-BETA-ANDROID/AGENTS.md
    - Lumina-BETA-ANDROID/CHANGELOG.md
  approvals: [user]
  follow_up: >-
    User adds Ktor (ktor-network) and Koin to gradle/libs.versions.toml and shared/build.gradle.kts
    and logs a CHG-FE entry with the verified versions. Then scaffold protocol/, transport/, data/,
    ui/, di/ with fakes and unit tests, and validate against ../Lumina/Testing_server.

# ---------------------------------------------------------------------------
# CHG-FE-0002 — Adopt Ktor, Koin, and kotlinx-serialization
# ---------------------------------------------------------------------------
- id: CHG-FE-0002
  date: 2026-09-21
  agent: opencode/deepseek-v4-flash
  type: decision
  status: applied
  invariants:
    - FE-INV-020
    - FE-INV-022
    - FE-INV-024
  supersedes: null
  summary: >-
    Recorded the companion-app dependencies the user added: Ktor 3.6.0 (io.ktor:ktor-network for
    raw UDP/TCP sockets, plus the ktor-client-* HTTP artifacts kept for later), Koin 4.2.2
    (koin-core, koin-compose, koin-compose-viewmodel, koin-android), and kotlinx-serialization-json
    1.11.0 with the serialization Gradle plugin. Updated FE-INV-020 (stack table), FE-INV-022
    (transport modules), and FE-INV-024 (amended now that versions are pinned). No runtime-repo
    files were touched.
  rationale: >-
    FE-INV-024 required verifying the exact modules and versions before adoption. ktor-network is the
    module that provides the raw socket API the contract needs (connect() for TCP, UdpSocket for
    UDP); the ktor-client-* HTTP artifacts cannot carry the 1 Hz UDP telemetry or the token-gated TCP
    control channel, so they are retained only for future use. Pinning the versions here closes
    FE-INV-024's "pin on adoption" state and gives future agents a single place to read the stack.
  files:
    - Lumina-BETA-ANDROID/gradle/libs.versions.toml
    - Lumina-BETA-ANDROID/shared/build.gradle.kts
    - Lumina-BETA-ANDROID/INVARIANTS.md
    - Lumina-BETA-ANDROID/AGENTS.md
    - Lumina-BETA-ANDROID/CHANGELOG.md
  approvals: [user]
  follow_up: >-
    Scaffold protocol/, transport/, data/, ui/, di/ with fakes and unit tests. Before writing the
    transport, verify the ktor-network connect()/UdpSocket API against the pinned 3.6.0 source
    contract or official docs (FE-INV-001).

# ---------------------------------------------------------------------------
# CHG-FE-0003 — Harden NEVER ASSUME and adopt MIUI as the design language
# ---------------------------------------------------------------------------
- id: CHG-FE-0003
  date: 2026-09-21
  agent: opencode/deepseek-v4-flash
  type: docs
  status: applied
  invariants:
    - FE-INV-001
    - FE-INV-025
  supersedes: null
  summary: >-
    Rewrote FE-INV-001 (NEVER ASSUME) to forbid assuming any API contract: every API must be
    verified against the pinned version's source-code contract or the official documentation for that
    exact version, because Kotlin and Compose Multiplatform change frequently. Added FE-INV-025 (MIUI
    is the app's design language): all UI uses MiuiX components, and default Compose/Material
    components are permitted only where MiuiX has no equivalent, documented with the missing
    component. Updated AGENTS.md (stack, coding style, doc sources, DoD, prohibited actions) and
    renumbered INVARIANTS.md sections.
  rationale: >-
    APIs recalled from memory or from examples written for another version are unreliable in a young
    language with a fast-moving UI toolkit; requiring the pinned source contract or official docs
    removes the guesswork. The user added every MiuiX package deliberately, so a consistent MIUI look
    and feel is a product requirement; without an explicit rule, agents default to Material
    components and fragment the design. MIUI is documented as the required design language so the
    fallback to defaults is the exception, not the norm.
  files:
    - Lumina-BETA-ANDROID/INVARIANTS.md
    - Lumina-BETA-ANDROID/AGENTS.md
    - Lumina-BETA-ANDROID/CHANGELOG.md
  approvals: [user]
  follow_up: >-
    When a MiuiX component is missing and a default is used, log it with the component name. Keep the
    MiuiX components documentation URL (https://compose-miuix-ui.github.io/miuix/components/) current
    in AGENTS.md section 9.

# ---------------------------------------------------------------------------
# CHG-FE-0004 — Establish the build plan for the companion app
# ---------------------------------------------------------------------------
- id: CHG-FE-0004
  date: 2026-09-21
  agent: opencode/deepseek-v4-flash
  type: docs
  status: applied
  invariants:
    - FE-INV-001
    - FE-INV-020
    - FE-INV-021
    - FE-INV-025
    - FE-INV-031
    - FE-INV-033
    - FE-INV-040
    - FE-INV-050
    - FE-INV-051
    - FE-INV-052
    - FE-INV-053
    - FE-INV-060
    - FE-INV-061
    - FE-INV-062
  supersedes: null
  summary: >-
    Added PLAN.md: the phase-gated execution plan for building the FR-11 companion client. It traces
    every requirement to the API contract and invariants, fixes the architecture (protocol/transport/
    data/ui/di), records the reviewed decisions (token entry + paste, Photo Picker only,
    multiplatform-settings, JVM unit tests only, Android-first), maps UI to MiuiX components with
    documented fallbacks, and defines Phases 0-6 with user-run gates.
  rationale: >-
    The app had governance and a stub but no agreed build order. Because the agent container has no
    JDK/Gradle, the user runs every build and test; the plan therefore front-loads API verification
    (FE-INV-001), keeps a pure test-first protocol core, proves a telemetry vertical slice early to
    de-risk Ktor raw sockets, and gates each phase on a user-run command plus a CHG-FE entry. The
    plan is a derived document: INVARIANTS.md and AGENTS.md still outrank it.
  files:
    - Lumina-BETA-ANDROID/PLAN.md
    - Lumina-BETA-ANDROID/CHANGELOG.md
  approvals: [user]
  follow_up: >-
    Execute Phase 0: verify the pinned APIs and record docs/API_VERIFICATION.md, then append
    CHG-FE-0005 recording multiplatform-settings 1.3.0 and amending FE-INV-020 + AGENTS section 3.

# ---------------------------------------------------------------------------
# CHG-FE-0005 — Human-readable README + fix the Testing_server relative path
# ---------------------------------------------------------------------------
- id: CHG-FE-0005
  date: 2026-09-21
  agent: opencode/deepseek-v4-flash
  type: docs
  status: applied
  invariants:
    - FE-INV-001
    - FE-INV-004
    - FE-INV-010
    - FE-INV-020
    - FE-INV-034
    - FE-INV-040
    - FE-INV-041
    - FE-INV-053
  supersedes: null
  summary: >-
    Replaced the default Kotlin Multiplatform template README with a human-readable project README:
    what Lúmina is, who it is for, the FR-11 feature surface and out-of-scope list, the tech stack,
    repository layout, build/run/test commands, the mock-device workflow, the host-mapping table,
    the governance documents, and contribution rules. Also corrected the mock-server relative path
    from ../Lumina/Testing_server to ../Testing_server in AGENTS.md (3 places) and PLAN.md (2
    places); the mock actually lives at ../Testing_server relative to this repository.
  rationale: >-
    The README was still the unmodified KMP template and did not describe the project, so a new human
    contributor had no entry point (AGENTS.md is written for AI agents, not people). The path fix is
    a factual correction: the repository sits at Lumina/Lumina-BETA-ANDROID, so its sibling is
    Lumina/Testing_server, i.e. ../Testing_server. The old path would have sent contributors to a
    non-existent directory. Past changelog entries were left untouched per the append-only rule.
  files:
    - Lumina-BETA-ANDROID/README.md
    - Lumina-BETA-ANDROID/AGENTS.md
    - Lumina-BETA-ANDROID/PLAN.md
    - Lumina-BETA-ANDROID/CHANGELOG.md
  approvals: [user]
  follow_up: >-
    The historical CHG-FE-0001 entry still contains the old ../Lumina/Testing_server path; it is left
    as-is because the log is append-only. CHG-FE-0006 (multiplatform-settings 1.3.0 decision) is now
    the next entry.

# ---------------------------------------------------------------------------
# CHG-FE-0006 — Phase 0: pinned-API verification + adopt multiplatform-settings
# ---------------------------------------------------------------------------
- id: CHG-FE-0006
  date: 2026-09-21
  agent: opencode/deepseek-v4-flash
  type: decision
  status: applied
  invariants:
    - FE-INV-001
    - FE-INV-010
    - FE-INV-020
    - FE-INV-024
    - FE-INV-031
    - FE-INV-034
    - FE-INV-053
    - FE-INV-061
  supersedes: null
  summary: >-
    Completed Phase 0. Verified the pinned APIs (FE-INV-001/024) and recorded docs/API_VERIFICATION.md
    with a source URL + access date and a confidence for each. Adopted multiplatform-settings 1.3.0 and
    multiplatform-settings-no-arg 1.3.0 as the persistence layer, amending FE-INV-020 (stack table) and
    AGENTS.md section 3. Fixed the shared JSON policy (ignoreUnknownKeys, explicitNulls=false,
    coerceInputValues=true) and the liveness boundary (offline at >= 5000 ms). No app code was written.
  rationale: >-
    FE-INV-001 forbids assuming any API. api.ktor.io publishes no 3.6.0 reference, so the Ktor raw-socket
    APIs were verified against the pinned 3.6.0 GitHub tag source (user-approved). That surfaced two
    drifts the transport must respect: ByteReadChannel.readUTF8Line is deprecated in 3.6.0 (use
    readLineStrict(limit = 16 MiB) to match MAX_LINE_BYTES) and Datagram.packet is now a kotlinx.io
    Source. multiplatform-settings-no-arg provides a no-arg Settings() for commonMain whose Android
    implementation self-initializes via androidx-startup (no manual init); it cannot run in Android unit
    tests, so MapSettings is the test fake. The user chose coerceInputValues=true so malformed/null
    enum-like fields fall back to defaults instead of throwing. Liveness was fixed at >= 5000 ms to make
    the offline transition deterministic and testable. MiuiX per-component signatures and the Photo
    Picker / liveRegion / activity-compose wiring are deferred to first use (recorded in
    docs/API_VERIFICATION.md section 4), because Phase 1 is pure protocol code with no UI.
  files:
    - Lumina-BETA-ANDROID/docs/API_VERIFICATION.md
    - Lumina-BETA-ANDROID/INVARIANTS.md
    - Lumina-BETA-ANDROID/AGENTS.md
    - Lumina-BETA-ANDROID/CHANGELOG.md
  approvals: [user]
  follow_up: >-
    Phase 1: build the pure protocol/ core (wire models, Json, command builders, status mapper,
    sentinels, error mapping, liveness reducer, image-cap validation) with JVM commonTest unit tests,
    then the user runs ./gradlew :shared:allTests and CHG-FE-0007 is appended. Before first UI use,
    verify each MiuiX component signature and add androidx.activity:activity-compose to
    shared/androidMain (Phase 4).

# ---------------------------------------------------------------------------
# CHG-FE-0007 — Fix the MiuiX navigation coordinate; retain miuix-nav provisionally
# ---------------------------------------------------------------------------
- id: CHG-FE-0007
  date: 2026-09-21
  agent: opencode/deepseek-v4-flash
  type: fix
  status: applied
  invariants:
    - FE-INV-020
    - FE-INV-024
    - FE-INV-061
  supersedes: null
  summary: >-
    The version catalog declared top.yukonga.miuix.kmp:miuix-navigation:0.9.4, which does not exist on
    Maven Central, so :shared:compileKotlinJvm failed to resolve its classpath. The coordinate was
    corrected (user-applied) to top.yukonga.miuix.kmp:miuix-nav:0.9.4. Verified against Maven Central
    and the module POMs and recorded in docs/API_VERIFICATION.md. NavigationBar itself comes from the
    miuix-ui "basic" package, so miuix-nav is not required for it; per user decision it is retained
    provisionally and must be removed in the final cleanup phase if still unused.
  rationale: >-
    Gradle reports "Could not find top.yukonga.miuix.kmp:miuix-navigation:0.9.4" before any Kotlin
    compilation, so the failure was a build-config error, not a Phase 1 code defect. The published
    artifacts include miuix-nav (POM description "Self-contained navigation runtime for Miuix") but no
    miuix-navigation. The docs import top.yukonga.miuix.kmp.basic.NavigationBar (miuix-ui), and the
    miuix-ui-android POM does not depend on miuix-nav, confirming the bottom bar does not need it. The
    user chose to keep miuix-nav for possible later use; the cleanup obligation is logged so it is not
    forgotten. This also fixes PLAN.md section 6, which named the wrong module.
  files:
    - Lumina-BETA-ANDROID/gradle/libs.versions.toml
    - Lumina-BETA-ANDROID/PLAN.md
    - Lumina-BETA-ANDROID/docs/API_VERIFICATION.md
    - Lumina-BETA-ANDROID/CHANGELOG.md
  approvals: [user]
  follow_up: >-
    Remove top.yukonga.miuix.kmp:miuix-nav from the build during the final cleanup phase if it is still
    unused (user decision).

# ---------------------------------------------------------------------------
# CHG-FE-0008 — Phase 1: pure protocol core
# ---------------------------------------------------------------------------
- id: CHG-FE-0008
  date: 2026-09-21
  agent: opencode/deepseek-v4-flash
  type: impl
  status: applied
  invariants:
    - FE-INV-001
    - FE-INV-003
    - FE-INV-030
    - FE-INV-031
    - FE-INV-032
    - FE-INV-033
    - FE-INV-034
    - FE-INV-041
    - FE-INV-050
    - FE-INV-052
    - FE-INV-053
    - FE-INV-060
    - FE-INV-061
  supersedes: null
  summary: >-
    Delivered the pure protocol/ core (Phase 1): one configured Json instance, wire models for status
    and every control reply, a t-discriminated reply decoder, a wire-to-domain status mapper, exact
    command builders for every APP SENDS line, a ControlResult/ErrorCode model with error mapping,
    EnrollEvent, a pure liveness reducer, and image-cap validation. Added 8 JVM commonTest files; the
    user-run gate ./gradlew :shared:allTests passed (92 tests = 46 x 2 targets: JVM and Android host).
  rationale: >-
    A fully pure, unit-tested core de-risks the fast-moving Ktor/Compose integration: no sockets, no
    device, no network, so the wire contract can be locked down before the transport is wired. Wire
    and domain types are kept separate so sentinels (-1/null) are interpreted in exactly one place and
    the UI never sees magic numbers. Enum-like wire fields are Strings so unknown values degrade to an
    Unknown domain member instead of throwing. Command builders are typed and assert exact wire
    strings (including the token and the frames default) so drift is caught by tests.
  files:
    - Lumina-BETA-ANDROID/shared/src/commonMain/kotlin/com/korealm/lumina/protocol/LuminaJson.kt
    - Lumina-BETA-ANDROID/shared/src/commonMain/kotlin/com/korealm/lumina/protocol/WireStatus.kt
    - Lumina-BETA-ANDROID/shared/src/commonMain/kotlin/com/korealm/lumina/protocol/WireReplies.kt
    - Lumina-BETA-ANDROID/shared/src/commonMain/kotlin/com/korealm/lumina/protocol/Reply.kt
    - Lumina-BETA-ANDROID/shared/src/commonMain/kotlin/com/korealm/lumina/protocol/DomainModels.kt
    - Lumina-BETA-ANDROID/shared/src/commonMain/kotlin/com/korealm/lumina/protocol/StatusMapper.kt
    - Lumina-BETA-ANDROID/shared/src/commonMain/kotlin/com/korealm/lumina/protocol/CommandBuilders.kt
    - Lumina-BETA-ANDROID/shared/src/commonMain/kotlin/com/korealm/lumina/protocol/ControlResult.kt
    - Lumina-BETA-ANDROID/shared/src/commonMain/kotlin/com/korealm/lumina/protocol/EnrollEvent.kt
    - Lumina-BETA-ANDROID/shared/src/commonMain/kotlin/com/korealm/lumina/protocol/ErrorMapper.kt
    - Lumina-BETA-ANDROID/shared/src/commonMain/kotlin/com/korealm/lumina/protocol/LivenessReducer.kt
    - Lumina-BETA-ANDROID/shared/src/commonMain/kotlin/com/korealm/lumina/protocol/ImageCaps.kt
    - Lumina-BETA-ANDROID/shared/src/commonTest/kotlin/com/korealm/lumina/protocol
    - Lumina-BETA-ANDROID/CHANGELOG.md
  approvals: [user]
  follow_up: >-
    Phase 2: Ktor transport behind TelemetrySource/ControlClient, Koin modules, and the desktop
    telemetry vertical slice. CHG-FE-0009 hardens the Phase 1 core after an audit.

# ---------------------------------------------------------------------------
# CHG-FE-0009 — Phase 1 audit hardening (proto gate + sentinel normalization)
# ---------------------------------------------------------------------------
- id: CHG-FE-0009
  date: 2026-09-21
  agent: opencode/deepseek-v4-flash
  type: fix
  status: applied
  invariants:
    - FE-INV-001
    - FE-INV-030
    - FE-INV-031
    - FE-INV-033
  supersedes: null
  summary: >-
    Applied the findings of a critical audit of the Phase 1 code. Added decodeStatus(), which enforces
    the t == "status" discriminator and a proto == 1 gate (returning UnsupportedProto otherwise) and
    carries proto into DeviceStatus; normalized enroll.total (must be > 0) and captured (>= 0) to null
    in both the status mapper and enroll.progress so the progress UI cannot divide by zero; forced
    DayNight.Unknown when luma is the -1 sentinel; bounded volumePercent to 0..100; made
    base64EncodedSize overflow-safe (saturates at Long.MAX_VALUE) and made fitsControlLine short-circuit
    huge inputs; rejected all-empty image batches as Undecodable (contract "no decodable images").
  rationale: >-
    The audit found two high-severity issues: telemetry had no t/proto validation (a breaking proto or
    a non-status datagram would be silently misinterpreted, and DeviceStatus dropped proto), and
    enroll.total/captured were passed through unnormalized, so a non-conforming total:0 could cause an
    integer divide-by-zero or NaN in the Phase 2 progress UI. Medium issues fixed: base64 length could
    overflow and make the line guard wrongly pass; an all-empty batch passed validation but the device
    rejects it; dayNight could disagree with an unknown luma (FE-INV-031); volume was unbounded. All
    are pure-code changes with added tests; no new dependencies.
  files:
    - Lumina-BETA-ANDROID/shared/src/commonMain/kotlin/com/korealm/lumina/protocol/StatusDecode.kt
    - Lumina-BETA-ANDROID/shared/src/commonMain/kotlin/com/korealm/lumina/protocol/StatusMapper.kt
    - Lumina-BETA-ANDROID/shared/src/commonMain/kotlin/com/korealm/lumina/protocol/DomainModels.kt
    - Lumina-BETA-ANDROID/shared/src/commonMain/kotlin/com/korealm/lumina/protocol/Reply.kt
    - Lumina-BETA-ANDROID/shared/src/commonMain/kotlin/com/korealm/lumina/protocol/ImageCaps.kt
    - Lumina-BETA-ANDROID/shared/src/commonTest/kotlin/com/korealm/lumina/protocol/DecodeStatusTest.kt
    - Lumina-BETA-ANDROID/shared/src/commonTest/kotlin/com/korealm/lumina/protocol/StatusMapperTest.kt
    - Lumina-BETA-ANDROID/shared/src/commonTest/kotlin/com/korealm/lumina/protocol/ReplyDecodeTest.kt
    - Lumina-BETA-ANDROID/shared/src/commonTest/kotlin/com/korealm/lumina/protocol/ImageCapsTest.kt
    - Lumina-BETA-ANDROID/CHANGELOG.md
  approvals: [user]
  follow_up: >-
    The transport must map decodeStatus -> UnsupportedProto to a user-visible "incompatible device"
    state and Malformed/ReplyDecodeResult.Malformed to ControlResult.Io (Phase 2). Re-run
    ./gradlew :shared:allTests after this change.

# ---------------------------------------------------------------------------
# CHG-FE-0010 — Formally close Phase 0 and Phase 1
# ---------------------------------------------------------------------------
- id: CHG-FE-0010
  date: 2026-09-21
  agent: opencode/deepseek-v4-flash
  type: chore
  status: applied
  invariants:
    - FE-INV-001
    - FE-INV-061
    - FE-INV-062
  supersedes: null
  summary: >-
    Phase 0 (pinned-API verification + governance) and Phase 1 (pure protocol/ core) are formally
    closed. The final user-run gate ./gradlew :shared:allTests is green with 130 tests (65 test
    methods x 2 targets: JVM and Android host), covering the audit-hardened core.
  rationale: >-
    The user confirmed the gate result, so the phases can be marked complete and the work can move
    to Phase 2 (transport + DI + desktop vertical slice). CHG-FE-0008 recorded the pre-hardening
    count (92 tests); this entry records the final post-hardening count (130). The changelog is
    append-only, so the earlier figure is left as history rather than edited.
  files:
    - Lumina-BETA-ANDROID/CHANGELOG.md
  approvals: [user]
  follow_up: >-
    Phase 2 begins under CHG-FE-0011.

# ---------------------------------------------------------------------------
# CHG-FE-0011 — Phase 2 decisions: FE-INV-026 + transport/DI/test approach
# ---------------------------------------------------------------------------
- id: CHG-FE-0011
  date: 2026-09-21
  agent: opencode/deepseek-v4-flash
  type: decision
  status: applied
  invariants:
    - FE-INV-001
    - FE-INV-020
    - FE-INV-022
    - FE-INV-025
    - FE-INV-026
    - FE-INV-050
    - FE-INV-052
  supersedes: null
  summary: >-
    Recorded the Phase 2 decisions: (1) add FE-INV-026 (modern, simple, spacious, highly accessible
    UI) to INVARIANTS.md; (2) approve kotlinx-coroutines-test 1.11.0 for commonTest; (3) ControlClient
    exposes only request/response methods in Phase 2 (enrollment methods arrive in Phase 4);
    (4) DashboardViewModel consumes TelemetrySource directly (DeviceRepository deferred to Phase 3);
    (5) Phase 2 includes the full 3-tab NavigationBar shell.
  rationale: >-
    FE-INV-026 is a direct user instruction governing all screens and must outrank ordinary style
    choices. kotlinx-coroutines-test provides runTest/virtual time, the only practical way to test
    the transport's subscribe/liveness ticker and Flow<TelemetryEvent> deterministically; it is in the
    already-pinned coroutines family (1.11.0) and the user approved it. Keeping enrollment out of the
    Phase 2 ControlClient avoids stubs and defers the blocking-connection/cancel-on-second-connection
    semantics to their own phase. Consuming TelemetrySource directly keeps Phase 2 small and matches
    the planned "ViewModel state with fake TelemetrySource" test. The 3-tab shell is the app root.
  files:
    - Lumina-BETA-ANDROID/INVARIANTS.md
    - Lumina-BETA-ANDROID/docs/API_VERIFICATION.md
    - Lumina-BETA-ANDROID/gradle/libs.versions.toml
    - Lumina-BETA-ANDROID/shared/build.gradle.kts
    - Lumina-BETA-ANDROID/CHANGELOG.md
  approvals: [user]
  follow_up: >-
    Implement Phase 2 (transport seams + Ktor impls, SettingsStore, Koin modules, dashboard vertical
    slice). Gate: ./gradlew :shared:allTests, then :desktopApp:run against npm run dev shows live
    status and "Sin conexión" within 5 s after the server stops.

# ---------------------------------------------------------------------------
# CHG-FE-0012 — Phase 2 implementation: transport + DI + dashboard vertical slice
# ---------------------------------------------------------------------------
- id: CHG-FE-0012
  date: 2026-09-21
  agent: opencode/deepseek-v4-flash
  type: impl
  status: applied
  invariants:
    - FE-INV-001
    - FE-INV-003
    - FE-INV-010
    - FE-INV-020
    - FE-INV-022
    - FE-INV-025
    - FE-INV-026
    - FE-INV-031
    - FE-INV-032
    - FE-INV-034
    - FE-INV-041
    - FE-INV-050
    - FE-INV-051
    - FE-INV-052
    - FE-INV-053
    - FE-INV-060
  supersedes: null
  summary: >-
    Implemented Phase 2: Ktor UDP/TCP behind interfaces (TelemetrySource/ControlClient) with testable
    seams (TelemetrySocket/ControlConnection + factories), a 5 s re-subscribe/liveness loop with
    injected Clock/Ticker and 1/2/5/10 s backoff, SettingsStore over multiplatform-settings, Koin
    wiring, a 3-tab MiuiX shell, and a Dashboard rendering live telemetry per FE-INV-026. Added
    kotlinx-coroutines-test 1.11.0 and 26 tests (transport framing/decoding, error mapping, timeouts,
    reducer, VM wiring). Added INTERNET permission and a LuminaApplication that starts Koin.
  rationale: >-
    Only the *Factory implementations import io.ktor.network, so all transport logic is unit-tested
    against fakes (no sockets) as FE-INV-022/050 require. Timing is injected so the 5 s liveness rule
    is deterministic (FE-INV-031). Ktor 3.6.0 drifts from the contract's pseudo-code: readUTF8Line is
    deprecated, so the TCP channel uses readLineStrict(limit = MAX_LINE_BYTES) and maps
    TooLongLineException/EOFException to ControlResult.Io; datagrams are built with
    kotlinx.io.Buffer and read via Source.readByteArray. The Koin ViewModel DSL is
    org.koin.core.module.dsl.viewModel (koin-core-viewmodel, exposed api by koin-compose-viewmodel),
    so no new dependency was needed. Deviations, all deliberate: (a) DashboardViewModel is a Koin
    `single` resolved with koinInject rather than viewModel{}+koinViewModel, because Compose
    Multiplatform desktop does not guarantee a host ViewModelStoreOwner; (b) the VM's collection uses
    viewModelScope but its only logic (reduceDashboardState) is a pure top-level function tested in
    commonTest, with the coroutine wiring tested in jvmTest; (c) Koin is started from a new
    LuminaApplication (once per process) instead of MainActivity.onCreate (which can run again on
    recreation and would throw); (d) unit symbols (%/°C/MB/s/FPS) are treated as data formatting, not
    translatable text, so every label/state/message still comes from strings.xml (FE-INV-041).
  files:
    - Lumina-BETA-ANDROID/shared/src/commonMain/kotlin/com/korealm/lumina/transport/Endpoint.kt
    - Lumina-BETA-ANDROID/shared/src/commonMain/kotlin/com/korealm/lumina/transport/TelemetryEvent.kt
    - Lumina-BETA-ANDROID/shared/src/commonMain/kotlin/com/korealm/lumina/transport/TelemetrySource.kt
    - Lumina-BETA-ANDROID/shared/src/commonMain/kotlin/com/korealm/lumina/transport/ControlClient.kt
    - Lumina-BETA-ANDROID/shared/src/commonMain/kotlin/com/korealm/lumina/transport/TelemetrySocket.kt
    - Lumina-BETA-ANDROID/shared/src/commonMain/kotlin/com/korealm/lumina/transport/ControlConnection.kt
    - Lumina-BETA-ANDROID/shared/src/commonMain/kotlin/com/korealm/lumina/transport/Time.kt
    - Lumina-BETA-ANDROID/shared/src/commonMain/kotlin/com/korealm/lumina/transport/Backoff.kt
    - Lumina-BETA-ANDROID/shared/src/commonMain/kotlin/com/korealm/lumina/transport/KtorTelemetrySocket.kt
    - Lumina-BETA-ANDROID/shared/src/commonMain/kotlin/com/korealm/lumina/transport/KtorControlConnection.kt
    - Lumina-BETA-ANDROID/shared/src/commonMain/kotlin/com/korealm/lumina/transport/KtorTelemetrySource.kt
    - Lumina-BETA-ANDROID/shared/src/commonMain/kotlin/com/korealm/lumina/transport/KtorControlClient.kt
    - Lumina-BETA-ANDROID/shared/src/commonMain/kotlin/com/korealm/lumina/data/SettingsStore.kt
    - Lumina-BETA-ANDROID/shared/src/commonMain/kotlin/com/korealm/lumina/data/SettingsStoreImpl.kt
    - Lumina-BETA-ANDROID/shared/src/commonMain/kotlin/com/korealm/lumina/data/PlatformDefaults.kt
    - Lumina-BETA-ANDROID/shared/src/jvmMain/kotlin/com/korealm/lumina/data/PlatformDefaults.jvm.kt
    - Lumina-BETA-ANDROID/shared/src/androidMain/kotlin/com/korealm/lumina/data/PlatformDefaults.android.kt
    - Lumina-BETA-ANDROID/shared/src/commonMain/kotlin/com/korealm/lumina/di/AppModule.kt
    - Lumina-BETA-ANDROID/shared/src/commonMain/kotlin/com/korealm/lumina/di/Koin.kt
    - Lumina-BETA-ANDROID/shared/src/commonMain/kotlin/com/korealm/lumina/ui/theme/AppTheme.kt
    - Lumina-BETA-ANDROID/shared/src/commonMain/kotlin/com/korealm/lumina/ui/AppRoot.kt
    - Lumina-BETA-ANDROID/shared/src/commonMain/kotlin/com/korealm/lumina/ui/dashboard/DashboardUiState.kt
    - Lumina-BETA-ANDROID/shared/src/commonMain/kotlin/com/korealm/lumina/ui/dashboard/DashboardViewModel.kt
    - Lumina-BETA-ANDROID/shared/src/commonMain/kotlin/com/korealm/lumina/ui/dashboard/DashboardScreen.kt
    - Lumina-BETA-ANDROID/shared/src/commonMain/composeResources/values/strings.xml
    - Lumina-BETA-ANDROID/shared/src/commonMain/kotlin/com/korealm/lumina/App.kt
    - Lumina-BETA-ANDROID/shared/src/commonTest/kotlin/com/korealm/lumina/transport/TransportFakes.kt
    - Lumina-BETA-ANDROID/shared/src/commonTest/kotlin/com/korealm/lumina/transport/BackoffTest.kt
    - Lumina-BETA-ANDROID/shared/src/commonTest/kotlin/com/korealm/lumina/transport/TelemetrySourceTest.kt
    - Lumina-BETA-ANDROID/shared/src/commonTest/kotlin/com/korealm/lumina/transport/ControlClientTest.kt
    - Lumina-BETA-ANDROID/shared/src/commonTest/kotlin/com/korealm/lumina/testing/TestData.kt
    - Lumina-BETA-ANDROID/shared/src/commonTest/kotlin/com/korealm/lumina/ui/dashboard/DashboardReducerTest.kt
    - Lumina-BETA-ANDROID/shared/src/jvmTest/kotlin/com/korealm/lumina/ui/dashboard/DashboardViewModelTest.kt
    - Lumina-BETA-ANDROID/androidApp/src/main/kotlin/com/korealm/lumina/LuminaApplication.kt
    - Lumina-BETA-ANDROID/androidApp/src/main/kotlin/com/korealm/lumina/MainActivity.kt
    - Lumina-BETA-ANDROID/androidApp/src/main/AndroidManifest.xml
    - Lumina-BETA-ANDROID/desktopApp/src/main/kotlin/com/korealm/lumina/main.kt
    - Lumina-BETA-ANDROID/gradle/libs.versions.toml
    - Lumina-BETA-ANDROID/shared/build.gradle.kts
    - Lumina-BETA-ANDROID/CHANGELOG.md
  approvals: [user]
  follow_up: >-
    Gate: ./gradlew :shared:allTests (expect 91 test methods x targets) then :desktopApp:run with
    ../Testing_server `npm run dev` showing live status and "Sin conexión" within 5 s of stopping the
    server. Known non-blockers: MiuiX NavigationBarItem fixes icon/label sizes (documented in
    API_VERIFICATION §6.4); SettingsStoreImpl has no unit test (MapSettings would need a new
    dependency); the retry loop is exercised by the desktop gate rather than an integration test.
    If Dispatchers.setMain is not visible from jvmTest, move DashboardViewModelTest to androidHostTest
    or inject the scope.

# ---------------------------------------------------------------------------
# CHG-FE-0013 — Phase 2 closed; Phase 3 (control + settings) implemented
# ---------------------------------------------------------------------------
- id: CHG-FE-0013
  date: 2026-09-21
  agent: opencode/deepseek-v4-flash
  type: impl
  status: applied
  invariants:
    - FE-INV-001
    - FE-INV-003
    - FE-INV-010
    - FE-INV-020
    - FE-INV-025
    - FE-INV-026
    - FE-INV-030
    - FE-INV-031
    - FE-INV-032
    - FE-INV-034
    - FE-INV-041
    - FE-INV-050
    - FE-INV-051
    - FE-INV-052
    - FE-INV-053
    - FE-INV-060
    - FE-INV-061
  supersedes: null
  summary: >-
    Closed Phase 2 (user gate green: :shared:allTests and the desktop vertical slice showed live
    status and "Sin conexión" after 5 s, no warnings) and implemented Phase 3: DeviceRepository
    facade; volume set/mute and runtime start/stop from the dashboard; a Settings screen (host, ports,
    token) persisted through SettingsStore; Spanish UX for unauthorized/busy/bad_request/internal/io;
    a shell-level MiuiX SnackbarHost for action feedback; and an endpoint-change restart in
    KtorTelemetrySource so a host edit reconnects immediately. Added API_VERIFICATION §7 and marked
    Phases 0/1/2 complete in PLAN.md.
  rationale: >-
    DeviceRepository is a thin facade (approved, CHG-FE-0011) so the UI depends on data/ and Phase 4's
    PeopleRepository sits beside it. volume.set is sent on onValueChangeFinished only: the contract's
    connection-per-request model makes a command per drag-frame wasteful, and MiuiX Slider exposes the
    end-of-drag callback plus TalkBack setProgress semantics. The host change must take effect at once
    (otherwise Ajustes appears to do nothing while connected), so the telemetry session ends when
    endpointProvider() differs and the outer retry loop reconnects with attempt reset to 0. Control
    failures are fixed Spanish strings, not the device's English message, because the users are
    Spanish-speaking and the contract forbids string-matching the message anyway. MiuiX Snackbar
    already sets liveRegion = Polite, so feedback is announced (FE-INV-010) without extra semantics.
  files:
    - Lumina-BETA-ANDROID/docs/API_VERIFICATION.md
    - Lumina-BETA-ANDROID/PLAN.md
    - Lumina-BETA-ANDROID/CHANGELOG.md
    - Lumina-BETA-ANDROID/shared/src/commonMain/kotlin/com/korealm/lumina/data/DeviceRepository.kt
    - Lumina-BETA-ANDROID/shared/src/commonMain/kotlin/com/korealm/lumina/data/DeviceRepositoryImpl.kt
    - Lumina-BETA-ANDROID/shared/src/commonMain/kotlin/com/korealm/lumina/transport/KtorTelemetrySource.kt
    - Lumina-BETA-ANDROID/shared/src/commonMain/kotlin/com/korealm/lumina/ui/dashboard/DashboardUiState.kt
    - Lumina-BETA-ANDROID/shared/src/commonMain/kotlin/com/korealm/lumina/ui/dashboard/DashboardViewModel.kt
    - Lumina-BETA-ANDROID/shared/src/commonMain/kotlin/com/korealm/lumina/ui/dashboard/DashboardScreen.kt
    - Lumina-BETA-ANDROID/shared/src/commonMain/kotlin/com/korealm/lumina/ui/dashboard/DashboardActions.kt
    - Lumina-BETA-ANDROID/shared/src/commonMain/kotlin/com/korealm/lumina/ui/dashboard/DashboardMessages.kt
    - Lumina-BETA-ANDROID/shared/src/commonMain/kotlin/com/korealm/lumina/ui/settings/SettingsUiState.kt
    - Lumina-BETA-ANDROID/shared/src/commonMain/kotlin/com/korealm/lumina/ui/settings/SettingsValidation.kt
    - Lumina-BETA-ANDROID/shared/src/commonMain/kotlin/com/korealm/lumina/ui/settings/SettingsViewModel.kt
    - Lumina-BETA-ANDROID/shared/src/commonMain/kotlin/com/korealm/lumina/ui/settings/SettingsScreen.kt
    - Lumina-BETA-ANDROID/shared/src/commonMain/kotlin/com/korealm/lumina/ui/AppRoot.kt
    - Lumina-BETA-ANDROID/shared/src/commonMain/kotlin/com/korealm/lumina/App.kt
    - Lumina-BETA-ANDROID/shared/src/commonMain/kotlin/com/korealm/lumina/di/AppModule.kt
    - Lumina-BETA-ANDROID/shared/src/commonMain/composeResources/values/strings.xml
    - Lumina-BETA-ANDROID/shared/src/commonTest/kotlin/com/korealm/lumina/data/FakeDeviceRepository.kt
    - Lumina-BETA-ANDROID/shared/src/commonTest/kotlin/com/korealm/lumina/data/FakeSettingsStore.kt
    - Lumina-BETA-ANDROID/shared/src/commonTest/kotlin/com/korealm/lumina/ui/dashboard/DashboardMessagesTest.kt
    - Lumina-BETA-ANDROID/shared/src/commonTest/kotlin/com/korealm/lumina/ui/dashboard/DashboardReducerTest.kt
    - Lumina-BETA-ANDROID/shared/src/commonTest/kotlin/com/korealm/lumina/ui/settings/SettingsValidationTest.kt
    - Lumina-BETA-ANDROID/shared/src/commonTest/kotlin/com/korealm/lumina/transport/TelemetrySourceTest.kt
    - Lumina-BETA-ANDROID/shared/src/jvmTest/kotlin/com/korealm/lumina/ui/dashboard/DashboardViewModelTest.kt
    - Lumina-BETA-ANDROID/shared/src/jvmTest/kotlin/com/korealm/lumina/ui/settings/SettingsViewModelTest.kt
  approvals: [user]
  follow_up: >-
    Gate: ./gradlew :shared:allTests :androidApp:assembleDebug; then :desktopApp:run with
    ../Testing_server `npm run dev` to exercise volume/mute/runtime and an Ajustes save that
    reconnects. Known non-blockers: Switch/Button/preference signatures are docs-main-verified and
    re-checked at first compile (API_VERIFICATION §7.3); SettingsStoreImpl still has no unit test;
    runtime start/stop optimistically relies on the next telemetry frame rather than an optimistic
    state write.

# ---------------------------------------------------------------------------
# CHG-FE-0014 — test double: FakeControlConnection didn't honor the line framing
# ---------------------------------------------------------------------------
- id: CHG-FE-0014
  date: 2026-09-22
  agent: opencode/deepseek-v4-flash
  type: fix
  status: applied
  invariants:
    - FE-INV-030
    - FE-INV-050
    - FE-INV-062
  supersedes: null
  summary: >-
    Made FakeControlConnection.writeLine append the trailing "\n" so the test double mirrors the
    real KtorControlConnection. This fixes the 10 Phase 3-gate failures (ControlClientTest's five
    "exact line" assertions on both targets). Production was already correct: KtorControlClient
    passes the bare command and KtorControlConnection.writeLine owns the newline framing, exactly
    once per line.
  rationale: >-
    ControlConnection's contract states "writeLine appends the \n" and the real implementation does
    (writeStringUtf8(line + "\n")); the fake recorded the bare line, so the assertions that expect
    the wire bytes (line + "\n") failed. Fixing the fake (test-only) keeps the newline responsibility
    in one place and preserves the framing assertion; moving the newline into KtorControlClient
    instead would double it on the real socket and contradict the documented contract. The mtimes
    show the test files (19:58-19:59) postdate the KtorControlClient edit (19:53), so this test+fake
    combination was not compiled together until the Phase 3 gate. FE-INV-062: root cause reported as
    the observed ComparisonFailure, not an impression.
  files:
    - Lumina-BETA-ANDROID/shared/src/commonTest/kotlin/com/korealm/lumina/transport/TransportFakes.kt
    - Lumina-BETA-ANDROID/CHANGELOG.md
  approvals: [user]
  follow_up: >-
    Re-run ./gradlew :shared:allTests (expect all 122 test methods x targets green) and
    :androidApp:assembleDebug; then the desktop control loop. No production code changed.

# ---------------------------------------------------------------------------
# CHG-FE-0015 — Phase 3 (control + settings) formally closed
# ---------------------------------------------------------------------------
- id: CHG-FE-0015
  date: 2026-09-22
  agent: opencode/deepseek-v4-flash
  type: chore
  status: applied
  invariants:
    - FE-INV-010
    - FE-INV-025
    - FE-INV-026
    - FE-INV-032
    - FE-INV-050
    - FE-INV-051
    - FE-INV-053
    - FE-INV-061
    - FE-INV-062
  supersedes: null
  summary: >-
    Closed Phase 3. User-run gate green: ./gradlew :shared:allTests = 232 tests (110 commonTest
    methods x 2 targets = 220, plus 12 jvmTest-only methods), and :androidApp:assembleDebug compiles
    cleanly with no warnings. Desktop target verified end to end by the user against ../Testing_server
    : volume set/mute, runtime start/stop and the token all work. Marked Phase 3 COMPLETE in PLAN.md
    and recorded the first-compile confirmation of the MiuiX signatures in API_VERIFICATION §7.3.
  rationale: >-
    FE-INV-061 requires a CHG-FE entry per phase close; FE-INV-062 requires reporting the measured
    gate result (232 green) rather than an impression. The closure also converts the §7.3
    "re-check at first compile" caveat into a verified fact, since the clean build exercised every
    MiuiX component Phase 3 uses (the miuix-preference module remains at first-use for Phase 4,
    where it is still unused). Phase 4 was intentionally not started (user instruction).
  files:
    - Lumina-BETA-ANDROID/PLAN.md
    - Lumina-BETA-ANDROID/docs/API_VERIFICATION.md
    - Lumina-BETA-ANDROID/CHANGELOG.md
  approvals: [user]
  follow_up: >-
    Phase 4 (People + enrollment) is NOT started. Its known entry items: add
    androidx.activity:activity-compose to shared/androidMain before the Photo Picker; PeopleRepository
    + People screen + refresh; camera enroll (progress, cancel on a second connection); photo enroll
    via ImagePreparer + enroll.images caps; "Start Lúmina" recovery when
    enroll.error.runtime != "started"; live-region enrollment announcements. Deferred cleanups:
    remove miuix-nav if still unused (final cleanup), SettingsStoreImpl unit test via MapSettings,
    and user messaging for transport Malformed/UnsupportedProto.

# ---------------------------------------------------------------------------
# CHG-FE-0016 — Phase 4 (people + enrollment) implemented
# ---------------------------------------------------------------------------
- id: CHG-FE-0016
  date: 2026-09-22
  agent: opencode/deepseek-v4-flash
  type: impl
  status: applied
  invariants:
    - FE-INV-001
    - FE-INV-010
    - FE-INV-020
    - FE-INV-022
    - FE-INV-025
    - FE-INV-026
    - FE-INV-030
    - FE-INV-031
    - FE-INV-032
    - FE-INV-033
    - FE-INV-041
    - FE-INV-050
    - FE-INV-051
    - FE-INV-052
    - FE-INV-053
    - FE-INV-060
    - FE-INV-061
  supersedes: null
  summary: >-
    Implemented Phase 4. Transport: ControlClient gained streaming enrollFromCamera/enrollFromImages
    (base64 via Kotlin stdlib Base64, stable since 2.2) and cancelEnrollment on a second connection,
    with a per-line 30 s read watchdog; EnrollEvent gained a client-side Failure(EnrollmentFailure)
    variant so busy/unauthorized rejections get Spanish copy (FE-INV-041) without inventing a wire
    message (FE-INV-030). Data: DeviceRepository gained requestPeople and now shares ONE telemetry
    session via shareIn on a DI app scope (so the dashboard and people screens do not open two UDP
    sockets); new PeopleRepository and EnrollmentRepository (prepare -> validate caps -> send);
    ImagePreparer expect/actual (Android BitmapFactory/Bitmap.compress, JVM ImageIO). UI: ConnectionState
    moved to ui/; new ui/people screen (list + refresh + inline add card + progress with phase
    liveRegion + cancel + "Iniciar Lúmina" recovery); ui/picker rememberPhotoPicker expect/actual
    (Android PickMultipleVisualMedia(5), desktop JFileChooser). The dashboard's read-only people card
    was removed (one purpose per screen). AppRoot/App/AppModule wired.
  rationale: >-
    Locked decisions from the plan review: remove the dashboard people card; desktop photo enrollment
    via JFileChooser; inline add-person card; photo picker capped at 5 and camera fixed at 10 frames;
    share one telemetry session. FE-INV-001: the riskiest APIs were verified before use — Kotlin
    Base64 (kotlinlang.org stdlib 2.4.20, stable since 2.2), MiuiX LinearProgressIndicator and
    TextField(enabled=) (docs), Android Photo Picker PickMultipleVisualMedia(maxItems) +
    PickVisualMediaRequest.Builder() (androidx-main source; activity 1.13.0 has no git tag, confidence
    0.90, re-verified at first use). FE-INV-020: no new dependency; androidx.activity:activity-compose
    was already catalogued and used by :androidApp, and is now also on :shared/androidMain.
  files:
    - Lumina-BETA-ANDROID/shared/src/commonMain/kotlin/com/korealm/lumina/protocol/EnrollEvent.kt
    - Lumina-BETA-ANDROID/shared/src/commonMain/kotlin/com/korealm/lumina/transport/ControlClient.kt
    - Lumina-BETA-ANDROID/shared/src/commonMain/kotlin/com/korealm/lumina/transport/KtorControlClient.kt
    - Lumina-BETA-ANDROID/shared/src/commonMain/kotlin/com/korealm/lumina/data/DeviceRepository.kt
    - Lumina-BETA-ANDROID/shared/src/commonMain/kotlin/com/korealm/lumina/data/DeviceRepositoryImpl.kt
    - Lumina-BETA-ANDROID/shared/src/commonMain/kotlin/com/korealm/lumina/data/PeopleRepository.kt
    - Lumina-BETA-ANDROID/shared/src/commonMain/kotlin/com/korealm/lumina/data/PeopleRepositoryImpl.kt
    - Lumina-BETA-ANDROID/shared/src/commonMain/kotlin/com/korealm/lumina/data/EnrollmentRepository.kt
    - Lumina-BETA-ANDROID/shared/src/commonMain/kotlin/com/korealm/lumina/data/EnrollmentRepositoryImpl.kt
    - Lumina-BETA-ANDROID/shared/src/commonMain/kotlin/com/korealm/lumina/data/ImagePreparer.kt
    - Lumina-BETA-ANDROID/shared/src/androidMain/kotlin/com/korealm/lumina/data/PlatformImagePreparer.android.kt
    - Lumina-BETA-ANDROID/shared/src/jvmMain/kotlin/com/korealm/lumina/data/PlatformImagePreparer.jvm.kt
    - Lumina-BETA-ANDROID/shared/src/commonMain/kotlin/com/korealm/lumina/ui/ConnectionState.kt
    - Lumina-BETA-ANDROID/shared/src/commonMain/kotlin/com/korealm/lumina/ui/people/PeopleUiState.kt
    - Lumina-BETA-ANDROID/shared/src/commonMain/kotlin/com/korealm/lumina/ui/people/PeopleMessages.kt
    - Lumina-BETA-ANDROID/shared/src/commonMain/kotlin/com/korealm/lumina/ui/people/PeopleActions.kt
    - Lumina-BETA-ANDROID/shared/src/commonMain/kotlin/com/korealm/lumina/ui/people/PeopleViewModel.kt
    - Lumina-BETA-ANDROID/shared/src/commonMain/kotlin/com/korealm/lumina/ui/people/PeopleScreen.kt
    - Lumina-BETA-ANDROID/shared/src/commonMain/kotlin/com/korealm/lumina/ui/picker/PhotoPicker.kt
    - Lumina-BETA-ANDROID/shared/src/androidMain/kotlin/com/korealm/lumina/ui/picker/PhotoPicker.android.kt
    - Lumina-BETA-ANDROID/shared/src/jvmMain/kotlin/com/korealm/lumina/ui/picker/PhotoPicker.jvm.kt
    - Lumina-BETA-ANDROID/shared/src/commonMain/kotlin/com/korealm/lumina/ui/dashboard/DashboardUiState.kt
    - Lumina-BETA-ANDROID/shared/src/commonMain/kotlin/com/korealm/lumina/ui/dashboard/DashboardScreen.kt
    - Lumina-BETA-ANDROID/shared/src/commonMain/kotlin/com/korealm/lumina/ui/dashboard/DashboardViewModel.kt
    - Lumina-BETA-ANDROID/shared/src/commonMain/kotlin/com/korealm/lumina/ui/AppRoot.kt
    - Lumina-BETA-ANDROID/shared/src/commonMain/kotlin/com/korealm/lumina/App.kt
    - Lumina-BETA-ANDROID/shared/src/commonMain/kotlin/com/korealm/lumina/di/AppModule.kt
    - Lumina-BETA-ANDROID/shared/src/commonMain/composeResources/values/strings.xml
    - Lumina-BETA-ANDROID/shared/build.gradle.kts
    - Lumina-BETA-ANDROID/PLAN.md
    - Lumina-BETA-ANDROID/docs/API_VERIFICATION.md
    - Lumina-BETA-ANDROID/CHANGELOG.md
  approvals: [user]
  follow_up: >-
    Gate: ./gradlew :shared:allTests :androidApp:assembleDebug; then :desktopApp:run against
    ../Testing_server `npm run dev` to exercise camera enrollment, the busy scenario (second
    enrollment), cancel, and photo enrollment (desktop file chooser). New tests: ControlClientEnrollTest,
    PrepareImageBatchTest, PeopleReducerTest (commonTest); PeopleViewModelTest, JvmImagePreparerTest,
    DeviceRepositorySharingTest (jvmTest). Known non-blockers: MiuiX TextField(enabled=) verified from
    docs but confirm at first compile; Activity 1.13.0 has no git tag (Photo Picker re-verified at
    first use); the desktop JFileChooser blocks the calling thread (dev-only path).

# ---------------------------------------------------------------------------
# CHG-FE-0017 — Phase 4 second-pass review fixes
# ---------------------------------------------------------------------------
- id: CHG-FE-0017
  date: 2026-09-22
  agent: opencode/deepseek-v4-flash
  type: fix
  status: applied
  invariants:
    - FE-INV-010
    - FE-INV-030
    - FE-INV-041
    - FE-INV-051
    - FE-INV-052
    - FE-INV-060
    - FE-INV-061
  supersedes: null
  summary: >-
    Adversarial second pass over the Phase 4 code. Fixed: (F1) the streaming enrollment now guards
    buildLine()/writeLine so a write or encoding failure becomes EnrollEvent.Failure(Io) instead of
    throwing across the flow, restoring the "failures are values" contract; the request line is built
    before the socket opens so an encoding failure cannot leak a connection. (F2) the Android photo
    picker reads the selected URIs with ContentResolver on Dispatchers.Default and delivers onPicked
    back on Main, so it no longer does multi-MB I/O on the UI thread. (F3/F13) a successful enrollment
    now clears needsToken and the name field. (F4) the "no face detected" notice is announced once per
    occurrence via a transition counter and a keyed live-region node (consecutive frames stay silent).
    (F5) an unrecognized message type during a stream is ignored (contract §10) instead of aborting the
    enrollment. (F7) the People connection banner shows "Conectando" neutrally instead of in error
    colors. (F9) prepareImageBatch catches a throwing ImagePreparer and returns NotPrepared. (F12) an
    empty picker result is a silent no-op (removed PeopleMessage.NoPhotos and its string).
  rationale: >-
    Requested adversarial review. F1/F9 close real gaps where unexpected throwables escaped the
    transport/data seams and relied on the view model's safety net, contradicting the KDoc and
    FE-INV-052/AGENTS §7. F2 removes an ANR risk. F3/F4/F5/F7/F12/F13 are correctness/accessibility
    fixes. Withdrawn after re-analysis: the earlier worry that canRestartRuntime should not be offered
    for enroll.error.runtime == "unchanged" is NOT a defect — contract §4.7 defines "unchanged" as the
    image route not touching the runtime, which includes "already down", and §6.5 is deliberately
    conservative (any state != "started" offers recovery). Deferred with logged notes: EXIF orientation
    handling (needs android.media.ExifInterface verification or a new dependency, FE-INV-020), the full
    enrollment-announcement overhaul (progress and off-tab announcements; TODO(ui) in PeopleScreen),
    and any DeviceRepositoryImpl shareIn hardening (safe only because KtorTelemetrySource.status() never
    completes; noted in KDoc).
  files:
    - Lumina-BETA-ANDROID/shared/src/commonMain/kotlin/com/korealm/lumina/transport/KtorControlClient.kt
    - Lumina-BETA-ANDROID/shared/src/commonMain/kotlin/com/korealm/lumina/data/ImagePreparer.kt
    - Lumina-BETA-ANDROID/shared/src/commonMain/kotlin/com/korealm/lumina/data/DeviceRepositoryImpl.kt
    - Lumina-BETA-ANDROID/shared/src/commonMain/kotlin/com/korealm/lumina/ui/people/PeopleUiState.kt
    - Lumina-BETA-ANDROID/shared/src/commonMain/kotlin/com/korealm/lumina/ui/people/PeopleViewModel.kt
    - Lumina-BETA-ANDROID/shared/src/commonMain/kotlin/com/korealm/lumina/ui/people/PeopleMessages.kt
    - Lumina-BETA-ANDROID/shared/src/commonMain/kotlin/com/korealm/lumina/ui/people/PeopleScreen.kt
    - Lumina-BETA-ANDROID/shared/src/commonMain/kotlin/com/korealm/lumina/ui/AppRoot.kt
    - Lumina-BETA-ANDROID/shared/src/androidMain/kotlin/com/korealm/lumina/ui/picker/PhotoPicker.android.kt
    - Lumina-BETA-ANDROID/shared/src/commonMain/composeResources/values/strings.xml
    - Lumina-BETA-ANDROID/shared/src/commonTest/kotlin/com/korealm/lumina/transport/TransportFakes.kt
    - Lumina-BETA-ANDROID/shared/src/commonTest/kotlin/com/korealm/lumina/transport/ControlClientEnrollTest.kt
    - Lumina-BETA-ANDROID/shared/src/commonTest/kotlin/com/korealm/lumina/data/PrepareImageBatchTest.kt
    - Lumina-BETA-ANDROID/shared/src/commonTest/kotlin/com/korealm/lumina/ui/people/PeopleReducerTest.kt
    - Lumina-BETA-ANDROID/shared/src/jvmTest/kotlin/com/korealm/lumina/ui/people/PeopleViewModelTest.kt
    - Lumina-BETA-ANDROID/CHANGELOG.md
  approvals: [user]
  follow_up: >-
    Re-run ./gradlew :shared:allTests :androidApp:assembleDebug, then the desktop scenario loop.
    New/updated tests: write-failure and unknown-message cases in ControlClientEnrollTest; throwing
    preparer in PrepareImageBatchTest; needsToken/name clearing and no-face transition in
    PeopleReducerTest; empty-pick no-op in PeopleViewModelTest. Phase 5: revisit the enrollment
    announcement overhaul (progress + off-tab), EXIF orientation, and shareIn hardening.

# ---------------------------------------------------------------------------
# CHG-FE-0018 — fix DeviceRepositorySharingTest hanging the JVM test run
# ---------------------------------------------------------------------------
- id: CHG-FE-0018
  date: 2026-09-22
  agent: opencode/deepseek-v4-flash
  type: test
  status: applied
  invariants:
    - FE-INV-061
    - FE-INV-062
  supersedes: null
  summary: >-
    The Phase 4 gate reported 1 of 313 tests failing: DeviceRepositorySharingTest, with
    kotlinx.coroutines.test.UncompletedCoroutinesError after 1m. Its two telemetry collectors were
    launched on the TestScope (which runTest awaits) but never complete, because they follow the
    shared upstream indefinitely. Moved both collectors onto TestScope.backgroundScope, which runTest
    cancels at the end of the body.
  rationale: >-
    A TestScope.launch child that never completes makes runTest fail with UncompletedCoroutinesError
    (FE-INV-062: the gate output named the two active child StandaloneCoroutines, "coroutine#5"/#6,
    matching the two collectors). backgroundScope is the documented place for coroutines that must be
    cancelled when the test body finishes; the view-model collectors in the other jvmTests are
    unaffected because they run on viewModelScope, not the TestScope.
  files:
    - Lumina-BETA-ANDROID/shared/src/jvmTest/kotlin/com/korealm/lumina/data/DeviceRepositorySharingTest.kt
    - Lumina-BETA-ANDROID/CHANGELOG.md
  approvals: [user]
  follow_up: >-
    Re-run ./gradlew :shared:allTests :androidApp:assembleDebug (expect 313 tests green: 144 commonTest
    x 2 targets + 25 jvmTest-only). No production code changed.

# ---------------------------------------------------------------------------
# CHG-FE-0019 — Phase 4 (people + enrollment) formally closed
# ---------------------------------------------------------------------------
- id: CHG-FE-0019
  date: 2026-09-22
  agent: opencode/deepseek-v4-flash
  type: chore
  status: applied
  invariants:
    - FE-INV-010
    - FE-INV-020
    - FE-INV-025
    - FE-INV-026
    - FE-INV-033
    - FE-INV-050
    - FE-INV-051
    - FE-INV-052
    - FE-INV-061
    - FE-INV-062
  supersedes: null
  summary: >-
    Closed Phase 4. User-run gate green: ./gradlew :shared:allTests = 313 tests (144 commonTest methods
    x 2 targets = 288, plus 25 jvmTest-only methods), and :androidApp:assembleDebug compiles cleanly
    with no warnings. Desktop target verified end to end by the user against ../Testing_server: people
    list + refresh, camera enrollment with progress, cancel on a second connection, photo enrollment
    via the Swing file chooser, and the "Start Lúmina" recovery path. Marked Phase 4 COMPLETE in
    PLAN.md and closed the API_VERIFICATION §8 at-first-compile caveats.
  rationale: >-
    FE-INV-061 requires a CHG-FE entry per phase close; FE-INV-062 requires reporting the measured gate
    result (313 green, taken from the pasted gate output) rather than an impression. This closure wraps
    the Phase 4 work recorded in CHG-FE-0016 (impl), CHG-FE-0017 (adversarial second-pass fixes) and
    CHG-FE-0018 (test-only fix for the DeviceRepositorySharingTest hang). Deliberately deferred to
    Phase 5, not blockers: the full enrollment-announcement overhaul (progress + off-tab; TODO(ui) in
    PeopleScreen), EXIF orientation handling, DeviceRepositoryImpl shareIn hardening, and a manual
    device run of the Android Photo Picker (verified by compile and by the desktop file chooser, but
    not yet on a handset). User explicitly approved recording these as Phase 5 residual.
  files:
    - Lumina-BETA-ANDROID/PLAN.md
    - Lumina-BETA-ANDROID/docs/API_VERIFICATION.md
    - Lumina-BETA-ANDROID/CHANGELOG.md
  approvals: [user]
  follow_up: >-
    Phase 5 (hardening + accessibility) is NOT started (user instruction). Entry items: full
    enrollment-announcement overhaul, EXIF orientation, shareIn hardening, a manual TalkBack pass, the
    PLAN §10 checklist, user messaging for transport Malformed/UnsupportedProto, a SettingsStoreImpl
    test via MapSettings, a manual device run of the Android Photo Picker, and final cleanup of
    miuix-nav if still unused (CHG-FE-0007).

# ---------------------------------------------------------------------------
# CHG-FE-0020 — Phase 5 resilience fixes: no startup offline flash; incompatible stays live
# ---------------------------------------------------------------------------
- id: CHG-FE-0020
  date: 2026-09-22
  agent: opencode/deepseek-v4-flash
  type: fix
  status: applied
  invariants:
    - FE-INV-010
    - FE-INV-031
    - FE-INV-052
    - FE-INV-061
    - FE-INV-062
  supersedes: null
  summary: >-
    Fixed two resilience defects found in the Phase 5 audit. (R1) KtorTelemetrySource.session seeded
    lastArrivalMs = null, so reduceLiveness(null, now) made the first 250 ms ticker pass emit Offline
    before any datagram was expected: the UI showed/announced "Sin conexión" on startup, contradicting
    DeviceRepositoryImpl's KDoc. It now seeds lastArrivalMs at connect time, so offline follows 5 s of
    silence. (R2) An unsupported-proto frame did not refresh liveness, so the ticker overwrote the
    "Dispositivo incompatible" banner with Offline within one tick; a frame the device did send now
    refreshes liveness (only undecodable garbage does not).
  rationale: >-
    FE-INV-031 defines offline as "no status for >= 5 s", and the timer must run from connection
    time; treating "no frame yet" as offline was a false positive (and a confusing TalkBack
    announcement, FE-INV-010). An incompatible device is present and sending frames, so it must stay
    live; only a datagram we cannot parse at all is ignored. Tests added: no early Offline; Offline at
    exactly 5 s with no frames; Offline->Online recovery; incompatible frame keeps liveness and expires
    5 s after it stops; socket failure -> Offline -> 1 s backoff -> reconnect -> Online (virtual time);
    a stalled enrollment stream ends with Failure(Io) via the 30 s read watchdog; and the shared
    telemetry flow resubscribes after the WhileSubscribed stop timeout.
  files:
    - Lumina-BETA-ANDROID/shared/src/commonMain/kotlin/com/korealm/lumina/transport/KtorTelemetrySource.kt
    - Lumina-BETA-ANDROID/shared/src/commonTest/kotlin/com/korealm/lumina/transport/TelemetrySourceTest.kt
    - Lumina-BETA-ANDROID/shared/src/commonTest/kotlin/com/korealm/lumina/transport/TransportFakes.kt
    - Lumina-BETA-ANDROID/shared/src/commonTest/kotlin/com/korealm/lumina/transport/ControlClientEnrollTest.kt
    - Lumina-BETA-ANDROID/shared/src/jvmTest/kotlin/com/korealm/lumina/data/DeviceRepositorySharingTest.kt
    - Lumina-BETA-ANDROID/CHANGELOG.md
  approvals: [user]
  follow_up: >-
    No behavior change for malformed datagrams (still silently dropped, by design). Re-run
    ./gradlew :shared:allTests and the desktop loop; the startup must no longer flash "Sin conexión"
    and the incompatible banner must stay stable.

# ---------------------------------------------------------------------------
# CHG-FE-0021 — Phase 5 accessibility pass + shell enrollment announcements
# ---------------------------------------------------------------------------
- id: CHG-FE-0021
  date: 2026-09-22
  agent: opencode/deepseek-v4-flash
  type: impl
  status: applied
  invariants:
    - FE-INV-010
    - FE-INV-025
    - FE-INV-026
    - FE-INV-051
    - FE-INV-060
    - FE-INV-061
  supersedes: null
  summary: >-
    Accessibility pass. Extracted the duplicated InfoRow and TokenWarning into ui/components/;
    InfoRow now uses Modifier.semantics(mergeDescendants = true) so a screen reader reads
    "etiqueta: valor" as one focus stop instead of two. Added a shell-level EnrollmentStatusLine shown
    while an enrollment runs and the Personas tab is not composed, so phase changes are announced on
    every tab (closes the TODO(ui) from CHG-FE-0017); phaseLabel/progressFraction moved there and the
    Personas card keeps the detailed view + cancel. AppRoot now lays out a Column with the optional
    status line plus the active screen (weight(1f)). Added docs/ACCESSIBILITY.md: the FE-INV-010/026
    checklist, a per-screen semantics/announcements inventory, the FE-INV-025 fallback register, and a
    step-by-step manual TalkBack script.
  rationale: >-
    FE-INV-010.6 requires enrollment progress to be announced, not only drawn; previously the
    liveRegion nodes lived only in PeopleScreen, so switching tabs silenced them. A visible shell line
    (rather than a hidden node) avoids relying on unverified off-screen semantics. FE-INV-060 requires
    a durable audit record; docs/ACCESSIBILITY.md is it. No Material3 is used; the only fallbacks are
    the platform pickers, liveRegion semantics, and Compose foundation layout (documented).
  files:
    - Lumina-BETA-ANDROID/shared/src/commonMain/kotlin/com/korealm/lumina/ui/components/InfoRow.kt
    - Lumina-BETA-ANDROID/shared/src/commonMain/kotlin/com/korealm/lumina/ui/components/TokenWarning.kt
    - Lumina-BETA-ANDROID/shared/src/commonMain/kotlin/com/korealm/lumina/ui/people/EnrollmentStatusLine.kt
    - Lumina-BETA-ANDROID/shared/src/commonMain/kotlin/com/korealm/lumina/ui/people/PeopleScreen.kt
    - Lumina-BETA-ANDROID/shared/src/commonMain/kotlin/com/korealm/lumina/ui/dashboard/DashboardScreen.kt
    - Lumina-BETA-ANDROID/shared/src/commonMain/kotlin/com/korealm/lumina/ui/AppRoot.kt
    - Lumina-BETA-ANDROID/docs/ACCESSIBILITY.md
    - Lumina-BETA-ANDROID/CHANGELOG.md
  approvals: [user]
  follow_up: >-
    Manual TalkBack pass (docs/ACCESSIBILITY.md §5) is the user gate; verify the token-reveal
    TextButton target and Slider/Switch label association there before changing them.

# ---------------------------------------------------------------------------
# CHG-FE-0022 — Phase 5 dependencies: add test-only settings fake, remove unused miuix-nav
# ---------------------------------------------------------------------------
- id: CHG-FE-0022
  date: 2026-09-22
  agent: opencode/deepseek-v4-flash
  type: decision
  status: applied
  invariants:
    - FE-INV-020
    - FE-INV-050
    - FE-INV-061
  supersedes: null
  summary: >-
    User-approved dependency changes. Added com.russhwolf:multiplatform-settings-test:1.3.0 to
    shared/commonTest (test-only) so SettingsStoreImpl can be tested with MapSettings; added
    SettingsStoreImplTest (defaults, endpoint/token round-trip, partial seed keeps port defaults),
    closing a gap open since Phase 2. Removed the unused top.yukonga.miuix.kmp:miuix-nav from the
    version catalog and shared/commonMain (never imported; obligation from CHG-FE-0007). Kept
    miuix-preference/miuix-squircle (unused, retained) and miuix-blur (manifest overrideLibrary).
  rationale: >-
    FE-INV-020 requires approval for any dependency change; the user approved the test-only artifact
    and the removal in Phase 5. MapSettings is the standard in-memory Settings fake and avoids
    hand-implementing the whole Settings interface. Removing miuix-nav shrinks the dependency surface
    without affecting behavior (NavigationBar comes from miuix-ui).
  files:
    - Lumina-BETA-ANDROID/gradle/libs.versions.toml
    - Lumina-BETA-ANDROID/shared/build.gradle.kts
    - Lumina-BETA-ANDROID/shared/src/commonTest/kotlin/com/korealm/lumina/data/SettingsStoreImplTest.kt
    - Lumina-BETA-ANDROID/CHANGELOG.md
  approvals: [user]
  follow_up: >-
    Confirm multiplatform-settings-test resolves and MapSettings compiles at first build; if the
    artifact name differs, re-verify (API_VERIFICATION §9.4).

# ---------------------------------------------------------------------------
# CHG-FE-0023 — Phase 5 EXIF orientation for photo enrollment (Android)
# ---------------------------------------------------------------------------
- id: CHG-FE-0023
  date: 2026-09-22
  agent: opencode/deepseek-v4-flash
  type: impl
  status: applied
  invariants:
    - FE-INV-001
    - FE-INV-033
    - FE-INV-050
    - FE-INV-061
  supersedes: null
  summary: >-
    Photo enrollment now applies EXIF orientation before scaling on Android. Added the pure
    data/ImageOrientation.kt (ImageOrientation/ImageFlip + imageOrientationFromExif mapping all 8
    values, unknown -> Normal) with ImageOrientationTest; PlatformImagePreparer.android.kt reads
    android.media.ExifInterface(ByteArrayInputStream(raw)) and applies rotation/mirror via a Matrix
    before the existing downscale/JPEG step, recycling each intermediate bitmap. The JVM preparer
    documents that it does not apply EXIF (no JDK reader; desktop dev loop only).
  rationale: >-
    FE-INV-033 controls size/format, but a portrait phone photo decoded by BitmapFactory comes out
    rotated, which can make face enrollment fail. Keeping the value->transform mapping pure and in
    common code makes it unit-testable with no device; the numeric EXIF constants are recorded at
    confidence 0.85 in API_VERIFICATION §9.1 and must be confirmed at first compile (FE-INV-001).
  files:
    - Lumina-BETA-ANDROID/shared/src/commonMain/kotlin/com/korealm/lumina/data/ImageOrientation.kt
    - Lumina-BETA-ANDROID/shared/src/commonTest/kotlin/com/korealm/lumina/data/ImageOrientationTest.kt
    - Lumina-BETA-ANDROID/shared/src/androidMain/kotlin/com/korealm/lumina/data/PlatformImagePreparer.android.kt
    - Lumina-BETA-ANDROID/shared/src/jvmMain/kotlin/com/korealm/lumina/data/PlatformImagePreparer.jvm.kt
    - Lumina-BETA-ANDROID/docs/API_VERIFICATION.md
    - Lumina-BETA-ANDROID/CHANGELOG.md
  approvals: [user]
  follow_up: >-
    Confirm android.media.ExifInterface's InputStream constructor and constants at first compile.
    Android picker + EXIF runtime path still needs a device run (docs/ACCESSIBILITY.md §6).

# ---------------------------------------------------------------------------
# CHG-FE-0024 — Phase 5 second-pass review fixes (EXIF order, liveRegion placement)
# ---------------------------------------------------------------------------
- id: CHG-FE-0024
  date: 2026-09-22
  agent: opencode/deepseek-v4-flash
  type: fix
  status: applied
  invariants:
    - FE-INV-001
    - FE-INV-010
    - FE-INV-033
    - FE-INV-050
    - FE-INV-060
    - FE-INV-061
    - FE-INV-062
  supersedes: null
  summary: >-
    Adversarial second pass over the Phase 5 code. Fixed: (F1) the Android EXIF transform applied the
    mirror before the rotation (matrix.postScale then postRotate), which swapped EXIF orientations 5
    (transpose) and 7 (transverse). ImageOrientation is now an ordered ImageOperation list (Rotate
    then Mirror for 5/7) and the Android actual applies it in order; ImageOrientationTest asserts the
    order so it cannot regress. (F2) liveRegion was set on the container Card in the new
    EnrollmentStatusLine and in both ConnectionBanners; it is now on the changing phase/label Text,
    since Compose announces the node whose content changed (a card-level liveRegion is ignored, or
    spams if the card merges descendants). (F3) removed the stale TODO in PeopleScreen referencing the
    enrollment-announcement overhaul implemented in CHG-FE-0021. (F4) marked InfoRow/TokenWarning
    internal (module-only use). (F5) the stalled-stream test now asserts the written start line.
  rationale: >-
    F1 was a genuine wrong-output bug: S∘R vs R∘S differ for the transpose cases (5 = rotate 90 then
    mirror horizontal; 7 = rotate 270 then mirror horizontal). The prior pure test only checked the
    (rotation, flip) pair, so it gave false confidence; making the order data makes it testable
    (FE-INV-001/062). F2 is an accessibility correctness fix (FE-INV-010.6): the people card already
    put liveRegion on its phase text, and the new shell line plus the banners did not. No behavior
    change for the common rotate-only orientations (3/6/8) or the single-axis mirrors (2/4).
  files:
    - Lumina-BETA-ANDROID/shared/src/commonMain/kotlin/com/korealm/lumina/data/ImageOrientation.kt
    - Lumina-BETA-ANDROID/shared/src/commonTest/kotlin/com/korealm/lumina/data/ImageOrientationTest.kt
    - Lumina-BETA-ANDROID/shared/src/androidMain/kotlin/com/korealm/lumina/data/PlatformImagePreparer.android.kt
    - Lumina-BETA-ANDROID/shared/src/commonMain/kotlin/com/korealm/lumina/ui/people/EnrollmentStatusLine.kt
    - Lumina-BETA-ANDROID/shared/src/commonMain/kotlin/com/korealm/lumina/ui/people/PeopleScreen.kt
    - Lumina-BETA-ANDROID/shared/src/commonMain/kotlin/com/korealm/lumina/ui/dashboard/DashboardScreen.kt
    - Lumina-BETA-ANDROID/shared/src/commonMain/kotlin/com/korealm/lumina/ui/components/InfoRow.kt
    - Lumina-BETA-ANDROID/shared/src/commonMain/kotlin/com/korealm/lumina/ui/components/TokenWarning.kt
    - Lumina-BETA-ANDROID/shared/src/commonTest/kotlin/com/korealm/lumina/transport/ControlClientEnrollTest.kt
    - Lumina-BETA-ANDROID/docs/API_VERIFICATION.md
    - Lumina-BETA-ANDROID/docs/ACCESSIBILITY.md
    - Lumina-BETA-ANDROID/CHANGELOG.md
  approvals: [user]
  follow_up: >-
    Re-run ./gradlew check :androidApp:assembleDebug, then the desktop loop. The manual TalkBack pass
    (docs/ACCESSIBILITY.md §5) confirms F2; the Android EXIF runtime path still needs the device run.

# ---------------------------------------------------------------------------
# CHG-FE-0025 — Phase 5 (hardening + accessibility) formally closed
# ---------------------------------------------------------------------------
- id: CHG-FE-0025
  date: 2026-09-22
  agent: opencode/deepseek-v4-flash
  type: chore
  status: applied
  invariants:
    - FE-INV-001
    - FE-INV-010
    - FE-INV-020
    - FE-INV-025
    - FE-INV-026
    - FE-INV-031
    - FE-INV-033
    - FE-INV-050
    - FE-INV-051
    - FE-INV-052
    - FE-INV-061
    - FE-INV-062
  supersedes: null
  summary: >-
    Closed Phase 5. User-run gate green: ./gradlew check :androidApp:assembleDebug = 336 tests (155
    commonTest methods x 2 targets = 310, plus 26 jvmTest-only methods) with a clean Android compile,
    and the desktop target verified end to end against ../Testing_server. Marked Phase 5 COMPLETE in
    PLAN.md and confirmed the at-first-compile items in API_VERIFICATION §9. The manual TalkBack/device
    checks are deferred to Phase 6 by explicit user decision.
  rationale: >-
    FE-INV-061 requires a CHG-FE entry per phase close; FE-INV-062 requires the measured result (336
    green, Android compile clean, desktop verified) rather than an impression. Phase 5 delivered:
    resilience correctness + matrix coverage (CHG-FE-0020: no startup offline flash, incompatible
    stays live, backoff/reconnect/shareIn/watchdog tests), the accessibility pass and shell enrollment
    announcements (CHG-FE-0021), the approved dependency changes + SettingsStoreImpl test
    (CHG-FE-0022), Android EXIF orientation (CHG-FE-0023), and the second-pass fixes (CHG-FE-0024:
    ordered-ops EXIF fix, liveRegion on the changing node). AGENTS §13 Definition of Done: builds
    (:shared:allTests + :androidApp:assembleDebug + desktop) PASS; pure logic unit-tested with fakes
    PASS; accessibility code-level PASS / device TalkBack PENDING (Phase 6); MiuiX used everywhere with
    the fallback register documented (docs/ACCESSIBILITY.md §7) PASS; immutable ViewModel state and
    unidirectional flow PASS; comments and Spanish string resources PASS; CHANGELOG entry present PASS;
    no unapproved dependency (only the user-approved test-only multiplatform-settings-test added,
    miuix-nav removed) PASS; runtime repository untouched PASS. Deliverables the gate did not cover are
    recorded as Phase 6 residual rather than claimed.
  files:
    - Lumina-BETA-ANDROID/PLAN.md
    - Lumina-BETA-ANDROID/docs/API_VERIFICATION.md
    - Lumina-BETA-ANDROID/docs/ACCESSIBILITY.md
    - Lumina-BETA-ANDROID/CHANGELOG.md
  approvals: [user]
  follow_up: >-
    Phase 6 (integration validation + docs) is NOT started. Residual items to close there: the manual
    TalkBack pass + PLAN §10 device items (docs/ACCESSIBILITY.md §5), the Android Photo Picker runtime
    path, the Android EXIF runtime path, and the end-to-end user transcript. miuix-preference /
    miuix-squircle remain declared-but-unused (retained by user decision).

# ---------------------------------------------------------------------------
# CHG-FE-0026 — Phase 6 prep: align the Android default host; docs + validation transcript
# ---------------------------------------------------------------------------
- id: CHG-FE-0026
  date: 2026-09-22
  agent: opencode/deepseek-v4-flash
  type: fix
  status: applied
  invariants:
    - FE-INV-034
    - FE-INV-041
    - FE-INV-053
    - FE-INV-061
  supersedes: null
  summary: >-
    Phase 6 preparation. (F1) The Android default gateway host was 10.0.2.2 (the emulator alias),
    contradicting FE-INV-034 / AGENTS §10, which state the product default is the hotspot gateway
    10.42.0.1 and list 10.0.2.2 only under "host mapping for development". Changed
    PlatformDefaults.android.kt to 10.42.0.1 and updated the PlatformDefaults.kt KDoc; the desktop
    default stays 127.0.0.1 (dev-only target). (F2) README documented a non-existent
    `:desktopApp:hotRun` task (no hot-reload plugin is applied) - removed, and the Android install
    command added. (F3/F4) README host table corrected, the physical-device UDP note added (adb
    reverse is TCP-only), the full mock scenario list documented, and the docs/ links added. Added
    docs/VALIDATION.md, the end-to-end transcript (desktop + physical Android device) covering the
    PLAN §9 matrix, enrollment, the TalkBack pass, the Android picker and EXIF.
  rationale: >-
    FE-INV-002 makes INVARIANTS.md the highest authority; FE-INV-034/AGENTS §10 specify 10.42.0.1 as
    the gateway default, so the code now matches instead of silently deviating (FE-INV-061: the change
    is logged). The dev mappings remain available by editing Ajustes, as the invariant intends. The
    README corrections remove a broken command and a default-host contradiction, and record the
    UDP/adb-reverse limitation a physical-device run hits. docs/VALIDATION.md is the artifact the
    Phase 6 gate ("user-confirmed end-to-end transcript") requires.
  files:
    - Lumina-BETA-ANDROID/shared/src/androidMain/kotlin/com/korealm/lumina/data/PlatformDefaults.android.kt
    - Lumina-BETA-ANDROID/shared/src/commonMain/kotlin/com/korealm/lumina/data/PlatformDefaults.kt
    - Lumina-BETA-ANDROID/README.md
    - Lumina-BETA-ANDROID/docs/VALIDATION.md
    - Lumina-BETA-ANDROID/CHANGELOG.md
  approvals: [user]
  follow_up: >-
    Run the gate: ./gradlew :shared:allTests :androidApp:assembleDebug, then docs/VALIDATION.md against
    ../Testing_server on desktop + a physical Android device. On green, append the Phase 6 closure
    entry and mark PLAN.md Phase 6 complete. Testing_server is retained (user decision).

# ---------------------------------------------------------------------------
# CHG-FE-0027 — fix Android control commands failing on the main thread (FE-INV-052)
# ---------------------------------------------------------------------------
- id: CHG-FE-0027
  date: 2026-09-22
  agent: opencode/deepseek-v4-flash
  type: fix
  status: applied
  invariants:
    - FE-INV-001
    - FE-INV-022
    - FE-INV-050
    - FE-INV-052
    - FE-INV-053
    - FE-INV-061
    - FE-INV-062
  supersedes: null
  summary: >-
    Fixed the physical-device bug where every control command ("Detener Lúmina", volume, enrollment)
    returned "No se pudo conectar con el dispositivo" (ControlResult.Io) while telemetry and the mock's
    HTTP admin worked. Root cause: control commands are launched from viewModelScope (= Dispatchers.Main
    on Android) and KtorControlConnectionFactory.open resolved the gateway address / opened the socket
    on the caller's thread, so the app threw NetworkOnMainThreadException before any TCP SYN (the mock
    logged no control connection). Telemetry was unaffected because it runs on the injected
    Dispatchers.Default scope. Added expect val ioDispatcher (actual = Dispatchers.IO on jvmMain and
    androidMain, since Dispatchers.IO is not in the KMP common API); request/cancelEnrollment now run in
    withContext(ioDispatcher), enrollmentStream uses .flowOn(ioDispatcher), and the connection factory
    open is wrapped too. Added a jvmTest (ControlClientDispatcherTest) that asserts the socket is not
    opened on the caller's thread, and a temporary host/port diagnostic log (never the token).
  rationale: >-
    FE-INV-052 is explicit: "Sockets and disk/IO work run on Dispatchers.IO; never block the main
    thread." The control path violated it (the data layer's telemetry path did not, which is why only
    commands failed). The bug was invisible on desktop because the Swing main dispatcher does not
    enforce Android's NetworkOnMainThreadException, and invisible to unit tests because they use fakes
    and the test dispatcher; only the Phase 6 physical-device run exposed it (FE-INV-062: the mock
    console showed no control connection while UDP subscribe traffic arrived). The diagnostic log
    records host/port only, never the token (FE-INV-053), and is marked TODO for removal after device
    validation.
  files:
    - Lumina-BETA-ANDROID/shared/src/commonMain/kotlin/com/korealm/lumina/transport/Dispatchers.kt
    - Lumina-BETA-ANDROID/shared/src/jvmMain/kotlin/com/korealm/lumina/transport/Dispatchers.jvm.kt
    - Lumina-BETA-ANDROID/shared/src/androidMain/kotlin/com/korealm/lumina/transport/Dispatchers.android.kt
    - Lumina-BETA-ANDROID/shared/src/commonMain/kotlin/com/korealm/lumina/transport/KtorControlClient.kt
    - Lumina-BETA-ANDROID/shared/src/commonMain/kotlin/com/korealm/lumina/transport/KtorControlConnection.kt
    - Lumina-BETA-ANDROID/shared/src/jvmTest/kotlin/com/korealm/lumina/transport/ControlClientDispatcherTest.kt
    - Lumina-BETA-ANDROID/docs/API_VERIFICATION.md
    - Lumina-BETA-ANDROID/CHANGELOG.md
  approvals: [user]
  follow_up: >-
    Rebuild + adb install -r, then repeat docs/VALIDATION.md on the physical device: the mock must log
    "control connection from <phone-ip>" and the commands must succeed; check adb logcat for the
    [Lumina] diagnostic if not. Remove the three TODO(transport) diagnostic println lines once verified.

# ---------------------------------------------------------------------------
# CHG-FE-0028 — Phase 6 (integration validation + docs) formally closed
# ---------------------------------------------------------------------------
- id: CHG-FE-0028
  date: 2026-09-22
  agent: opencode/deepseek-v4-flash
  type: chore
  status: applied
  invariants:
    - FE-INV-010
    - FE-INV-020
    - FE-INV-025
    - FE-INV-026
    - FE-INV-033
    - FE-INV-050
    - FE-INV-051
    - FE-INV-052
    - FE-INV-061
    - FE-INV-062
  supersedes: null
  summary: >-
    Closed Phase 6. User-run gate green: ./gradlew check :androidApp:assembleDebug = 337 tests (155
    commonTest methods x 2 targets = 310, plus 27 jvmTest-only methods) with a clean Android build;
    the desktop target and a physical Android device (with TalkBack) were verified end to end against
    ../Testing_server. The phase's device bug (all control commands failing on the main thread) was
    found, fixed and confirmed on device (CHG-FE-0027), and its temporary diagnostics removed. Filled
    the end-to-end transcript in docs/VALIDATION.md (37/37 PASS), ticked PLAN.md §10, and marked
    Phase 6 COMPLETE.
  rationale: >-
    FE-INV-061 requires a CHG-FE entry per phase close; FE-INV-062 requires the measured result (337
    green, clean Android build, device verified) rather than an impression. The physical-device run
    was decisive: it exposed the FE-INV-052 violation (control socket work on Dispatchers.Main) that
    neither the desktop target nor the unit tests could surface; the fix and a regression test
    (ControlClientDispatcherTest) are recorded in CHG-FE-0027. The TalkBack pass confirms the
    accessibility requirements (FE-INV-010/026); the observed mixed Spanish/English phrasing is
    TalkBack's own hint string following the device language, not app text. No defects remain open.
    Non-blocking residuals (not blockers, recorded for the future): no dark/high-contrast theme
    variant; TalkBack hint language is device-owned; Testing_server retained for demos; the unused
    miuix-preference/miuix-squircle modules and the TODO(di) desktop ViewModel wiring are unchanged.
  files:
    - Lumina-BETA-ANDROID/shared/src/commonMain/kotlin/com/korealm/lumina/transport/KtorControlClient.kt
    - Lumina-BETA-ANDROID/PLAN.md
    - Lumina-BETA-ANDROID/docs/VALIDATION.md
    - Lumina-BETA-ANDROID/docs/ACCESSIBILITY.md
    - Lumina-BETA-ANDROID/docs/API_VERIFICATION.md
    - Lumina-BETA-ANDROID/CHANGELOG.md
  approvals: [user]
  follow_up: >-
    All phases of PLAN.md are complete. Retained: ../Testing_server (demos), miuix-preference /
    miuix-squircle, and the TODO(di) desktop ViewModel wiring. Optional future work: a
    high-contrast/dark theme variant, an app locale declaration so TalkBack prefers Spanish hints, and
    a final dependency cleanup.

# ---------------------------------------------------------------------------
# CHG-FE-0029 — brand header (LogoLong) in the app shell
# ---------------------------------------------------------------------------
- id: CHG-FE-0029
  date: 2026-09-22
  agent: opencode/deepseek-v4-flash
  type: impl
  status: applied
  invariants:
    - FE-INV-001
    - FE-INV-010
    - FE-INV-025
    - FE-INV-026
    - FE-INV-060
    - FE-INV-061
  supersedes: null
  summary: >-
    Added a brand header to the app shell. New ui/components/BrandHeader.kt renders the user-supplied
    shared/src/commonMain/composeResources/drawable/LogoLong.webp (1920 x 637, ~3.01:1) via
    painterResource + foundation Image with ContentScale.Fit, contentDescription = null (decorative),
    full width and height capped at 120 dp so it stays a slim banner on large windows. AppRoot renders
    it as the first child of the shell content Column, above the enrollment status line and the tab
    screens, so it is visible and fixed on Dashboard, Personas and Ajustes while each screen scrolls
    beneath it.
  rationale: >-
    FE-INV-026 asks for modern, spacious branding; a single shell-level header avoids duplicating the
    image per screen. contentDescription = null keeps it out of TalkBack (FE-INV-010.5: decorative
    elements excluded) because the app name is already announced by the tab labels/titles. foundation
    Image/ContentScale are Compose primitives (no MiuiX image component exists), so FE-INV-025 is
    unaffected. The webp is decoded by the platform (Android BitmapFactory, desktop Skia); recorded in
    API_VERIFICATION §11 to confirm at first run (FE-INV-001), with a PNG export as the fallback.
  files:
    - Lumina-BETA-ANDROID/shared/src/commonMain/kotlin/com/korealm/lumina/ui/components/BrandHeader.kt
    - Lumina-BETA-ANDROID/shared/src/commonMain/kotlin/com/korealm/lumina/ui/AppRoot.kt
    - Lumina-BETA-ANDROID/docs/API_VERIFICATION.md
    - Lumina-BETA-ANDROID/CHANGELOG.md
  approvals: [user]
  follow_up: >-
    Build and run desktop + device to confirm LogoLong renders (webp decode) and that the header does
    not crowd the top; tune the 120 dp cap / padding if desired.

# ---------------------------------------------------------------------------
# CHG-FE-0030 — localization scope: English and indigenous locales deliberately deferred
# ---------------------------------------------------------------------------
- id: CHG-FE-0030
  date: 2026-09-22
  agent: opencode/deepseek-v4-flash
  type: decision
  status: applied
  invariants:
    - FE-INV-041
    - FE-INV-061
  supersedes: null
  summary: >-
    Evaluated adding a second locale and decided against it. (a) English: adding values-en would
    change the rank-1 FE-INV-041 ("all user-facing text is Spanish es-MX"), and the English build is
    not demonstrated as improving the primary (blind, Spanish-first) audience, so the user chose to
    keep Spanish-only for the demo. (b) Indigenous (Mayan/Nahuatl): technically feasible via BCP-47
    resource qualifiers (values-b+nah / values-b+yua) plus Android per-app language, but the core
    users rely on TalkBack, whose TTS engines have no Mayan/Nahuatl voices, so screen-reader output
    would be mispronounced Spanish/English; reliable translation also needs native-speaker review.
    Recorded as a roadmap item instead (the product brief already lists "future support for
    indigenous languages").
  rationale: >-
    FE-INV-041 is a SCOPE invariant; per the change protocol it may not be changed silently, and the
    user explicitly chose not to amend it. FE-INV-061 requires logging the decision so a future agent
    does not re-derive it. No code changed. If i18n is revisited, CMP supports language qualifiers with
    the unqualified values/ as the Spanish fallback (verified in the CMP resources docs), and an
    indigenous locale additionally depends on a bundled on-device TTS voice.
  files:
    - Lumina-BETA-ANDROID/docs/API_VERIFICATION.md
    - Lumina-BETA-ANDROID/CHANGELOG.md
  approvals: [user]
  follow_up: >-
    If a second locale is wanted later, add values-<lang>/strings.xml (no code change) and, for Android
    13+, a localeConfig; treat a Mayan/Nahuatl locale as blocked on a TTS voice for TalkBack.

# ---------------------------------------------------------------------------
# CHG-FE-0031 — dark mode (follows the system) + dark logo
# ---------------------------------------------------------------------------
- id: CHG-FE-0031
  date: 2026-09-22
  agent: opencode/deepseek-v4-flash
  type: impl
  status: applied
  invariants:
    - FE-INV-001
    - FE-INV-025
    - FE-INV-026
    - FE-INV-060
    - FE-INV-061
  supersedes: null
  summary: >-
    Added dark-mode support. AppTheme now selects the MiuiX palette from the system theme
    (isSystemInDarkTheme -> darkColorScheme() else lightColorScheme(), remembered per mode), and
    BrandHeader renders the user-supplied LogoLongDarkMode.webp in dark mode and LogoLong.webp in
    light mode. Both logos are 1920 x 637 and stay decorative (contentDescription null).
  rationale: >-
    The default MiuiX light scheme met FE-INV-026's contrast rule, but a dark variant was a recorded
    accessibility gap (ACCESSIBILITY.md §6); following the system is the least surprising behavior and
    needs no extra settings UI or expect/actual. MiuiX 0.9.4 exposes lightColorScheme()/darkColorScheme()
    and MiuixTheme(colors=...), and CMP defines the current theme via isSystemInDarkTheme() (recorded in
    API_VERIFICATION §12.1). Visual-only change; no protocol/state impact.
  files:
    - Lumina-BETA-ANDROID/shared/src/commonMain/kotlin/com/korealm/lumina/ui/theme/AppTheme.kt
    - Lumina-BETA-ANDROID/shared/src/commonMain/kotlin/com/korealm/lumina/ui/components/BrandHeader.kt
    - Lumina-BETA-ANDROID/docs/API_VERIFICATION.md
    - Lumina-BETA-ANDROID/docs/ACCESSIBILITY.md
    - Lumina-BETA-ANDROID/CHANGELOG.md
  approvals: [user]
  follow_up: >-
    Confirm on desktop + device that the app follows the OS light/dark switch and shows the correct
    logo. Optional polish: an androidApp night theme / themed window background to avoid a light flash
    before Compose draws.

# ---------------------------------------------------------------------------
# CHG-FE-0032 — MiuiX icons across every section, row and control
# ---------------------------------------------------------------------------
- id: CHG-FE-0032
  date: 2026-09-22
  agent: opencode/deepseek-v4-flash
  type: impl
  status: applied
  invariants:
    - FE-INV-001
    - FE-INV-010
    - FE-INV-025
    - FE-INV-026
    - FE-INV-060
    - FE-INV-061
  supersedes: null
  summary: >-
    Added decorative MiuiX icons throughout the UI so low-vision users can navigate by shape while
    TalkBack users keep the (unchanged) text labels. New ui/components/SectionTitle.kt (Icon + subtitle
    text, because MiuiX SmallTitle has no icon slot) replaces SmallTitle on all three screens; InfoRow
    gained an optional leading icon; TokenWarning, EnrollmentStatusLine and both ConnectionBanners got
    leading/state icons; buttons (start/stop, refresh, camera, photos, cancel, save, runtime recovery),
    the name/host/port/token fields (leadingIcon) and the saved confirmation all carry an icon. Icons
    are from MiuixIcons (extended + basic); every icon uses contentDescription = null.
  rationale: >-
    FE-INV-026 wants big, clear, recognizable controls; icons are a functional aid for partially
    sighted users while the text labels (FE-INV-041) remain the source of truth for TalkBack, so the
    icons must be decorative (FE-INV-010.5). FE-INV-025 keeps it MiuiX. The MiuiX icon set lacks a few
    domain glyphs (thermometer, speedometer, device), so InfoRow icons are best-fit and are easy to
    swap. Verified signatures in API_VERIFICATION §12.2/§12.3.
  files:
    - Lumina-BETA-ANDROID/shared/src/commonMain/kotlin/com/korealm/lumina/ui/components/SectionTitle.kt
    - Lumina-BETA-ANDROID/shared/src/commonMain/kotlin/com/korealm/lumina/ui/components/InfoRow.kt
    - Lumina-BETA-ANDROID/shared/src/commonMain/kotlin/com/korealm/lumina/ui/components/TokenWarning.kt
    - Lumina-BETA-ANDROID/shared/src/commonMain/kotlin/com/korealm/lumina/ui/people/EnrollmentStatusLine.kt
    - Lumina-BETA-ANDROID/shared/src/commonMain/kotlin/com/korealm/lumina/ui/dashboard/DashboardScreen.kt
    - Lumina-BETA-ANDROID/shared/src/commonMain/kotlin/com/korealm/lumina/ui/people/PeopleScreen.kt
    - Lumina-BETA-ANDROID/shared/src/commonMain/kotlin/com/korealm/lumina/ui/settings/SettingsScreen.kt
    - Lumina-BETA-ANDROID/docs/API_VERIFICATION.md
    - Lumina-BETA-ANDROID/docs/ACCESSIBILITY.md
    - Lumina-BETA-ANDROID/CHANGELOG.md
  approvals: [user]
  follow_up: >-
    Compile + run desktop and device: confirm every icon renders/aligned and that TalkBack still reads
    only the labels (icons decorative). Swap any poor-fit InfoRow icon (e.g. Temperature/Info) if you
    prefer. Confirm TextField leadingIcon at compile (API_VERIFICATION §12.3).

# ---------------------------------------------------------------------------
# CHG-FE-0033 — fix: pad the TextField leading icons
# ---------------------------------------------------------------------------
- id: CHG-FE-0033
  date: 2026-09-22
  agent: opencode/deepseek-v4-flash
  type: fix
  status: applied
  invariants:
    - FE-INV-001
    - FE-INV-025
    - FE-INV-026
    - FE-INV-061
    - FE-INV-062
  supersedes: null
  summary: >-
    Fixed the cramped leading icons added in CHG-FE-0032. MiuiX TextField's chrome (v0.9.4,
    TextFieldChrome/TextFieldDecorationBox) places the leading/trailing icon composables flush against
    the field edges and applies its inside margin only to the text box (end padding when a leading icon
    exists; none horizontally when both icons exist), so the icon slots must supply their own spacing.
    The name, host, UDP, TCP and token fields now use
    `Modifier.padding(start = TextFieldDefaults.InsideMargin.width, end = 8.dp).size(20.dp)` — 16 dp
    from the border (the field's content margin) and an 8 dp gap before the text.
  rationale: >-
    The icon was visually touching the border and the text (FE-INV-026: spacious, clear controls). The
    root cause was verified against the pinned MiuiX source (the padding logic lives in
    TextFieldChrome), not guessed. Padding is applied before `size` so the 20 dp glyph is preserved.
    Only the leading icons changed; the trailing token-reveal TextButton already carries button margins.
  files:
    - Lumina-BETA-ANDROID/shared/src/commonMain/kotlin/com/korealm/lumina/ui/people/PeopleScreen.kt
    - Lumina-BETA-ANDROID/shared/src/commonMain/kotlin/com/korealm/lumina/ui/settings/SettingsScreen.kt
    - Lumina-BETA-ANDROID/docs/API_VERIFICATION.md
    - Lumina-BETA-ANDROID/CHANGELOG.md
  approvals: [user]
  follow_up: >-
    Run desktop + device: confirm the icon now sits 16 dp off the border with an 8 dp gap to the text in
    the name, host, UDP, TCP and token fields; tune the 8 dp gap if you want more separation.

# ---------------------------------------------------------------------------
# CHG-FE-0034 — Consume the additive `initializing` flag; tri-state runtime control
# ---------------------------------------------------------------------------
- id: CHG-FE-0034
  date: 2026-09-22
  agent: opencode/deepseek-v4-flash
  type: impl
  status: applied
  invariants:
    - FE-INV-001
    - FE-INV-010
    - FE-INV-031
    - FE-INV-041
    - FE-INV-051
    - FE-INV-061
    - FE-INV-062
  supersedes: null
  summary: >-
    Decoded the runtime's additive `initializing` flag (runtime CHG-0091, contract §4.1/§4.4) through
    WireStatus/WireReplies -> DomainModels -> StatusMapper/Reply (optional, defaults to false so an
    older agent still decodes), and used it to make the start/stop control tri-state. DashboardUiState
    gained `transition` (RuntimeTransition.Starting/Stopping) plus a pure `runtimeButtonState(state)`
    helper; `reduceRuntimeResult` now takes the RuntimeCommand so a start enters Starting only while
    the device reports initializing and a stop holds Stopping until telemetry confirms. The toggle is
    disabled and labeled "Iniciando…"/"Deteniendo…" while settling, `onRuntimeToggle` ignores re-taps
    during a transition, and a 10 s grace job (RUNTIME_START_GRACE_MS) clears an unconfirmed start
    with the new ControlMessage.RuntimeStartUnconfirmed. The device's `initializing` also drives the
    "starting" state for an external start the app did not send. New strings action_starting,
    action_stopping, msg_runtime_start_unconfirmed.
  rationale: >-
    After tapping "Iniciar Lúmina" the button re-enabled almost immediately and could re-send start,
    because the label keyed off telemetry `running` (false until the runtime's status file appears,
    ~18-60 s) while the reply only cleared `pending`. The agent now reports initializing authoritatively
    and the app mirrors it, so the user sees the real state and cannot spam commands. The local
    transition only bridges the gap between the command reply and the next telemetry frame; the device
    owns the truth thereafter. Additive wire fields keep proto 1 and the client already ignores unknown
    keys. All user-facing text is es-MX resources.
  files:
    - Lumina-BETA-ANDROID/shared/src/commonMain/kotlin/com/korealm/lumina/protocol/WireStatus.kt
    - Lumina-BETA-ANDROID/shared/src/commonMain/kotlin/com/korealm/lumina/protocol/WireReplies.kt
    - Lumina-BETA-ANDROID/shared/src/commonMain/kotlin/com/korealm/lumina/protocol/DomainModels.kt
    - Lumina-BETA-ANDROID/shared/src/commonMain/kotlin/com/korealm/lumina/protocol/StatusMapper.kt
    - Lumina-BETA-ANDROID/shared/src/commonMain/kotlin/com/korealm/lumina/protocol/Reply.kt
    - Lumina-BETA-ANDROID/shared/src/commonMain/kotlin/com/korealm/lumina/ui/dashboard/DashboardUiState.kt
    - Lumina-BETA-ANDROID/shared/src/commonMain/kotlin/com/korealm/lumina/ui/dashboard/DashboardViewModel.kt
    - Lumina-BETA-ANDROID/shared/src/commonMain/kotlin/com/korealm/lumina/ui/dashboard/DashboardMessages.kt
    - Lumina-BETA-ANDROID/shared/src/commonMain/kotlin/com/korealm/lumina/ui/dashboard/DashboardScreen.kt
    - Lumina-BETA-ANDROID/shared/src/commonMain/kotlin/com/korealm/lumina/ui/AppRoot.kt
    - Lumina-BETA-ANDROID/shared/src/commonMain/composeResources/values/strings.xml
    - Lumina-BETA-ANDROID/shared/src/commonTest/kotlin/com/korealm/lumina/protocol/StatusMapperTest.kt
    - Lumina-BETA-ANDROID/shared/src/commonTest/kotlin/com/korealm/lumina/protocol/ReplyDecodeTest.kt
    - Lumina-BETA-ANDROID/shared/src/commonTest/kotlin/com/korealm/lumina/ui/dashboard/DashboardReducerTest.kt
    - Lumina-BETA-ANDROID/shared/src/jvmTest/kotlin/com/korealm/lumina/ui/dashboard/DashboardViewModelTest.kt
    - Lumina-BETA-ANDROID/CHANGELOG.md
  approvals: [user]
  follow_up: >-
    Run `./gradlew :shared:allTests :androidApp:assembleDebug` and verify on the device: tapping Start
    shows "Iniciando Lúmina…" with the toggle disabled until ready, a second tap is ignored, and the
    button becomes "Detener Lúmina" only once the runtime is up.

# ---------------------------------------------------------------------------
# CHG-FE-0035 — Keep the people list when the runtime is stopped; enrollment coordination
# ---------------------------------------------------------------------------
- id: CHG-FE-0035
  date: 2026-09-22
  agent: opencode/deepseek-v4-flash
  type: fix
  status: applied
  invariants:
    - FE-INV-001
    - FE-INV-010
    - FE-INV-031
    - FE-INV-041
    - FE-INV-051
    - FE-INV-061
  supersedes: null
  summary: >-
    Stopped blanking the people list when the runtime is unreachable. StatusMapper no longer maps
    `people` to `[]` when `runtime.reachable` is false (the runtime agent now reads names from the
    persisted enrolled store — runtime CHG-0090), so the list survives a stopped runtime, a cold boot
    and a camera enrollment. Added PeopleSnapshot.runtimeActive (= running || initializing) and
    PeopleUiState.runtimeActive; the Personas screen shows a polite live-region note
    (people_runtime_stopped, "Lúmina detenida — mostrando personas registradas") when online but the
    runtime is stopped. The dashboard runtime toggle is disabled while `status.enroll.active` (the
    camera route stops the runtime itself). Updated StatusMapperTest (names kept when unreachable) and
    PeopleReducerTest.
  rationale: >-
    The app showed "Aún no hay personas registradas" whenever the runtime was stopped even though
    people were enrolled, because the mapper defensively emptied the list on unreachable and the agent
    had no names to send. With the runtime agent sourcing names from the store, the app can trust them
    and should say why they are shown. During a camera enrollment the runtime is stopped by design
    (libcamera is single-client), which previously made the list vanish mid-flow; it now stays, and the
    toggle is locked so the dashboard cannot fight the enrollment. runtimeActive covers initializing so
    the note does not flash during the startup window.
  files:
    - Lumina-BETA-ANDROID/shared/src/commonMain/kotlin/com/korealm/lumina/data/PeopleRepository.kt
    - Lumina-BETA-ANDROID/shared/src/commonMain/kotlin/com/korealm/lumina/data/PeopleRepositoryImpl.kt
    - Lumina-BETA-ANDROID/shared/src/commonMain/kotlin/com/korealm/lumina/protocol/StatusMapper.kt
    - Lumina-BETA-ANDROID/shared/src/commonMain/kotlin/com/korealm/lumina/ui/people/PeopleUiState.kt
    - Lumina-BETA-ANDROID/shared/src/commonMain/kotlin/com/korealm/lumina/ui/people/PeopleViewModel.kt
    - Lumina-BETA-ANDROID/shared/src/commonMain/kotlin/com/korealm/lumina/ui/people/PeopleScreen.kt
    - Lumina-BETA-ANDROID/shared/src/commonMain/composeResources/values/strings.xml
    - Lumina-BETA-ANDROID/shared/src/commonTest/kotlin/com/korealm/lumina/protocol/StatusMapperTest.kt
    - Lumina-BETA-ANDROID/shared/src/commonTest/kotlin/com/korealm/lumina/ui/people/PeopleReducerTest.kt
    - Lumina-BETA-ANDROID/CHANGELOG.md
  approvals: [user]
  follow_up: >-
    Verify on the device: with Lúmina stopped the Personas list shows the enrolled people plus the
    "detenida" note; a camera enrollment keeps the list visible and the dashboard toggle disabled.

# ---------------------------------------------------------------------------
# CHG-FE-0036 — Desktop image preparer encodes JPEG at the contract quality
# ---------------------------------------------------------------------------
- id: CHG-FE-0036
  date: 2026-09-22
  agent: opencode/deepseek-v4-flash
  type: fix
  status: applied
  invariants:
    - FE-INV-001
    - FE-INV-033
    - FE-INV-061
  supersedes: null
  summary: >-
    The desktop (JVM) ImagePreparer now encodes JPEG through an ImageWriter with
    ImageWriteParam.compressionQuality = JPEG_QUALITY / 100 (0.80) instead of ImageIO.write's implicit
    default (~0.75), matching the Android Bitmap.compress path and contract §4.10. Resize (height
    <= 1080, aspect preserved) and the no-EXIF-rotation limitation are unchanged.
  rationale: >-
    The contract asks for JPEG ~quality 80; the two platforms should not differ. ImageIO.write's
    default is 0.75, so an explicit writer is needed. Desktop remains the developer loop, so the
    JDK's lack of an EXIF reader is still documented rather than worked around (no new dependency).
  files:
    - Lumina-BETA-ANDROID/shared/src/jvmMain/kotlin/com/korealm/lumina/data/PlatformImagePreparer.jvm.kt
    - Lumina-BETA-ANDROID/CHANGELOG.md
  approvals: [user]
  follow_up: >-
    Optional: assert the encoded size/quality in JvmImagePreparerTest if a stable metric is found.

# ---------------------------------------------------------------------------
# CHG-FE-0037 — Second-pass fixes for the runtime tri-state control
# ---------------------------------------------------------------------------
- id: CHG-FE-0037
  date: 2026-09-22
  agent: opencode/deepseek-v4-flash
  type: fix
  status: applied
  invariants:
    - FE-INV-001
    - FE-INV-031
    - FE-INV-041
    - FE-INV-051
    - FE-INV-061
  supersedes: null
  summary: >-
    Aggressive second pass over CHG-FE-0034/0035/0036. (H1) A failed stop no longer sticks: the agent
    replies with the real `is-active`, so `reduceRuntimeResult` now holds `Stopping` only when the
    device confirms it is not running; a reply of `running=true` clears the transition and reports
    RuntimeStopFailed. (M1) Added RuntimeStartFailed/RuntimeStopFailed so a rejected start/stop is
    announced instead of masquerading as "Lúmina detenida"/"iniciada". (M2) An agent that predates the
    additive `initializing` field now still blocks re-taps: a `Start` reply of `running` enters
    `Starting`, and the fallback window is chosen by the reply — RUNTIME_START_GRACE_MS (10 s) when
    `initializing` is present (confirmed within ~1 s; a telemetry outage hits the 5 s Offline
    threshold first and cancels it), or RUNTIME_START_FALLBACK_MS (240 s) when only `running` is
    present. The 240 s bound is the runtime's measured worst case (~21 s model load + up to 180 s
    BlueALSA sink wait = ~201 s; docs/PERFORMANCE.md §14.3, docs/BLUETOOTH.md §2) plus margin, so it
    never false-fails a start that is genuinely still initializing. (L1) `onRuntimeToggle` also
    ignores taps while the device reports initializing (not just during a local transition).
    (L2) The desktop JPEG writer null-guards `defaultWriteParam`/`createImageOutputStream`.
    (L3) Moved RuntimeCommand/RuntimeTransition/RuntimeButtonState/runtimeButtonState into
    RuntimeControlState.kt.
  rationale: >-
    The stop reply reflects `systemctl is-active`, so treating every accepted stop as `Stopping`
    disabled the button forever when the stop actually failed. A failed start was indistinguishable
    from a stop. And the optimistic `Starting` was gated on `initializing`, so a partially-updated
    deployment (old agent) silently regressed to the double-send bug. The fallback window is derived
    from the runtime's own measured timings rather than guessed, per FE-INV-001/FE-INV-062, and is
    only a last resort because a real failure normally makes the device go offline first. No wire
    shape changed; no new dependency; all text is es-MX.
  files:
    - Lumina-BETA-ANDROID/shared/src/commonMain/kotlin/com/korealm/lumina/ui/dashboard/RuntimeControlState.kt
    - Lumina-BETA-ANDROID/shared/src/commonMain/kotlin/com/korealm/lumina/ui/dashboard/DashboardUiState.kt
    - Lumina-BETA-ANDROID/shared/src/commonMain/kotlin/com/korealm/lumina/ui/dashboard/DashboardViewModel.kt
    - Lumina-BETA-ANDROID/shared/src/commonMain/kotlin/com/korealm/lumina/ui/dashboard/DashboardMessages.kt
    - Lumina-BETA-ANDROID/shared/src/commonMain/kotlin/com/korealm/lumina/ui/AppRoot.kt
    - Lumina-BETA-ANDROID/shared/src/commonMain/composeResources/values/strings.xml
    - Lumina-BETA-ANDROID/shared/src/jvmMain/kotlin/com/korealm/lumina/data/PlatformImagePreparer.jvm.kt
    - Lumina-BETA-ANDROID/shared/src/commonTest/kotlin/com/korealm/lumina/ui/dashboard/DashboardReducerTest.kt
    - Lumina-BETA-ANDROID/shared/src/commonTest/kotlin/com/korealm/lumina/ui/dashboard/DashboardMessagesTest.kt
    - Lumina-BETA-ANDROID/shared/src/jvmTest/kotlin/com/korealm/lumina/ui/dashboard/DashboardViewModelTest.kt
    - Lumina-BETA-ANDROID/docs/API_VERIFICATION.md
    - Lumina-BETA-ANDROID/CHANGELOG.md
  approvals: [user]
  follow_up: >-
    Re-run `./gradlew :shared:allTests :androidApp:assembleDebug`. On the device confirm: a failed
    stop (e.g. with the sudoers rule removed) re-enables the button and says "No se pudo detener
    Lúmina"; a normal start still shows "Iniciando…" until ready.

# ---------------------------------------------------------------------------
# CHG-FE-0038 — Tolerate a null telemetry volume from the device
# ---------------------------------------------------------------------------
- id: CHG-FE-0038
  date: 2026-09-22
  agent: opencode/deepseek-v4-flash
  type: fix
  status: applied
  invariants:
    - FE-INV-001
    - FE-INV-031
    - FE-INV-061
  supersedes: null
  summary: >-
    WireSensors.volume gained a default of -1 so a `null` (or absent) volume is coerced by
    LuminaJson's coerceInputValues instead of rejecting the datagram. Added StatusDecodeTest cases
    for `"volume":null` and an absent volume field. No other behaviour changed; StatusMapper still maps
    the -1 sentinel (and any out-of-range value) to `null` -> the UI shows "—".
  rationale: >-
    Field diagnosis: the runtime agent serialized an unknown volume as `"volume":null` (contract §4.1
    says -1), and WireSensors.volume was a required non-null Int with no default, so
    decodeFromJsonElement threw -> decodeStatus returned Malformed -> KtorTelemetrySource ignored the
    frame -> the dashboard showed "Sin Conexión" whenever the BlueALSA mixer was unavailable (runtime
    stopped and/or earbuds disconnected). Giving the field a default makes the app resilient to the
    device's actual behaviour; the agent is also corrected to emit -1 (runtime CHG-0093). Confirmed by
    capturing the live datagram and by the app's own coerceInputValues contract comment.
  files:
    - Lumina-BETA-ANDROID/shared/src/commonMain/kotlin/com/korealm/lumina/protocol/WireStatus.kt
    - Lumina-BETA-ANDROID/shared/src/commonTest/kotlin/com/korealm/lumina/protocol/StatusDecodeTest.kt
    - Lumina-BETA-ANDROID/docs/API_VERIFICATION.md
    - Lumina-BETA-ANDROID/CHANGELOG.md
  approvals: [user]
  follow_up: >-
    Re-run `./gradlew :shared:allTests :androidApp:assembleDebug` and redeploy; with the earbuds off
    and the runtime stopped the app should connect and show volume "—".

# ---------------------------------------------------------------------------
# CHG-FE-0039 — Archive: final-state README + contest banner
# ---------------------------------------------------------------------------
- id: CHG-FE-0039
  date: 2026-09-26
  agent: opencode/deepseek-v4-flash
  type: docs
  status: applied
  invariants:
    - FE-INV-061
  supersedes: null
  summary: >-
    Rewrote README.md as the archival final-state document: an "Archived — built for the Innovatec
    2026 (InnovaTecNM) contest" banner, a Final state section (what shipped and was verified), a
    Known limitations section, and a Contest and outcome section (local stage, did not advance). The
    rest of the README (what it does, tech, layout, getting started, mock server, connecting, docs,
    contributing) is unchanged. No code changed.
  rationale: >-
    The event concluded and the project was not selected; both repositories are being archived as a
    reference. A future reader needs an unambiguous statement of what the companion client was,
    what actually worked on-device, and what was deferred.
  files:
    - Lumina-BETA-ANDROID/README.md
    - Lumina-BETA-ANDROID/CHANGELOG.md
  approvals: [user]
  follow_up: >-
    None. This is the final entry for the archived companion app; the repository is set read-only on
    GitHub.

# ---------------------------------------------------------------------------
# CHG-FE-0040 — License the project under GPLv3
# ---------------------------------------------------------------------------
- id: CHG-FE-0040
  date: 2026-09-26
  agent: opencode/deepseek-v4-flash
  type: chore
  status: applied
  invariants:
    - FE-INV-061
  supersedes: null
  summary: >-
    Added a LICENSE file (verbatim GNU GPL version 3 text, Copyright (C) 2026 Lúmina team) and
    licensed the companion app GPL-3.0-only, and added a README "License" section stating that
    Lúmina is GPLv3 (permanently) while third-party dependencies keep their own licenses. No code
    changed.
  rationale: >-
    The contest concluded and the team chose a permanent GPLv3 license for both Lúmina projects.
    The app has no copyleft dependencies, so GPLv3 is a pure licensing choice; declaring it keeps
    both repositories consistent.
  files:
    - Lumina-BETA-ANDROID/LICENSE
    - Lumina-BETA-ANDROID/README.md
    - Lumina-BETA-ANDROID/CHANGELOG.md
  approvals: [user]
  follow_up: >-
    None. The license is permanent GPLv3.
```
