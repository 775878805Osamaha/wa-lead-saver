# Startup crash investigation

User reported Android system crash dialog for WA Lead Saver - Flutter QA. The screenshot establishes process failure, not the exception or the exact installed APK. Do not label any speculative runtime change as a fix.

- Previous successful APK builds 46038680969 and 46036099226 checked signing and manifest but did not run Android startup.
- Reproduce both exact prior artifacts on a fresh Android API 33 x86_64 emulator; capture crash buffer, startup logs, foreground activity, UI hierarchy and screenshots.
- Require two cold starts to reach Flutter login gate before new APK upload. This is an emulator check, not proof on Samsung ARM hardware.
- PR #3 test run 46039484269 reported 36 passed / 1 failed: expected font family ThmanyahSans, actual Roboto. The public QA has no font assets. Test now checks actual platform fallback; RTL and other assertions unchanged. This does not explain the phone crash.
- No production, customer database, auth policy or runtime startup code changed pending stack-trace evidence.

## Reproduced root cause (run 46040739666)

The exact previous admin/settings APK from run 46038680969 installed successfully on API 33 x86_64 and crashed on the first start. Diagnostic artifact 15219434569, admin/crash.log, reports FATAL EXCEPTION: main, Unable to instantiate activity, caused by java.lang.ClassNotFoundException: Didn't find class "com.example.flutterbridge.WaFlutterHostActivity". The process was gone. The older auth APK was not reached in this run because the emulator runner stopped executing the multiline script after the first failing command.

The repository uses AGP 9.1.1 built-in Kotlin but registered the optional Kotlin bridge directory via java.srcDir. The official migration guide (https://developer.android.com/build/migrate-to-built-in-kotlin) explicitly requires AndroidSourceSet.kotlin for extra Kotlin sources. The fix registers the same directory using kotlin.directories, keeping the feature conditional. No change to authentication, production ID or data.

New gate uses dexdump to check the actual class definition in the compiled APK, not merely the manifest or string references. Existing two-cold-start Android gate remains mandatory. The previous reproduction script is now a single Bash invocation to collect both historical results. Candidate verification is pending; do not claim the Samsung phone is fixed until tested.
