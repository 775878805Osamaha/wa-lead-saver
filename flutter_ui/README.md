# WA Lead Saver — Flutter UI migration (in progress)

This directory starts a **non-destructive, incremental** Flutter/Dart interface
migration for the existing native Android application in `app/`.

## Implemented in source

- Dark RTL Arabic Material 3 theme and responsive navigation.
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
| `getOverview` | none | map: `{total, saved, review, monitoringActive}` |
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
