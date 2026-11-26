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

The MethodChannel **is now implemented** in an opt-in native FlutterActivity:
`app/src/flutterBridge/java/com/example/flutterbridge/WaFlutterHostActivity.kt`.
The ordinary Android build still uses Kotlin/Compose. The hybrid source builds
only with `-PwaFlutterUi=true` and has not passed real-device acceptance tests.
Do not install any same-package debug APK over a production installation
unless its signing identity and backup/migration are verified.

## Native bridge contract implemented in the opt-in host

Channel: `wa_lead_saver/native_v1`.

| Method | Inputs | Result |
|---|---|---|
| `getOverview` | none | map: `{total, saved, review, monitoringActive, accuracyPercentage?, evaluatedCases?, passedCases?}`; `accuracyPercentage` must be null or absent when unverified |
| `getLeads` | `{query, status, cursor, limit, reviewOnly}` | `{items, total, hasMore, nextCursor}` |
| `getHistory` | `{filter, cursor, limit}` | `{items, total, hasMore, nextCursor}` |
| `getSettings` | none | map of existing setting keys to bool |
| `setSetting` | `{key, enabled}` | completes only after native storage succeeds |
| `saveLead` | `{id}` | completes only after native Contacts write succeeds |
| `callLead` | `{number}` | open native dialer only after validating number |
| `openWhatsAppLead` | `{number}` | open native WhatsApp Intent using public Android APIs |
| `createLead` | `{phone, name}` | add to existing Room only, enforcing license and uniqueness |
| `exportHistory` | none | stream a CSV and share via FileProvider |
| `clearHistory` | `{confirmation: "CLEAR_ALL_AUDIT_HISTORY"}` | delete local audit log only for an active license, after explicit UI confirmation |
| `openNativeTool` | `{tool: "camera" | "scan"}` | return to original Kotlin camera/scan dialog |

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
GitHub Actions verified native `testDebugUnitTest` (run 45061665081) and
Flutter analysis/widget tests plus the hybrid build (run 45061735834),
before the subsequent camera/scan changes. Those last changes require
fresh verification before release.

## Release gate

1. Validate the existing opt-in Flutter module and MethodChannel integration
   on a real Android device. The native data and license layer remains
   authoritative, and Kotlin UI remains the default.
2. Finish login/admin/account UX parity in Flutter; keep native Auth and
   notification listeners unchanged until acceptance tests pass.
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
CSV export, confirmed audit deletion and manual lead creation now use the
real Android bridge. Camera and scan buttons hand off to the existing
Kotlin dialogs. The independent DEMO build is deliberately read-only and
cannot use any real Android actions. Test on a device before production.

## Native settings status fields and visual controls

The light reference settings use six permission/monitoring cards. The
`getSettings` channel response may include `notificationPermission`,
`contactsPermission`, and `batteryOptimizationIgnored` as read-only
booleans read from Android itself. Missing values are displayed as unknown,
not as granted. The four existing user preferences retain real
`setSetting` writes and failure feedback. The bridge is implemented in the opt-in hybrid source, while independent
DEMO builds show unknown permission status and return unsupported actions.
Normal Kotlin builds are unchanged.

## Rebuild the real Flutter/Kotlin hybrid locally (experimental)

Use Flutter >=3.27 and the Android/JDK versions configured in the repo.
The module files are generated locally and ignored by Git:

```sh
flutter create --template module --org com.walspreview --project-name wa_lead_saver_ui flutter_module
cp flutter_ui/pubspec.yaml flutter_module/pubspec.yaml
cp -R flutter_ui/lib/. flutter_module/lib/
cd flutter_module && flutter pub get && cd ..
./gradlew -PwaFlutterUi=true :app:assembleDebug
```

Authenticated users may open the Flutter UI from the existing Kotlin
Settings screen in a hybrid-enabled build. Kotlin still handles login, admin,
notification monitoring and the existing database. Camera and scan return
to the native screen. Unknown or expired sessions are denied by the bridge.

**Do not publish or install this debug package over a currently working app
without verifying its signing key.** An APK with the same package name but
a different signature cannot update the old app in place; uninstalling the
old app could delete local customers and their audit log.

The licensed Thmanyah font remains user-supplied only. To render it in a
private local hybrid build, run `flutter_ui/tool/install_thmanyah.py` first,
copy its local font assets into the generated module, and do not publish
OTF files or public build artifacts containing them.

Hybrid GitHub build checks intentionally do not upload APKs.

## Account, dialing prefix and Android permission screen parity

The opt-in Flutter host uses the original encrypted cached session and
license manager to show a read-only account summary. It does **not** expose
session tokens. A return button closes Flutter to reveal the native Kotlin
authentication/admin screen, even when the user needs to log in again.

The Flutter default country prefix editor validates the international
prefix before updating `SettingsDataStore` through the native router.
Tap the permission rows to open Android's actual Notification Listener,
Contacts or battery settings. On return, the Flutter view requests fresh
permission statuses rather than assuming they were granted.

These changes still require real-device acceptance. The standalone DEMO
version only simulates the country prefix and does not access or change
Android permissions or authenticated accounts.
