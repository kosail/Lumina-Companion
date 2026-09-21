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
```
