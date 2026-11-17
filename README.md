# Qist Customer for Android

Customer Android app with the supplied Home, Plan, My phones and payment-method designs. The copied `:app` locker module is unchanged.

## Login and API

Sign in using the enrolled Qist Locker on this phone, a short-lived code shown inside Locker on the financed phone, or an existing password. Same-phone sign-in asks for confirmation of the verified linked number before exchanging a session. A first-time customer must verify through Locker and then create a password. Entering a phone number alone never authorizes password creation. WhatsApp/SMS authentication is removed.

Configure `CUSTOMER_API_URL` or `qist.customer.apiUrl` with the full HTTPS customer endpoint base (default `https://qist.restora.live/api/v1/customer-app/`). If only `API_URL` is configured, it is the API root and `/customer-app/` is appended. The paired backend change must be available before distributing this client. Deploy the Locker code endpoints before distributing this client; no provider credentials belong in the APK.

One authenticated overview supplies profile, owned phones, selected plan/schedule, retailer contacts/payment instructions and the first payment page. Switching tabs adds no requests. Selecting a phone makes one overview request; Load more requests only the next receipt page and deduplicates ids. Pull down to refresh; there is no refresh button. Money stays integer paisa until formatting, and installments use their actual statuses, partial payments and fees.

## UI and state

The reference palette, typography, spacing, cards, payment page/sheet and bottom navigation are preserved. Shared navigation renders under loading, content and error states: white background, blue active icon/label, muted inactive items and no selection pill. Full-screen payment/help/receipt pages keep their own back handling.

Skeleton cards appear only while signed-in content first loads or the selected phone changes. Pull-down refresh keeps loaded content visible. Failures end loading and provide useful feedback; initial failures offer retry. Buttons guard duplicate submissions. Password visibility uses the eye icon; passwords, OTPs and setup proofs stay only in memory. An app-scoped ViewModel owns request/state lifetime across rotation and HTTP calls cancel with their coroutine.

Sessions are encrypted using Android Keystore under `noBackupFilesDir`; the current session is restored after reopening and removed when expired/unauthorized. Updated privacy acceptance is required. Receipt exports use real receipt identifiers and metadata. Call/WhatsApp links use the retailer's provided number. Payment methods come from the selected retailer, with no invented fallback accounts. The notification inbox receives account updates from the authenticated overview. Push alerts are opt-in and new events refresh the overview silently when the customer is signed in.

## Verification

```bash
./gradlew :customer:assembleDebug :customer:testDebugUnitTest :customer:lintDebug
```

23 JUnit tests passed in an isolated JVM harness using Kotlin 2.4.20 and the repository's declared library versions. The harness compiles the actual API repository, ViewModel and test sources, with the unchanged storage interface/navigation enums extracted from their Android source files. Coverage includes phone branching, OTP/password setup, duplicate submits, expiry, consent, offline refresh, resend failures, pagination, bearer paths, receipt metadata and money/date mapping. This does not compile Compose screens or the Android Keystore implementation.

The full Gradle command was attempted but failed before compilation because the Gradle 9.8.0 distribution could not be downloaded. Resource XML/reference checks and `git diff --check` pass. APK, device, Compose visual/layout, Keystore and live Locker delivery checks remain outstanding. No signing/toolchain upgrades, merge or deployment are included. Review the hosted privacy policy and Play disclosures before distributing real authentication.

### Account and service states

About includes the installed app version, terms, privacy and a confirmation before deleting the app identity. Deletion calls `DELETE /customer-app/account`, clears this device’s encrypted session/snapshot and signs out; retailer contracts, customers, payments and device records remain. Exported receipts are not removed. A deleted customer can register again using their retailer-linked phone and enrolled Locker verification.

The last successfully viewed overview is stored encrypted in Android Keystore-backed, backup-excluded storage. Offline fallback is limited to the same unexpired session and selected plan, only after a transport failure. Authentication errors and server errors never use cached fallback. Logout/account deletion clears it. Other phones and older payment pages are not preloaded. Pull down to refresh. Home distinguishes overdue, last acknowledged locked and fully paid plans; completed plans link to receipts. Home/Plan loading placeholders preserve the bottom navigation.

`GET /customer-app/config` supplies the maintenance state. Version updates use optional Google Play flexible updates: download while using the app, choose Later freely, and restart only after consent when the download finishes. About includes a manual check. There is no mandatory version gate. Play eligibility/signing/distribution determine whether an update can be offered; sideloaded builds continue working when Play cannot offer one.

### Native input and realtime notifications

The phone screen offers Google Play services Phone Number Hint automatically when blank and through a button. The user chooses a SIM-provided number; it is normalized and checked as a Pakistani mobile number, never submitted automatically. Manual entry remains available on devices without an available hint or Play services. Password fields expose current Autofill content types to the user's credential provider. The app does not read SMS, contacts, IMEI or other account data from the device. Locker codes support manual entry and user-initiated paste; they are not SMS Retriever codes.

The customer module uses its own Firebase configuration for `pk.qist.customer` from the existing Qist Firebase project. Remote alerts are opt-in, with Android 13+ permission handled after choosing Allow. FCM auto-init is disabled until enabled. A session-bound token registration is synchronized on sign-in, opt-in and rotation. Foreground push events refresh the authoritative overview, coalescing changes during in-flight requests; resume repairs missed delivery. There is no periodic client polling. Notification taps open the real inbox. The latest 30 inbox entries arrive in the existing overview request and can be opened or marked read. Disabling alerts unregisters the session; logout/deletion clears server registrations and hides local notifications. Push previews contain no balance, phone or customer details.

Empty payments, notifications and missing-phone states retain the navigation appropriate to their screen. Authentication expiry clears account data and opens the dedicated sign-in-again screen. Verification errors distinguish incorrect codes and server-directed lockout times; no fabricated cooldown is used. Clipboard success uses the shared snackbar. Receipt download uses Android's file picker and reports success only after writing completes, with View opening the saved URI; the message does not assume the user chose Downloads. Receipt sharing uses the Android share sheet.

Before release, deploy matching backend endpoints, create the new notification/push-token indexes and verify FCM using the matching service account. Test an eligible Play-installed build for flexible updates and real devices for SIM hints and Android notification permissions. Local unit tests cannot prove these external/native flows.

### Rendering and review boundaries

Dashboard owns one active page and delegates each main tab to its own screen. Home keeps its small fixed card layout. Payment history, schedule rows, phone choices and notifications use LazyColumn with stable identity keys/content types. Lazy panel sections keep continuous white cards and the existing corner/spacing tokens while composing rows near the viewport. The profile preference collector is mounted only on Profile. Shared action buttons, badges, card sections and back controls use the same design tokens.

The verification timer owns its seconds in a leaf composable; the form observes only whether the lockout has expired. Notification permissions are requested only after explicit opt-in. Resume respects permission revocation, and an already-applied token/unregister state is not sent again on each resume. Play update status distinguishes downloading, unavailable flexible updates and an up-to-date installation.

Review verification includes source compilation with the Compose compiler, compiler skippability reports, data/ViewModel regression tests, XML validation and backend type/lint/unit checks. A full Gradle APK/lint run and physical-device frame/recomposition measurements require a working Android build environment; compiler reports do not measure runtime frame rates or count actual device recompositions.

Device/UI regression tests live in `customer/src/androidTest`. Run `./gradlew :customer:connectedDebugAndroidTest` on a configured emulator/device. They cover password masking and visibility restoration, Unicode OTP entry, lockout enablement, bottom navigation, and composition of distant lazy rows only after scrolling. These tests must be reported separately from the JVM tests; source compilation alone does not mean they have run.

Customer-owned confirmation dialogs share `CustomerDialog`: the supplied rounded card, centered badge/title/body, pill actions, destructive styling, inline errors and a busy state that prevents dismissal/actions. Actions stack for narrow layouts or large text. Retailer contact, logout, account deletion and optional update prompts use it; missing-browser errors also use the custom card. Contact failures keep the card open with an inline error. Android permission requests, phone-number picking and Google Play's own update flow remain system-managed. Dialog behavior UI tests are in `CustomerDialogTest` and require an emulator/device; they are not established as passing by source review.
