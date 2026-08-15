# Repostinho

A private app for the residents of a student republic ("república") at Unicamp — house
finances, chores, calendar, and meeting notes in one place, built to feel like a small
product rather than a spreadsheet with a UI bolted on.

This README focuses on the engineering: what the system does, why it's shaped the way
it is, and the constraints that drove each decision. It's written as a portfolio piece
as much as documentation.

<p align="center">
  <img src="docs/screenshots/home.jpeg" width="260" alt="Home screen, showing the current resident's balance, weekly chore, and recent meeting notes" />
  &nbsp;&nbsp;
  <img src="docs/screenshots/calendario.jpeg" width="260" alt="Calendar screen, showing the house's fixed yearly events in the app's light theme" />
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

**Dependency injection via Koin**, with the object graph assembled once in `Koin.kt` from
a `cacheDirectory` string supplied per-platform (it depends on Android's `Context`,
which doesn't exist at DI-setup time on iOS). Repositories are read-only by design where
the source of truth is external — `BankSheetRepository` has no write path, because
entries are still made in the spreadsheet's own macro-driven form; the app reflects the
ledger, it doesn't edit it.

Stack: Kotlin Multiplatform, Compose Multiplatform (Material 3), Ktor client
(OkHttp/Darwin/JS engines), kotlinx.serialization, Koin, AndroidX ViewModel/Lifecycle.

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
