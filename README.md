# Qist Customer for Android

Customer Android app with the supplied Home, Plan, My phones and payment-method designs. The copied `:app` locker module is unchanged.

## Login and API

Phone first: `auth/start` checks for a non-cancelled plan at an active retailer. Existing passwords lead to Password; first-time accounts receive a WhatsApp OTP. `auth/verify-code` verifies that code before opening Create password. A five-minute single-use proof is exchanged at `auth/set-password` for a session. Forgotten passwords use `auth/request-code` and the same verify/set sequence. There is no demo code or sample financial data in production.

Configure `CUSTOMER_API_URL` or `qist.customer.apiUrl` with the full HTTPS customer endpoint base (default `https://qist.restora.live/api/v1/customer-app/`). If only `API_URL` is configured, it is the API root and `/customer-app/` is appended. The paired backend change must be available before distributing this client. WhatsApp uses the backend's existing provider settings; no secrets belong in the APK.

One authenticated overview supplies profile, owned phones, selected plan/schedule, retailer contacts/payment instructions and the first payment page. Switching tabs adds no requests. Selecting a phone makes one overview request; Load more requests only the next receipt page and deduplicates ids. Pull down to refresh; there is no refresh button. Money stays integer paisa until formatting, and installments use their actual statuses, partial payments and fees.

## UI and state

The reference palette, typography, spacing, cards, payment page/sheet and bottom navigation are preserved. Shared navigation renders under loading, content and error states: white background, blue active icon/label, muted inactive items and no selection pill. Full-screen payment/help/receipt pages keep their own back handling.

Skeleton cards appear only while signed-in content first loads or the selected phone changes. Pull-down refresh keeps loaded content visible. Failures end loading and provide useful feedback; initial failures offer retry. Buttons guard duplicate submissions. Password visibility uses the eye icon; passwords, OTPs and setup proofs stay only in memory. An app-scoped ViewModel owns request/state lifetime across rotation and HTTP calls cancel with their coroutine.

Sessions are encrypted using Android Keystore under `noBackupFilesDir`; the current session is restored after reopening and removed when expired/unauthorized. Updated privacy acceptance is required. Receipt exports use real receipt identifiers and metadata. Call/WhatsApp links use the retailer's provided number. Payment methods come from the selected retailer, with no invented fallback accounts. The backend has no customer notification API, so the inbox is empty and its switch is marked coming soon.

## Verification

```bash
./gradlew :customer:assembleDebug :customer:testDebugUnitTest :customer:lintDebug
```

23 JUnit tests passed in an isolated JVM harness using Kotlin 2.4.20 and the repository's declared library versions. The harness compiles the actual API repository, ViewModel and test sources, with the unchanged storage interface/navigation enums extracted from their Android source files. Coverage includes phone branching, OTP/password setup, duplicate submits, expiry, consent, offline refresh, resend failures, pagination, bearer paths, receipt metadata and money/date mapping. This does not compile Compose screens or the Android Keystore implementation.

The full Gradle command was attempted but failed before compilation because the Gradle 9.8.0 distribution could not be downloaded. Resource XML/reference checks and `git diff --check` pass. APK, device, Compose visual/layout, Keystore and live WhatsApp delivery checks remain outstanding. No signing/toolchain upgrades, merge or deployment are included. Review the hosted privacy policy and Play disclosures before distributing real authentication.

### Account and service states

About includes the installed app version, terms, privacy and a confirmation before deleting the app identity. Deletion calls `DELETE /customer-app/account`, clears this device’s encrypted session/snapshot and signs out; retailer contracts, customers, payments and device records remain. Exported receipts are not removed. A deleted customer can register again using their retailer-linked phone and WhatsApp verification.

The last successfully viewed overview is stored encrypted in Android Keystore-backed, backup-excluded storage. Offline fallback is limited to the same unexpired session and selected plan, only after a transport failure. Authentication errors and server errors never use cached fallback. Logout/account deletion clears it. Other phones and older payment pages are not preloaded. Pull down to refresh. Home distinguishes overdue, last acknowledged locked and fully paid plans; completed plans link to receipts. Home/Plan loading placeholders preserve the bottom navigation.

`GET /customer-app/config` supplies maintenance/minimum-version controls; every request includes `X-Customer-App-Version`. A startup check runs alongside session restoration; server error codes also activate the dedicated full-screen state. Retry rechecks configuration. Required updates open this app’s Google Play listing. The backend defaults both gates off; publish a usable Play release before setting a minimum version.
