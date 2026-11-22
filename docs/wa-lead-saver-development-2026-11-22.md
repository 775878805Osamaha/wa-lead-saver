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
