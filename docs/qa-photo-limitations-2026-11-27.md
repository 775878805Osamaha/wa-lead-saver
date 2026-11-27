# QA photo tools: verified source limitations

- PhotoScanDialog gallery callback substitutes fixed sample numbers for the selected URI.
- CameraScannerView shutter calls onPhotoCaptured with fixed numbers, not recognized image text.
- The QA-only follow-up disables both simulated paths, removes the photo sample control, suppresses the group sample button, and displays a truthful Arabic notice. Manual input remains unchanged; no new data access, OCR, or background collection capability is introduced.
- Production behavior is intentionally not modified in this draft; the underlying legacy issue remains there.
- Admin accessibility labels and photo dialog labels are localized in the separate QA package.
- Font assets, real-device screenshots, and true image recognition remain unverified.
- This stacked draft must be tested before it joins PR #2. Do not cancel or overwrite the running build for the earlier head.
