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
