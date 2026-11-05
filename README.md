# Qist Customer for Android

Connected customer app, package `pk.qist.customer`, version `0.2.0`. It uses `qist-backend` at `https://qist.restora.live/api/v1/customer-app/`. The copied `:app` locker remains separate and unchanged.

## Customer flow

1. A retailer registers the customer's correct phone number in the existing portal and records their contracts/payments.
2. In Android, accept the privacy notice, then use **First time / Forgot password**. A six-digit code is sent to the registered WhatsApp number.
3. Enter that code and choose a password. Ordinary sign-in then uses **phone and password**, without another code.
4. Home, Payments, Plan, Profile and the in-app inbox show the account's actual records. Choose among multiple plans on Home; use Refresh to reload authoritative server data. Payment history loads additional cursor pages only when requested.

There is no demo login or sample-record fallback. WhatsApp setup/reset requires the backend's Meta sender credentials and approved authentication template. Missing configuration produces a clear error; the app does not invent a code or pretend it was delivered. See [backend setup and API contracts](https://github.com/ubaidbadar/qist-backend/blob/feature/customer-app-api/docs/customer-app.md).

Retailer call/WhatsApp buttons open the dialer or a chat for the selected plan's branch number. Receipts use actual payment/receipt IDs and can be shared or exported as text. Money is integer paisa, including partial payments and late fees; installment schedules use actual row amounts, statuses and frequency. Cancelled plans do not request another payment. The app never locks/unlocks a device or processes a payment.

## Architecture

Sources: `customer/src/main/kotlin/pk/qist/customer/`.

| Path | Responsibility |
| --- | --- |
| `data/CustomerModels.kt` | Typed API data, exact money formatting and derived display values |
| `data/HttpCustomerApi.kt` | One OkHttp client, cancellable requests, bounded timeouts, JSON contracts; no request-body logging or automatic network retries |
| `data/EncryptedCustomerSession.kt` | Android Keystore AES-GCM token storage, with blocking storage/crypto off the main thread |
| `data/CustomerPreferences.kt` | Privacy/reminder/read-state preferences; initial load off the main thread |
| `ui/CustomerViewModel.kt` | Session restoration, authentication, recovery, loading/retry state, plan selection and paginated payment updates |
| `ui/CustomerShell.kt` | Lifecycle-aware state collection and top-level rendering |
| `ui/auth/`, `dashboard/`, `payments/`, `plan/`, `profile/`, `notifications/` | Feature UI, using shared tokens/components |
| `ui/components/`, `ui/theme/`, `ui/localization/` | Existing design language, skeletons, shared controls and English/Urdu selection |

A ViewModel is roughly a screen-level React state controller that survives rotation. StateFlow is its read-only observable store; `collectAsStateWithLifecycle` pauses UI collection when the Activity is not visible. State flows down and actions flow up. Screens reuse the same overview instead of independently fetching the same plan. Superseded plan requests are cancelled. Growing lists use stable receipt/row keys in LazyColumn.

Passwords and reset codes stay in memory only. The bearer token is encrypted on disk; no financial snapshot is persisted. Backup is disabled. Password reset revokes all server sessions; logout clears this device's token and attempts remote revocation even if the connection fails. Local preference and notice-read state remain until app storage is cleared.

In-app reminders are derived from fetched records and respect the local preference. They are **not background push notifications**. Phone status is the last acknowledgment received by the retailer/device system; refresh is required to obtain changes.

## Build and tests

```bash
./gradlew :customer:assembleDebug :customer:bundleRelease :customer:lintDebug :customer:lintRelease :customer:test
```

JUnit tests cover money units, part payments, date arithmetic, phone normalization, session restoration, wrong passwords, duplicate-submit prevention, expired sessions, network retry, cancellation of stale requests, recovery transitions and offline local sign-out. Robolectric/MockWebServer tests cover request paths, authorization, empty records, malformed responses and API failures.

Follow [.agent/skills/kotlin-android-excellence/SKILL.md](.agent/skills/kotlin-android-excellence/SKILL.md). No new library versions or DI/navigation framework are introduced; networking, lifecycle and test dependencies use the existing catalog.

Before rollout, deploy the matching backend API, configure and verify the Meta authentication sender/template, publish the matching public privacy policy, and update the Play Data safety disclosures for the connected app. The old offline-demo declaration does not describe this version. Branch publishing still follows [Play release instructions](docs/PLAY_STORE_RELEASE.md).

Manual verification still requires an Android device: first WhatsApp password setup, login/logout/restart, wrong/expired codes, actual retailer records, multiple plans, partial/overdue/cancelled plans, receipt export, denied/missing external apps, rotation, background/resume, poor connection, small screens, large fonts, RTL and TalkBack. Build/unit checks do not prove visual fidelity or handset delivery. Performance has not been benchmarked.
