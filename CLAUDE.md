# CLAUDE.md

Guidance for Claude Code when working in this repository.

## What this app is

**Seller Helper** is an Android app that helps a casual seller list a secondhand item
(e.g. a garage-sale shirt) on eBay. The user photographs the item, Gemini analyzes the
photos and fills an eBay-compatible item-specifics form (only fields it has visual
evidence for, each with a confidence indicator), then the app connects to the user's eBay
account and creates a **draft** listing. The app never publishes — the user finishes the
listing in the eBay app/site.

It started from Google's Firebase AI "Baking" quickstart; that Gemini setup is kept, the
sample screens were replaced.

## Build & test

Gradle needs `JAVA_HOME` set to the Android Studio bundled JDK (it is not on PATH):

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
.\gradlew.bat assembleDebug testDebugUnitTest --console=plain
```

Install / debug on a connected device (adb is under the SDK, not on PATH):

```powershell
$adb = "C:\Users\jocho\AppData\Local\Android\Sdk\platform-tools\adb.exe"
& $adb install -r "app\build\outputs\apk\debug\app-debug.apk"
& $adb logcat -c            # clear before reproducing an issue
& $adb logcat -d -b crash   # dump the crash buffer
```

Build-config notes (already in the repo, don't remove):
- `gradle.properties`: `android.disallowKotlinSourceSets=false` — required for AGP 9 + KSP/Room.
- `app/build.gradle.kts`: `compileSdk = release(37)` — androidx.core 1.19.0 requires API 37.
- Secrets are read from `local.properties` into `BuildConfig` (see `EBAY_SETUP.md`); values
  are baked at compile time, so rebuild after changing them.

## Tech stack

Kotlin + Jetpack Compose (Material3), single `:app` module. minSdk 24. Kotlin 2.2.10,
Compose BOM 2026.02.01. Firebase AI SDK (`gemini-flash-latest`), Room (KSP),
Navigation-Compose with `@Serializable` type-safe routes, Retrofit 3 + OkHttp 5 +
kotlinx-serialization, DataStore (preferences), Coil 3, exifinterface. **No Hilt** — a
hand-rolled `AppContainer` on the `SellerHelperApp` Application class; ViewModels use
`viewModelFactory` + `appContainer()` (see `ui/ViewModelFactories.kt`).

## Architecture

Package layout under `com.example.sellerhelperebay`:
- `data/db` — Room entities, DAOs, `AppDatabase`. Three tables: `item_entries`, `photos`,
  `field_values` (PK `entryId`+`fieldKey`). Photos/fields cascade-delete with the entry.
- `data/ai` — `GeminiAnalyzer` (single call: verify images + extract fields, JSON
  `responseSchema`), `AnalysisSchema`, DTOs.
- `data/ebay` — `EbayConfig` (sandbox/prod switch), OAuth (`EbayAuthManager`,
  `EbayTokenStore`, `EbayAuthApi`), `EbayListingApi`/`EbayRepository` (createItemDraft),
  `DraftMapper`, `CategoryMap`, `ConditionMap`.
- `data/images` — `ImageStore`: copies every picked/captured photo into
  `filesDir/photos/<entryId>/<uuid>.jpg`, downscaled ≤1536px with EXIF rotation. One file
  serves both the UI (Coil) and the Gemini upload.
- `data/repo` — `ItemRepository`: the single source of truth the UI talks to.
- `domain/model` — `FieldKey` (the clothing field set + `ebayAspectName`), `Confidence`
  (color buckets), `EntryStatus`/`PhotoMatchStatus`/`Provenance` enums.
- `domain/FieldMerger` — pure merge of new analysis into existing fields (unit-tested).
- `ui/<screen>` — `entrylist`, `entrydetail`, `review`, `ebay`, `settings`; routes in
  `ui/Navigation.kt`.

Tests are JVM unit tests in `app/src/test`: `FieldMergerTest`, `AnalysisDtosTest`,
`DraftMapperTest`. Keep `FieldMerger` and `DraftMapper` pure so they stay testable.

## Key domain rules (don't quietly break these)

- **Per-field provenance/confidence.** A field is `AI`, `MANUAL`, or `WEB`. The colored
  `ConfidenceDot` shows green >75, yellow 60–75, red <60, **blue** for MANUAL/WEB. The
  analyzer must never emit values for fields the user owns (MANUAL/WEB are passed as
  "locked" and excluded from the prompt).
- **Merge rules** (`FieldMerger`): MANUAL/WEB never overwritten; blank takes incoming;
  same AI value keeps max confidence; different AI values → higher confidence wins.
- **Mismatch flow.** The analyzer flags photos that don't show the primary item; the user
  resolves each (delete / same item / part of lot). User verdicts are final and re-fed to
  the next analysis as hints. "Part of lot" photos inform the description only.
- **Drafts only.** The app calls `createItemDraft` and stops; it must never publish.
- **DraftMapper:** `BRAND`/`TITLE`/`DESCRIPTION` map to top-level `product` fields, other
  fields become `aspects` (array of `{name, values}`, using exact `ebayAspectName`); blank
  fields are omitted.

## eBay integration status (important — current blocker)

**OAuth is fully working in BOTH sandbox and production** (confirmed end-to-end via device
logs: 200 token exchange, real access+refresh tokens, production `api.ebay.com` host). The
WebView capture depends on `shouldInterceptRequest` reading the `?code=` from the redirect
to the accepted URL *before* that URL's host (ochoajuan.com) redirects and strips the
query — keep all the `EbayAuthScreen` WebView callbacks. The RuName must be in **OAuth**
mode (not Auth'n'Auth) with a non-blank accepted URL.

**The project is parked here pending eBay granting `sell.item.draft` / Sell Listing API
access to the production app** (App ID `juanocho-SellerHe-PRD-0184a0250-8275a5ec`).
Confirmed facts:
- `createItemDraft` (`/sell/listing/v1_beta/item_draft/`) returns **404** in sandbox AND
  production — the app isn't enrolled in the limited-release Listing API.
- Requesting the `sell.item.draft` OAuth scope returns **`error=invalid_scope`**. eBay
  confirmed the scope must be assigned to the keyset by eBay (Account → Application Keys →
  OAuth Scopes); it is not assigned. Base `api_scope` works fine.
- The 404 and invalid_scope are the **same gate** (Listing API access). Don't try to "fix"
  the 404 by changing the path/version — it's access, not a code bug. The push code and
  `DraftMapper` are correct and will work once access is granted (request still ON).
- eBay's front-line support is an unreliable automated tool; it falsely claimed no API
  makes a Seller-Hub-finishable draft. Per eBay's own API docs, `createItemDraft` returns
  `sellFlowUrl`/`sellFlowNativeUri` that do exactly that — it's the right endpoint, just
  gated.

There's a **scope toggle** in Settings ("Request draft permission") backed by
`EbayTokenStore.requestDraftScope`: ON requests `api_scope + sell.item.draft`, OFF requests
base `api_scope` only (used to prove production OAuth works without the gated scope). Token
refresh reuses the scopes granted at connect time (`StoredTokens.grantedScope`).

**Open decision (was being asked when work paused):** if eBay won't grant Listing API
access, the alternatives are (a) pivot the push to the Inventory API + an in-app publish
step (ungated `sell.inventory`, but breaks "app never posts" and isn't a Seller Hub draft),
or (b) keep the app as a prep tool with no API push. No decision made yet — the user is
waiting on eBay. See `EBAY_SETUP.md` for the production switch steps.

## Implementation plan

The full phased plan lives at `C:\Users\jocho\.claude\plans\lovely-floating-pearl.md`.
Phases P0–P4 are built; P5 (web-search gap fill, Gemini + Google Search grounding) is
deferred and not yet implemented.
