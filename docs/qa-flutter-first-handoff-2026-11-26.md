# WA Lead Saver — 26 November 2026: Flutter-first QA launcher fix

## User-reported defect
Two Samsung Android screenshots showed the original dark Kotlin/Compose
settings and dashboard after installing the previously published
WA-Lead-Saver-Hybrid-QA.apk. This was **not** the white-and-green Flutter
design supplied in the earlier visual preview.

## Root cause verified in source
The former QA APK declared com.example.MainActivity as Android MAIN/LAUNCHER.
That activity always constructed the old Compose MainScreen. Flutter existed
as a secondary Activity behind Settings > فتح الواجهات الجديدة (Flutter),
and therefore never appeared automatically on cold launch.
A successful Android build did not prove which Activity the phone opened.

## Corrective implementation (draft PR #1)
- The normal production Kotlin package and launcher remain unchanged in behavior.
- The new separate test package ID is `com.example.waleadsaver.flutterqa`,
  distinct from the installed production `com.example.waleadsaver` and older
  QA `com.example.waleadsaver.hybridqa`.
- The new QA Android manifest uses the **real exported**
  `com.example.flutterbridge.WaFlutterHostActivity` as its MAIN/LAUNCHER.
  The native Kotlin launcher alias is enabled only in ordinary builds.
- The Flutter entrypoint `main.dart` checks auth before entering
  `WaAppShell`; unauthorized sessions see a light white/green login gate,
  not any fabricated customer data.
- Kotlin/Supabase login is retained via `openNativeLogin`; returning from
  sign-in triggers session recheck and the Flutter dashboard opens only
  when the account and license permit. Android Room, WhatsApp notification
  listener, FileProvider, and Contacts remain managed by the original Kotlin
  services.
- Return-to-native account and camera/scan flows remain available.
- The QA launcher uses Android's light no-action-bar theme rather than
  inheriting the old application's dark splash.
- Three new Flutter widget tests cover guest gate brightness and login,
  authorized UI, and unlock on Android resume.

## Verification and release controls
- **31 Flutter widget tests passed** and native Room tests succeeded on the
  26 November 2026 sources. The initial Flutter-first QA APK compile also
  succeeded, including APK signature v2 and package `.flutterqa`.
- The first QA publication attempt intentionally FAILED the new
  `aapt dump badging` launcher check: a Flutter activity-alias did not
  appear as launchable. **No APK was published.**
- Corrective commit `baf6e5a84be5332f1ece1f372dc5825e5b7ddebf` replaces
  the alias with the actual exported Flutter Activity as MAIN/LAUNCHER.
- The updated GitHub workflow must pass **all** Flutter tests, Android
  compilation, APK v2 signature, exact package ID, and `launchable-activity`
  matching the Flutter Activity BEFORE publishing any download link.
- The Android first-login form still uses the original Kotlin auth flow;
  the main CRM pages and guest landing page are Flutter. This is NOT a
  complete visual redesign of every administrator/login workflow.
- A physical Android installation, real account/license, capture/Contacts,
  and permission checks are outstanding. Do not label as production-ready
  or merge PR #1 solely on CI evidence.

## Installation protection
Do not uninstall the existing WA Lead Saver application, or the previous
QA version if it contains any useful test data. The new Flutter QA package
has an independent Room database and may have a different debug signature.
Wait for a **verified new release** before suggesting a browser download.
