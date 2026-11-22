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
