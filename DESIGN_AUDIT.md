# Qist customer app — design and release audit

Audit date: 7 November 2026. Status: **planning only; implementation not approved**.

## Scope and evidence

Repository: [ubaidbadar/qist-customer](https://github.com/ubaidbadar/qist-customer). Audited branch: `dev`, commit `2e18985555465cc5dd57f8fa36afe149a16e1013`, tree `8f63a6d438664164479ae4dc7d43124f739c2c6c`. The available branches were `dev`, `main`, `stage`, and `optimize/customer-structure`. Neither `customer-app` nor `ui-polish` existed. No remote branch, source file, setting, API, or locker file was changed. Local source copies were used for inspection only.

The supplied task brief and all 17 attached reference screens were read. References are 780×1688 raster images. The brief requests px=dp and text px=sp; apply its explicit token measurements. The reference viewport/export scale is not independently verified, so exact pixel comparison with a 360dp capture remains pending. Play Store reference screenshots were mentioned but were not separately attached in this workspace.

**Verification limits:** Java is present; `adb`, `emulator`, and the checked standard Android SDK directories are absent. No emulator screenshots, installation, Android build, Gradle tests, lint, Accessibility Scanner, startup timing, jank trace, APK/AAB size, merged-manifest inspection, or 16KB runtime verification was performed. Findings below are code-level evidence, not device-tested claims. Existing unit tests were inspected, not rerun. No screenshot-match claim is made.

## Main findings

1. **P0 release gates:** no current build/device/release artifact verification; account deletion applicability unresolved for OTP/password activation; backup configuration and the merged customer manifest need explicit review; publication policy must match actual behavior. These are unresolved gates, not proof of a current Play rejection.
2. **P1 behavior gaps:** consent appears after authentication; Decline returns to Welcome rather than closing; selection is not persisted; notification inbox has no data integration; no durable offline account/receipt cache; no per-app locale; logout cannot finish locally when the server request fails.
3. **P1 visual gaps:** card radius 22 vs 20, several palette mismatches, default Material typography rather than the required scale, 54dp buttons vs 52/56, OTP height 56 vs 60, inconsistent icon sizes/strokes, repeated one-off rows/buttons/sheets.
4. **Existing work worth preserving:** phone-first password/WhatsApp OTP routing, secure session storage, selected-contract API queries, cancellation-aware networking, pagination, password eye icons, pull-to-refresh with content retention, real receipt text sharing/saving, outline vector assets, licensed Inter fonts, and basic accessibility labels.

## Discovery

| Area | Observed implementation |
| --- | --- |
| Module | `:customer` in `customer/`, independent package `pk.qist.customer`; root also includes locker `:app` |
| Language/UI | Kotlin, Jetpack Compose, Material 3; no View layout screens found |
| Toolchain declared | AGP 9.4.1, Kotlin Compose plugin 2.4.20, Compose BOM 2026.09.00, Java source/target 17; artifact availability not resolved in this audit |
| Android | minSdk 24, compileSdk 37, targetSdk 37 |
| Core dependencies | Activity Compose 1.13.0; Lifecycle 2.11.0; coroutines 1.11.0; OkHttp 5.5.0; JSON serialization 1.11.0 |
| Entry | `MainActivity` calls `enableEdgeToEdge`, `QistTheme`, then `CustomerShell` |
| Architecture | `CustomerViewModel` owns a `StateFlow<CustomerState>`; `HttpCustomerRepository` owns HTTPS/session requests and JSON mapping; shell collects with lifecycle awareness |
| Navigation | Route/tab enums plus remembered booleans/strings and `BackHandler`; no Navigation Compose dependency |
| Loading | One overview populates Home/Plan/Payments/Profile; selected plan triggers another overview; payment pages use cursor fetching |
| Cache | In-memory dashboard in ViewModel; refresh retains data. No durable overview/receipt cache found |
| Preferences | SharedPreferences for privacy acceptance and notification preference; selected plan and app locale not persisted |
| Credentials | AES-GCM using Android Keystore; AtomicFile under `noBackupFilesDir`; expiry checked on restore; passwords not saved |
| Network | HTTPS required in repository; cleartext disabled in customer source manifest; no request-body logging found in inspected customer code |
| Permissions | Customer source manifest declares INTERNET only; no device-admin, overlay, SMS, storage, FCM service or POST_NOTIFICATIONS declaration. Transitive merged permissions remain unverified |
| Release | versionName 0.2.1; versionCode override/property/env/build-customer.txt fallback chain, default 1; R8 and resource shrinking enabled; signing conditional on supplied keystore |
| Tests | Three customer JVM test files cover mapping, HTTP repository, and ViewModel. No customer screenshot/UI/instrumentation tests or debug gallery found |
| Legacy documentation | `CUSTOMER_APP_PLAN.md` describes an old fake-data experiment and `customer-app`; it does not describe current API-backed `dev`. Do not execute its obsolete release/demo instructions |

## Status and severity notation

`P` = Present in inspected code; `T` = Partial; `M` = Missing; `W` = Wrong against the brief/reference; `—` = not applicable to that screen. Cells append severity, for example `T1` means Partial/P1. `P` does not mean runtime verified. P0 blocks release pending resolution/evidence; P1 affects core behavior, fidelity or accessibility; P2 is secondary polish. Shared rows apply to every screen and avoid pretending static screens need network skeletons.

## Screen × checklist matrix

### A–B. Design and shared components

Columns cover A: palette, geometry/spacing, typography, icons/status; B: reuse and complete component states. Detailed inventories follow.

| Screen | Colors | Shapes/spacing | Type | Icons/status | Shared components | Component states |
| --- | --- | --- | --- | --- | --- | --- |
| 01 Consent | T1 | T1 | T1 | T1 | T1 | T1 |
| 02 Welcome | T1 | T1 | T1 | T1 | T1 | T2 |
| 03 Phone | T1 | T1 | T1 | W1 | T1 | T1 |
| 04 Verify | T1 | W1 | T1 | W1 | T1 | T1 |
| 05 Notification sheet | T1 | T1 | T1 | W1 | T1 | M1 |
| 06 Home | T1 | T1 | T1 | T1 | T1 | T1 |
| 07 Notifications | T1 | T1 | T1 | T1 | T1 | M1 |
| 08 Payments | T1 | T1 | T1 | T1 | T1 | T1 |
| 09 Receipt | T1 | T1 | T1 | T1 | T1 | T1 |
| 10 Plan | T1 | T1 | T1 | T1 | T1 | T1 |
| 11 How to pay | T1 | T1 | T1 | T1 | T1 | T1 |
| 12 My phones | T1 | T1 | T1 | W1 | T1 | T1 |
| 13 Profile | T1 | T1 | T1 | T1 | T1 | T1 |
| 14 Language | T1 | T1 | T1 | T1 | T1 | T1 |
| 15 Help | T1 | T1 | T1 | W1 | T1 | T1 |
| 16 Privacy | T1 | T1 | T1 | T1 | T1 | T1 |
| 17 Logout | W1 | T1 | T1 | W1 | T1 | M1 |

### C. Loading, feedback and data states

“Refresh” includes gesture, accessible alternative, empty/error eligibility, no duplicate request, and stale-resume behavior. “Action” includes spinner/disable/double-tap protection. Receipt, help and static sheets inherit data where applicable; no independent fetch is invented.

| Screen | Skeleton/layout/delay/motion | Refresh | Empty | Error/validation/401 | Offline/cache | Action loading | Snackbar/copy feedback |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 01 Consent | — | — | — | — | — | T1 | — |
| 02 Welcome | — | — | — | — | — | T2 | — |
| 03 Phone | — | — | — | T1 | M1 | T1 | — |
| 04 Verify | — | — | — | T1 | M1 | T1 | — |
| 05 Notification sheet | — | — | — | M1 | — | M1 | M1 |
| 06 Home | T1 | T1 | T1 | T1 | T1 | T1 | M1 |
| 07 Notifications | M1 | M1 | T1 | M1 | M1 | M1 | M1 |
| 08 Payments | T1 | T1 | T1 | T1 | T1 | T1 | M1 |
| 09 Receipt | M1 | — | M1 | T1 | T1 | M1 | M1 |
| 10 Plan | T1 | T1 | T1 | T1 | T1 | T1 | M1 |
| 11 How to pay | M1 | — | T1 | T1 | T1 | T1 | W1 |
| 12 My phones | M1 | M1 | M1 | M1 | T1 | T1 | M1 |
| 13 Profile | T1 | — | — | T1 | T1 | T1 | M1 |
| 14 Language | — | — | — | — | P | T2 | M2 |
| 15 Help | — | — | — | T1 | T1 | T1 | M1 |
| 16 Privacy | — | — | — | T1 | P | T1 | M2 |
| 17 Logout | — | — | — | T1 | W1 | M1 | M1 |

`CustomerLoading` is generic, static, immediate and tab-based. It does not mirror Receipt/How-to-pay/My-phones/Notifications; switching plan replaces the screen tree. No 300ms gate or shimmer exists. Static placeholders already avoid animation but do not implement the requested reduced-motion-aware shimmer. Refresh keeps old content and ViewModel `busy` blocks duplicate work. It also wraps Profile unintentionally, misses Notifications/My phones and the full-screen load-error branch, has no refresh accessibility action or resume freshness threshold. Errors appear in a yellow inline surface, not a Retry snackbar. Offline data survives only while the ViewModel survives. Clipboard toast is unconditional, duplicating Android 13+ feedback.

### D. Feature behavior matrix

| Screen | Primary feature behavior | Selected-plan consistency | Localization/formatting | Navigation/restoration | Lists/pagination |
| --- | --- | --- | --- | --- | --- |
| 01 Consent | W1 | — | T1 | W1 | — |
| 02 Welcome | P | — | T1 | T1 | — |
| 03 Phone | T1 | — | T1 | T1 | — |
| 04 Verify | T1 | — | T1 | T1 | — |
| 05 Notification sheet | W1 | — | T1 | T1 | — |
| 06 Home | T1 | T1 | T1 | T1 | T2 |
| 07 Notifications | M1 | — | T1 | T1 | M1 |
| 08 Payments | T1 | T1 | T1 | T1 | T1 |
| 09 Receipt | T1 | P | T1 | T1 | — |
| 10 Plan | T1 | T1 | T1 | T1 | T1 |
| 11 How to pay | T1 | P | T1 | T1 | T2 |
| 12 My phones | T1 | T1 | T1 | T1 | T1 |
| 13 Profile | W1 | P | T1 | T1 | — |
| 14 Language | T1 | — | W1 | T1 | — |
| 15 Help | T1 | P | T1 | T1 | T2 |
| 16 Privacy | T1 | — | W1 | T1 | T2 |
| 17 Logout | T1 | — | T1 | T1 | — |

| Checklist item | Evidence and gap | Priority |
| --- | --- | --- |
| Auth routing | Preserves phone → password when present, otherwise WhatsApp OTP → password setup; no-plan guidance exists. Password/reset routes are additional current behavior, not to be removed for old screenshots | P1 preserve |
| Phone validation/paste | Normalizes Unicode digits, +92/0, limits to ten digits and checks `3[0-9]{9}`. `0092…` becomes an incorrect value. Shell always passes `error=false`; field border never reflects validation. No IME submit/autofill hints | P1 |
| OTP | One underlying numeric field handles paste and deletion; six visual cells. No explicit autofill API/hints, keyboard submit, wrong/expired/rate-limit state types. Server text displayed; timer exists but lifecycle behavior needs testing | P1 |
| OTP channel | Current copy/API use WhatsApp. SMS Retriever/User Consent cannot read WhatsApp. Adding SMS delivery requires a separate product/backend decision; do not add READ_SMS | P1 decision |
| Consent | Checkbox defaults false; action disabled, acceptance stored, policy accessible. Gate occurs after authentication; decline clears session and returns Welcome rather than finishing Activity | P1 decision |
| Sessions/logout | Encrypted token restores; authenticated 401 clears session/data and goes Phone. Logout clears local session only after successful server response. No cache/push/selection cleanup owner; no push token exists currently | P1 |
| Permission notifications | Rationale UI exists but `showPrompt` is never set true in Dashboard. Allow callback writes preference only. Profile deliberately shows a disabled coming-soon switch. No runtime permission, channels, FCM, data/read endpoints, or deep links | P1 decision |
| My phones | Data-backed Active/Completed/Cancelled groups; radio semantics and keyed composition. Selected ID exists only in dashboard; no persistent choice/fallback policy. Backend owns default order; nearest-due default cannot be verified from customer code | P1 |
| Cross-screen plan | Home/Plan/Payments/Receipt/How-to-pay use the same dashboard; overview uses `contractId`. Payments lacks the reference phone selector. Selection failure leaves old overview, but sheet closes before confirmation | P1 |
| How to pay | Data-backed methods, per-method copy, dialer and WhatsApp intents; missing-handler exception handled. Extra contact confirmation/dialog and payment-methods sheet differ from reference. Payment-proof opens chat without prefilled proof text/attachment; do not claim automatic proof upload | P1 |
| Receipt | Real text share via Android chooser; `CreateDocument("text/plain")` saves `qist-receipt-<number>.txt`, no storage permission. Not MediaStore, no spinner/success, share handler lacks explicit failure handling, receipt only in memory; export type/path needs approval | P1 |
| Language | `LocalUrdu` Boolean + layout RTL; rememberSaveable survives Activity recreation only. No per-app locale, Urdu resource bundle, plurals or Nastaliq font. Policy body and API-derived labels remain English; arrow text/glyphs vary | P1 |
| Formatting | `formatAmount` centralizes paisa → decimal; screens still assemble labels separately. Date formatter UTC/date-only, no centralized relative-day “Today”, days frozen from server `today`, no midnight/resume refresh | P1 |
| Back/restore | Custom BackHandlers provide tabs → Home, pushed pages → tabs and modal closure. No predictive progress animation; tab scroll state created with `key(tab,id)` is discarded on tab removal. Route/selection not SavedStateHandle-backed; no process-death flow test | P1 |
| Lists | Payments cursor loading and ID dedup exist; rendered with eager `forEachIndexed` in scrolling Column, no row keys. Schedule eager and next-unpaid search repeated per row. Phone rows keyed, not lazy; inbox pagination unavailable | P1 |

### E–H. Platform, accessibility, security/performance and tests

| Screen | Insets/system bars/IME | Adaptive 320dp–tablet/200% | Semantics/focus/targets | Contrast/non-color meaning | Security/privacy | Perf/release evidence | Automated UI/screenshot QA |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 01 Consent | T1 | T1 | T1 | T1 | T1 | M0 | M1 |
| 02 Welcome | T1 | T1 | T1 | T1 | P | M0 | M1 |
| 03 Phone | T1 | T1 | T1 | T1 | T1 | M0 | M1 |
| 04 Verify | T1 | T1 | T1 | T1 | T1 | M0 | M1 |
| 05 Notification sheet | T1 | T1 | T1 | T1 | T1 | M0 | M1 |
| 06 Home | T1 | T1 | T1 | T1 | T1 | M0 | M1 |
| 07 Notifications | T1 | T1 | T1 | T1 | T1 | M0 | M1 |
| 08 Payments | T1 | T1 | T1 | T1 | T1 | M0 | M1 |
| 09 Receipt | T1 | T1 | T1 | T1 | T1 | M0 | M1 |
| 10 Plan | T1 | T1 | T1 | T1 | T1 | M0 | M1 |
| 11 How to pay | T1 | T1 | T1 | T1 | T1 | M0 | M1 |
| 12 My phones | T1 | T1 | T1 | T1 | T1 | M0 | M1 |
| 13 Profile | T1 | T1 | T1 | T1 | T1 | M0 | M1 |
| 14 Language | T1 | T1 | T1 | T1 | T1 | M0 | M1 |
| 15 Help | T1 | T1 | T1 | T1 | T1 | M0 | M1 |
| 16 Privacy | T1 | T1 | T1 | T1 | T1 | M0 | M1 |
| 17 Logout | T1 | T1 | T1 | T1 | T1 | M0 | M1 |

| Checklist coverage | Result/evidence | Severity |
| --- | --- | --- |
| E edge-to-edge/status/nav/cutouts/IME | enableEdgeToEdge and systemBarsPadding present, auth uses imePadding. Scaffold flips status icons only for Home; Welcome starts with light-status-bar style despite navy. Nav-bar contrast, 3-button mode, keyboard and modal insets unverified | P1 |
| E splash/launcher | Adaptive and monochrome assets/wrappers exist. No dedicated SplashScreen theme/API, navy splash or customer core-splashscreen dependency | P1 |
| E orientation/tablets/configuration | No portrait lock; no 600dp content width cap. Scrollable layouts partly flexible; fixed OTP row, consent footer row, paired buttons and receipt facts need 320dp/large-font checks. No locale persistence or full restore | P1 |
| E interaction | Material ripple mostly inherited. Custom haptics absent (not inherently a defect); focus/pressed/disabled variants not centralized | P2 |
| F contrast | White/brand-blue 7.22:1; requested #04122E on #22D3EE 10.27:1; muted #5F6E94 on #F5F7FC 4.73:1. Requested #C4CFE6 on white only 1.57:1: if this border alone identifies a field it fails 3:1 UI contrast. Retain screenshot appearance only with another adequate identifying cue, or approve stronger border. Runtime compositing not tested | P1 decision |
| F semantics | Back/eye/bell labels and language/phone radio roles exist; skeleton live-region label, inline auth error live-region present. No common heading semantics, expanded FAQ state, refresh action/result announcements or focus-order tests | P1 |
| F targets/meaning | Many buttons explicitly >=48dp; some rows depend on content height. Status badge has icon + text; schedule status is raw English and overdue uses warning styling; Locked missing. No TalkBack/keyboard/scanner proof | P1 |
| G storage/traffic/logs | Keystore token + noBackup and HTTPS present. No customer analytics/logging SDK seen. Source manifest has no explicit allowBackup or extraction exclusions for preferences/future caches; actual release merged manifest unverified | P0 review |
| G R8/exports/permissions | Minify+shrink enabled; launcher exported is expected. No customer admin/overlay/SMS/storage permissions. Conditional release signing can leave unsigned output; merged manifest, dependency keep rules, release logs and signature must be checked | P0 evidence |
| G performance | Lifecycle collection and cancellable IO present; no StrictMode/debug gallery/customer baseline profile or benchmarks. Eager lists and repeated schedule search are code-level candidates; no measured startup, jank, memory or artifact size | P1/P2 |
| G crash/analytics | Absent; keep absent unless specifically approved | Present |
| G privacy/version/Data safety | Inline policy URL, partially English policy, unverified public URL and deletion applicability. Version source chain can default to 1; compare actual release code with last accepted Play code before upload | P0 release review |
| H unit/UI/screenshots/manual QA | Existing unit tests inspected, none run here. No customer screenshot/UI suite, component previews/gallery or QA_CHECKLIST.md. No scanner/lint/critical-flow execution. Existing tests cover some auth/dedup/401/refresh logic, not complete formatter/error/RTL/restore requirements | P1 |

## Screen reference differences

| Screen | Concrete difference to review |
| --- | --- |
| Consent | Reference first screen; current gate after login. Reference optional-notification benefits replaced with honest unavailable copy. Footer can crowd at large fonts |
| Welcome | Theme typography and cyan-on-navy text differ from requested exact tokens; status-bar icons need per-screen check |
| Phone | Current Continue and phone-first password routing replace screenshot Send code. Preserve routing; approve copy rather than restoring misleading Send code |
| Verify | Current Check your WhatsApp rather than generic Verify your number; 56dp cells, flexible widths vs target 48×60. Password and setup screens lack supplied designs |
| Notification permission | Reference promises reminders; current app has no push capability and sheet is unreachable. Requires feature decision, not cosmetic wiring |
| Home | Core cards/data exist; amounts/types/card radius mismatch. Lock-state data parsed but not shown as Locked badge; recent empty payments panel lacks requested state |
| Notifications | Current empty explanatory text instead of populated feed. Do not fabricate notifications from sample screenshots |
| Payments | No phone selector under heading; real totals/history/pagination exist; screen eager-renders rows |
| Receipt | Current reference row is additional; share is text, save is Android document picker. No icons on action buttons matching screenshot and no export progress/success |
| Plan | Current plan length uses installment count + raw frequency, not fixed months (preserve actual frequency). Raw status labels and warning overdue treatment need presentation mapping |
| How to pay | Additional payment-methods sheet/icon and contact dialog not in reference. Data-backed methods present, missing-method guidance plain |
| My phones | Extra Cancelled section represents real contract state. No empty-section guidance, refresh or remembered choice |
| Profile | Initial avatar single letter vs reference initial placeholder; contact buttons lack reference icons; disabled coming-soon notification control is intentional current limitation |
| Language | Visual sheet/radio selection present. Apply changes local Boolean only; no persistent Android locale, Nastaliq or resource-backed localization |
| Help | FAQ arrow uses text glyph, no expanded semantics; footer lacks reference support email. Do not invent support contact copy |
| Privacy | Actual current policy text should replace placeholder reference text; it remains English in Urdu mode. Public URL inline and not availability-verified |
| Logout | Current password-based login copy correctly differs from old code-only screenshot; destructive color off target; dialog closes before async logout, no action spinner |

## Token inventory and proposed corrections

All hex Compose colors currently live in `ui/theme/QistTheme.kt`; named White/Transparent uses and alpha values still appear elsewhere. Resource vectors contain additional hardcoded stroke/fill colors.

| Existing token | Current | Required/action |
| --- | --- | --- |
| Navy / DeepSurface / Muted / Blue | #0A1A3F / #11275A / #A9B8DD / #2F3FE0 | Match |
| Cyan | #22D3EE | Rename semantic accent cyan; add distinct cyan #06B6D4 (currently only gradient start) |
| BrandGradientStart | #06B6D4 | Match; logo gradient only |
| Canvas / BlueBackground / Coral | #F5F7FC / #EEF1FF / #FFA383 | Match |
| PaidGreen / PaidBackground | #08674D / #DDF6EC | Success foreground → #0B6B49; background match |
| Divider | #E4E9F4 | → #E6EBF5 |
| DueAmber / DueBackground | #795000 / #FFF1D5 | → #7A4E00 / #FFF4DB |
| Destructive / DestructiveBackground | #AA381D / #FFE8E0 | Split destructive button #B3401E from danger text #A23A1B; background #FFE9E2 |
| InputBorder | #BFCBE4 | Requested #C4CFE6, subject to contrast decision |
| onSurfaceVariant literal | #4A5A80 | Correct value; name secondary text token |
| Missing semantic colors | — | Card #FFF, label #26365F, muted/placeholder #5F6E94, accent text #04122E, neutral #E9ECF5/#26365F, disabled #C9D1EA/#5F6E94, success-on-dark #34D399, dark border white 12% |
| Shapes | Card22, Dialog28, Sheet28, Input14, Option16, CodeCell13, badge3 dp | Card20; keep intended 14/16 input/small-card variants; sheet/dialog28. Name OTP/decorative badge tokens explicitly |
| Spacing | Screen20, Auth24, Section16, Item12 dp | Existing four match; card20/compact16 and other approved geometry need semantic tokens |
| Typography | Inter 400/500/600/700 but default Material sizes/line heights; not all roles overridden | Tab28/34, pushed24/30, amount34/42, section17, body15/22, secondary14/20, caption13; clarify unspecified line heights in previews |
| Dimensions | Button54; auth54; consent56; OTP height56/weighted widths; nav default; badge icons13/19/30/32 | App52, auth56, OTP48×60 where space permits, nav76 content intent plus safe inset decision; icon16/18/20/22/24, stroke2; circles36/40/44 with documented logo/hero exceptions |
| Elevation/opacity | NavigationBar tonalElevation0; default Material button/sheet/dialog elevation; arbitrary blue alpha .08, scrim .60/.72 | Centralize explicit elevation and scrim roles; defaults need visual approval, not arbitrary new shadows |

### Component inventory

| Required component family | Existing implementation / duplicates | Missing states or consolidation |
| --- | --- | --- |
| Button | Pill, SecondaryAction; AuthScreen, consent, HowToPay, profile/help, dialog and copy buttons inline | One variants API; pressed/focus, loading, disabled; preserve platform minimum targets |
| Text/phone/OTP field | PhoneField, PasswordField, CodeField in Authentication | Shared helper/error/disabled/focus/autofill/IME; semantic OTP grouping |
| Card/list row | Panel; DetailRow, SettingsRow, ConsentItem, payment rows, notification cards, plan facts | Shared compact/standard card/row; retain genuinely different content slots |
| Icon circle | ScreenBadge, Badge, NoticeIcon, Home bell/avatar/logout custom boxes | One scalable component; decorative hero/logo documented |
| Status chip | PlanStatusBadge; schedule status inline | All seven requested statuses incl Locked/Paid/Upcoming; 28dp minimum content height, text expansion |
| Top bar / bottom nav | CustomerBackButton, PaymentPage, ProfileInformation/inbox/auth custom headers; CustomerScaffold nav | One top-bar structure and nav geometry/state policy |
| Sheet/dialog | NotificationPrompt, LanguagePicker, PhonePicker, PaymentMethodsSheet; custom LogoutConfirmation, Material contact dialog | Shared surface/insets/scrim; action busy/error consistency |
| Progress/section/banner | Home linear progress; inline headings/PaymentNotice/yellow error surface | Theme progress, SectionHeader, InfoBanner |
| Empty/error/offline | Generic CustomerLoadError; plain empty texts; no OfflineBanner | Reusable approved messages/actions; do not invent new screens |
| Snackbar/skeleton/refresh/copy | No snackbar; SkeletonLine/CustomerLoading; Dashboard PullToRefreshBox; inline clipboard | Single wrappers; screen skeletons; OS-aware feedback; accessible refresh |
| Gallery/previews | None found | Debug-only gallery and Compose previews, absent from release |

### Icon asset inventory

All non-launcher icons are local vector XML; no downloaded icon library is used. Exact sizes/strokes/fills and per-file literal counts are included in the generated inventory appendix below. Typical source vector viewport is 24×24, stroke 1.8, round caps/joins. Some stroke colors are black, others navy; runtime tint often masks this. Back in auth and FAQ arrows are text glyphs. These should join the outline set. Filled keyhole/launcher artwork is a deliberate logo exception, not a UI icon to convert to outline. Directional vectors require RTL behavior; do not mirror digits, receipt numbers or phone/account strings.

## Android/Play requirements checked against current official sources

1. **Target API:** from 31 August 2026 new phone apps/updates require API36+; the stated extension ended 1 November 2026. Declared target37 meets the numeric threshold; only a built manifest proves the delivered target. [Play target requirements](https://support.google.com/googleplay/android-developer/answer/11926878) and [Android target guidance](https://developer.android.com/google/play/requirements/target-sdk).
2. **16KB:** current compatibility guidance says API35+ apps on Play must support 16KB on 64-bit devices, with incompatible updates blocked from 1 February 2027. Older 2025 announcements use an earlier date; the current guidance is used here. Java/Kotlin-only apps including their dependencies support it by default; inspect actual native libraries before claiming compliance. [Current 16KB guidance](https://developer.android.com/guide/practices/page-sizes). No NDK source found in customer; transitive release `.so` contents remain unknown.
3. **Edge-to-edge/back:** for target36+ on Android16+, edge-to-edge cannot be opted out and predictive system animations are default. AndroidX BackHandler is preferable to deprecated Activity back APIs but does not itself prove in-app progress animations. [Android16 behavior changes](https://developer.android.com/about/versions/16/behavior-changes-16).
4. **Target37 adaptive behavior:** Android17 removes the target36 opt-out for ignoring large-screen orientation/resizability constraints. Recommend adaptive landscape/tablet layout, not portrait locking as a workaround. [Android17 changes](https://developer.android.com/about/versions/17/behavior-changes-17).
5. **Material3:** retain `PullToRefreshBox` with explicit blue-on-white indicator and state; use Scaffold SnackbarHost for Retry results; remove modal sheets from composition after hiding, with state/cancellation handled. [Pull-to-refresh](https://developer.android.com/develop/ui/compose/components/pull-to-refresh), [snackbar](https://developer.android.com/develop/ui/compose/components/snackbar), [bottom sheets](https://developer.android.com/develop/ui/compose/components/bottom-sheets).
6. **Data safety/account deletion:** declarations must include SDK/backend data handling. In-app account creation requires an in-app deletion path plus a web resource; retailer-created contract vs customer OTP/password account activation needs a scoped decision before release. [Data safety](https://support.google.com/googleplay/android-developer/answer/10787469), [deletion requirements](https://support.google.com/googleplay/android-developer/answer/13327111). Do not silently add a deletion API or screen.

### Draft Data safety review table (not submitted)

The customer source proves transmitted auth fields and downloaded account records, not every backend retention/sharing practice. Downloading existing retailer data alone is not evidence it is newly collected from the device. Final categories require backend/provider review.

| Data | Observed flow | Draft declaration/review |
| --- | --- | --- |
| Phone number | Sent to auth/start, OTP/verify, password/login endpoints | Personal info / phone; account management/functionality; required |
| Password, OTP, setup/session tokens | HTTPS authentication; encrypted session token locally | Auth secrets transmitted; do not log; map account identifiers/security handling accurately in Console |
| Name, phone, plan/device label/status, schedule | Retrieved in overview; customer code does not edit these | Existing account data; check backend collection/user-ID linkage and retention |
| Payment amounts/receipts | Retrieved; user can export/share text | Financial/payment or purchase-history category depends on service data definition; user-directed sharing reviewed under policy exceptions |
| WhatsApp OTP | Backend/provider delivers to registered number | Review Meta/service-provider handling; cannot infer “not shared” from Android code |
| Retailer chat/call | User opens dialer or wa.me URL | External action; not CALL_PHONE/contacts/SMS collection |
| Photos/contacts/location/SMS/device management | No customer permission/API found | No such collection evidenced in customer; do not copy locker declarations |
| Analytics/crash/device push identifier | No customer SDK/service found | No collection evidenced now; reassess if notifications/analytics approved |
| Encryption/deletion | HTTPS + Keystore local token; policy points to email/retailer | Transit encryption supported in inspected code; deletion resource/account scope unresolved |

## Intentional deviations proposed for approval

- Keep password-first routing where password exists and WhatsApp-specific instructions, despite old code-only screenshots.
- Use actual dynamic frequency/status/payment data, never screenshot sample placeholders or fake notifications.
- Preserve actual privacy text and honest notification limitation until feature scope is approved; translate reviewed copy rather than placeholders.
- Permit responsive OTP width below nominal geometry on 320dp screens and >100% font scaling; 48dp is the touch region minimum, not a mandatory 48dp visible border width at every screen size.
- Respect safe insets and growing text instead of forcing a total physical 76dp nav or fixed button height that clips text.
- Use adaptive landscape/tablet layouts with ~600dp centered content; target37 precludes relying on portrait restriction for large screens.
- Resolve input border contrast with an approved stronger boundary or another adequate identifying cue.

## Before/after tracking

This document records **before only**. No item is marked fixed. After each approved milestone append commit, evidence/screenshots, changed matrix cells, remaining P0/P1 items and any explicitly approved deferral. A task is not done solely because static literal counts fall to zero.

## Inventory appendix

The following appendix is generated from the inspected source copies. It records matching occurrences, not distinct values. Counts include White/Transparent literals; alpha multipliers, integer size parameters and XML dimensions are listed separately to avoid undercount claims.

### Kotlin literal counts

Regex equivalents: `Color\(0x[0-9A-Fa-f]+\)|Color\.(White|Black|Transparent)` and `\b[0-9]+(\.[0-9]+)?\.(dp|sp)\b`. Entire customer main Kotlin tree, including data files and theme; comments are not stripped. These are transparent lexical counts, not a semantic linter.

| File relative to customer/src/main | Color occurrences | dp/sp occurrences |
| --- | ---: | ---: |
| `kotlin/pk/qist/customer/MainActivity.kt` | 0 | 0 |
| `kotlin/pk/qist/customer/data/CustomerPreferences.kt` | 0 | 0 |
| `kotlin/pk/qist/customer/data/CustomerRepository.kt` | 0 | 0 |
| `kotlin/pk/qist/customer/data/CustomerSessionStore.kt` | 0 | 0 |
| `kotlin/pk/qist/customer/ui/CustomerShell.kt` | 0 | 1 |
| `kotlin/pk/qist/customer/ui/CustomerViewModel.kt` | 0 | 0 |
| `kotlin/pk/qist/customer/ui/auth/Authentication.kt` | 10 | 28 |
| `kotlin/pk/qist/customer/ui/auth/PrivacyConsent.kt` | 1 | 4 |
| `kotlin/pk/qist/customer/ui/components/CustomerComponents.kt` | 3 | 9 |
| `kotlin/pk/qist/customer/ui/components/PaymentPage.kt` | 0 | 1 |
| `kotlin/pk/qist/customer/ui/dashboard/CustomerScaffold.kt` | 2 | 12 |
| `kotlin/pk/qist/customer/ui/dashboard/Dashboard.kt` | 2 | 15 |
| `kotlin/pk/qist/customer/ui/localization/CustomerLanguage.kt` | 0 | 0 |
| `kotlin/pk/qist/customer/ui/notifications/CustomerNotifications.kt` | 2 | 17 |
| `kotlin/pk/qist/customer/ui/payments/CustomerReceipt.kt` | 0 | 3 |
| `kotlin/pk/qist/customer/ui/payments/HowToPay.kt` | 1 | 9 |
| `kotlin/pk/qist/customer/ui/payments/PaymentHistory.kt` | 0 | 0 |
| `kotlin/pk/qist/customer/ui/payments/PaymentMethods.kt` | 2 | 18 |
| `kotlin/pk/qist/customer/ui/phones/PhonePicker.kt` | 3 | 14 |
| `kotlin/pk/qist/customer/ui/plan/PlanDetails.kt` | 0 | 7 |
| `kotlin/pk/qist/customer/ui/profile/CustomerProfile.kt` | 1 | 8 |
| `kotlin/pk/qist/customer/ui/profile/PrivacyPolicy.kt` | 0 | 3 |
| `kotlin/pk/qist/customer/ui/profile/ProfileDialogs.kt` | 4 | 12 |
| `kotlin/pk/qist/customer/ui/profile/ProfileInformation.kt` | 0 | 6 |
| `kotlin/pk/qist/customer/ui/theme/QistTheme.kt` | 20 | 12 |
| **Total (including theme)** | **51** | **179** |

### Every distinct numeric dp/sp literal by file

These contain sizes, spacing, borders, radii and elevation; the theme owns only a subset today. Values passed as integer parameters, for example `ScreenBadge(..., 64)` and `SkeletonLine(..., 34)`, are not counted by the dp/sp regex and remain audit findings.

| File | Values and occurrence count |
| --- | --- |
| `CustomerShell.kt` | `12.dp` ×1 |
| `Authentication.kt` | `1.dp` ×2, `10.dp` ×2, `104.dp` ×1, `12.dp` ×3, `16.dp` ×2, `17.dp` ×1, `18.dp` ×1, `2.dp` ×2, `20.dp` ×2, `22.dp` ×1, `24.dp` ×2, `28.dp` ×1, `28.sp` ×1, `48.dp` ×2, `54.dp` ×1, `56.dp` ×1, `6.dp` ×1, `8.dp` ×2 |
| `PrivacyConsent.kt` | `14.dp` ×1, `4.dp` ×1, `48.dp` ×1, `56.dp` ×1 |
| `CustomerComponents.kt` | `1.dp` ×1, `10.dp` ×1, `12.dp` ×1, `18.dp` ×1, `19.dp` ×1, `36.dp` ×1, `48.dp` ×1, `54.dp` ×2 |
| `PaymentPage.kt` | `16.dp` ×1 |
| `CustomerScaffold.kt` | `0.dp` ×1, `12.dp` ×2, `16.dp` ×1, `18.dp` ×1, `20.dp` ×3, `24.dp` ×2, `78.dp` ×1, `8.dp` ×1 |
| `Dashboard.kt` | `10.dp` ×1, `12.dp` ×2, `16.dp` ×3, `160.dp` ×1, `26.dp` ×1, `44.dp` ×1, `48.dp` ×1, `6.dp` ×1, `8.dp` ×4 |
| `CustomerNotifications.kt` | `16.dp` ×1, `20.dp` ×1, `24.dp` ×3, `32.dp` ×1, `4.dp` ×4, `40.dp` ×1, `48.dp` ×1, `72.dp` ×1, `8.dp` ×3, `9.dp` ×1 |
| `CustomerReceipt.kt` | `12.dp` ×1, `4.dp` ×1, `6.dp` ×1 |
| `HowToPay.kt` | `1.dp` ×1, `20.dp` ×3, `54.dp` ×2, `8.dp` ×3 |
| `PaymentMethods.kt` | `1.dp` ×1, `10.dp` ×3, `12.dp` ×4, `14.dp` ×2, `16.dp` ×2, `18.dp` ×1, `20.dp` ×1, `3.dp` ×1, `40.dp` ×1, `48.dp` ×1, `6.dp` ×1 |
| `PhonePicker.kt` | `1.dp` ×1, `10.dp` ×3, `12.dp` ×1, `13.dp` ×1, `14.dp` ×1, `2.dp` ×1, `28.dp` ×1, `3.dp` ×1, `4.dp` ×1, `40.dp` ×1, `5.dp` ×1, `6.dp` ×1 |
| `PlanDetails.kt` | `12.dp` ×2, `14.dp` ×1, `18.dp` ×1, `4.dp` ×2, `8.dp` ×1 |
| `CustomerProfile.kt` | `10.dp` ×1, `14.dp` ×2, `48.dp` ×2, `52.dp` ×1, `56.dp` ×1, `8.dp` ×1 |
| `PrivacyPolicy.kt` | `16.dp` ×1, `52.dp` ×1, `6.dp` ×1 |
| `ProfileDialogs.kt` | `1.dp` ×1, `16.dp` ×1, `2.dp` ×1, `24.dp` ×3, `30.dp` ×1, `48.dp` ×2, `64.dp` ×1, `8.dp` ×2 |
| `ProfileInformation.kt` | `12.dp` ×1, `14.dp` ×1, `48.dp` ×2, `54.dp` ×1, `8.dp` ×1 |
| `QistTheme.kt` | `12.dp` ×1, `13.dp` ×1, `14.dp` ×1, `16.dp` ×2, `20.dp` ×1, `22.dp` ×1, `24.dp` ×1, `28.dp` ×3, `3.dp` ×1 |

### Every customer drawable XML asset

Source location: `customer/src/main/res/drawable/<name>.xml`. Width/height are intrinsic size, not necessarily runtime rendering size. Runtime tint usually uses theme tokens. Launcher background/foreground/monochrome are logo exceptions.

| Asset | Intrinsic size | Stroke widths | Literal path colors | Auto mirrored |
| --- | --- | --- | --- | --- |
| `chevron_forward` | 24dp × 24dp | 1.8 | #0A1A3F | true |
| `detail_bell` | 24dp × 24dp | 1.8 | #000000 | false |
| `detail_check` | 24dp × 24dp | 1.8 | #000000 | false |
| `detail_clock` | 24dp × 24dp | 1.8 | #000000 | false |
| `detail_help` | 24dp × 24dp | 1.8 | #000000 | false |
| `detail_language` | 24dp × 24dp | 1.8 | #000000 | false |
| `detail_logout` | 24dp × 24dp | 1.8 | #000000 | false |
| `detail_shield` | 24dp × 24dp | 1.8 | #000000 | false |
| `ic_launcher_background` | 108dp × 108dp | none |  | false |
| `ic_launcher_foreground` | 108dp × 108dp | 5.79, none | #FFFFFF | false |
| `ic_launcher_monochrome` | 108dp × 108dp | 5.79, none | #000000 | false |
| `notice_back` | 24dp × 24dp | 1.8 | #000000 | true |
| `notice_overdue` | 24dp × 24dp | 1.8 | #000000 | false |
| `notice_unlock` | 24dp × 24dp | 1.8 | #000000 | false |
| `password_eye` | 24dp × 24dp | 1.8 | #0A1A3F | false |
| `password_eye_off` | 24dp × 24dp | 1.8 | #0A1A3F | false |
| `payment_bank` | 24dp × 24dp | 1.8 | #0A1A3F | false |
| `payment_call` | 24dp × 24dp | 1.8 | #0A1A3F | false |
| `payment_chat` | 24dp × 24dp | 1.8 | #0A1A3F | false |
| `payment_copy` | 24dp × 24dp | 1.8 | #0A1A3F | false |
| `payment_info` | 24dp × 24dp | 1.8 | #0A1A3F | false |
| `payment_shop` | 24dp × 24dp | 1.8 | #0A1A3F | false |
| `phone_outline` | 24dp × 24dp | 1.8 | #0A1A3F | false |
| `tab_home` | 24dp × 24dp | 1.8 | #0A1A3F | false |
| `tab_payments` | 24dp × 24dp | 1.8 | #0A1A3F | false |
| `tab_plan` | 24dp × 24dp | 1.8 | #0A1A3F | false |
| `tab_profile` | 24dp × 24dp | 1.8 | #0A1A3F | false |

### Text-style, alpha and sized-helper usage

Complete lexical inventory, with source line references for integer-size helper calls. Material default typography remains an additional implicit source of dimensions.

| File | Text styles | Alpha literals | Sized helpers (line: call) |
| --- | --- | --- | --- |
| `Authentication.kt` | bodyLarge, headlineLarge, headlineMedium, titleLarge, titleMedium | — | — |
| `PrivacyConsent.kt` | headlineMedium, titleMedium | — | 27: `ScreenBadge(R.drawable.detail_shield, Color.White, Blue, 56)`; 54: `ScreenBadge(icon, Blue, BlueBackground, 44)` |
| `CustomerComponents.kt` | bodyMedium, labelLarge, labelMedium, titleMedium | 0.08f | 49: `internal fun ScreenBadge(icon: Int, color: Color, background: Color, size: Int) {` |
| `PaymentPage.kt` | headlineMedium | — | — |
| `CustomerScaffold.kt` | labelMedium, titleLarge | — | 63: `SkeletonLine(0.6f, 22)`; 66: `if (tab != CustomerTab.Home) SkeletonLine(0.42f, 28)`; 68: `SkeletonLine(0.45f, 18); Spacer(Modifier.height(16.dp)); SkeletonLine(0.7f, 34)`; 69: `Spacer(Modifier.height(18.dp)); SkeletonLine(0.85f, 16)`; 70: `Spacer(Modifier.height(20.dp)); SkeletonLine(1f, if (tab == CustomerTab.Home) 54 else 16)`; 72: `Panel { SkeletonLine(0.7f, 18); Spacer(Modifier.height(12.dp)); SkeletonLine(0.45f, 14) }`; 76: `SkeletonLine(0.8f, 18); Spacer(Modifier.height(8.dp)); SkeletonLine(0.55f, 14)`; 85: `private fun SkeletonLine(fraction: Float, height: Int) {` |
| `Dashboard.kt` | bodyMedium, headlineLarge, headlineMedium, titleLarge, titleMedium | 0.08f | — |
| `CustomerNotifications.kt` | bodyMedium, headlineMedium, labelMedium, titleLarge, titleMedium | 0.60f | 47: `NoticeIcon(R.drawable.detail_bell, Blue, BlueBackground, large = true)`; 105: `NoticeIcon(icon, tint, background)`; 119: `private fun NoticeIcon(icon: Int, tint: Color, background: Color, large: Boolean = false) {` |
| `CustomerReceipt.kt` | headlineLarge, titleMedium | — | 76: `ScreenBadge(R.drawable.detail_check, PaidGreen, PaidBackground, 64)` |
| `HowToPay.kt` | bodyMedium, headlineLarge, titleMedium, titleSmall | — | — |
| `PaymentHistory.kt` | titleLarge | — | — |
| `PaymentMethods.kt` | bodyMedium, bodySmall, titleLarge, titleSmall | 0.72f | 61: `ScreenBadge(if (method.cash) R.drawable.payment_shop else if (method.bank.isNotEmpty()) R.drawable.payment_bank else R.drawable.phone_outline,` |
| `PhonePicker.kt` | bodyMedium, headlineMedium, labelMedium, titleMedium, titleSmall | 0.72f | 55: `ScreenBadge(if (completed) R.drawable.detail_check else R.drawable.phone_outline,` |
| `PlanDetails.kt` | titleLarge, titleMedium | — | 30: `ScreenBadge(R.drawable.phone_outline, Blue, BlueBackground, 44)` |
| `CustomerProfile.kt` | titleLarge, titleMedium | 0.08f | — |
| `PrivacyPolicy.kt` | titleLarge, titleMedium | — | — |
| `ProfileDialogs.kt` | headlineMedium, titleMedium | 0.60f | — |
| `ProfileInformation.kt` | headlineMedium, titleLarge, titleMedium | — | — |

### Inventory interpretation

Excluding QistTheme.kt, the lexical scan finds **31 named color occurrences and 167 numeric dp/sp occurrences**. There are **zero hexadecimal Color(...) declarations outside the theme**. This means the primary color problem is incorrect/missing semantic tokens and named/alpha/resource literals, not widespread inline hex colors. A final check must also cover strings, XML colors/dimensions, alpha, integer-size helper arguments and legitimate exceptions. Do not replace every number mechanically.

## Milestone 1 — after implementation (7 November 2026)

The baseline matrices above remain the **before** record. The user approved recommended defaults. `customer-app` was created at the audited `dev` commit and `ui-polish` contains the theme milestone. No release/merge is authorized by this record.

| Audit finding | After milestone 1 | Remaining evidence/work |
| --- | --- | --- |
| Palette mismatches/missing roles | Corrected in split theme palette and Material roles | Runtime contrast/compositing and screenshot review |
| Card22, app/auth54, OTP56 | Card20, app52/auth56, OTP60; nominal48 width adaptive | Device/font-scale layout verification |
| Implicit Material type scale | Every text role has explicit Inter size/weight/line height; pushed titles distinct from tabs | Urdu Nastaliq and locale work in milestone6 |
| Inline dimensions/named colors | Outside theme UI scan167→0 numeric dp/sp and31→0 named colors; zero inline hex outside theme maintained | Full strings/resources/literal enforcement in milestone9 |
| Mixed strokes/text arrows | Resource outlines2 round; auth/FAQ vectors; allowed icon sizes | Screenshot/TalkBack checks |
| Strong field outline | Approved #8795B2, 3.01:1 on white; reference border retained decoratively | Actual focus/error/disabled states in milestone2 |
| Design rules | AGENTS.md added | Apply to all later work |
| P0 release/build evidence | Still unresolved | Gradle distribution download blocked by network; no SDK/emulator |

See `docs/UI_POLISH_M1.md` for exact verification results, review targets and exceptions. No functionality gaps or P0 release gates have been silently marked fixed. Locker, API/data contracts, financial calculations, CustomerViewModel and release configuration are unchanged.
