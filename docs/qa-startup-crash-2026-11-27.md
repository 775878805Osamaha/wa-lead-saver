# Startup crash investigation

User reported Android system crash dialog for WA Lead Saver - Flutter QA. The screenshot establishes process failure, not the exception or the exact installed APK. Do not label any speculative runtime change as a fix.

- Previous successful APK builds 46038680969 and 46036099226 checked signing and manifest but did not run Android startup.
- Reproduce both exact prior artifacts on a fresh Android API 33 x86_64 emulator; capture crash buffer, startup logs, foreground activity, UI hierarchy and screenshots.
- Require two cold starts to reach Flutter login gate before new APK upload. This is an emulator check, not proof on Samsung ARM hardware.
- PR #3 test run 46039484269 reported 36 passed / 1 failed: expected font family ThmanyahSans, actual Roboto. The public QA has no font assets. Test now checks actual platform fallback; RTL and other assertions unchanged. This does not explain the phone crash.
- No production, customer database, auth policy or runtime startup code changed pending stack-trace evidence.
