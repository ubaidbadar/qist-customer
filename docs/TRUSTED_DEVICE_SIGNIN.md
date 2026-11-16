# Trusted Locker sign-in and adaptive layouts

The security protocol and rollout plan live in [the backend plan](https://github.com/ubaidbadar/qist-backend/blob/main/docs/TRUSTED_DEVICE_AUTH.md).

The Customer app restores existing sessions first. Otherwise it can prove local presence through the explicitly bound, signature-verified Locker service. The backend creates either a normal customer session or a password-setup proof for a first login. On another phone, enter the registered number, then enter the displayed three-digit number in **Qist sign-in** on the financed phone and approve. Password and WhatsApp recovery remain available.

Nonce and password-setup proof stay in ViewModel memory. Binder receives only a request ID and nonce and returns a boolean. Only an HTTPS ticket exchange can create a session. Approval polling stops while the app is in the background, and explicit logout suppresses automatic sign-in on the next launch.

Text entry is scoped to each authentication screen; typing no longer invalidates CustomerShell. Timers update only their local readers. Screens observe state with lifecycle-aware collection. Android dp/sp and accessibility font scaling are preserved. Narrow windows use smaller outer spacing, money/detail cards stack on narrow windows or enlarged text, and large-window content is centered and bounded.

## Verify before production
Run `:customer:assembleDebug :customer:bundleRelease :customer:lintDebug :customer:lintRelease :customer:test`. Host layout tests use native Robolectric graphics for actual font measurements. Check 320dp and normal phone widths, tablets, landscape/split-screen, English/Urdu and enlarged text; verify password/recovery, logout, cancellation and first-login setup.

For a signed test APK, manually run **Customer CI** on the feature branch. It builds with the existing repository signing secrets and uploads a validation APK without publishing to Google Play. Compare the APK certificate with the financed phone's installed Locker; a Play upload certificate does not establish the Play-delivered APK certificate. On an enrolled Android 12 test phone, verify local Binder/Keystore sign-in, remote approval with and without notifications, kiosk navigation, then lock/unlock regression behavior. Do not factory reset or release a financed phone to test login.

The Cloudflare production Locker APK and Play production channel remain unchanged until this verification is complete.
