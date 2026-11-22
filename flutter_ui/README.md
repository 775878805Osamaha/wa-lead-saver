# WA Lead Saver — Flutter UI migration (in progress)

This directory starts a **non-destructive, incremental** Flutter/Dart interface
migration for the existing native Android application in `app/`.

## Implemented in source

- Light RTL Arabic Material 3 theme and responsive navigation, based on the approved 5-screen reference.
- Thmanyah Sans 1.2 Arabic font family configured in ThemeData; licensed binaries are installed locally, never committed to the public repository.
- Dashboard with backend-provided counts and monitoring status.
- Customer search, status filters, review queue, and explicit save action.
- History keyset-style cursor API, scrolling with lazy ListView.builder.
- Settings toggles with a backend-confirmed write (no optimistic fake success).
- Loading/empty/error/retry states and WidgetTester fixtures.
- Native bridge interface: `MethodChannel('wa_lead_saver/native_v1')`.

No artificial customer counts or fake successful server responses are used in
the actual Flutter UI. Widget tests use clearly synthetic fixtures.

## Important: the Flutter module is NOT a replacement APK yet

The production Android app currently uses Kotlin/Compose, Android
NotificationListenerService, Room `waleadsaver_db`, Contacts permissions,
Supabase authentication, licenses, and account activation. Those must **not**
be removed or reimplemented by guessing. In particular, Flutter cannot replace
Android notification-listener behavior without a native Android host.

The platform MethodChannel is **not wired to the Kotlin host yet**.
On a standalone Flutter run, missing bridge calls display an error state.
Do not install this prototype on top of the existing app or use it for customer data.

## Native bridge contract to implement in the host

Channel: `wa_lead_saver/native_v1`.

| Method | Inputs | Result |
|---|---|---|
| `getOverview` | none | map: `{total, saved, review, monitoringActive, accuracyPercentage?, evaluatedCases?, passedCases?}`; `accuracyPercentage` must be null or absent when unverified |
| `getLeads` | `{query, status, cursor, limit, reviewOnly}` | `{items, total, hasMore, nextCursor}` |
| `getHistory` | `{filter, cursor, limit}` | `{items, total, hasMore, nextCursor}` |
| `getSettings` | none | map of existing setting keys to bool |
| `setSetting` | `{key, enabled}` | completes only after native storage succeeds |
| `saveLead` | `{id}` | completes only after native Contacts write succeeds |

Lead item fields: `id, name, phone, source, status, isSaved`.
History item fields: `id, status, details, source, timestamp`, where
`timestamp` is UNIX **milliseconds**. The cursor must encode a stable
`(timestamp, id)` pair for history and a deterministic ordering for leads.
The host must validate input, limit page size, perform I/O off main thread,
enforce account permissions, and never expose Supabase service-role keys.

## Verification

Once Flutter SDK is available:

```sh
cd flutter_ui
flutter pub get
flutter analyze
flutter test
```

The native Kotlin source remains testable with `./gradlew testDebugUnitTest`.
Neither test suite is claimed to have run in this working session.

## Release gate

1. Integrate the Flutter module using official Flutter add-to-app Android setup.
2. Implement and test the native MethodChannel adapter, keeping Room, listener,
   permissions, sessions and licensing authoritative.
3. Verify native database migration 2→3 on a backed-up test installation.
4. Profile startup, scrolling (1,260 leads / 55,971 audit rows), notification
   capture and save flows on the same Samsung device.
5. Test RTL, talkback, font scaling, keyboard, small screens and app resume.
6. Build and test a signed candidate only after functional parity.

Flutter add-to-app docs: https://docs.flutter.dev/add-to-app/android
Performance: https://docs.flutter.dev/perf/best-practices

## Using the user-supplied licensed Thmanyah font

The user supplied an archive containing the five `thmanyahsans12` faces
(Light / Regular / Medium / Bold / Black). The GitHub repository is **PUBLIC**,
so the licensed OTF binaries are intentionally excluded from commits and
downloads. Only the app's font family setting and the local installer are
tracked. Do **not** commit, redistribute, or include the font archive in
public GitHub Actions artifacts.

From a machine that has the user's licensed font ZIP and Flutter SDK:

```sh
cd flutter_ui
python3 tool/install_thmanyah.py "/path/to/خطوط تطبيق ثمانية السعودي.zip"
flutter pub get
flutter test
```

The installer validates the ZIP entries by name, copies only five Sans faces
into the git-ignored `assets/fonts` directory, and registers them in the
**local** `pubspec.yaml`. Do not commit the generated font-asset manifest
change unless the asset delivery strategy is private and rights-cleared.
Without running this step, Flutter may fall back to an installed Arabic font;
it is not an accurate Thmanyah render. Visual previews may be created locally
using the supplied font without distributing its source files.

Font weight mapping: 300 light, 400 regular, 500 medium, 700 bold,
900 black.

## Dashboard visual parity

The five-screen light reference uses a three-card metrics row, accuracy panel,
four quick actions, customer/contact cards, a review queue, audit filters,
and setting switches. The Flutter dashboard now matches the reference hierarchy.
Some actions (CSV export and camera capture) currently show a clear "native
Android integration required" message instead of performing a fake action.
Do not claim these are production-ready until the Kotlin bridge is implemented.
