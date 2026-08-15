# Repostinho

A private app for the residents of a student republic ("república") at Unicamp — house
finances, chores, calendar, and meeting notes in one place, built to feel like a small
product rather than a spreadsheet with a UI bolted on.

This README focuses on the engineering: what the system does, why it's shaped the way
it is, and the constraints that drove each decision. It's written as a portfolio piece
as much as documentation.

<p align="center">
  <img src="docs/screenshots/home.png" width="260" alt="Home screen, showing the current resident's balance, weekly chore, and recent meeting notes" />
  &nbsp;&nbsp;
  <img src="docs/screenshots/calendario.png" width="260" alt="Calendar screen, showing the house's fixed yearly events in the app's light theme" />
</p>

<p align="center"><sub>Home (dark theme) and Calendar (light theme). Banco and Tarefas
also list every other resident's balance and chore assignment by name, so those two
screens are described in this README rather than pictured.</sub></p>

## The problem

Brazilian student republics run their shared finances out of an Excel workbook: rent,
utilities, groceries, who paid what, who owes whom. It works, but nobody wants to open
a spreadsheet on their phone to check if they're in the red this month. The residents
wanted a native-feeling app that shows the same numbers the treasurer already trusts —
not a second, competing source of truth.

That last constraint shaped the whole architecture: **the app never recalculates a
balance**. It displays the "Saldo Final" column the spreadsheet already computed. Doing
the arithmetic client-side would open the door to the app and the spreadsheet
disagreeing by a few cents, with no way to know which one is right — worse than the
spreadsheet alone.

## System overview

Two projects live in one repository on purpose: the JSON contract's field names are
literally the app's Kotlin `data class` names, so a change to one is a change to both,
and they ship as a single commit.

```
Repostinho/
├── REPOSTINHO/     Kotlin Multiplatform app (Compose, Android + iOS + Web/Wasm)
└── banco-api/      Cloudflare Worker that turns the spreadsheet into JSON
```

```
Banco atual.xlsm (OneDrive)  →  Cloudflare Worker (SheetJS)  →  JSON  →  Ktor client  →  on-disk cache  →  UI
```

The Worker exists because of a hard platform limit: `.xlsm` is a ZIP of XML files, and
Kotlin/Native (the iOS target) has no `inflate` or xlsx library. Rather than pull in a
heavy parsing stack on-device, or write one, a Cloudflare Worker downloads the sheet,
parses it with SheetJS, and returns plain JSON that both platforms can deserialize with
zero extra dependencies.

## Backend — `banco-api`

A Cloudflare Worker with two read-only, token-gated endpoints:

- `GET /banco` — account balances, the transaction ledger, and the shared "caixinha" fund
- `GET /atas` — the three most recent meeting-minutes documents from a Google Drive folder

**Parsing by cell coordinates, not headers.** The workbook's sheet names and cell ranges
are constants at the top of `src/index.js`. A renamed sheet makes the Worker fail loudly
(502) instead of silently returning an empty balance list — a deliberate trade-off: a
visible error is safer than a resident seeing a wrong "you owe nothing."

**Money as integer cents.** Every value is parsed as `Math.round(value * 100)` and
carried as an integer through the JSON contract and the Kotlin models. Floating-point
currency math is a classic source of off-by-a-cent bugs; cents avoids the whole class of
issue.

**Cache versioning as a deploy safety net.** `wrangler deploy` does *not* invalidate
Cloudflare's edge cache. Early on this caused three separate incidents where a shipped
fix appeared to do nothing because stale JSON kept serving. The fix is a `CACHE_VERSION`
constant folded into the cache key — bumping it on any response-shape change forces a
clean cache, deterministically, instead of relying on TTL expiry or manual purges.

**Ordering meeting notes by content, not metadata.** The three most recent meeting notes
are ordered by the date *inside the filename* (matching `18/08/2026`, `2026-08-18`, `18
de agosto de 2026`, accented or not — day-before-month, as in Brazilian usage), not by
Drive's file-creation timestamp. Uploading an old, backlogged document later shouldn't
bump it to the top of the list. Files without a recognizable date fall back to creation
time, which mixes two sort criteria in one list — a known, tested edge case
(`test/atas.test.mjs`).

**A silent-failure trap worth naming:** a Google API key can only see what's shared as
"anyone with the link." A restricted folder returns `200` with an empty file list, not
`403` — "no permission" and "empty folder" look identical over the wire. The Worker
includes an explicit check to tell those two apart rather than showing residents an
empty meeting-notes card and no explanation.

**Threat model.** The app no longer carries a shared secret. It sends the signed Firebase
token of whoever is logged in, and the Worker verifies it against Google's public keys —
so extracting the binary yields nothing usable. The old `x-rep-token` survives as an
administration key for command-line resident registration, held by one person rather than
distributed across fifteen phones.

Stack: JavaScript, [SheetJS](https://sheetjs.com/) for `.xlsx`/`.xlsm` parsing, deployed
on Cloudflare Workers. Tests in `test/` cover the date-parsing and cell-extraction logic
with plain Node — no framework.

## Client — Kotlin Multiplatform app

One Compose UI, one set of view models and repositories, targeting Android, iOS and the
browser from `commonMain`. Platform-specific code is kept to the minimum the platforms
actually force: HTTP engine selection (OkHttp, Darwin, JS), storage, and the Firebase SDK.

```
composeApp/src/
├── commonMain/    UI (Compose), view models, repositories, models, DI (Koin)
├── mobileMain/    Firebase via the GitLive SDK — shared by Android and iOS only
├── androidMain/   Ktor OkHttp engine, Context-scoped cache directory, Application entry
├── iosMain/       Ktor Darwin engine, iOS file-system cache path, MainViewController
└── wasmJsMain/    Ktor JS engine, localStorage, Firebase JS SDK bridge, PWA entry point
```

**`expect`/`actual` kept to three seams, each forced by a real platform gap.** Cache
storage (`TextFileStore` — a file on mobile, `localStorage` in the browser), the platform
name, and the Firebase provider. Everything else — parsing, state, navigation, every
screen — is common code.

`TextFileStore` is an `expect fun` factory returning a plain interface rather than an
`expect class`: expect/actual classes are still Beta in Kotlin and warn on every build,
so the platform boundary is drawn one level down instead.

**An intermediate source set exists for exactly one dependency.** The GitLive Firebase
SDK publishes no WebAssembly artifact, so while it sat in `commonMain` no browser target
could compile at all. `mobileMain` holds it for Android and iOS; the browser implements
the same three-function seam (`authenticate`, `signOut`, `idToken`) over the Firebase
JavaScript SDK.

That intermediate level is declared by extending `applyDefaultHierarchyTemplate`, not
with a manual `dependsOn`. The manual form silently disables the default hierarchy for
the targets it touches — `iosMain` drops out of the native compilation and every iOS
`actual` goes missing, with an error that names the `expect` and never mentions the
source set.

**Stale-then-fresh loading, not spinner-then-content.** `RemoteBankSheetRepository`
publishes the last known-good response from disk immediately on `init`, tagged
`SyncState.Cached`, then fires a network request that replaces it with
`SyncState.Live` — or, on failure, falls back to the same cached data with the failure
reason attached. The screen always has *something* to show and is explicit about
whether what's on screen is current or stale; only a first-ever offline launch shows an
empty state.

```kotlin
sealed interface SyncState {
    data object Loading : SyncState
    data class Live(val generatedAtLabel: String) : SyncState
    data class Cached(val generatedAtLabel: String) : SyncState
    data class Failed(val reason: String) : SyncState
}
```

**Pull-to-refresh needed its own cache-busting param.** The Worker caches `/banco` for 5
minutes at the edge. A resident pulling to refresh inside that window used to get back
the exact same cached payload — the gesture existed specifically to skip waiting, so
silently doing nothing was a real bug. `fresh: Boolean` threads from the UI gesture down
through the repository and `BankApi` to a `?fresh=1` query param the Worker uses to
bypass its own cache.

**Sign-in is Firebase Auth with email and password**, not a magic link. The link flow has
to return to the app, which on iOS means universal links, which need the Associated
Domains entitlement — and personal (free) Apple developer teams don't support it. A
password needs no deep link at all. Accounts are created by hand in the Firebase console;
the app never signs anyone up, so "only residents get in" holds at the provider before
the app's own allowlist check even runs.

That allowlist check still exists, and it is the point: authenticating proves an email is
yours, never that you live here. A resident's `email` field is what maps the account to a
person — and to a balance, a chore, a photo.

**Firebase config is gitignored too.** `google-services.json` and
`GoogleService-Info.plist` identify the Firebase project used for sign-in. Google
considers them non-secret — they ship inside the app binary either way, and security
comes from Firebase's own rules rather than from hiding the file. But this repository is
public, and committing them lets anyone aim sign-in attempts at the house's project for
free. They sit beside `local.properties` in `.gitignore`; without them the app builds
with sign-in disabled rather than failing, so a fresh clone still runs.

**Secrets never enter the repository.** The API base URL and the release keystore
credentials live in a gitignored `local.properties`; a Gradle task generates a small
`BankApiConfig.kt` at build time and injects it into `commonMain`'s source set. The web
build has its own equivalent, `firebase-config.js`, also gitignored. With those files
absent the app still compiles and runs against bundled placeholder data — cloning the
repo never leaves a contributor blocked on values they don't have.

```kotlin
val bankApiProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}
// ... writes build/generated/bankApi/.../BankApiConfig.kt at build time
```

**A Kotlin/Native pitfall worth documenting:** casting a Kotlin `String` directly to
`NSString` compiles cleanly and crashes at runtime — they are not the same type despite
appearances. The fix is going through `NSString.create(...)`, not a cast.

**Composition-captured state going stale.** The dark-mode toggle read `darkTheme` — a
value captured at composition time — inside its own `onClick`. It worked once per
composition and then silently stopped after the app was backgrounded and reopened,
because the captured value no longer matched the persisted preference. The fix reads the
live `themeMode` inside the click handler instead of the value closed over when the
composable was built — a category of bug that's easy to reintroduce anywhere state is
read inside a lambda rather than at call time.

**A two-color theming rule, enforced through one function.** Gold marks selection and
ownership ("this is yours"); blue carries structure and navigation. Which color is the
*accent* flips by theme through a single `accentColor()`: blue-on-cream reads clearly in
light mode, but in dark mode blue is already everywhere (bars, containers), so gold
becomes the only thing that stands out. Every screen asks this one function instead of
branching on theme itself, so the rule can't drift screen-by-screen.

```kotlin
@Composable
fun accentColor(): Color =
    if (LocalDarkTheme.current) MaterialTheme.colorScheme.secondary
    else MaterialTheme.colorScheme.primary
```

**Money never truncates.** On balance and transaction screens, currency values are never
allowed to clip or ellipsize — the label next to them is what gives up space instead
(`AutoSizeLabel`). A truncated `R$ 1.234,56` is silently misleading in a way a shortened
label never is.

**Dependency injection via Koin**, with the object graph assembled once in `Koin.kt` from
a `cacheDirectory` string supplied per-platform (it depends on Android's `Context`,
which doesn't exist at DI-setup time on iOS). Repositories are read-only by design where
the source of truth is external — `BankSheetRepository` has no write path, because
entries are still made in the spreadsheet's own macro-driven form; the app reflects the
ledger, it doesn't edit it.

Stack: Kotlin Multiplatform, Compose Multiplatform (Material 3), Ktor client
(OkHttp/Darwin/JS engines), kotlinx.serialization, Koin, AndroidX ViewModel/Lifecycle.

## The third target — a PWA, for $99 reasons

Putting the app on the residents' iPhones costs US$ 99 a year through the Apple Developer
Program, or re-signing every build every seven days with a free account. For a fifteen-
person house app that neither ships to a store nor earns anything, both are the wrong
price. The same Compose UI compiles to WebAssembly and installs from Safari's
*Add to Home Screen* instead.

Nothing about the UI changed to make this work. What it cost was the intermediate source
set above, an HTTP engine, two `actual` implementations, and the tail below.

**Storage is `localStorage`, and Safari will delete it.** WebKit evicts site data after
roughly a week of non-use. Everything cached is a copy of what `banco-api` already holds,
so nothing is lost — but the session lives there too, which means a resident who skips a
couple of weeks signs in again. Acceptable; worth knowing before someone reports it as a
bug.

**Timezones don't exist in a browser.** `kotlinx-datetime` reads the IANA database from
the OS on JVM and Native. The browser exposes nothing, so `TimeZone.of("America/Sao_Paulo")`
throws — and because the chore rotation resolves that timezone in a constructor, Koin
failed there and the whole app died before rendering. Declaring the `@js-joda/timezone`
npm dependency isn't enough: nothing imports it, and webpack won't bundle a module no one
references. It needs an explicit `@JsModule` declaration, kept alive against dead-code
elimination. The tell was the bundle size not moving — an entire timezone database
doesn't arrive without showing up on the scale.

**CORS had to be added to the Worker, and the preflight is the trap.** A native client
ignores origins; a browser does not. The Worker verified its token *before* routing, so
the preflight — which arrives with no credentials by design — got a `401`, and the browser
cancelled the real request before it was ever sent. Nothing appears in the server log,
because nothing reached the server. `OPTIONS` now answers ahead of authentication.

The allow-list of request headers is reflected back rather than enumerated. A fixed list
turns into whack-a-mole: Coil sends `cache-control` when fetching a resident's photo, and
one missing entry blocks the whole response with an error that talks about CORS rather
than about photos. The origin check is what actually gates access.

**Two rendering facts that cost hours.** Compose attaches its canvas on the first frame,
so in a background tab — where `requestAnimationFrame` never fires — the page stays blank
with no error at all, which looks exactly like a broken build. And Kotlin 2.3.20's
incremental compilation for Wasm crashes on every source edit, so each rebuild starts by
clearing the Wasm build directories.

The bundle is ~4.7 MB over the wire, most of it Skia rather than application code — a
fixed cost that doesn't grow with the app. A service worker caches the content-hashed
`.wasm` files aggressively and everything else network-first, so a published update
actually reaches people who already opened the app. See [`docs/pwa.md`](docs/pwa.md) for
the deploy runbook.

## Data flow, end to end

1. The treasurer edits `Banco atual.xlsm` in OneDrive — unchanged workflow, no retraining.
2. On open (or pull-to-refresh), the app calls the Worker.
3. The Worker fetches the current file, parses fixed cell ranges with SheetJS, converts
   currency to integer cents, and returns JSON shaped exactly like the app's models.
4. The Ktor client deserializes directly into `data class`es — no DTO layer — and the
   repository writes the response to disk as the new "last known good" state.
5. If the network fails at any point, the UI falls back to that cached state and says so,
   rather than guessing.

## Testing

- `banco-api/test/` — Node-native tests for the date-parsing heuristics behind meeting-
  note ordering and the cell-extraction logic, run with `node test/*.test.mjs`.
- `REPOSTINHO/composeApp/src/commonTest/` — Kotlin tests, including the movement/weight-
  splitting logic used to divide shared expenses among residents, run via
  `./gradlew :composeApp:testDebugUnitTest`.

## Running it locally

```bash
# Worker
cd banco-api
npm install
wrangler dev

# App (compiles all three targets, runs common + platform tests)
cd REPOSTINHO
./gradlew :composeApp:compileKotlinIosSimulatorArm64 :composeApp:testDebugUnitTest
./gradlew :composeApp:wasmJsBrowserDistribution
```

Editing anything under `wasmJsMain` needs the Wasm build directories cleared first —
Kotlin 2.3.20 crashes on incremental compilation for that target. It affects the build
loop only, never the published artifact.

The app builds and runs without any secrets configured — it just falls back to bundled
placeholder data instead of live numbers. See [`banco-api/README.md`](banco-api/README.md)
for the full deploy and secrets setup (in Portuguese, matching the project's working
language for domain-specific docs).

## Why this stack

Kotlin Multiplatform + Compose let one person ship a genuinely native app on both iOS
and Android from a single codebase, without the runtime and plugin-ecosystem trade-offs
of a cross-platform framework like React Native or Flutter. Cloudflare Workers gave a
zero-maintenance, effectively free home for the one piece of logic (`.xlsx` parsing)
that had no reasonable answer on-device — no server to patch, no container to keep
warm, deployed with a single `wrangler deploy`.

The web target is where that bet paid off most concretely. A third platform arrived
without a second UI codebase, a second set of view models, or a second definition of what
a balance is — the parts that would have had to be written twice in any stack where the
app and the web app are different projects.
