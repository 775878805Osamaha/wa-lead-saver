# WA Lead Saver development handoff — 2026-11-22

## User-observed baseline
Screenshots showed local customers 1,260, review queue 111, audit log 55,971.
Cloud admin had 2 active license accounts (different dataset from local
contacts). The screenshots do not prove a performance gain or permission sync.

## Code observations
- Native stack: Kotlin + Jetpack Compose + Room v2, not Flutter.
- HistoryDao used unbounded Flow<List<HistoryEntity>>.
- MainViewModel kept full audit history (and its filtered copy).
- LeadRepository.captureStats recomputed counts in memory across all history.
- Dashboard only needs recent rows, not entire history.

## Implemented — native performance branch
- SQL aggregate counts for capture statistics, preserving category criteria.
- Small recent activity query for dashboard (LIMIT 6).
- Keyset history query ordered by timestamp + ID, 80 rows at a time.
- Count query by status filter.
- History timestamp/ID index and non-destructive Room v2 → v3 migration.
- History UI loads more near viewport end; main dashboard uses only six recent rows.
- CSV history export streams bounded 500-record pages to a UTF-8 file via FileProvider.
- CSV cell quoting defends against spreadsheet formula injection, with regressions.
- Regression test for tied timestamps, filtered counts and count aggregates.

## Implemented — Flutter migration source
- `flutter_ui/` self-contained Dart UI layer and native MethodChannel contract.
- Main dashboard, customer list/search/filters, review save action, audit
  history paging and settings.
- No fake production data; synthetic data exists only inside widget tests.
- The Kotlin capture service, local DB and Supabase login are untouched.

## Additional SQL-only verification (not an Android test)
- Executed the exact aggregate/filter/keyset SQL logic against an in-memory
  SQLite database with **55,971 synthetic audit rows**.
- All 55,971 IDs were retrieved **once**, across 700 keyset pages with
  80-row page size; no missing or duplicate rows with repeated timestamps.
- SQL aggregation/filter queries completed and returned consistent counts.
- These are host SQLite checks only: they are **not** Robolectric tests,
  Flutter tests, APK/device tests or proof of Android performance improvement.

## NOT implemented / verification needed
- Android Flutter add-to-app integration + Kotlin MethodChannel host.
- Native licensing and Contacts permission validation for Flutter actions.
- Flutter analyzer and tests: Flutter SDK was unavailable in this environment.
- Android Gradle/Robolectric tests: Android SDK/toolchain was unavailable here.
- Native CSV regression tests were added but have not been executed.
- Device benchmark and side-by-side screenshot comparison: not performed.
- New installable APK: not built. Branch must not be merged or shipped blindly.

## Next integration checkpoints
1. Run native tests first; fix compile or DAO query errors, if any.
2. Execute the manual Flutter UI workflow and address analyzer/test failures.
3. Add the native bridge and restore one-to-one functionality.
4. Verify that historical audit filters, streamed CSV and clear-history
   operations work without all-rows allocation; full CSV still includes all rows.
5. Profile on actual Android hardware before making speed claims.
6. Update this report after every merge, build, or significant change.

Repository: https://github.com/775878805Osamaha/wa-lead-saver
Development branch: feature/flutter-ui-and-history-performance-20261122

## 2026-11-22 follow-up — small-screen layout + visual previews
- Updated `WaCounterCard` to allow a compact horizontal presentation.
- Updated `flutter_ui/lib/src/screens/dashboard_page.dart` so narrow
  screens display two counter cards and a full-width review counter, removing
  an otherwise unused grid cell. Larger screens retain three columns.
- Rendered five **HTML/CSS design previews** locally for dashboard, customers,
  review queue, audit history, and settings. Their colors, RTL direction,
  content hierarchy, and component types follow the Flutter source, but these
  ARE NOT screenshots from a running Flutter/Android build. Preview data uses
  sample or previously observed counts, not live backend records.
- Browser rendering was visually inspected; native Flutter device capture is
  still blocked on Flutter SDK/host integration and remains unverified.

## 2026-11-22 light-theme follow-up — user-requested color change
- User explicitly rejected the dark appearance and requested continued development.
- Updated `flutter_ui/lib/src/theme/wa_theme.dart` to a **light-only** Material 3 theme:
  background `#F7FAF8`, cards `#FFFFFF`, borders `#E0EAE4`,
  primary accessible green `#147D45`, WhatsApp accent `#25D366`,
  main text `#17251E`, secondary text `#64736B`.
- Updated shared counters, toolbar brand icon, and switch thumb colors to
  avoid dark-theme remnants; applied to dashboard, customers, review, history,
  and settings through shared ThemeData.
- Updated widget test to assert `Brightness.light` on the application theme.
- A source inspection found no `Brightness.dark`, old dark palette colors,
  or `WaPalette.white` in the Flutter UI files.
- Rendered five mobile-sized **HTML/CSS previews** in Chromium with the
  new palette; inspected a combined contact sheet. The previews use sample
  labels and previously observed screenshot counts and are **not** output
  from Flutter running on Android, nor proof of visual parity.
- Calculated contrast against white (WCAG formula):
  primary green 5.19:1, muted text 4.99:1, primary text 15.91:1.
- **Not run:** Flutter analyzer, widget tests, Android APK/device screenshot.
  Flutter module is still not wired to the original Kotlin host, remains
  unmerged, and does not alter the installed Android application.

## 2026-11-22 — explicit reference screenshot and user-owned Thmanyah pack
- The user supplied a single five-screen light reference PNG
  (settings / history / review / customers / dashboard, right-to-left screens)
  and the ZIP `خطوط تطبيق ثمانية السعودي.zip`.
- Inspected the archive in-session: 15 OTFs, grouped as
  Thmanyah Sans / Serif Text / Serif Display, with Light, Regular,
  Medium, Bold and Black weights in each family.
- **Chosen application UI family:** `ThmanyahSans` (weights 300/400/500/700/900).
- **Repository is PUBLIC.** Do not commit or redistribute the licensed OTFs.
  Added a local font installer `flutter_ui/tool/install_thmanyah.py` that
  reads the supplied ZIP, validates all five Sans faces, writes git-ignored
  local font assets and appends the font asset manifest to LOCAL pubspec.
- `ThemeData(fontFamily: 'ThmanyahSans')` and app bar title font family
  prepared in source; Flutter will fall back until the user runs the installer
  on an actual build machine with the licensed ZIP.
- Reference-oriented changes to Flutter dashboard (three stats across,
  source-backed accuracy panel, four quick actions), settings (separate
  status/setting cards), history (CSV/clear actions with explicit not-yet-wired
  feedback), customer cards (native call/WhatsApp method-channel contracts).
- Accuracy stays unknown unless returned by the native source; we did **not**
  hardcode screenshot sample `100%` into production.
- Rendered one **1536x768** composite preview in a temporary local renderer
  with the real user-provided Thmanyah Sans faces. PDF font inspection
  confirmed ThmanyahSans subsets present in the render. The user received
  only the resulting raster image, not downloadable font binaries.
- IMPORTANT: This image is a **design preview**, NOT an emulator/device
  screenshot of Flutter. No claim of pixel-perfect runtime visual parity.
- Flutter SDK and native add-to-app bridge remain unavailable here.
  Flutter analyze/test and Android build/device checks were NOT run.
- Kotlin/Compose production code, Supabase, licenses and installed app
  remain unchanged by this typography/design-only follow-up.

## Continuation — Flutter native data bridge and performance correctness

### Source changes saved in this branch
- Added `LeadDao.getFilteredLeadsCount` and `LeadDao.getLeadsPage`:
  bound-parameter SQL search with wildcard escaping, review/status filters,
  chronological keyset cursor `(createdAt,id)` and a caller-bounded page size.
  No new table or destructive database migration.
- Added `AndroidLeadUiDataSource` (Kotlin; no Flutter imports) to provide
  data maps for the existing Dart `LeadGateway`: `getOverview`,
  `getLeads`, `getHistory`, `getSettings`, `setSetting`,
  `saveLead`, `callLead`, and `openWhatsAppLead`.
  Save delegates to existing `LeadRepository.saveLead` for license checks,
  permission checks, duplicate protection, and Android Contacts writes.
- Added `NativeUiCommandRouter` with explicit method whitelist and typed
  argument validation. This router is **not yet registered with Flutter's
  MethodChannel**; add-to-app and generated Flutter module integration remain
  required before runtime usability.
- `MainActivity` now observes a dedicated SQL COUNT flow for the customer
  bottom-navigation badge/title rather than subscribing to the complete
  customer list only for its size. The existing Compose screen remains intact.
- Capture statistics now return an accuracy percentage only after a labeled
  benchmark has been run; saved leads and auto-generated CONFIRMED tags are
  **not** treated as independent evidence that accuracy is 100%.
- Added `FlutterNativeDataSourceTest.kt` for page boundaries/tied
  timestamps, query filters, literal wildcard search, read-only permissions,
  malformed cursors, and non-fabricated capture accuracy.
- Added Flutter widget-test scenarios for narrow 360px Android screens,
  disconnected native backend, and review-save callback invocation.

### Executed verification — SQL only
Executed the native keyset-query shape using the Python SQLite engine with
1,260 synthetic customer records. Every page was unique; tested 1,260 all,
840 unsaved, 630 NEW, and 420 unsaved FOLLOW_UP records, plus escaped wildcard
and Arabic search. The query shapes passed these local checks. This **does not
validate Android Room compilation** or actual on-device performance.

### Still pending / release blockers
1. `flutter_ui` is currently Dart UI source without a generated Android
   Flutter module and without a registered native MethodChannel.
2. The user-provided licensed Thmanyah Sans faces are excluded from the
   **public** GitHub repository. The existing local installer must run on a
   properly licensed build machine before any font-parity screenshot/build.
3. Android/Room unit tests and `flutter analyze`/`flutter test` were
   **not executed**, because Flutter SDK and Android toolchain are not
   installed in the current environment. GitHub CI was not triggered to avoid
   unexpected Actions charges.
4. Keep PR #1 draft/unmerged; verify retained local DB, notification listener,
   authentication, licenses and contacts; then run Android real-device tests
   before any production release APK.

## 2026-11-22 — Installable Flutter UI preview (DEMO only)

**User requested an installable build to inspect the reference-matched UI.**
Created `flutter_ui/lib/preview_main.dart` with in-memory synthetic contacts,
history and settings, and a clearly labeled DEMO banner. No real numbers,
Supabase credentials, notification service, or device Contacts data are
accessed. Save/contact actions are unsupported in this preview.

Created workflow `.github/workflows/wa-lead-ui-preview-apk.yml`.
It generates an isolated Flutter Android app with application ID
`com.walspreview.wa_lead_saver_ui` (NOT the production
`com.example.waleadsaver`), verifies Dart code, executes widget tests, and
builds a standalone DEBUG APK.

### Confirmed result
- GitHub Actions run: https://github.com/775878805Osamaha/wa-lead-saver/actions/runs/45053412361
- Workflow result: SUCCESS, based on commit `e6a67e127780d573dd9b10089cad36799ac3bd1d`.
- Flutter version: 3.50.1 stable.
- `flutter analyze lib --no-fatal-infos`: exit 0, **11 deprecation infos**
  (modern Flutter package transitions) remain. No analyzer errors.
- `flutter test`: **6 tests passed**.
- `flutter build apk --debug --target lib/preview_main.dart`: SUCCESS.
- Verified APK file exists, is a ZIP-compatible Android package, contains an
  AndroidManifest.xml and DEX bytecode, and was uploaded by Actions.
- Artifact ID: `14765564536`, name `WA-Lead-Saver-UI-Preview-DEMO`,
  expires on 2026-11-25.
- APK SHA256:
  `3c0af621822c21b604ecc8ef0d5871187559fb047154543de5d015f006a43aa3`.
- Size: 150,196,021 bytes (debug, multi-architecture).
- The user was provided a direct conversation download link to the actual
  verified APK file.

### Non-negotiable distinctions
- This is **only a visual UI preview**, not a usable production update.
  The production app, user contacts, Supabase and licenses are untouched.
- The private user-supplied Thmanyah font is **NOT embedded** in this public
  Actions build. Flutter falls back to the default Arabic font. Do not
  claim that the installed preview demonstrates exact Thmanyah typography.
- Native Kotlin MethodChannel and full functional Android migration remain
  unimplemented; PR #1 remains a draft, not merged.
- Tests on a physical Samsung/Android device have NOT been run; install and
  layout may still require on-device fixes.
- Initial preview builds failed due to two Dart `.isNotEmpty()` invocations
  and a test reading a nonexistent ThemeData.fontFamily getter. These were
  fixed before the successful run.

## 2026-11-22 — Phone feedback: compact UI & demo transparency
- User installed the visual preview and sent five actual Samsung screen
  captures. Observed large vertical spacing, oversized header/metrics, audit
  status text in English, and demo controls claiming permission status.
- Reduced AppBar 72→64 logical pixels; navigation bar to 68, metric cards
  130→114, shortcuts 92→80, section heading padding 12→6.
- Reduced list/card spacing in customers and settings, with compact
  setting tiles. Converted audit status labels to Arabic for presentation
  while leaving the stored native enum/data unchanged.
- The preview entrypoint now passes `isPreview: true` and displays a
  persistent disclosure that values are synthetic. Demo metrics now reflect
  the four synthetic entries (total 4, saved 0, review 4) rather than copying
  real user screenshot totals. Android permission keys are omitted so the
  UI renders unknown rather than an unverified green approval.
- These changes affect **only** the Flutter UI preview; original Kotlin app,
  Supabase, local contacts and licensing are untouched.
- Added widget-test cases covering preview notice/permissions, localized
  status and compact phone screen navigation. Verification is pending the
  automatically triggered GitHub preview workflow for this commit.

## Follow-up: installed preview screenshots and layout regression repair
The user installed the DEMO APK and supplied five real screenshots from the
Android device. These screenshots are visual feedback only; they do not prove
that live Contacts/WhatsApp functionality is present in Flutter.

### Implemented in Flutter source
- AppBar reduced 72→64dp, navigation bar 68dp, section header gaps reduced.
- Dashboard metric cards reorganized using Flexible count/label children
  after Flutter tests caught a 3px RenderFlex overflow at 360dp width.
- Smaller settings rows, customer list gaps, and shortcut tiles.
- Audit status text localized for presentation (Saved, Needs Review,
  Duplicate, Failed), retaining raw status values for database queries.
- Synthetic preview now shows consistent 4/0/4 demo counts and disables
  fabricated permission approvals (unknown states displayed instead).
- Persistent preview notice differentiates UI sample values from the actual
  device's customers/permissions.
- Black full-width snackbars replaced with light floating notices to match
  the user's light-only request.

### Verification / release gates
- Older preview workflows failed on 360dp dashboard overflow; subsequent
  `flutter_ui` layout code now uses Flexible to constrain text.
- For commit `782d7e58c55d56a8271de3785cff7eb1a0317625`,
  GitHub workflow `45056833474` passed Dart analysis and widget tests,
  while the Android APK build was still running at last inspection.
- Updated light-snackbar commit `c9cade248173ff4f6c6c0ef2e916ad3475c5930b`
  is checked by workflow `45056974486`, which may still be running.
- Do not claim a new APK is installable without a completed success run and
  non-expired Artifact.
- Thmanyah font binaries remain outside public GitHub builds; visual DEMO
  still uses Android/Flutter fallback font.
- Production Flutter/native integration remains incomplete. Do not merge or
  replace the installed Kotlin application.

## 2026-11-22 continuation — verified preview build and Room KPI optimization

### Completed GitHub build
- GitHub Actions preview run `45056974486` finished **SUCCESS** at commit
  `c9cade248173ff4f6c6c0ef2e916ad3475c5930b`.
- Artifact `WA-Lead-Saver-UI-Preview-DEMO`, ID `14768046663`,
  was present and not expired when verified. GitHub lists expiry on
  **2026-11-25**. This is an isolated synthetic-data demo APK,
  **not** a production release, and it does not include licensed
  Thmanyah OTF font binaries.
- Main Kotlin app and user data have not been modified by this preview.

### New native optimization
- Found `LeadRepository.captureStats` still collecting and iterating
  the complete `allLeads: Flow<List<LeadEntity>>` whenever the dashboard
  KPIs invalidate; this was unnecessary after history SQL optimization.
- Added `LeadCaptureCounts` in `LeadDao`, containing a single
  `COUNT(*)`/conditional `SUM` query for total, saved and unsaved
  `NEEDS_REVIEW` counts. Updated `captureStats` to combine this
  bounded SQL projection with the existing history aggregate and
  labeled benchmark results.
- Kept exact previous review rule `!isSaved && tags.contains("NEEDS_REVIEW")`
  by translating it to SQLite `INSTR(tags, 'NEEDS_REVIEW') > 0`.
  No tables, records, migrations, or account logic were changed.
- Added Robolectric test sources for five representative rows and an
  empty database. Native Gradle/Robolectric suite has **not** run.
- Executed a local Python SQLite equivalence check using 1,260 synthetic
  records: `total=1260, saved=635, needsReview=379` matched original
  in-memory logic. Empty query returned `0,0,0`. This is a
  database-query check, **not** device benchmarking or Android build proof.

### Remaining blockers
1. Native `./gradlew testDebugUnitTest` is still **unverified** and
   configured as manual workflow only for GitHub Actions cost control.
2. Actual Kotlin-to-Flutter embedding/MethodChannel registration and
   license/auth UX parity are still unimplemented.
3. A release with user-provided Thmanyah fonts must use a privately
   licensed build pipeline; public GitHub APK preview cannot include
   font assets.
4. Do not merge PR #1 or replace user's installed original app without
   native tests, migration tests, and real-device validation.

### Additional history counter consistency repair
- `HistoryDao.observeCaptureCounts()` previously excluded status
  `Phone number unavailable` / `unavailable` from
  `invalidOrExcluded` while `getFilteredCount('EXCLUDED')`
  counted it. This could produce inconsistent totals.
- Added `LOWER(status) LIKE '%unavailable%'` to the same
  aggregate classification. Updated the existing Robolectric regression
  assertion from 2 to 3 for the provided set of statuses.
- Local SQLite synthetic check: all 5 excluded/invalid/system/unavailable
  rows counted identically by aggregate and EXCLUDED filter.
- No Room/Gradle compilation was executed: the repository's native
  build workflow is intentionally `workflow_dispatch`-only to control
  GitHub Actions costs. The Flutter DEMO APK does **not** include this
  native change. Manual native tests are still required before merge.

## 2026-11-22 continued implementation — Flutter Add-to-App and real actions

### Verified milestones
- Ran GitHub Actions native unit tests `testDebugUnitTest` on isolated branch:
  https://github.com/775878805Osamaha/wa-lead-saver/actions/runs/45060723447
  **SUCCESS**. This precedes the later integration/functionality commits.
- Generated real Flutter module at build time and compiled it into the
  existing Android host with `-PwaFlutterUi=true`:
  https://github.com/775878805Osamaha/wa-lead-saver/actions/runs/45061150624
  **SUCCESS**. The hybrid debug APK was intentionally NOT published.
- Core Kotlin-only build remains the default if no opt-in Gradle property is
  provided. `MainActivity` login/admin, database, Supabase, notification
  listener and Contacts service remain authoritative.

### New sources and functionality
- Root settings.gradle.kts, app/build.gradle.kts and AndroidManifest now
  feature-gate `WaFlutterHostActivity`, the Flutter module dependency and
  optional sourceSet. The bridge Activity is not exported and is disabled in
  ordinary Kotlin builds. Authenticated users can opt into Flutter from
  existing native Settings when compiled with the flag.
- `WaFlutterHostActivity` registers the actual
  `wa_lead_saver/native_v1` MethodChannel, validates Supabase session plus
  cached license status with 30-second authorization caching, and dispatches
  only typed whitelisted operations in `NativeUiCommandRouter`.
- `AndroidLeadUiDataSource` now supports creating a lead using the existing
  license-aware `LeadRepository.createLead`, streaming real history CSV
  via Android FileProvider, and clearing audit history only for active
  accounts. The router requires an exact clear-history confirmation token.
- `flutter_ui/lib/src/data/gateway.dart` implements the corresponding
  typed Dart methods; the synthetic DEMO gateway explicitly refuses real
  actions.
- Customer list has an add-manual-lead dialog with phone validation.
  History export button invokes native CSV sharing, and history deletion
  requires a separate explicit confirmation dialog before invoking native
  deletion. Dashboard CSV quick action invokes the same real bridge.
- Added Flutter widget tests to check manual add, export and cancel-before-
  delete actions, plus native tests rejecting malformed/unconfirmed requests.

### Current validation in flight (do not claim completed)
- Native regression run `45061665081` for the new Kotlin router changes.
- Hybrid compile and RTL Flutter widget tests run `45061735834` for these
  changes. These run IDs must be checked before any release.
- Public DEMO APK workflow is now **manual-only** to prevent repeated
  GitHub Actions minutes. Several older push-triggered jobs may finish
  independently; do not confuse their artifacts with a current hybrid build.

### Release protection and next steps
- Keep PR #1 **draft and unmerged** until native tests, current Flutter
  tests, Android install, Supabase auth, notification capture, Contacts
  permissions and migration are all verified.
- Do not publish a same-package APK to overwrite existing client data or
  require uninstall. The current integrated workflow intentionally uploads
  no APK. Existing production APK signing must be confirmed first.
- Public GitHub builds do NOT contain the user's licensed Thmanyah font
  binaries. Use the local installer and a private/license-compliant build
  pipeline before claiming exact font parity.
- Remaining work: re-use native camera/scan flows from Flutter, account
  lifecycle UI, admin functions, settings parity, device profiling,
  connection/error states and complete acceptance tests.

## Further continuation — reuse original native camera/scan flows
- Added `openNativeTool(camera|scan)` to typed Kotlin and Dart bridge.
  Unknown commands are rejected. Flutter quick actions now invoke this
  method rather than showing a placeholder or navigating incorrectly.
- Kotlin `MainActivity` receives a guarded intent extra and opens the
  original CameraX photo dialog or existing scan dialog after restoring
  the authenticated screen. This uses the original capture implementation,
  not a second listener or separate customer database.
- Added Flutter widget test source for camera/scan callbacks; native
  test source rejects unknown tool commands.
- Latest integrated branch verification must include those changes:
  workflow `45062082504` was triggered for hybrid compilation,
  while native regression workflow `45061986847` tests the Kotlin changes.
  Check their actual conclusions before making any readiness claim.
- Added `/flutter_module/` to root gitignore: generated Flutter SDK
  wrappers and temporary assets must not enter the public repository.
- No production APK release, merge or client-data modification occurred.
