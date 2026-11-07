# Qist Customer for Android

Qist Customer displays installment plans, payments, receipts, and retailer guidance. The `:customer` application is independent of the copied `:app` locker module. It does not lock or manage devices.

The current app is a local demonstration: enter a ten-digit mobile number starting with `3`, then use verification code `123456`. There is no SMS, backend login, remote push registration, analytics, or payment processing. Financial records and retailer contact controls are demo data.

## Build and check

Use the committed Gradle wrapper and the SDK/toolchain configured in the repository:

```bash
./gradlew :customer:assembleDebug :customer:bundleRelease :customer:lintDebug :customer:lintRelease :customer:test
```

The customer module currently contains no unit-test cases; a successful `:customer:test` task with `NO-SOURCE` is not runtime coverage. Customer CI uploads the debug APK and check reports. Release signing, branch-to-Play-track mapping, and publishing requirements are documented in [Play Store release instructions](docs/PLAY_STORE_RELEASE.md).

## Source layout

Kotlin sources are under `customer/src/main/kotlin/pk/qist/customer/`. Packages match their folders.

| Path | Responsibility |
| --- | --- |
| `MainActivity.kt` | Android entry point, edge-to-edge setup, theme |
| `data/` | Customer models, deterministic demo repository, persisted preferences |
| `ui/CustomerShell.kt` | Sign-in and top-level navigation state |
| `ui/auth/` | Welcome, phone/code controls, resend timer, privacy consent |
| `ui/dashboard/` | Dashboard navigation, overlays, Home cards |
| `ui/payments/` | Payment history, payment instructions, receipt sharing/export |
| `ui/plan/` | Plan facts and installment schedule |
| `ui/profile/` | Profile, language/logout dialogs, help, privacy policy |
| `ui/notifications/` | Demo notification prompt and inbox |
| `ui/components/` | Shared cards, controls, captions, detail rows, contact dialog |
| `ui/localization/` | Existing English/Urdu selection |
| `ui/theme/` | Inter typography, existing palette, shared corners and spacing |

Follow [.agent/skills/kotlin-android-excellence/SKILL.md](.agent/skills/kotlin-android-excellence/SKILL.md) for Android changes. Add reusable UI to the existing components and theme instead of creating per-screen design systems.

## State and data ownership

`CustomerShell` owns sign-in inputs, language, root navigation, selected tab, and payment-help visibility. `Dashboard` owns its notification prompt, unread flags, receipt selection, and profile subpage. Dialog and FAQ state stays with the screen that uses it. Existing `rememberSaveable` boundaries remain in place.

`rememberSaveable` is approximately React local state with Android screen-restoration support. `remember` is approximately `useMemo`/`useRef`, scoped to the current composition. State and callbacks flow down to reusable components; there is no new DI or state-management framework.

`CustomerPreferences` uses application context and preserves the existing private preference files, keys, defaults, and asynchronous `apply()` writes. It does not retain an Activity. Privacy acceptance and notification preference remain durable; transient UI state remains in Compose. The demo repository returns one read-only sample model shared by its consumers.

This restructure does not add real authentication, networking, device management, or notification delivery. The privacy disclosures must be reviewed before those features are introduced.

## Device verification

Build/lint checks do not verify visual fidelity or Android lifecycle behavior. Before merging or distributing, check:

1. Welcome → phone input → verification; local/international pasted numbers, invalid input, resend countdown, and keyboard.
2. Consent → privacy notice → back → agree or decline, including acceptance after restarting.
3. Home, Payments, Plan, and Profile; payment totals, receipts, back navigation, and the navy Home status bar.
4. Notification prompt, saved notification toggle, unread markers, and mark-all-read.
5. Help questions, retailer demo dialogs, privacy browser link, language selection/RTL, and logout confirmation.
6. Receipt share and download, including canceling the file picker.
7. Rotation, background/resume, process recreation, small displays, large font settings, and TalkBack.

Performance has not been measured. For a before/after comparison, use the same device, release-like build, and journey in Android Studio Profiler/System Trace. Check main-thread stalls, frame times, and retained Activities; use Layout Inspector for recomposition and layout checks.

## Customer design reference update

The design branch adds the phone-switch card on Home, Change action on My plan, the My phones bottom sheet (active/completed groups), and the payment instructions page plus compact Payment methods sheet. Open the compact sheet from the wallet icon beside the payment-method heading. Both payment presentations reuse one card component and copy placeholder demo account details. Call/WhatsApp actions retain the existing demo contact notice.

Phone selection is shared across Home, Payments, Plan, Profile and How to pay, and survives screen recreation. Pull down from the top of a main tab to reload the local demo collection without changing the selected phone/tab. There is no refresh button or network request.

The bottom navigation uses a white background, blue active icon/label, muted inactive items, and no selection pill, with system navigation insets. It is shared by all four main tabs; full-page payment/help/receipt views and modal sheets retain their own back/dismiss handling.

Current phone/code demo login is unchanged. An optional PasswordField component is prepared with an accessible eye/eye-off icon instead of Show/Hide text; it is not inserted into code login. No API integration, password authentication, dependency upgrades, merge or deployment is included.

Validation for this update: `git diff --check`, resource XML parsing, source/resource-reference checks and review of the changed navigation/callback paths. `./gradlew :customer:assembleDebug :customer:lintDebug` was attempted but failed before compilation because the Gradle distribution download is blocked (`Network is unreachable`). Compilation and device/visual verification remain outstanding; no runtime or screenshot-match claims are made.
