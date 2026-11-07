# Qist Customer for Android

The independent `:customer` app shows real customer plans, payment history, receipts and retailer contact/payment details. The copied `:app` locker module is unchanged.

## Customer API

`CUSTOMER_API_URL` (environment / `.env`), `qist.customer.apiUrl` (Gradle), sets the HTTPS customer endpoint base URL. If only `API_URL` is configured, it is the API root and `/customer-app/` is appended. The default is `https://qist.restora.live/api/v1/customer-app/`.

| Action | Endpoint relative to that base |
| --- | --- |
| Sign in with phone and password | POST `auth/login` |
| Request a WhatsApp setup/recovery code | POST `auth/forgot-password` |
| Save first/new password with six-digit code | POST `auth/reset-password` |
| Load profile, plans, selected schedule, contact/payment methods and first payment page | GET `overview?contractId=...` |
| Load the next payment page on demand | GET `payments?contractId=...&cursor=...` |
| Revoke this session | POST `auth/logout` |

The backend must have WhatsApp authentication delivery configured for password setup/recovery. Ordinary sign-in does not send a code. Passwords must contain 8–128 characters. Demo code `123456` and sample financial data are removed.

## State and UI

The restored `dev` theme, fonts, shapes, spacing, cards, bottom navigation, and welcome screen are preserved. Auth adds password input and recovery using the existing controls. Additional states use existing text/cards/buttons; multiple-plan selection appears only for accounts with multiple plans. Refresh sits in the existing screen header.

`CustomerViewModel` owns requests across recomposition and rotation. One overview supplies all tabs and the first payment page; changing tabs does not request more data. Refresh and plan selection are explicit. Pagination requests only the next page and deduplicates receipts by server id. Mutations are guarded while in flight; failed reads preserve existing content. Unauthorized sessions clear private account content.

`HttpCustomerRepository` uses existing catalog dependencies (OkHttp, serialization JSON and coroutines). It cancels active HTTP calls when their coroutine is cancelled. Money remains `Long` integer paisa until formatting; schedule statuses, partial payments, fees, payment methods, receipt numbers and receiver names come from the backend. No equal-monthly-installment or paid-row-position assumptions are used.

Session tokens are encrypted with Android Keystore and stored under `noBackupFilesDir`. Passwords and codes stay only in memory and never enter saved-instance state. A valid session restores after reopening; the updated privacy notice requires new acceptance even if the old demo notice was accepted, and still appears before account data is loaded. Logout revokes the server session before removing the local token; network failures remain visible for retry.

Remote notifications have no customer backend endpoint or registration contract. The existing inbox shows an empty state and its settings switch is disabled instead of displaying fabricated events or promising delivery. No polling, FCM, device-management changes, or payment processing is added.

## Verification

```bash
./gradlew :customer:assembleDebug :customer:testDebugUnitTest :customer:lintDebug
```

Tests cover paisa/large values, partial and non-sequential payments, empty/completed accounts, receipt identity and metadata, bearer headers, API paths, pagination, server errors, session expiry, privacy restoration, duplicate actions, offline refresh and the recovery cooldown.

For this change the command was attempted but blocked before compilation: the environment cannot download `gradle-9.8.0-bin.zip` (`Network is unreachable`). Tests are added, not claimed as passing. `git diff --check` passed. No device/emulator or visual runtime checks have run.

Before distributing, run the command above and check auth/recovery, slow/offline requests, logout, multiple/empty plans, receipt sharing, rotation/reopening, Urdu/RTL, small screens and large font sizes. Review/update the hosted privacy policy and Google Play data disclosures for real authentication before release. This change does not publish or deploy anything.
