# ACCESSIBILITY.md — Lúmina Companion App

> Audience: AI agents and maintainers. This is the **audit record** for the accessibility phase
> (PLAN Phase 5). It does not replace `INVARIANTS.md`; FE-INV-010 (accessibility is a hard
> requirement) and FE-INV-026 (modern, simple, spacious, highly accessible UI) are rank 1 and 2.
>
> Scope: Android + Desktop (JVM). The primary users are **blind and low-vision** people, so this
> document is the evidence for the `PLAN.md` §10 checklist and the manual TalkBack gate.

---

## 1. Requirements checklist (FE-INV-010 / FE-INV-026)

| # | Requirement | Status | Evidence |
|---|-------------|--------|----------|
| 1 | Every interactive element has a meaningful Spanish label | ✅ verified | All controls are MiuiX `Button`/`TextButton`/`TextField`/`Switch`/`Slider` with a **visible Spanish text label** or a MiuiX `label`; there are **no icon-only controls**. `NavigationBarItem`s carry `label`. Verified with TalkBack on a physical device (2026-09-22, `CHG-FE-0028`). |
| 2 | Touch targets ≥ 48 dp | ✅ verified | MiuiX `Button`/`Switch`/`Slider` minima; `NavigationBarItem` height 64 dp; `TextField` ≥ 48 dp; the token-reveal `TextButton` was confirmed ≥ 48 dp on device (2026-09-22). |
| 3 | Never color alone | ✅ code | Every color-coded card also writes its state as text: connection banner label, `audio_not_ready`, field error text, no-face notice, recovery card (§3). |
| 4 | High contrast via theme colors | ✅ code | All colors come from `MiuixTheme.colorScheme`; no ad-hoc low-contrast values. **Dark mode follows the system** (`isSystemInDarkTheme` → MiuiX `darkColorScheme()`; `CHG-FE-0031`) with a matching dark logo; verify on device. |
| 5 | Logical focus order; decorative nodes excluded | ✅ verified | Screens are single `Column`s in reading order; dividers/labels are non-focusable. Swipe order verified with TalkBack on a physical device (2026-09-22, `CHG-FE-0028`). |
| 6 | State changes announced (`liveRegion`) | ✅ code | See the announcements inventory (§4). |

---

## 2. Screen semantics inventory

Every section header, status row and control also carries a **decorative** MiuiX icon
(`contentDescription = null`) so partially sighted users can navigate by shape; TalkBack ignores the
icons and reads the text labels/titles (FE-INV-026/010, `CHG-FE-0032`).

**Dashboard** (`ui/dashboard/DashboardScreen.kt`)
- Connection banner → its label `Text` carries `liveRegion = Polite` (one per state).
- `TokenWarning` (shared component) → `liveRegion = Polite`.
- Runtime `Button` → text "Iniciar/Detener Lúmina".
- `audio_not_ready` → error color + text when the sink is not ready.
- Volume `Slider` → adjacent "Volumen" label + numeric value; `enabled` only when online.
- Mute `Switch` → adjacent "Silenciado" label.
- Status rows → shared `InfoRow`, merged into one node per row (§3).

**Personas** (`ui/people/PeopleScreen.kt`)
- Connection banner and `TokenWarning` → `liveRegion = Polite`.
- People list → `InfoRow` per name (merged).
- Refresh / camera / photo `Button`s → visible text.
- Name `TextField` → MiuiX floating `label` + inline error text.
- Enrollment card → phase `Text` `liveRegion`, `LinearProgressIndicator`, `captured/total` text (drawn, not announced per frame), no-face notice keyed to announce once per occurrence, Cancel `Button`.
- Runtime recovery card → `liveRegion` + "Iniciar Lúmina" `Button`.

**Shell** (`ui/AppRoot.kt`)
- `NavigationBar` tabs → visible labels.
- `SnackbarHost` → MiuiX `Snackbar` announces itself (`liveRegion`).
- `EnrollmentStatusLine` → compact status shown **while enrolling and the Personas tab is not
  composed**; its phase `Text` is a `liveRegion` (the per-frame counter is not), so phase changes are
  announced on every tab (CHG-FE-0021).

**Ajustes** (`ui/settings/SettingsScreen.kt`)
- Host/UDP/TCP/Token `TextField`s → floating `label` + inline error text.
- Token reveal `TextButton` → visible "Mostrar/Ocultar" text (not an icon).
- Save `Button` → visible text; the "saved" confirmation is a `liveRegion`.

---

## 3. Color pairing

Every use of color is paired with text (FE-INV-010.3):

| Region | Color | Paired text |
|---|---|---|
| Connection banner | secondary / tertiary / error container | "Conectando" / "Conectado" / "Sin conexión" / "Dispositivo incompatible" |
| Token warning | error container | title + body |
| Audio not ready | `error` | "Audio no listo" |
| Field error | `error` | inline error message |
| No-face notice | `error` | "No se detecta un rostro…" |
| Runtime recovery | error container | explanation + "Iniciar Lúmina" |

`InfoRow` uses `Modifier.semantics(mergeDescendants = true)` so a screen reader reads
"Etiqueta: valor" as **one** focus stop instead of two (CHG-FE-0021).

---

## 4. Announcements inventory

| Event | Mechanism | Tab-independent |
|---|---|---|
| Connection lost/restored | each screen's connection banner `liveRegion` | yes (every screen shows one) |
| `unauthorized` | `TokenWarning` `liveRegion` | yes |
| Enrollment phase change | Personas card `liveRegion` + shell `EnrollmentStatusLine` | yes |
| No-face notice | keyed `liveRegion` (announces once per occurrence) | while on Personas |
| Enrollment finished / cancelled / failed | shell `Snackbar` (self-announcing) + card | yes |
| Volume/mute/runtime outcome | shell `Snackbar` | yes |
| Settings saved | `liveRegion` confirmation | yes |

Per-frame counters are deliberately **not** announced (no screen-reader spam). A `liveRegion` is
placed on the **node whose content changes** (the phase/label `Text`), not on the surrounding card:
Compose emits an accessibility event for the changed node, so a card-level `liveRegion` is either
ignored or, if the card merges descendants, re-announced on every progress frame (CHG-FE-0024).
`TokenWarning`/`RuntimeRecoveryCard` are the exception — they appear/disappear as a whole, so a
node-insertion announcement on the container is correct.

---

## 5. Manual TalkBack test script (user gate)

Run on an Android device/emulator with the mock server (`../Testing_server`, `npm run dev`).

1. Enable TalkBack (Ajustes del sistema → Accesibilidad), then relaunch Lúmina.
2. **Dashboard**: swipe through. Each status row must read as "etiqueta: valor" in one stop; the
   connection banner must be announced when it changes; the volume slider must read its label and
   percentage, and adjusting it via TalkBack must produce **one** confirmation snackbar.
3. **Runtime**: activate "Iniciar/Detener Lúmina" and confirm the snackbar is announced.
4. **Token**: set a wrong token in Ajustes; trigger a control action; confirm the token warning is
   announced.
5. **Enrollment**: on Personas, start a camera enrollment; confirm each phase change is announced,
   then switch to the Dashboard **while it runs** — the shell status line must be announced; return
   and cancel; confirm the cancellation is announced.
6. **Photos**: run a photo enrollment from the picker (also the manual Android picker check).
7. **Ajustes**: edit and save; confirm "Guardado" is announced; toggle the token reveal.
8. **Offline**: stop the mock server; after ~5 s confirm "Sin conexión" is announced; restart and
   confirm "Conectado".

Record the result (pass/fail per step) in `docs/VALIDATION.md` §6. Result: all steps **PASS** on a
physical Android device with TalkBack (2026-09-22, `CHG-FE-0028`).

---

## 6. Known gaps / follow-ups

Verified on a physical Android device with TalkBack (2026-09-22, `CHG-FE-0028`): the token-reveal
`TextButton` meets the 48 dp target; TalkBack reads the Slider/Switch labels together with their
controls, so no extra `contentDescription` was needed; the Android Photo Picker runtime path works.

Remaining (non-blocking):

- **Dark mode** now follows the system (`CHG-FE-0031`); a user-selectable Light/Dark/System override is
  a possible future addition.
- **TalkBack hint language**: the app content is announced in Spanish, but TalkBack appends its own
  hint (e.g. "double tap to activate") in the device/TalkBack language. That string is owned by
  TalkBack, not the app; aligning it means changing the device's TalkBack/system language, or
  optionally declaring an app locale so TalkBack prefers Spanish hints.

---

## 7. MIUI fallback register (FE-INV-025)

A default Compose/Material component is allowed **only** where MiuiX has no equivalent, and must be
justified and recorded. Current fallbacks:

| Fallback | Where | Why MiuiX cannot be used |
|---|---|---|
| Android system Photo Picker | `ui/picker/PhotoPicker.android.kt` | It is a platform activity-result contract, not a component. |
| Swing `JFileChooser` | `ui/picker/PhotoPicker.jvm.kt` | Desktop-only developer loop; no MiuiX file dialog. |
| `liveRegion` semantics (`Modifier.semantics`) | banner/token/progress components | Compose accessibility primitive; MiuiX has no wrapper. |
| Compose foundation layout (`Column`/`Row`/`verticalScroll`) | all screens | Layout primitives, not design components. |

**No Material3 component is used anywhere.** The `compose.material3` dependency remains catalogued but
is not imported by app code.

**Unused MiuiX modules.** `miuix-nav` was declared but never imported; it is **removed** in Phase 5
(CHG-FE-0022, per the obligation recorded in CHG-FE-0007). `miuix-preference` and `miuix-squircle` are
also not imported yet and are retained (user decision). `miuix-blur` is retained because
`AndroidManifest.xml` carries `tools:overrideLibrary="top.yukonga.miuix.kmp.blur"`.
