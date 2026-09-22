# API_VERIFICATION.md — Phase 0 (pinned-API verification)

> **AUDIENCE: AI AGENTS.** This document records the API verifications required by **FE-INV-001**
> and **FE-INV-024** before any code depends on these APIs. It is evidence, not a source of truth:
> `INVARIANTS.md` outranks it, and the wire contract `../Lumina-BETA-RPI-2W/docs/API_CONTRACT.md` is
> authoritative for the protocol.
>
> **Access date for every source below: 2026-09-21.** Versions are the ones pinned in
> `gradle/libs.versions.toml`.

---

## 1. Method

Per FE-INV-001, each API was checked against **one of**:

1. the **source-code contract of the exact pinned version** (preferred), or
2. the **official documentation for that exact version**.

Each row records the source (URL + access date), the finding, and a self-assessed **confidence**.
Any item below `0.80` would be **UNRESOLVED** and would halt the phase. There are none; the lowest
is `0.85`, and the two `0.90` items carry an explicit caveat.

**How this differs from the plan.** `PLAN.md` Phase 0 listed "MiuiX 0.9.4: signatures for every
component in §6". Verifying all 30+ component signatures up front has no Phase 1 consumer (Phase 1
is pure protocol code with no UI), so this document verifies the **component inventory + import
namespace + one representative component in full** and defers per-component signature verification
to **first use** (Phase 2+). This is compliant with FE-INV-001 ("verify before using") and is
recorded as a follow-up in §4. Flagged for the user.

---

## 2. Summary

| # | API / concern | Pinned version | Source | Confidence |
|---|---|---|---|---|
| 1 | Ktor raw sockets (`ktor-network`, `ktor-io`) | 3.6.0 | GitHub tag `3.6.0` source | `1.00` |
| 2 | kotlinx-serialization-json `Json` config | 1.11.0 | kotlinlang.org API (see caveat) | `0.95` |
| 3 | Koin Compose (`koinViewModel`, `startKoin`) | 4.2.2 | insert-koin.io 4.2 docs | `1.00` |
| 4 | MiuiX KMP components | 0.9.4 | compose-miuix-ui docs | `0.85` |
| 5 | `multiplatform-settings` + `-no-arg` | 1.3.0 | GitHub README | `0.95` |
| 6 | Android Photo Picker | activity 1.13.0 | AndroidX source (see caveat) | `0.90` |
| 7 | Compose MP `liveRegion` semantics | 1.12.0 | compose-multiplatform-core source | `0.90` |
| 8 | MiuiX `miuix-nav` (navigation runtime) | 0.9.4 | Maven Central + POM | `1.00` |

---

## 3. Detailed findings

### 1. Ktor raw sockets — 3.6.0 — confidence `1.00`

**Source (pinned source-code contract):**
- Tag exists: `https://github.com/ktorio/ktor/tree/3.6.0` (commit `111c5807d0a9004c8e0f17ae01e12a71aae56a29`).
- `https://raw.githubusercontent.com/ktorio/ktor/3.6.0/ktor-network/common/src/io/ktor/network/sockets/Builders.kt`
- `.../ktor-network/common/src/io/ktor/network/sockets/UDPSocketBuilder.kt`
- `.../ktor-network/common/src/io/ktor/network/sockets/TcpSocketBuilder.kt`
- `.../ktor-network/common/src/io/ktor/network/sockets/Datagram.kt`
- `.../ktor-network/common/src/io/ktor/network/sockets/Sockets.kt`
- `.../ktor-network/common/src/io/ktor/network/selector/SelectorManagerCommon.kt`
- `.../ktor-io/common/src/io/ktor/utils/io/ByteReadChannelOperations.kt`

**Confirmed signatures (exact, from the 3.6.0 sources):**

```kotlin
// io.ktor.network.selector
expect fun SelectorManager(dispatcher: CoroutineContext = EmptyCoroutineContext): SelectorManager

// io.ktor.network.sockets
fun aSocket(selector: SelectorManager): SocketBuilder
class SocketBuilder { fun tcp(): TcpSocketBuilder; fun udp(): UDPSocketBuilder }

// TCP client
suspend fun TcpSocketBuilder.connect(hostname: String, port: Int, configure: ... = {}): Socket
// UDP
suspend fun UDPSocketBuilder.bind(hostname: String = "0.0.0.0", port: Int = 0, configure: ... = {}): BoundDatagramSocket
suspend fun UDPSocketBuilder.connect(remoteAddress: SocketAddress, ...): ConnectedDatagramSocket

// Channels
fun AReadable.openReadChannel(): ByteReadChannel
fun AWritable.openWriteChannel(autoFlush: Boolean = false): ByteWriteChannel

// Datagrams
class Datagram(val packet: kotlinx.io.Source, val address: SocketAddress)
suspend fun DatagramWriteChannel.send(datagram: Datagram)
suspend fun DatagramReadChannel.receive(): Datagram
```

**⚠ Drift findings (important for Phase 2):**
1. **`ByteReadChannel.readUTF8Line(max: Int = Int.MAX_VALUE): String?` is DEPRECATED in 3.6.0.**
   The source directs callers to `readLine` / `readLineStrict`. For the token-gated TCP channel the
   transport must use **`readLineStrict(limit = 16 * 1024 * 1024)`** (it throws `TooLongLineException`
   past the limit, matching `MAX_LINE_BYTES`) rather than the contract §8 pseudo-code's
   `readUTF8LineSequence()` — that pseudo-code is guidance for an older Ktor, not the wire contract.
2. `Datagram.packet` is a **`kotlinx.io.Source`** (not the legacy `ByteReadPacket`). Sending a
   datagram means constructing a `kotlinx.io.Buffer` and wrapping it in `Datagram(buffer, address)`.
3. `io.ktor.network.sockets.tcpNoDelay()` is deprecated ("noDelay is true by default").

**Note:** `https://api.ktor.io` only publishes reference docs up to **3.5.x**; there is **no 3.6.0
API reference**. That is why the pinned **tag source** was used, per the user-approved approach.

---

### 2. kotlinx-serialization-json — 1.11.0 — confidence `0.95`

**Source:** `https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-json/kotlinx.serialization.json/-json-builder/`

`JsonBuilder` exposes `ignoreUnknownKeys: Boolean`, `explicitNulls: Boolean`,
`coerceInputValues: Boolean`, `isLenient: Boolean`, `decodeEnumsCaseInsensitive: Boolean`. These are
long-standing options.

**Caveat:** the live API page is versioned **1.12.0-RC**, not 1.11.0. These four flags have been
present and stable since kotlinx.serialization 1.0, so confidence is `0.95`; if a 1.11.0-specific
surprise appears at first compile, re-verify.

**Decision (user-approved, CHG-FE-0006):** one shared `Json` instance with
`ignoreUnknownKeys = true`, `explicitNulls = false`, `coerceInputValues = true`. Wire "enum-like"
fields are `String` with defaults so `coerceInputValues` can substitute on `null`; the domain mapper
converts unknown values to `Unknown` (FE-INV-031).

---

### 3. Koin — 4.2.2 — confidence `1.00`

**Source:** `https://insert-koin.io/docs/reference/koin-compose/compose/` (docs version selector: 4.2)

Confirmed:
- `startKoin { androidContext(this@Application); modules(appModule) }` (external setup), and
  `KoinApplication(configuration = koinConfiguration { modules(appModule) }) { ... }` (Compose-managed).
- `koinViewModel<T>()`, `koinInject<T>()`.
- Packages in use: `koin-compose`, `koin-compose-viewmodel` (multiplatform), plus `koin-android` on Android.

**Note:** `KoinMultiplatformApplication` is deprecated in favor of `KoinApplication`. Phase 2 will
choose `startKoin` in the Android/JVM entry points.

---

### 4. MiuiX KMP — 0.9.4 — confidence `0.85`

**Sources:**
- `https://compose-miuix-ui.github.io/miuix/components/` (component inventory)
- `https://compose-miuix-ui.github.io/miuix/components/textfield` (full signature)

Confirmed the component inventory used by `PLAN.md` §6 exists, including `Scaffold` (required
wrapper for the `Overlay*`/`Window*` popups), `Surface`, `TopAppBar`, `NavigationBar`, `Card`,
`Button`, `IconButton`, `Text`, `SmallTitle`, `TextField`, `Switch`, `Slider`, `ProgressIndicator`,
`Snackbar`, `Divider`, `PullToRefresh`, `OverlayDialog`, `WindowDialog`, `ArrowPreference`,
`SwitchPreference`, `SliderPreference`, and that **`LazyColumn` was removed** (commit `f28cdf4`).

`TextField` verified in full, import **`top.yukonga.miuix.kmp.basic.TextField`**:
`value`/`onValueChange`, `label`, `useLabelAsPlaceholder`, `enabled`, `readOnly`, `textStyle`,
`keyboardOptions`, `keyboardActions`, `leadingIcon`, `trailingIcon`, `singleLine`, `maxLines`,
`minLines`, **`visualTransformation`**, `interactionSource`, `cursorBrush`; plus `TextFieldDefaults`
(`CornerRadius = 16.dp`, `InsideMargin = DpSize(16.dp, 16.dp)`, `textFieldColors(...)`). The docs
show the token-input pattern (`PasswordVisualTransformation` + reveal `trailingIcon`) directly.

**Caveat / confidence `0.85`:** the docs site is not version-selectable and tracks the library
ahead of/around 0.9.4; the exact 0.9.4 signatures of each component are therefore **verified at
first use** (see §4). The `top.yukonga.miuix.kmp` namespace is confirmed by the docs' own imports.

---

### 5. multiplatform-settings + `-no-arg` — 1.3.0 — confidence `0.95`

**Source:** `https://github.com/russhwolf/multiplatform-settings`

Confirmed:
- The `multiplatform-settings-no-arg` module exports a top-level **`Settings()`** factory for
  common code: `val settings: Settings = Settings()`.
- On **Android** it delegates to the equivalent of `PreferenceManager.getDefaultSharedPreferences()`
  and obtains a `Context` via **androidx-startup** (a `ContentProvider`), so **no manual init is
  required**.
- On **JVM** it uses the `Preferences` implementation with `Preferences.userRoot()`.
- **You cannot call `Settings()` from an Android unit test** (the startup internals do not run, not
  even under Robolectric). This does not affect us: tests use `MapSettings` from
  `multiplatform-settings-test`/`-no-arg` fakes.

**Decision (user-approved, CHG-FE-0006):** persist host/ports/token with
`multiplatform-settings` 1.3.0 + `multiplatform-settings-no-arg` 1.3.0; construct with `Settings()`
in `commonMain`; use `MapSettings` as the test fake. Fallback if Android init proves unreliable:
explicit `SharedPreferencesSettings`/`PreferencesSettings` via `expect`/`actual` (documented in
`PLAN.md` §11).

---

### 6. Android Photo Picker — activity 1.13.0 — confidence `0.90`

**Source:** `https://raw.githubusercontent.com/androidx/androidx/androidx-main/activity/activity/src/main/java/androidx/activity/result/contract/ActivityResultContracts.kt`

Confirmed from the AndroidX source:
```kotlin
class PickMultipleVisualMedia(private val maxItems: Int = getMaxItems()) :
    ActivityResultContract<PickVisualMediaRequest, List<Uri>>()
// init requires maxItems > 1; output is a (possibly empty) List<Uri>
```
Input is `PickVisualMediaRequest` (builder with `.setMediaType(...)`, `.setMaxItems(...)`), output is
`List<Uri>`. It prefers the system Photo Picker (`MediaStore.ACTION_PICK_IMAGES`) and falls back to
`ACTION_OPEN_DOCUMENT` on older devices; no storage permission is required. `maxItems` is clamped to
`MediaStore.getPickImagesMaxLimit()`.

**Caveat / confidence `0.90`:** the AndroidX repo does **not** publish an `androidx.activity-1.13.0`
git tag (checked), so this was verified against the `androidx-main` source. `PickMultipleVisualMedia`
has been stable since activity **1.7.0**; the class is Android-only and must be reached through
`expect`/`actual` (`PLAN.md` §7). Phase 4 will re-verify at first use.

**Known Phase 4 wiring item (not a blocker now):** `shared/androidMain` does not yet depend on
`androidx.activity:activity-compose` (it is in the version catalog and used by `androidApp`). Adding
it to `shared/androidMain` is a build change to log before Phase 4.

---

### 7. Compose Multiplatform `liveRegion` — 1.12.0 — confidence `0.90`

**Source:** `https://raw.githubusercontent.com/JetBrains/compose-multiplatform-core/jb-main/compose/ui/ui/src/commonMain/kotlin/androidx/compose/ui/semantics/SemanticsProperties.kt`

Confirmed in `commonMain` (so it is multiplatform, not Android-only):
```kotlin
val SemanticsProperties.LiveRegion: SemanticsPropertyKey<LiveRegionMode>   // AccessibilityKey
var SemanticsPropertyReceiver.liveRegion: LiveRegionMode by SemanticsProperties.LiveRegion
value class LiveRegionMode { val Polite; val Assertive }
```
Used as `Modifier.semantics { liveRegion = LiveRegionMode.Polite }` (FE-INV-010 announcements).

**Caveat / confidence `0.90`:** verified against `jb-main`; the tag `v1.12.0` exists
(`JetBrains/compose-multiplatform-core`, sha `5290ec35fa8f0aff7ff153765e74f9feb61001a0`). The
`liveRegion` API is long-standing and present in `commonMain`; re-verify at first use.

---

### 8. MiuiX `miuix-nav` — 0.9.4 — confidence `1.00`

**Sources:**
- `https://repo1.maven.org/maven2/top/yukonga/miuix/kmp/` (artifact list)
- `https://repo1.maven.org/maven2/top/yukonga/miuix/kmp/miuix-nav-android/0.9.4/miuix-nav-android-0.9.4.pom`

Confirmed: `top.yukonga.miuix.kmp:miuix-nav:0.9.4` exists on Maven Central (POM `name` `miuix-nav`,
description *"Self-contained navigation runtime for Miuix"*). It is **not** required for
`NavigationBar` (which lives in the `miuix-ui` "basic" package) and is currently unused. Per user
decision (CHG-FE-0007) it is retained provisionally because it may be useful, and must be removed in
the final cleanup phase if still unused.

---

## 4. Follow-ups (non-blocking)

- [ ] **`miuix-nav` cleanup** — remove `top.yukonga.miuix.kmp:miuix-nav` from the build in the final
  cleanup phase if it is still unused (user decision, CHG-FE-0007).

- [ ] **MiuiX per-component signatures** — verify each component in `PLAN.md` §6 against the docs
  immediately before first use (Phase 2+), per FE-INV-001. Record the URL + access date then.
- [ ] **kotlinx-serialization 1.11.0** — if any `Json` builder option differs from the 1.12.0-RC
  docs, re-verify and log.
- [ ] **Android Photo Picker** — re-verify against activity 1.13.0 at first use (Phase 4) and add
  `androidx.activity:activity-compose` to `shared/androidMain` then.
- [ ] **CMP `liveRegion`** — re-verify against the `v1.12.0` tag at first use (Phase 2).

## 5. Outcome

All Phase 0 items are verified at or above the `0.80` threshold; **none is UNRESOLVED**. The two
`0.90` items and the `0.85` MiuiX item are verified at first use and tracked in §4. Phase 0's
governance amendment is recorded in `CHANGELOG.md` as **CHG-FE-0006**.

---

## 6. Phase 2 verifications (transport + DI + dashboard)

> **Access date for every source below: 2026-09-21.** These close the §4 follow-ups that Phase 2
> touches. Recorded per FE-INV-001/024 before writing any Phase 2 code.

### 6.1 Ktor 3.6.0 — TCP line framing + UDP datagrams — confidence `1.00`

**Sources (pinned tag `3.6.0`):**
- `ktor-io/common/src/io/ktor/utils/io/ByteReadChannelOperations.kt`
- `ktor-io/common/src/io/ktor/utils/io/ByteWriteChannelOperations.kt`
- `ktor-network/common/src/io/ktor/network/sockets/Datagram.kt` (already cited in §3.1)

**Confirmed:**
```kotlin
// io.ktor.utils.io
suspend fun ByteReadChannel.readLineStrict(limit: Long = Long.MAX_VALUE,
                                           lineEnding: LineEnding = LineEnding.Default): String?
// returns null on clean EOF; throws TooLongLineException past `limit` and EOFException if the
// channel closes after content but before a line delimiter. LineEnding.Default = LF / CRLF.
suspend fun ByteWriteChannel.writeStringUtf8(value: String)
suspend fun ByteWriteChannel.writeFully(value: ByteArray, startIndex: Int = 0, endIndex: Int = value.size)
```
Decision: the control connection writes one line with `writeStringUtf8(line + "\n")` and reads with
`readLineStrict(limit = MAX_LINE_BYTES)`; `TooLongLineException`/`EOFException` map to
`ControlResult.Io` (never thrown across a boundary).

### 6.2 kotlinx-io (Ktor 3.6.0's I/O layer) — building/reading a `Datagram` — confidence `0.95`

**Source:** `https://kotlinlang.org/api/kotlinx-io/kotlinx-io-core/kotlinx.io/read-byte-array.html`
(docs render 0.8.1).

**Confirmed:** `Buffer()`; `Buffer.write(ByteArray)`; `Source.readByteArray(): ByteArray`. `Buffer`
implements both `Source` and `Sink`, so a datagram is built as
`Datagram(Buffer().apply { write(payload) }, remoteAddress)` and read as `datagram.packet.readByteArray()`.

**Caveat / `0.95`:** verified against the current kotlinx-io docs (0.8.1), not the exact version
resolved by Ktor 3.6.0; these are long-stable core APIs. Re-check if the build reports a signature
mismatch.

### 6.3 Koin 4.2.2 — ViewModel DSL + `koinViewModel` in common code — confidence `1.00`

**Sources (pinned tag `4.2.2`):**
- `projects/core/koin-core-viewmodel/src/commonMain/kotlin/org/koin/core/module/dsl/ModuleExt.kt`
- `projects/compose/koin-compose-viewmodel/src/commonMain/kotlin/org/koin/compose/viewmodel/ViewModel.kt`
- `projects/compose/koin-compose-viewmodel/build.gradle.kts`

**Confirmed:**
```kotlin
// in a Koin module (common code):
import org.koin.core.module.dsl.viewModel
viewModel { DashboardViewModel(get()) }         // org.koin.core.module.dsl (koin-core-viewmodel)

// at the call site:
import org.koin.compose.viewmodel.koinViewModel
val vm: DashboardViewModel = koinViewModel()
```
`koin-compose-viewmodel` declares `api(project(":core:koin-core-viewmodel"))` and
`api(libs.jb.composeViewmodel)`, so the DSL and `LocalViewModelStoreOwner` are on the compile
classpath transitively — **no new dependency**. Note: `org.koin.compose.viewmodel.dsl.viewModel` is
deprecated (its KDoc points to `org.koin.core.module.dsl.*`), so the core DSL is used.

### 6.4 MiuiX 0.9.4 — shell components + text styles + icons — confidence `0.95`

**Source:** repo `compose-miuix-ui/miuix`, tag `v0.9.4` (sha `39c40f99844227b853f0049a0933b1f3ae6c00ba`),
module dir `miuix-ui`, package `top.yukonga.miuix.kmp.basic` / `…theme` / `…icon`.

**Confirmed (exact signatures):**
- `Scaffold(modifier, topBar, bottomBar, floatingActionButton, floatingActionButtonPosition, floatingToolbar, floatingToolbarPosition, snackbarHost, popupHost, containerColor, contentWindowInsets, content: @Composable (PaddingValues) -> Unit)`.
- `NavigationBar(modifier, color, showDivider, defaultWindowInsetsPadding, mode, content: @Composable RowScope.() -> Unit)`.
- `RowScope.NavigationBarItem(selected, onClick, icon: ImageVector, label: String, modifier, enabled, colors, badge)`; `NavigationBarDefaults.ItemHeight = 64.dp`, `IconSize = 26.dp`, `LabelFontSize = 12.sp`.
- `Card(modifier, cornerRadius, insideMargin, colors, content: @Composable ColumnScope.() -> Unit)`; `CardDefaults.CornerRadius = 16.dp`.
- `SmallTitle(text, modifier, textColor, insideMargin)`.
- `MiuixTheme(colors: Colors = MiuixTheme.colorScheme, textStyles: TextStyles = MiuixTheme.textStyles, content)`; `MiuixTheme.textStyles` members and default sizes: `title1` 32sp, `title2` 24sp, `title3` 20sp, `title4` 18sp, `main` 17sp, `body1` 16sp, `body2` 14sp, `subtitle` 14sp bold.
- Icons: `import top.yukonga.miuix.kmp.icon.MiuixIcons` + `top.yukonga.miuix.kmp.icon.extended.<Name>`; use e.g. `MiuixIcons.Regular.<Name>` (each icon exposes Light/Normal/Regular/Medium/Demibold variants).

**FE-INV-026 note:** `NavigationBarItem` fixes icon/label sizes (26 dp / 12 sp) and exposes no
per-item sizing parameter. FE-INV-026 requires big text/icons, so the tab bar uses `NavigationBar`
with visible labels plus large page headers/values; the fixed nav sizing is a documented MiuiX
constraint (candidate escape hatch: `FloatingNavigationBar`, 28 dp icons, or a MiuiX-primitive tab
bar under FE-INV-025 if the constraint proves unacceptable on device).

### 6.5 Compose Multiplatform 1.12.0 — string resources — confidence `0.95`

The project already configures `compose.resources { publicResClass = true }` and generates
`lumina.shared.generated.resources.Res` (used by `desktopApp/main.kt` for `Res.drawable.logo`).
Phase 2 adds `shared/src/commonMain/composeResources/values/strings.xml` and reads strings with
`stringResource(Res.string.<name>)` (import `org.jetbrains.compose.resources.stringResource`). This
is the standard CMP 1.12.0 API; re-verify at first compile if the generated accessor name differs.

### 6.6 kotlinx-coroutines-test 1.11.0 — test-only dependency — confidence `1.00`

**Source:** `https://repo1.maven.org/maven2/org/jetbrains/kotlinx/kotlinx-coroutines-test/1.11.0/`
(artifact + `.module` present).

User-approved addition to `commonTest` (FE-INV-020). Provides `runTest`, `TestScope`,
`StandardTestDispatcher`, `advanceTimeBy`/virtual time — used to test the transport's
subscribe/liveness ticker and `Flow<TelemetryEvent>` deterministically.

---

## 7. Phase 3 verifications (control + settings)

> **Access date for every source below: 2026-09-21.** These close the §4 "MiuiX per-component
> signatures" follow-up for the components Phase 3 uses, verified against the **pinned tag**.

### 7.1 MiuiX 0.9.4 — `Slider` (volume control) — confidence `1.00`

**Source (pinned tag `v0.9.4`):**
- `https://raw.githubusercontent.com/compose-miuix-ui/miuix/v0.9.4/miuix-ui/src/commonMain/kotlin/top/yukonga/miuix/kmp/basic/Slider.kt`

**Confirmed (exact signature):**
```kotlin
@Composable fun Slider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    @IntRange(from = 0) steps: Int = 0,
    onValueChangeFinished: (() -> Unit)? = null,
    reverseDirection: Boolean = false,
    height: Dp = SliderDefaults.MinHeight,
    colors: SliderColors = SliderDefaults.sliderColors(),
    hapticEffect: SliderDefaults.SliderHapticEffect = SliderDefaults.DefaultHapticEffect,
    showKeyPoints: Boolean = false,
    keyPoints: List<Float>? = null,
    magnetThreshold: Float = 0.02f,
)
```
It carries `progressBarRangeInfo` + `setProgress` semantics (TalkBack can adjust it) and
`onValueChangeFinished` fires on drag end — used to send exactly one `volume.set` per gesture.

### 7.2 MiuiX 0.9.4 — `Snackbar`/`SnackbarHost` (action feedback) — confidence `1.00`

**Source (pinned tag `v0.9.4`):**
- `https://raw.githubusercontent.com/compose-miuix-ui/miuix/v0.9.4/miuix-ui/src/commonMain/kotlin/top/yukonga/miuix/kmp/basic/Snackbar.kt`

**Confirmed:**
```kotlin
class SnackbarHostState { suspend fun showSnackbar(
    message: String, actionLabel: String? = null,
    withDismissAction: Boolean = false, duration: SnackbarDuration = SnackbarDuration.Short,
): SnackbarResult }
@Composable fun SnackbarHost(state: SnackbarHostState, modifier: Modifier = Modifier,
    canSwipeToDismiss: Boolean = true, content: @Composable (SnackbarData) -> Unit = { Snackbar(it) })
```
`SnackbarDuration` = `Short` (~4 s) / `Long` (~10 s) / `Indefinite` / `Custom`, scaled by the system
accessibility timeout (`calculateRecommendedTimeoutMillis`). **`Snackbar` sets
`liveRegion = LiveRegionMode.Polite` itself**, so action feedback is announced without extra
semantics (FE-INV-010).

### 7.3 MiuiX 0.9.4 — `Switch` / `Button` / preference components — confidence `0.95`

**Sources:** docs `https://compose-miuix-ui.github.io/miuix/components/switch|button|sliderpreference`
(tracks `main`); tag file listing confirms the preference classes exist at `v0.9.4`:
- `miuix-preference/src/commonMain/kotlin/top/yukonga/miuix/kmp/preference/{ArrowPreference,SwitchPreference,SliderPreference,CheckboxPreference,RadioButtonPreference}.kt`

**Confirmed:** `Switch(checked: Boolean, onCheckedChange: ((Boolean) -> Unit)?, modifier, colors,
enabled)`; `Button(onClick, modifier, enabled, cornerRadius, minWidth, minHeight, colors,
insideMargin, interactionSource, indication, content: @Composable RowScope.() -> Unit)` with
`ButtonDefaults.buttonColorsPrimary()`. Preference package is `top.yukonga.miuix.kmp.preference`.

**Caveat / `0.95`:** the docs site tracks `main`; the exact 0.9.4 `Switch`/`Button`/preference
signatures are re-checked at first compile. Phase 3 uses only `Switch` + `Button` (settings uses
`TextField`, already verified in §3.4), so no preference component is on the critical path.

**✅ Confirmed by first compile (2026-09-22).** Phase 3's gate compiled `:shared:allTests`
(232 tests green) and `:androidApp:assembleDebug` with **no signature errors and no warnings**, so
the at-first-use items are now closed: `Slider` (§7.1), `Snackbar`/`SnackbarHost` (§7.2),
`Switch`, `Button`/`ButtonDefaults.buttonColorsPrimary()`, `TextButton`, `TextField`'s
String overload with `visualTransformation`/`keyboardOptions`/`trailingIcon`, and the whole
`MiuixTheme.colorScheme` member set used by the dashboard/settings screens (verified in the pinned
`Colors.kt` — `error`/`errorContainer`/`onErrorContainer`/`primary`/`onBackground`/…). The
`miuix-preference` module was not exercised yet (no preference component used), so its signatures
remain at first-use for Phase 4.

### 7.4 No new dependencies

Phase 3 adds no dependency: `miuix-ui`, `miuix-icons`, `miuix-preference`, `multiplatform-settings`,
Koin and Ktor are already in the build. `SettingsViewModel` reuses the `single`-not-`viewModel{}`
pattern from Phase 2 (CHG-FE-0011) for the same desktop `ViewModelStoreOwner` reason.

---

## 8. Phase 4 verifications (people + enrollment)

`URL + access date = 2026-09-22` for every item below (FE-INV-001).

### 8.1 Kotlin stdlib `Base64` — Kotlin 2.4.20 — confidence `0.95`

**Source:** `https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.io.encoding/-base64/`

Confirmed **stable** in the 2.4 stdlib reference (`Since Kotlin 2.2`), so **no `@OptIn`** is needed
(unlike the experimental `@ExperimentalEncodingApi` of Kotlin 1.8–2.1). API used:
`Base64.Default.encode(ByteArray): String` (RFC 4648 standard alphabet with `=` padding, exactly what
`enroll.images` expects — raw base64, no `data:` prefix). Used in `KtorControlClient`.

### 8.2 MiuiX `ProgressIndicator` — 0.9.4 — confidence `0.90`

**Source:** `https://compose-miuix-ui.github.io/miuix/components/progressindicator` (tracks `main`)

Confirmed: `LinearProgressIndicator(modifier, progress: Float?, colors, height)` from
`top.yukonga.miuix.kmp.basic`; `progress == null` is indeterminate. Used for enrollment progress (the
numeric `captured`/`total` is rendered separately so a screen reader is not spammed). Re-verified at
first compile per §7.3's rule.

### 8.3 MiuiX `TextField(enabled=)` — 0.9.4 — confidence `0.90`

**Source:** `https://compose-miuix-ui.github.io/miuix/components/textfield`

Confirmed the `value`/`onValueChange` overload has `enabled: Boolean = true` and
`readOnly: Boolean = false`, plus `TextFieldDefaults.textFieldColors(labelColor, borderColor, …)`.
The people screen's name field uses `enabled` (disabled while offline/enrolling). Re-verified at first
compile.

### 8.4 Android Photo Picker — activity 1.13.0 — confidence `0.90` (re-verify at first use)

**Source:** `https://raw.githubusercontent.com/androidx/androidx/androidx-main/activity/activity/src/main/java/androidx/activity/result/contract/ActivityResultContracts.kt`
and `.../result/PickVisualMediaRequest.kt`

Confirmed from AndroidX source:
- `PickMultipleVisualMedia(private val maxItems: Int = getMaxItems())` with `init { require(maxItems > 1) }`,
  input `PickVisualMediaRequest`, output `List<Uri>` (possibly empty); prefers the system Photo Picker
  (`MediaStore.ACTION_PICK_IMAGES`) and falls back to the system-fallback picker / `ACTION_OPEN_DOCUMENT`.
  No storage permission.
- The top-level `PickVisualMediaRequest(mediaType, …)` helpers are `@Deprecated(level = HIDDEN)`; the
  **current** API is `PickVisualMediaRequest.Builder().setMediaType(...).setMaxItems(...).build()`.
  `PickVisualMedia.ImageOnly` is the media type.

**Caveat / confidence `0.90`:** the AndroidX repo publishes no `androidx.activity-1.13.0` git tag, so
this is verified against `androidx-main`; the classes have been stable since activity 1.7.0. Re-verify
at first use (Phase 4). `androidx.activity:activity-compose` is now on `:shared/androidMain` (it was
already catalogued and used by `:androidApp`); no new dependency (FE-INV-020).

### 8.5 Platform image re-encoding (FE-INV-033)

**Sources:** `https://developer.android.com/reference/android/graphics/BitmapFactory.Options`
(`inJustDecodeBounds`, `inSampleSize`), `https://developer.android.com/reference/android/graphics/Bitmap`
(`createScaledBitmap`, `compress(CompressFormat.JPEG, quality, stream)`), and
`https://docs.oracle.com/en/java/javase/11/docs/api/java.desktop/javax/imageio/ImageIO.html`
(`read(InputStream)`, `write(RenderedImage, "jpg", OutputStream)`).

Android: bounds-only decode → power-of-two `inSampleSize` → `createScaledBitmap` to ≤1080 px height →
`compress(JPEG, 80)`. JVM: `ImageIO.read` → bilinear scale → draw onto an `TYPE_INT_RGB` surface with a
white background (JPEG has no alpha) → `write(…, "jpg", …)`. Both return `null` on an undecodable input
rather than throwing.

### 8.6 No new dependencies

Phase 4 adds no dependency: `androidx.activity:activity-compose` was already in the version catalog and
used by `:androidApp`; it is now also declared on `:shared/androidMain`. Base64 is Kotlin stdlib; the
platform image code uses the JDK/Android SDK. The desktop `JFileChooser` is Swing (JDK).
