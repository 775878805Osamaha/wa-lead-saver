# QA UI polish

Separate stacked branch from fc484809f4601d2c096d72f36aa2ae7673c34183 to preserve the running auth handoff check.
- Settings: preserve unsaved country-code edits on resume, explicit discard, current account status only and visible permission state. No new permission or collection capability.
- History: normalize status codes, separate failure and unknown colors from success.
- Customer cards: current CRM status visible without opening details; Unicode code point avatar.
- Dashboard: adaptive card heights for increased font sizes and basic validation of measured accuracy counts.
- App bar: constrain long labels. Adds two status unit tests and one settings widget test.
- Runtime gates unchanged. Production and customer records untouched. OCR, licensed fonts and device verification remain outstanding.

## Follow-up on actual CI failures

- Run 46044207479: 39 Flutter tests passed / 1 failed. The Settings resume regression revealed a pre-existing setState callback returning Future<AccountInfo> (also present in explicit refresh). The fix starts the future outside setState and assigns it inside a synchronous block. The original regression remains unchanged; added explicit refresh coverage.
- Run 46043804391: 37 Flutter tests passed, APK compiled and DEX check passed; Android startup check failed at am start -W Status: timeout, 11040 ms. Artifact 15221108985 contains an empty crash buffer, active process, resumed WaFlutterHostActivity and a still-visible splash screen. This is not proof of a runtime crash nor of a successful startup.
- Updated startup test allows further observation after the framework timeout, but still requires foreground QA activity, real login UI including recheck, two cold starts, and no process crash or ANR. A bounded 90-second observation is a test-harness allowance, not a performance acceptance threshold. UI dumps are fresh and individual dump commands bounded. Guest handoff still required before APK upload. No new APK is approved by these source edits alone.
- Existing PR #3 remains historical; corrections are on the stacked PR #4, without merging or changing production.
