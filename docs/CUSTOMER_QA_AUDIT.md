# Customer app QA — 14 November 2026

The customer module was audited from `dev` commit `f21335462954fe23c77ce7d54075a8a62bfdb431` (including the proposed refresh lifecycle fix in PR #14). The backend's cancellation rules were also checked. No backend source, retailer records, payment records, passwords or account settings were changed during live checks.

## Verified results

| Check | Result |
| --- | --- |
| Android debug APK | Built successfully |
| Android instrumentation APK | Built successfully |
| Optimized release APK, including R8/resource shrinking | Built successfully; unsigned validation artifact, 2,384,492 bytes (about 2.27 MiB) |
| Debug and release lint | Passed; 0 errors and 15 warnings in each variant |
| Regular data/state tests | 81 passed |
| Combined data/state and Compose host tests | **99 passed: 81 data/state + 18 Compose UI tests**, 0 failures/errors |
| Additional native-graphics visual probes | Three rendered and reviewed: Home skeleton, Profile and custom logout dialog at 2x font scale; these are bounded component checks, not complete device-screen/accessibility coverage |
| Live HTTP smoke checks | Passed: password routing, login, account/plan/payment reads, unauthenticated access rejection, non-owned-plan rejection, logout and revoked-token rejection |
| Actual Kotlin HTTP repository against the live test account | Passed for all three plans, including financial reconciliation after the cancelled-plan fix; local session/cache cleared on logout |
| Source hygiene | `git diff --check` passed; credential scan found no supplied credentials in source files |

The Compose tests ran through **Robolectric 4.17 with native graphics on simulated API 35**, using the existing dependency catalog. They are not physical-device or successful emulator-instrumentation results. The same test sources compile into the instrumentation APK.

A software-rendered emulator was attempted with API 37 and API 35. This host has no KVM acceleration; device boot/install was not sufficiently reliable to count an instrumentation run as passed. No measured phone startup, FPS, jank, ANR, memory-retention or battery result is claimed.

The debug APK is 15,040,679 bytes. Its size must not be compared as a speed measurement. The unsigned release APK is for validation and was not signed, uploaded or deployed.

## Confirmed defects fixed

1. **Cancelled plans counted refunded money and showed an outstanding balance.** The backend cancellation operation voids the down-payment receipt and returns the phone to stock, while retaining the original agreement's `downPayment` field. The client previously counted that field as money still paid and treated the cancelled schedule as collectible. This failed reconciliation against the live cancelled test plan. Mapping now reports no retained advance, paid balance, next due amount or remaining balance for cancelled plans, while retaining the original sale price as history. Schedule rows say Cancelled rather than Overdue/Due soon. A mapping regression failed before the fix; mapping and UI regressions now pass.
2. **Corrupt session expiry suffixes could authenticate locally.** `SimpleDateFormat.parse(String)` accepted a valid future date followed by arbitrary trailing content. Parsing now requires consuming the complete stored timestamp; invalid or corrupt sessions are cleared. A failing regression reproduced the problem before the fix.

The proposed PR #14 refresh wrapper was also exercised: content updates do not create an idle indicator, interrupted pulls are cleared on pause/resume, pull position does not survive Activity state restoration, and a real swipe requests a refresh. These host UI tests pass. The reported Play Store flash has **not** been reproduced and confirmed fixed on a physical phone.

## Reproducible commands

Use the project's declared toolchain: JDK 25 for the Gradle daemon, Gradle 9.8.0, AGP 9.4.1, Kotlin 2.4.20 and the Android SDK required by the project. Dependency versions were not changed for this audit. In a restricted network, configure the build environment's HTTPS proxy and trusted CA appropriately; no proxy settings or local SDK/JDK paths were committed.

```sh
# Standard build, unit, lint and shrinker validation
./gradlew :customer:assembleDebug :customer:assembleDebugAndroidTest \
  :customer:testDebugUnitTest :customer:lintDebug
./gradlew :customer:assembleRelease :customer:lintRelease

# Optional device-free UI regression suite, plus the regular unit tests
./gradlew --init-script qa/customer-ui-host.init.gradle \
  :customer:testDebugUnitTest --no-configuration-cache

# When an Android device/emulator is ready
./gradlew :customer:connectedDebugAndroidTest
```

`qa/customer-ui-host.init.gradle` adds only host-test dependencies and resources. It leaves production APK dependencies unchanged. It uses AGP's Kotlin source directory configuration so the existing instrumentation test sources can also execute on the host, without duplicating test implementations. The simulated API/graphics mode is in `qa/robolectric-resources/robolectric.properties`.

## Coverage exercised

- **Authentication/session:** phone branching, OTP proof handling, verification lockout, duplicate submissions, consent, malformed success responses, expiry, cancellation, offline logout and account isolation.
- **Refresh:** cold account loading retains the skeleton, silent resume/push updates, manual-only indicator state, interrupted gestures, restoration, user actions superseding background reads and a burst of 1,000 notifications coalescing into one queued follow-up request.
- **Payments/plans:** paisa/large-amount formatting, partial allocations, cancellation/refund semantics, server-calendar due dates, payment paging/deduplication, receipt identity and retention when a refreshed first page omits an older open receipt.
- **UI/content:** all four navigation tabs, password masking and recreation, Unicode OTP input, verification lockout, lazy rendering/scrolling to the 200th row, shared dialog actions/busy/error state, 1.6x text in the one-action dialog, English/Urdu labels, retailer-free Profile and missing-phone contact actions in Help.
- **Offline/storage:** session-bound cache, wrong-plan isolation, malformed snapshots, storage failures, reconstruction, authorization errors never falling back to cached data and clearing local credentials/snapshots on logout.

## Live-account handling

The supplied test account was used only for authentication and bounded reads. The account had two active plans and one cancelled plan. Test sessions were logged out. No account deletion, payment creation/voiding, device lock command, password reset, OTP request, push-token registration or mark-read mutation was performed. Temporary account data and credential files were removed; no credentials, access tokens, customer identifiers or raw account responses were committed.

Warm requests through the actual Kotlin client were roughly 0.37–0.82 seconds on this host/network path, while cold requests were much slower and the five-second configuration check timed out. This includes the test environment's proxy/network behavior and is **not** a backend-only measurement or a phone performance benchmark. It does not justify changing production timeouts by itself.

## Remaining device checks

1. Reproduce the user's Play Store sequence: open, press Home, return, receive a notification, open an older receipt and switch plans. Capture frames/network events and confirm the top indicator appears only after a manual pull.
2. Test real FCM delivery/permission revocation and Google Play updates on a Play-enabled device; the host UI tests do not validate those services.
3. Check encrypted storage with the Android Keystore, process death, app recreation, offline recovery, export picker cancellation and actual installed dial/share/WhatsApp handlers.
4. Review small screens, maximum text/display scaling, Urdu/RTL, TalkBack and Switch Access. Automated semantics checks do not substitute for accessibility-service review.
5. Benchmark a release-like build on representative physical phones using Macrobenchmark/Perfetto, then examine startup/frame distributions, heap retention across repeated navigation and battery/network behavior. No performance dependency or speculative optimization was added without measurements.

The 15 lint warnings concern an inlined notification-permission constant, unused artwork, launcher-icon shape guidance and optional KTX style suggestions. They are recorded rather than suppressed; branding assets were not redesigned during QA. Gradle also reports future Gradle 10 deprecations; no toolchain upgrade was attempted.

## Primary references

- [Android activity/lifecycle testing](https://developer.android.com/guide/components/activities/testing)
- [Compose state restoration and common testing patterns](https://developer.android.com/develop/ui/compose/testing/common-patterns)
- [Compose accessibility testing](https://developer.android.com/develop/ui/compose/accessibility/testing)
- [Android performance measurement](https://developer.android.com/topic/performance/measuring-performance)
- [Macrobenchmark](https://developer.android.com/topic/performance/benchmarking/macrobenchmark-overview)
- [AGP built-in Kotlin source directories](https://developer.android.com/build/migrate-to-built-in-kotlin)
- [Android CLI](https://developer.android.com/tools/agents/android-cli)
