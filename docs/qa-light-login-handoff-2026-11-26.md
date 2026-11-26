# WA Lead Saver — light native sign-in handoff (2026-11-26)

## Verified baseline
- Base development branch: feature/flutter-ui-and-history-performance-20261122 at 59b499f234ec6e55b67d2a77a22b05763ecfe1f7.
- The older Hybrid QA release wa-hybrid-qa-45190651567 launched Kotlin MainActivity. The new independent Flutter QA release wa-flutter-qa-45877784082 passed the launcher gate and its Actions job succeeded.
- Existing browser install: https://github.com/775878805Osamaha/wa-lead-saver/releases/download/wa-flutter-qa-45877784082/WA-Lead-Saver-Flutter-QA.apk
- Baseline native regression run: https://github.com/775878805Osamaha/wa-lead-saver/actions/runs/45877784086 (success).
- The verified QA release is unchanged and does not include the new work below.

## New draft implementation
- Scoped the existing Kotlin phone/email login, registration, and in-form account recovery UI to a light white/green palette only when launched by the Flutter authentication handoff.
- White fields, readable text, green buttons, light error/success cards and light Material3 color scheme use the Flutter color references. The ordinary native login retains its previous colors.
- No change to authentication logic, sessions, licenses, notification listener, database, permissions, application ID, or signing. Separate password-reset deep link screen and admin/native tools are not redesigned here.
- Added FlutterLoginAppearanceTest with palette and contrast assertions, and preservation of the ordinary native palette.
- Independent sRGB contrast calculation from the new source colors: ink on white 15.91:1, secondary on white 4.99:1, white on green 5.19:1, error on light error card 5.38:1, green on light success card 4.64:1.

## Verification status and next actions
- Source changes and test were committed to fix/flutter-qa-light-auth-20261126; not merged.
- New Android compile, Gradle test execution and physical-device screenshots are NOT yet verified. No new APK was built or published from this draft.
- Before merge: run native regression checks on this exact draft head, then test the Flutter QA launcher, sign-in/register/recovery modes, light input and error states, return to Flutter after valid sign-in, and denied/expired-license handling. Keep original production app installed; use test data only.
- Do not reuse the previous build's green checks as evidence for this draft.
- Font assets remain absent from the public QA build; do not copy licensed font files into a public repository without explicit distribution authorization.
