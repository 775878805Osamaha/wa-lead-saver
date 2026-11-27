# QA UI polish

Separate stacked branch from fc484809f4601d2c096d72f36aa2ae7673c34183 to preserve the running auth handoff check.
- Settings: preserve unsaved country-code edits on resume, explicit discard, current account status only and visible permission state. No new permission or collection capability.
- History: normalize status codes, separate failure and unknown colors from success.
- Customer cards: current CRM status visible without opening details; Unicode code point avatar.
- Dashboard: adaptive card heights for increased font sizes and basic validation of measured accuracy counts.
- App bar: constrain long labels. Adds two status unit tests and one settings widget test.
- Runtime gates unchanged. Production and customer records untouched. OCR, licensed fonts and device verification remain outstanding.
