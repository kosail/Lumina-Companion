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
```
