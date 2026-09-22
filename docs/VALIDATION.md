# VALIDATION.md — Lúmina Companion App end-to-end transcript

> **Status: COMPLETE (2026-09-22).** Result was `PASS` on every executed step; see §7 for the summary
> and §8 for notes. The gate was run against the mock in `../Testing_server` (desktop + physical
> Android device with TalkBack). Facts only, per FE-INV-062.
>
> This is the machine-readable, user-confirmed end-to-end transcript the Phase 6 gate requires; it is
> linked from `README.md` and referenced by `CHG-FE-0028`.

---

## 0. Environment and setup

| Item | Value (fill in) |
|---|---|
| Date | 2026-09-22 |
| Tester | User (physical Android device with TalkBack enabled) |
| Desktop host OS | — |
| Android target | physical Android device |
| Dev host LAN IP (used by the device) | 192.168.100.16 |
| App commit / build | local debug build; `./gradlew check :androidApp:assembleDebug` = 337 tests green |

Commands (run from the repo root unless noted):

```bash
# 1. Full unit gate + Android debug build (expect 337 tests green)
./gradlew check :androidApp:assembleDebug

# 2. Start the mock on the LAN (binds 0.0.0.0 by default; fast frames for enrollment)
cd ../Testing_server && npm run dev -- --enroll-ms=100

# 3. Install the Android debug build on the connected device
adb install -r androidApp/build/outputs/apk/debug/androidApp-debug.apk
```

In the app's **Ajustes**: set **Host** to the dev host LAN IP from §0, ports `47600`/`47601`,
**Token** `dev-token`, then **Guardar** (Save). Relaunch if needed.

Admin helpers (mock only): `curl http://127.0.0.1:47602/state` and
`curl -X POST http://127.0.0.1:47602/scenario/<name> -d '{"enabled":true}'`.

---

## 1. Build and launch

| # | Action | Expected | Result | Notes |
|---|---|---|---|---|
| 1.1 | Run the unit gate + `:androidApp:assembleDebug` | 337 tests green; build succeeds | PASS | 337 tests green; Android build clean |
| 1.2 | Launch desktop (`:desktopApp:run`) | Window opens at Dashboard, "Conectado" | PASS | |
| 1.3 | Launch the Android app | Opens at Dashboard, "Conectado" | PASS | |
| 1.4 | Watch startup for ~5 s | **No** "Sin conexión" flash before the first frame | PASS | R1 regression (CHG-FE-0020) |

---

## 2. Connectivity and liveness (PLAN §9)

| # | Action | Expected | Result | Notes |
|---|---|---|---|---|
| 2.1 | Stop the mock (`Ctrl-C`) | Within ~5 s the banner shows "Sin conexión" (desktop + device) | PASS | |
| 2.2 | Restart the mock | Banner returns to "Conectado" without restarting the app | PASS | auto-recovery |
| 2.3 | Set a wrong token, trigger a control action, then fix it | Persistent "Token incorrecto" warning; clears after a successful action | PASS | |
| 2.4 | `curl -X POST .../scenario/camera-dark -d '{"enabled":true}'` | Day/night shows "Desconocido" | PASS | |
| 2.5 | `.../scenario/volume-unknown` → `{"enabled":true}` | Volume shows "—"; slider still enabled | PASS | |
| 2.6 | `.../scenario/earbuds-absent -d '{"enabled":true}'` | "Audio no listo" appears | PASS | |
| 2.7 | `.../scenario/runtime-down -d '{"enabled":true}'` | Runtime shows stopped; people list empties; "Iniciar Lúmina" available | PASS | |
| 2.8 | `POST /reset` | All scenarios off; device healthy again | PASS | |

> Optional: point the client at a **proto ≠ 1** device to see "Dispositivo incompatible" stay stable
> (R2 regression, CHG-FE-0020). Not provided by the mock; skip if no fixture.
>
> **Status: N/A** — the mock provides no unsupported-proto fixture, so this optional check was not
> run. The R2 behavior itself is covered by `TelemetrySourceTest.anIncompatibleFrameKeepsTheDeviceLive…`.

---

## 3. Dashboard controls

| # | Action | Expected | Result | Notes |
|---|---|---|---|---|
| 3.1 | Move the volume slider | One "Volumen actualizado" snackbar after the gesture | PASS | |
| 3.2 | Toggle mute | Switch reflects state; "Silenciado"/"Sonido activado" message | PASS | |
| 3.3 | Stop runtime, then start it | Button label flips Iniciar/Detener; matching message | PASS | |
| 3.4 | Verify status rows | Running / Audio / Faces / Uptime / FPS / Memory / Temp / Load all render | PASS | |

---

## 4. People and enrollment

| # | Action | Expected | Result | Notes |
|---|---|---|---|---|
| 4.1 | Open Personas | List shows the seeded person ("David Solís") | PASS | |
| 4.2 | Press "Actualizar" | List refreshes; "Lista actualizada" message | PASS | |
| 4.3 | Add a name, enroll from **camera** | Progress card + phase announcements; ends with success and the person appears | PASS | |
| 4.4 | Start a camera enroll, then **switch to Dashboard** | Shell enrollment status line appears and announces phase changes | PASS | CHG-FE-0021 |
| 4.5 | Return and press **Cancelar** | Enrollment ends with the cancellation message | PASS | second connection |
| 4.6 | Run `.../scenario/enroll-slow -d '{"enabled":true}'` and enroll | Progress advances slowly; cancel still works | PASS | |
| 4.7 | Start an enroll, then start a second one | "Ocupado" (busy); the active enrollment is unaffected | PASS | natural `busy` |
| 4.8 | `.../scenario/enroll-fail -d '{"enabled":true}'` and enroll | Spanish failure message; "Iniciar Lúmina" recovery card appears | PASS | |
| 4.9 | Press "Iniciar Lúmina" on the recovery card | Runtime starts; recovery card clears | PASS | |
| 4.10 | Enroll from **photos** (pick 3–5 images) | Captures are prepared and sent; success message; person appears | PASS | Android picker |
| 4.11 | Enroll a **portrait** photo (EXIF ≥ 6) | Face is not sideways; enrollment succeeds | PASS | CHG-FE-0023 |
| 4.12 | Select **no** photos in the picker | No-op (no error, no message) | PASS | F12 behavior |

---

## 5. Settings

| # | Action | Expected | Result | Notes |
|---|---|---|---|---|
| 5.1 | Edit host/ports/token and Save | "Guardado" announced; values persist across an app restart | PASS | |
| 5.2 | Toggle the token reveal | Masked ↔ visible; control is labelled | PASS | |
| 5.3 | Enter an invalid port | Inline error; save does not persist | PASS | |

---

## 6. Accessibility — manual TalkBack pass

Follow `docs/ACCESSIBILITY.md` §5 on the Android device with TalkBack enabled.

| # | Step | Expected | Result | Notes |
|---|---|---|---|---|
| 6.1 | Dashboard status rows | Each row reads "etiqueta: valor" as **one** stop | PASS | `mergeDescendants` |
| 6.2 | Volume slider | Reads its label + percentage; adjusting gives **one** confirmation | PASS | |
| 6.3 | Connection change | "Sin conexión" / "Conectado" announced without focusing | PASS | |
| 6.4 | Enrollment phase + off-tab shell line | Phase changes announced; shell line announced off Personas | PASS | |
| 6.5 | Snackbars | Outcome messages announced once | PASS | |
| 6.6 | Focus order + touch targets | Logical swipe order; token-reveal button ≥ 48 dp | PASS | PLAN §10 items verified on device |

> Note on the mixed-language announcement: TalkBack read the app content in Spanish correctly. The
> trailing English "BUTTON, DOUBLE TAP TO ACTIVATE" is TalkBack's **own** hint string and follows the
> device/TalkBack language, not the app's strings — not an app defect (see `ACCESSIBILITY.md` §6).

---

## 7. Results summary

| Area | Pass | Fail | Blocked |
|---|---|---|---|
| 1. Build/launch | 4 | 0 | 0 |
| 2. Connectivity | 8 | 0 | 0 |
| 3. Dashboard | 4 | 0 | 0 |
| 4. People/enrollment | 12 | 0 | 0 |
| 5. Settings | 3 | 0 | 0 |
| 6. Accessibility | 6 | 0 | 0 |
| **Total** | **37** | **0** | **0** |

Sign-off: User  Date: 2026-09-22  Verified on: desktop + physical Android device (TalkBack)

---

## 8. Notes, defects, and residual items

- **No defects found.** Every executed step passed (37/37). The only non-run item is the optional
  `proto ≠ 1` check (§2, no mock fixture); its behavior is covered by the unit test
  `TelemetrySourceTest.anIncompatibleFrameKeepsTheDeviceLiveUntilItGoesSilent`.
- **Fixed during this phase:** on a physical Android device every control command returned
  "No se pudo conectar con el dispositivo" because the control path ran on `Dispatchers.Main`
  (`NetworkOnMainThreadException`); fixed by moving socket work to `ioDispatcher` (`CHG-FE-0027`).
- **TalkBack language:** app content is announced in Spanish; the trailing English "double tap to
  activate" is TalkBack's own hint string (device/TalkBack language), not an app bug.
- **Retained (user decision):** `Testing_server` is kept for demos; `miuix-preference` /
  `miuix-squircle` remain declared-but-unused; the `TODO(di)` desktop ViewModel wiring is unchanged.
