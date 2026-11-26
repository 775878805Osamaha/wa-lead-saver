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

## Final continuation of 22 Nov — CI camera/scan regression repair

- Native Kotlin regression run `45061986847`: **SUCCESS** for camera/scan
  hand-off and previously integrated features.
- Integrated Flutter/Kotlin workflow `45062082504`: **FAILED** during the
  Flutter widget-test step with 12 passing and 1 failing tests. The single
  failure was a camera quick-action test tapping text partly obscured by the
  bottom navigation bar at Flutter's default 800×600 test viewport. This was
  a **widget hit-test failure** before invoking the fake gateway, not evidence
  of a native CameraX crash. The hybrid APK build step did not run.
- Updated this focused test to use a 420×1000 device logical viewport
  (with tear-down restoration), keeping existing 360×800 overflow regression
  tests intact. Source commit `9da09c04fa036f3a48f1f5fcaa1405a46d16315f`.
- Triggered a fresh integrated test/build using workflow change commit
  `fcd49086e7d7dc9debdaef633445d60cf7050a09`.
  GitHub Actions run: `45062338344`. **Do not call this run a success**
  before checking its final status, widget-test summary and APK compile step.
- All hybrid APKs remain **unpublished**, same-package signing and local
  migration/device tests remain unverified. The working Kotlin app is intact.
- The complete hand-off and source are in draft PR #1. No merge to main.

## Continued development — account/security/country/permission parity

### Source changes
- Flutter Settings now displays an authenticated native account summary:
  email, customer account number, role, and license status. No access token,
  refresh token or device fingerprint is serialized to Flutter.
- Added `closeFlutter` to the bridge: the user can return to native Kotlin
  login/account management even if the Flutter session is no longer valid.
- Hardened 30-second auth optimization in `WaFlutterHostActivity`:
  check the encrypted local session on every request; invalidate the cached
  authorization and cached user ID in `onPause()`. Network revalidation
  retains the existing Supabase/offline-grace logic for allowed requests.
- Implemented default country-prefix read/write in the original DataStore,
  validating an explicit + followed by 1-4 digits (first digit non-zero)
  before changes. Added compact Flutter editor and confirmation feedback.
- Permission-status cards now link to Android's system settings for
  Notification Listener, app Contacts permissions and battery optimization.
  The Flutter settings screen re-reads actual permissions on resume; it
  never marks Android permissions as granted simply from button taps.
- The separate demo preview retains only synthetic account data and an
  in-memory country prefix. Real Android permission actions remain disabled.
- Added Flutter widget tests and Kotlin Robolectric tests for account
  disclosure, denied arbitrary system settings, country-prefix validation
  and return-to-native action.

### Repair / verification log
- Found and repaired a malformed Dart source edit in the temporary country
  code validation. Reconstructed the settings page and DEMO gateway from
  the last known good Git revisions before introducing syntax-independent
  digit validation. No customer data was touched.
- Native Kotlin/Room regression run `45066866638`: **SUCCESS** for all
  current account/country/permission source changes.
- Flutter integrated run `45066866468`: **FAILED** with 15 tests passing
  and 2 tests failing because old tests assumed all lazy settings cards
  were mounted simultaneously. The new country, permission and account tests
  passed in that run.
- Updated those brittle UI tests without changing the Android behavior.
  Fresh integrated test / hybrid assemble run `45067055808` was triggered
  by commit `e41ee0e626c974896a188bcbd4078b9dbb52daee`.
  **Final status has not yet been verified** as of this report.
- Normal Kotlin build and users' installed APK remain unchanged; the PR
  is draft and hybrid binaries are intentionally not published.

### Next acceptance requirements
- Confirm latest CI has 0 failing tests and successful hybrid APK compile;
  examine logs rather than assuming success.
- Independently test logout while Flutter is foreground/background,
  auth expiry, pending/stopped licenses, system permission grant/resume,
  country-code persistence, real Contacts save and notification capture.
- Verify signed update compatibility, Room 2→3 migration, and optional
  Thmanyah font delivery privately before a releasable APK.

## 2026-11-23 — Customer status editing and truthful monitoring (continuation)

### Verified prior state
- Most recent **successfully completed** hybrid Flutter/Kotlin build before this
  batch: https://github.com/775878805Osamaha/wa-lead-saver/actions/runs/45067055808
  — 17 Flutter widget tests passed, Flutter analyze passed with 11 informational
  deprecation notices, and `-PwaFlutterUi=true :app:assembleDebug` succeeded.
- Native branch unit tests from run 45066866638 completed SUCCESS.
- PR #1 remains draft/unmerged, preserving the existing Kotlin app and data.

### New code on this branch
- Real customer status editing added to `flutter_ui/lib/src/screens/leads_page.dart`:
  six existing statuses (NEW, CONTACTED, INTERESTED, FOLLOW_UP, SOLD,
  NOT_INTERESTED), Arabic captions, status choice chips in the customer
  details sheet, and list refresh after successful mutation.
- Typed gateway `updateLeadStatus(id,status)` dispatches through
  `NativeUiCommandRouter` to existing `LeadRepository.updateLeadStatus`.
  Native source validates status against `LeadStatus.entries` and confirms
  customer existence; no duplicate customer database or Contacts mutation.
- Added Flutter test verifying status choice invokes gateway; added Kotlin
  Room test verifying valid state writes and invalid/missing ID rejection.
- Dashboard `monitoringActive` now requires BOTH notification-listener
  permission and an enabled WhatsApp or WhatsApp Business monitoring preference.
  Permission alone never claims monitoring is configured. The UI explicitly
  distinguishes configuration readiness from proof of incoming notifications.
- Added pure native test for all permission/source-switch combinations.

### Verification in progress
- Native regression workflow run 45080454440, for head 0e2b089..., was
  in progress at the time of report creation.
- Hybrid compilation and Flutter widget tests workflow 45080404475 started
  at source commit fcd2e2e... (after customer status changes, before
  monitoring dashboard labels changed), and was in its APK compilation step.
- Do not claim these changes are tested or release-ready until BOTH runs
  complete successfully. Final hybrid check for the current branch head
  may be needed if later source commits were not covered by that run.

### Release blockers remain
- No physical Android device acceptance test, no verified installed same-
  package upgrade signing path, no migration/data backup verification.
- Public GitHub hybrid builds deliberately do NOT publish a production APK
  or redistribute licensed Thmanyah OTF binaries.
- Finish functional verification of notifications, license changes/logout,
  Contacts writes, camera and native scan round-trips, and Android permissions.

## 2026-11-23 — Login recovery, status editing and current verification

**Implemented on draft PR #1** (existing Kotlin app remains default):
- Flutter customer details now offer all six original Room lead statuses.
  Writes go through `NativeLeadGateway.updateLeadStatus` →
  `NativeUiCommandRouter` → `LeadRepository.updateLeadStatus`, with valid
  enum checks and customer existence validation.
- Customer list refreshes on successful status update. A user-visible error
  replaces false success feedback when the bridge rejects the edit.
- Dashboard monitoring card now treats listener permission AND an enabled
  WhatsApp/WhatsApp Business monitoring source as necessary before displaying
  a configured state. It does **not** claim notification events are currently
  arriving.
- Settings error state now exposes a clear **return to native login/account**
  action even if the native bridge refuses to load settings. The
  `closeFlutter` channel action remains usable on an expired session.
- New Flutter widget tests cover status action and login-return fallback;
  native Room tests cover allowed statuses and invalid/missing customer IDs,
  plus monitoring permission/switch combinations.

**Verified:** Native Android `testDebugUnitTest` for source commit
`0e2b089dab9c8a2baeede9ca6eb5b8a4d6caacd8` completed **SUCCESS**:
https://github.com/775878805Osamaha/wa-lead-saver/actions/runs/45080454440 .
Earlier hybrid run `45067055808` had 17 passing Flutter tests and a
successful debug APK compile, before this status-editing change.

**In progress / not yet verified:** A new hybrid validation run
`45080680935` was triggered for commit
`be70877a816bf4a6c9e07c960ddf48109f2f741f`. Treat all changes
after `45067055808` as awaiting current Flutter/widget/Gradle checks until
this run completes. Do not report a final APK as ready for real customer use.

**Still pending:** physical Android lifecycle/logout, listener message
capture, Contacts permission/save, session activation, native camera/scan
round-trip, large-list profiling, same-key APK upgrade and backup/migration,
private embedding of licensed Thmanyah fonts. Do not merge or publish an
APK into the original app installation until these pass.

## 2026-11-23 — Flutter customer notes, widget lifecycle and bounded native CI retry

### Verified starting point
- Existing hybrid run `45080680935` was SUCCESS at commit
  `be70877a816bf4a6c9e07c960ddf48109f2f741f`: 19 Flutter widget
  tests, analysis and experimental Flutter/Kotlin assembly.
- Existing native regression run `45080454440` was SUCCESS.
- PR #1 remained draft and unmerged.

### New implementation
- Added `notes` to the existing typed `LeadEntry` and the paginated
  Kotlin Room-to-Flutter customer map. No database schema change.
- Added `updateLeadNotes(id, notes)` to the whitelisted native router and
  Dart gateway, delegating to existing `LeadRepository.updateLeadNotes`.
  Native validation rejects IDs <= 0, missing customers, non-string/missing
  notes and notes longer than 1500 characters. An **empty** string is allowed
  to clear a note deliberately. No Contacts, WhatsApp or Supabase write.
- Added Arabic view/edit dialog on the customer details bottom sheet,
  capped at 1500 characters. Customer details are scrollable on narrow
  phones; save refreshes from Room. Cancel makes no write.
- `flutter_ui/test/wa_app_test.dart` adds notes save/cancel regression
  cases; Robolectric test source adds actual Room persistence, clear and
  invalid-input rejection cases.
- A Flutter test exposed a genuine lifecycle bug: disposing the
  `TextEditingController` immediately after `showDialog` returned
  caused use-after-dispose during route reverse animation.
  Fixed by moving controller ownership to the private StatefulWidget
  `_LeadNotesDialog`, whose `dispose` runs when the route is removed.
  Regressions additionally check `tester.takeException() == null`.

### CI evidence and remaining gates
- Initial hybrid run `45083701208` FAILED two new widget tests with
  `A TextEditingController was used after being disposed`. The code was
  fixed, never marked as a successful build.
- Initial native run `45083701187` FAILED at dependency resolution
  because Maven Central returned HTTP 429 for
  `org.robolectric:nativeruntime-dist-compat:1.0.18`; this is a download
  error, not a Kotlin assertion failure.
- Native workflow now retries **once only** for the 429 error; ordinary
  compilation/test failures never get hidden by retries.
- Recheck final results before claiming success:
  Flutter hybrid run `45083907444` (analysis + widget tests completed
  SUCCESS; hybrid APK build in progress at report-writing time), and
  native regression run `45083960402` (in progress).
- No production release APK was published; the old Kotlin app, active
  customers and licensed Thmanyah font assets were not modified or leaked.
  Same-package upgrade and on-device testing remain mandatory.

### 2026-11-23 verification closure — both regression workflows green
- Flutter + Kotlin CI run **45083907444** at source commit
  `663c210ea2848ff5835c8a637e13e493caff8904`:
  **SUCCESS**. `flutter analyze lib --no-fatal-infos` passed (11
  informational/deprecation notices remained), **21 of 21 widget tests
  passed**, and `./gradlew -PwaFlutterUi=true :app:assembleDebug`
  succeeded. APK was intentionally NOT published.
  https://github.com/775878805Osamaha/wa-lead-saver/actions/runs/45083907444
- Kotlin/Room CI run **45083960402** after the one-retry-on-429
  workflow update: **SUCCESS** at source commit
  `f61aaf847835bc371ebef6cf26b3102041da9e58`.
  It includes the new native notes/validation tests. The earlier Maven
  throttling failure was not mistaken for a code assertion failure.
  https://github.com/775878805Osamaha/wa-lead-saver/actions/runs/45083960402
- A subsequent **documentation-only** commit records these outcomes.
  No Kotlin/Flutter source changes were made after these successful runs.
- This is a source/CI milestone, **not** a real-device certification:
  signed same-package upgrade, Supabase user/license transitions,
  background WhatsApp notification capture, Contacts writes, CameraX
  and permission flows must still be exercised on physical Android.
  Do not merge PR #1 or distribute the production APK yet.

## 2026-11-23 — Refresh after Android resume (Flutter lifecycle)

### Implemented on the isolated branch
- Dashboard now observes app foreground transitions and fetches a fresh
  `getOverview` result after return from a background Android activity.
- Customer pages re-query real Room-backed `getLeads` when Flutter resumes
  from dialer/WhatsApp/native tools; the current search text, status filter
  and review-only mode remain selected, while old page cursors are cleared.
  Existing generation guards discard responses from superseded page loads.
- Audit history re-queries real `getHistory` on resume without changing
  the selected audit filter, and clears stale cursor state.
- Each screen registers and unregisters `WidgetsBindingObserver` with
  widget lifecycle, preventing callbacks after pages are disposed.
- Existing settings page already refreshed permission, country-code and
  account state on resume. No Kotlin database schema, notification listener,
  account settings, or Contacts writes were changed in this batch.
- Added three focused Flutter tests for dashboard refresh, customer search
  preservation and history refresh after simulated paused/resumed events.

### Verification status
- Source commit: `39a4d45eab7ee624c54f3f9d8442729d667ab422`.
- Automatic Flutter/native integration workflow:
  https://github.com/775878805Osamaha/wa-lead-saver/actions/runs/45180803614
  Started automatically after the widget test file changed.
  **Check the final workflow result before claiming the new lifecycle
  regression tests passed.** Kotlin native tests were last verified green
  by https://github.com/775878805Osamaha/wa-lead-saver/actions/runs/45083960402
  and are not rerun for this Flutter-only commit.
- This work is still not an APK release. No device install, WhatsApp
  capture, session expiry, or on-device permissions benchmark was completed.
- PR #1 is draft/unmerged. The user's original Kotlin app and live contacts
  remain unchanged.

## 2026-11-23 — Verified lifecycle refresh and dashboard quick-add

### Completed verification
- Regression build `45180803614` failed two lifecycle widget tests:
  1. dashboard `setState` assignment lambda returned a `Future`,
     violating Flutter's synchronous state-callback contract;
  2. test drove an invalid direct `paused -> resumed` lifecycle transition.
- Fixed the dashboard callback to a block returning `void` and used
  valid `inactive -> resumed` transitions in tests.
- New Flutter+Kotlin workflow `45181178590` **SUCCESS** at commit
  `834b76e12d29260a7f91e862e6c6fefef5fd3e5e`:
  **24 / 24 Flutter widget tests passed**, `flutter analyze` passed with
  11 nonfatal deprecation/info notices, and the opt-in hybrid APK compiled.
  https://github.com/775878805Osamaha/wa-lead-saver/actions/runs/45181178590
- App foreground-resume behavior retains current customer search/filter
  while refreshing counts, customers and history from the Kotlin-native
  source. Pages unregister Flutter lifecycle observers on disposal.
- No Android device tests or same-package APK release occurred.

### Follow-up UX implementation
- Dashboard `إضافة يدوية` now requests a real add-customer dialog on
  the customers tab rather than merely navigating to the list.
- `WaAppShell` consumes the one-time request. `LeadsPage` opens the
  dialog after the frame is ready, preventing repeated popups when
  revisiting the customers tab.
- Added widget regression verifying the dialog appears, can be canceled,
  and does not reopen on subsequent tab navigation.
- Commit `108c5a4ae336ca2c558ffce2e50827008ec9aa5d`, automated hybrid
  workflow `45181860070`. **Check its final conclusion** before
  declaring this new shortcut verified.

### Remaining production blockers
- The original Kotlin login, Supabase license, notifications, contacts,
  CameraX, in-app admin, and actual upgrade path need end-to-end phone
  testing. The debug integrated build is not published by CI.
- Thmanyah OTF files remain excluded from public build artifacts;
  a lawful private font installation/build process is required.
- PR #1 remains draft and unmerged to protect the current installed app
  and local database.

## 2026-11-23 — CRM-only customer display-name editing

### What changed
- Added `LeadDao.updateLeadDisplayName` as a narrow SQL UPDATE on
  `leads.displayName` plus `updatedAt`, returning the affected-row
  count. It does not write to the device Contacts provider or change
  `phoneNumber`, `contactName`, `isSaved`, or license/session fields.
- `LeadRepository.updateLeadDisplayName` validates ID and a trimmed
  nonempty name of at most 120 characters and fails when no matching
  record was updated.
- Added typed `updateLeadDisplayName(id,name)` to the Kotlin MethodChannel
  command whitelist and `LeadGateway`. The independent synthetic preview
  explicitly refuses actual writes.
- Added an Arabic rename button in the scrollable customer-detail sheet.
  The dialog owns/disposes its TextEditingController in StatefulWidget
  state to avoid the previously discovered route-animation use-after-
  dispose bug. The UI clearly says it changes the name inside the app
  only, not the phone contact. Blank names cannot be submitted.
- Added Flutter widget tests for successful rename, cancel without write,
  and disabled save for whitespace-only names.
- Added Robolectric/Room tests confirming CRM-only updates, preservation
  of saved/phone/contactName fields, and rejection of empty, oversized,
  non-string, missing and unknown-ID updates.

### Verification evidence
- Native Android branch CI `45184088556` **SUCCESS** on commit
  `e493f984088250e380d83d47740d62e0f9e20222`:
  https://github.com/775878805Osamaha/wa-lead-saver/actions/runs/45184088556
- First Flutter integration CI `45184247279` FAILED before executing
  widget tests due to duplicate fake-gateway field/method declarations
  in the test file; no production APK was built or released.
- Corrected test duplicate in source commit
  `ef40d5bc56e576e437f23e1d9850cd58f2ba8974`.
- Follow-up hybrid CI `45184595768` completed Flutter analysis and
  widget tests **SUCCESS**. At the time of this report addition, the
  hybrid APK compile step had not yet been confirmed complete:
  https://github.com/775878805Osamaha/wa-lead-saver/actions/runs/45184595768
  Check its final conclusion before claiming a fully green hybrid build.

### Release gates
- Still not merged, and no same-package APK was published.
- Must verify physical-device login/license transitions, Contacts
  preservation, app database upgrade, WhatsApp notification capture,
  CameraX/scan round trips, and legitimate private Thmanyah font
  integration before labeling the user-facing app complete.

### 2026-11-23 — Final CI verification for CRM rename

- Source commit `ef40d5bc56e576e437f23e1d9850cd58f2ba8974`:
  full Flutter-native integrated CI **SUCCESS**, run `45184595768`.
  https://github.com/775878805Osamaha/wa-lead-saver/actions/runs/45184595768
- `flutter analyze lib --no-fatal-infos` succeeded with 11 remaining
  nonfatal analyzer deprecation/info notices.
- **28 of 28 Flutter widget tests passed** (includes 3 new rename tests,
  existing native bridge UI flows, app lifecycle and RTL layout tests).
- `./gradlew --no-daemon -PwaFlutterUi=true :app:assembleDebug` built
  successfully. Integrated debug APK intentionally **not published**.
- Kotlin Room `testDebugUnitTest` had already **succeeded** in run
  `45184088556` for the same native-source changes, including CRM-only
  rename preservation and invalid-input cases.
- A documentation-only commit follows; **the working Kotlin app has not
  been merged, overwritten, or installed over the existing version**.
  Physical Android tests, signer continuity, Supabase login/license
  lifecycle, notification capture and backup/migration validation remain
  mandatory before production.

## 2026-11-23 — Installable browser-download QA APK (user requested)

### Direct verified download
- **Release**: https://github.com/775878805Osamaha/wa-lead-saver/releases/tag/wa-hybrid-qa-45190651567
- **APK**: https://github.com/775878805Osamaha/wa-lead-saver/releases/download/wa-hybrid-qa-45190651567/WA-Lead-Saver-Hybrid-QA.apk
- Release asset name `WA-Lead-Saver-Hybrid-QA.apk`, verified existing at
  169,943,790 bytes (approximately 170 MB).
- GitHub Actions run `45190651567`: **SUCCESS**. Source commit
  `de8e64abfd1dee5692f46e302052df327b606a1c`.
- **28 Flutter widget tests passed**, `flutter analyze` passed with
  11 non-blocking/deprecation notices; debug Android assembly succeeded.
- Android `apksigner verify` confirmed APK v2 signature; `aapt`
  verified package `com.example.waleadsaver.hybridqa`, versionName
  `1.0-hybrid-qa`. Included Flutter engine libraries were checked.
- Supabase URL and anonymous key **were present** in the GitHub build
  environment; no secret values were printed in the source report.
- Artifact backup is retained in workflow Artifacts for 14 days.
- QA release was published as **prerelease**, not a production release.

### Independent package safety
- Added opt-in Gradle `-PwaInstallableTest=true` (requires
  `-PwaFlutterUi=true`). Debug package applicationIdSuffix is
  `.hybridqa`, and label is `WA Lead Saver - QA`.
- Manifest FileProvider authority now uses
  `${applicationId}.fileprovider`, matching the existing
  `CsvExportHelper` and preventing authority conflicts if both apps
  are installed.
- The old production identifier remains `com.example.waleadsaver`
  and its Room/DataStore data are unchanged by installing the QA package.
- A QA install begins with an **empty isolated database** and no
  preexisting native session. To see Flutter, sign in from the existing
  Kotlin UI and use Settings > فتح الواجهات الجديدة (Flutter).
  This login may still depend on server-side license/device allowance.
- QA is debug-signed with CI signing identity, so subsequent QA updates
  may require uninstalling the *QA app only*; **never uninstall the
  original app**. The test APK lacks the licensed Thmanyah font binaries.
- Not certified on a physical Android device. Avoid using live customer
  data, writing real contacts, or enabling notification monitoring on
  production accounts before on-device acceptance testing.

### Next stage
Verify launch, Android package side-by-side installation, Supabase
login/session/license state, native Flutter bridge, customer mutation,
WhatsApp notifications, Android contact permissions, CameraX, CSV export,
and device/keyboard/RTL stability from actual user screenshots.
PR #1 remains draft/unmerged and the original app untouched.
