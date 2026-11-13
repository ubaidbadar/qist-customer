# Customer app QA audit — 14 November 2026

Baseline: `main` commit `ff1d864a2bbe537a092a193c7ebf5d975f15331f`.
Scope: the Android customer module, its HTTP repository, session/cache storage, ViewModel, lifecycle/push binding, account screens, payment/receipt content, and existing automated tests. Backend code was consulted for API shapes and limits; this change does not modify it.

Reinstatement: the QA changes below were restored after rollback, alongside the earlier custom-dialog width, spacing, icon sizing and 48dp actions adapted into the shared dialog component. The reference Home skeleton was already present and is unchanged. The 79-test result below is from the original audit of the identical JVM sources; this reinstatement verified source identity rather than rerunning that temporary JVM harness. A fresh Android build/test/lint attempt remains blocked by the Gradle download. The dialog styling still requires device review.

## Result and limits

**79 JVM tests passed.** The focused JVM run compiles the real repository, session/cache, notification-permission policy, account labels and ViewModel with Kotlin 2.4.20, the declared coroutine/network/serialization dependencies and Android/lifecycle classes. A temporary file supplies only the navigation enums normally declared beside the Compose shell. HTTP responses use OkHttp interceptors; no customer account or production API was mutated.

This is **not an Android build or device certification**. `./gradlew :customer:testDebugUnitTest :customer:lintDebug :customer:assembleDebug` stopped before compilation because the Gradle 9.8.0 distribution could not be downloaded (`Network is unreachable`). There is no Android SDK, emulator or connected `adb` device in this environment. All **13 instrumentation tests**, including five added content tests, remain unrun and uncompiled. Pixel layout, TalkBack, process recreation, real FCM/Google Play integration, startup timing, jank, memory and battery remain unverified.

Static checks passed: `git diff --check`, parsing all 42 main XML files, and resolving referenced drawable/font/mipmap resources. These checks do not replace Android resource compilation or lint.

## Confirmed defects and changes

| Priority | Reproduction or evidence | Result after the fix |
| --- | --- | --- |
| High | Hold a foreground-triggered overview request, then confirm logout or switch phones. The shared busy flag discarded the action. | Automatic reads keep the interface interactive; a user action cancels and supersedes the read. First-load and plan-switch skeletons remain. |
| High | Cancel the previous read while a user action starts. Its completion handler can otherwise reset another operation's state. | Operation ownership protects loading state; a test also uses immediate coroutine execution to exercise cancellation order. |
| High | Logout with a disconnected server, a 401 response or a 503 response. The local session and snapshot were retained on failure. | Remote revocation is attempted with a five-second request timeout, then local credentials and cached data are cleared even if the request fails or is cancelled. |
| Medium | Return a successful login response with a boolean, number or blank string as its token. Required text accepted these values. | Required response strings must be JSON strings with nonblank content before they can be persisted. |
| Medium | Disconnect during password login with no loaded account data. The error claimed saved data was being shown. | The message asks the user to reconnect; saved-data wording is used only when account data exists. |
| Medium | Open a cancelled plan whose historical schedule is still overdue. Home offered payment instructions and displayed the next amount. | Home shows cancelled status, retailer guidance and a receipts action. It makes no claim that retailer debts were forgiven. |
| Medium | Switch to Urdu: payment descriptions and methods remained English/server enum values; plan frequency was raw. | Shared label functions now serve Home, Payments, receipts (including export) and Plan. Unknown labels retain their supplied content. |
| Medium | Open an older paginated receipt, then replace overview data with the server's first payment page. The receipt no longer matched that list and the app fell back to the tab. | The open receipt retains its selected snapshot for the same plan/id while using updated receipt data when present. An instrumentation regression was added. |
| Low | Open a down-payment receipt or Home with no recorded payments. | Non-single-installment receipts use a Payment label; Home explains the empty recent-payments card. |

The first five new regression tests produced four failures against the baseline. Later tests independently reproduced the misleading offline-login message and malformed-token acceptance before their fixes. Final pass: 79 tests (65 existing plus 14 added). Content defects were identified by source inspection; their Compose assertions are added but remain unrun.

Local logout cannot guarantee remote revocation while disconnected; the server session may remain valid until expiry. The device no longer retains the local session or saved overview after logout succeeds locally.

## Automated coverage

| Area | Executed evidence |
| --- | --- |
| Auth routing | Registered password accounts, OTP-first accounts, unregistered numbers, single-use proof flow, consent boundary, duplicate submission and verification lockout. |
| Refresh and concurrency | Initial skeleton state, retained data, manual-only top indicator, user action priority, immediate cancellation, duplicate resume events and queued realtime updates. |
| Request amplification | A burst of 1,000 notifications during an active automatic read queues exactly one follow-up overview. This is a deterministic request-count test, not a throughput benchmark. |
| Session isolation | Expired sessions clear account state; an authentication failure never falls back to offline data; late push registration results cannot affect the next account. |
| Offline/cache | Encrypted-store interface behavior, session binding, wrong-plan isolation, corrupt snapshots, storage-full handling and reconstruction after repository recreation. Hardware-backed encryption itself requires a device. |
| HTTP errors | Invalid envelopes, HTML/non-JSON authentication failures, broken response streams, maintenance, retry timing, malformed token values and failed/cancelled logout. |
| Payments | Paisa formatting, partial allocations, server totals, receipt identity/reference/method, pagination deduplication and server-calendar due dates. |
| Content | English/Urdu payment labels, all supported payment methods, plan frequencies and preservation of unknown labels. |

## Device and release validation still required

Run these against a test backend/account, after installing the project's declared Android toolchain:

```sh
./gradlew :customer:testDebugUnitTest :customer:lintDebug :customer:assembleDebug
./gradlew :customer:connectedDebugAndroidTest
```

| Scenario | Steps and expected result |
| --- | --- |
| Cold/warm/hot launch | Fresh install, force-stop/reopen, Home-button/reopen and notification entry. First authenticated load shows the skeleton; resume preserves content without the top indicator. Signed-out startup does not show a fake dashboard. |
| Slow connection | Hold overview for 20 seconds; switch phone, open inbox or logout. User action remains responsive, cancelled responses cannot restore the previous plan/session. Manual pulls own the refresh indicator. |
| Connection failures | Toggle airplane mode during headers/body, restore connectivity, return 401/429/503 and malformed JSON. Preserve eligible saved data; expire unauthorised sessions; show usable retry/error states. |
| Notification delivery | Exercise foreground/background delivery, permission revocation, duplicate events and account switching. Verify only the signed-in account receives visible updates and registration requests are deduplicated. |
| Payments and receipts | Use partial, waived, completed, cancelled and no-payment plans. Load multiple payment pages, open an older receipt, then trigger a push/resume. Verify the selected receipt stays open while the underlying overview returns to the first payment page; returning to history may require loading older pages again. |
| Account screens | Retailer with/without phone/address; verify Profile has no retailer card, Help hides absent contact actions, FAQ expands down-to-up, native intent failures show feedback, deletion requires the shared confirmation. |
| External actions | Deny document access, cancel download, rotate while document picker is open, test no dial/share/view handler and both installed/missing WhatsApp. Verify saved and shared receipt content. |
| Small/large UI | 320dp and 360dp widths, landscape, display cutouts, keyboard, maximum font scale, English and Urdu/RTL. Check card alignment, sheet/dialog scrolling, button reachability and all bottom tabs. |
| Accessibility | TalkBack and Switch Access through login, OTP, password visibility, tabs, FAQ, sheets, dialogs and receipts. Check labels, focus order, selected state, error announcements and touch regions. |
| Process/lifecycle | Rotate and kill/recreate the process on auth, Home, phone picker, receipt and deletion dialog. Verify sensitive input is not restored and session/tab/navigation restoration is understandable. |
| Updates/maintenance | Test maintenance entry/retry, Play update download/failure/cancel/restart, and an installation outside Google Play. Confirm content remains usable when update checks are unavailable. |

## Performance measurements

The source already moves HTTP/storage work off the main thread, uses lazy schedule/history rows, collects screen data with lifecycle awareness and avoids periodic polling. This audit adds cancellation of obsolete automatic reads and verifies request coalescing. Those observations are not measured FPS, latency, memory, battery or ANR results.

Before release, measure a release-like build on a representative low-end physical phone and at least one other manufacturer/API level:

1. Use Macrobenchmark for cold/warm/hot startup and scrolling long schedules/payment history. Record time to initial display, time to fully usable content and frame timing distributions, with build/device/compilation mode and dataset.
2. Compare identical before/after runs, including offline/cache launch and notification entry. Avoid treating debug-build or host-emulator numbers as physical-device performance.
3. Capture a Perfetto/system trace when startup or frames regress; investigate main-thread disk/network work, layout/recomposition, GC and allocations before optimizing.
4. Use memory profiling across repeated tab/receipt/picker navigation, rotation and background/foreground cycles. Check retained Activities and unbounded data growth.
5. Measure network calls and battery behavior under notification bursts and repeated resume. Confirm there is no active background polling and no duplicated listeners.
6. Generate and benchmark Baseline Profiles only after the toolchain and representative journeys can run; no unmeasured profile or dependency upgrade was added here.

## Primary guidance consulted

- [Android coroutine best practices](https://developer.android.com/kotlin/coroutines/coroutines-best-practices): cooperative cancellation, lifecycle ownership, main-safe work and deterministic testing.
- [Android performance measurement](https://developer.android.com/topic/performance/measuring-performance): measure startup, frames, memory and other user-visible performance with appropriate tools.
- [Macrobenchmark](https://developer.android.com/topic/performance/benchmarking/macrobenchmark-overview): release-like target and device-level journey measurements.
- [Compose accessibility testing](https://developer.android.com/develop/ui/compose/accessibility/testing): combine automated semantics checks with manual accessibility-service testing.
- [Android architecture recommendations](https://developer.android.com/topic/architecture/recommendations): lifecycle-aware state collection and testable state/data ownership.
