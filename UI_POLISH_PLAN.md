# Qist customer app — UI polish plan

7 November 2026 · **Recommended defaults approved by user on 7 November 2026. Milestone 1 in review.**

## Baseline and boundaries

Audit: `ubaidbadar/qist-customer`, `dev` at `2e18985555465cc5dd57f8fa36afe149a16e1013`. `customer-app` is absent. Created `customer-app` at this reviewed `dev` commit, then `ui-polish` from it. Recheck HEAD first and report any intervening changes. Never merge/deploy as part of this work.

Only customer presentation, platform integration and approved UI state behavior change. Preserve business calculations, repository interfaces, endpoints, DTOs, phone/password/WhatsApp routing and locker `:app`. API-dependent features that cannot work under current contracts become explicit decisions/deferred work. The old CUSTOMER_APP_PLAN.md describes a demo and is not the current specification.

## Tokens and components

Use one customer theme with the brief's exact palette: brand blue #2F3FE0, cyan #06B6D4, accent #22D3EE/text #04122E; navy #0A1A3F/raised #11275A; canvas #F5F7FC/white cards; complete light text, status, disabled, border and dark-surface roles. Correct success/warning/danger/divider/input colors listed in DESIGN_AUDIT.md. No dynamic palette or extra dark-mode design.

Inter 400–700, explicit sizes/line heights: tabs28/34, pushed24/30, amounts34/42, sections17, body15/22, secondary14/20, captions13. Add reviewed Urdu fallback Noto Nastaliq Urdu with font license and adequate line height. Shapes: card20, compact/input14–16, sheet/dialog28; button52/app or56/auth minimum, nav76 content intent with safe insets, fields56, OTP nominal48×60. Tokenize layout-specific geometry without hiding it in arbitrary strings. One outline set: stroke2, round joins/caps, sizes16/18/20/22/24; keep logo artwork as the documented exception.

Build shared Button variants, TextField/PhoneField/OtpField, Card, ListRow, IconCircle, StatusChip, TopBar/BottomNav, BottomSheet/ConfirmDialog, ProgressBar, SectionHeader/InfoBanner, EmptyState/ErrorState/OfflineBanner, Snackbar, Skeleton, PullToRefresh and CopyButton. Prefer slots over giant switch-heavy components. Put gallery in `src/debug`, previews in appropriate preview/debug sources; verify absence from release. After approval, add Design rules to AGENTS.md.

## State and feedback

Keep existing ViewModel/StateFlow and repository contracts. Add narrowly scoped UI state for first-load visibility, refresh/action status and presentation errors. Do not replace business models. Keep route-specific transient secrets out of saved state; persist approved locale/selected plan separately, and restore nonsecret form/navigation state with saved-state support.

First load: layout-specific skeleton after 300ms; skip for faster loads. Shimmer 1.2s between #E6EBF5/#F5F7FC, static with animations disabled. Refresh retains content; no skeleton. One request in flight; error/empty content remains refreshable. Retry snackbar above bottom nav, navy/white/radius14; auth validation remains inline with error border. Central error-to-message mapping distinguishes offline, timeout, 5xx, not-found, 401 and validation; preserve server semantics without exposing raw technical failures.

Propose foreground refresh only when relevant cached data is older than **5 minutes**, deduplicated against in-flight work; recompute date-relative presentation on local midnight/resume. No periodic background polling. Durable cache proposal: private app storage of already-returned account/receipt data, no repository contract changes, account-bound and cleared at logout; storage/backup review precedes implementation. Refresh accessibility action provides a non-gesture route without adding a visible refresh button. No “Updated [time]” label by default.

## Milestones and review gates

| Milestone | Work and acceptance evidence |
| --- | --- |
| 0 — P0 release/discovery gates | Confirm base branch, consent/notification/deletion decisions and release scope; inspect reproducible toolchain, customer merged manifest, backup policy, privacy URL and actual versionCode/signature. Record missing evidence as blocking; do not silently fix backend logic |
| 1 — Tokens and design rules | Correct palette/type/shapes/icons/spacing, add AGENTS.md rules, define authorized visual exceptions. Compare representative auth/Home/Plan/dialog screens. **Stop for user review** |
| 2 — Components | Replace duplicated presentation incrementally; gallery/previews show focused/pressed/error/loading/disabled states, TalkBack labels and 200% font cases. **Stop for user review** |
| 3 — States and refresh | Exact screen skeletons, delayed shimmer, error/empty/offline/snackbar, retained refresh, accessible refresh and stale-resume policy. Test request dedup, failure and 401 |
| 4 — Auth/consent/permissions | Preserve password/OTP routes; fix paste/IME/autofill/validation, approved consent behavior, secure restore/logout UI. Notifications only within approved capability; no fake permission success |
| 5 — Core data screens | Home/Plan/Payments/My phones/Receipt/How-to-pay fidelity, selected-plan persistence/fallback, lazy stable lists, approved export/cache and intent feedback |
| 6 — Profile/secondary screens | Notifications if authorized, persistent English/Urdu locale/RTL, help/privacy/dialogs; preserve reviewed copy and approve missing states |
| 7 — Platform/accessibility | Splash, bar colors/insets/IME, back behavior, restoration, responsive/tablet layouts, TalkBack/keyboard/contrast/targets |
| 8 — Release/performance | Backup/permissions/exports/R8/signature/log review; 16KB artifact/runtime checks, startup/size/jank measurements where hardware available; no analytics without approval |
| 9 — Regression/cleanup | Expand unit/UI/screenshot suites; QA_CHECKLIST.md/device matrix; scoped literal check; remove replaced resources and debug code from release; update audit before/after |

One topic/commit per milestone. After each code milestone run `./gradlew :customer:assembleDebug :customer:lintDebug :customer:testDebugUnitTest`; run configured customer formatting checks if present. Add targeted UI/screenshot checks for touched flows. Release gate adds `:customer:bundleRelease` and inspection of the actual signed artifact. Do not invoke unqualified locker tasks. Report pass/fail/not-run, changed files, review targets and deviations; preserve missing-environment blockers honestly.

## Dependencies and risks

Prefer existing Compose/Material/AndroidX; write shimmer locally. Justified additions, subject to approval: AndroidX core-splashscreen for compatible splash; locale integration compatible with the current ComponentActivity (compare platform API33+ plus persisted older-API support with AppCompat migration before choosing); Compose tooling/test artifacts already listed but not wired to customer; Robolectric for host-side critical UI tests and screenshot capture if compatible. Propose Roborazzi only if existing test tooling cannot provide maintainable golden comparison; do not add it automatically. SMS consent/retriever dependencies are conditional on actual SMS delivery approval. No new networking/analytics/icon package.

Main risks: source/branch drift; tooling availability; snapshot mismatch at scaled reference resolution; accessibility contrast conflict; state restoration across selection; accidental data-model or financial-calculation changes; unavailable notification/read/deep-link APIs. Keep backend/locker untouched. Roll back a milestone by reverting its isolated commit after checking subsequent dependencies; never reset shared history.

## Current platform guidance

API36+ is required for new phone apps/updates since 31 August 2026; target37 is already declared ([Play](https://support.google.com/googleplay/android-developer/answer/11926878)). Current [16KB guidance](https://developer.android.com/guide/practices/page-sizes) states incompatible updates are blocked from 1 February 2027; inspect dependencies/artifact rather than assume Kotlin source proves compatibility. [Android16](https://developer.android.com/about/versions/16/behavior-changes-16) mandates edge-to-edge and enables predictive system back for target36+. [Android17](https://developer.android.com/about/versions/17/behavior-changes-17) removes the large-screen orientation opt-out. Use official Material3 [refresh](https://developer.android.com/develop/ui/compose/components/pull-to-refresh), [snackbar](https://developer.android.com/develop/ui/compose/components/snackbar) and [sheet](https://developer.android.com/develop/ui/compose/components/bottom-sheets) APIs. Complete [Data safety](https://support.google.com/googleplay/android-developer/answer/10787469) and resolve [account deletion](https://support.google.com/googleplay/android-developer/answer/13327111) applicability before publication.

## Decisions (recommended defaults)

1. Approve recreating `customer-app` from audited `dev`, then `ui-polish`?
2. Move consent before authentication and close on Decline as requested? This deliberately changes current routing.
3. Keep notifications explicitly deferred for this polish, or authorize a separately scoped backend/push integration? Current contracts cannot provide the designed feed.
4. Keep WhatsApp OTP with paste/manual entry? SMS autofill requires an actual SMS channel, which this app does not currently use.
5. Approve adaptive landscape/tablets, Western digits, five-minute stale refresh, and stronger accessible input boundaries?
6. Approve permission-free text receipt export/cache? Use MediaStore Downloads on API29+, document picker fallback on API24–28; propose PDF/image separately if wanted.
7. Approve the proposed dependency categories and confirm the published privacy/support/deletion destination/copy before release? No new screens, analytics or account-deletion API will be invented.

Approval may accept the recommended defaults while explicitly deferring notifications and any backend-dependent deletion work. Those deferrals stay visible in the audit; release remains blocked if the final product/policy requires them.

## Milestone 1 implementation record

Theme colors, typography, shapes, spacing, component geometry and icon resources now use centralized tokens. See `docs/UI_POLISH_M1.md` for changes, static checks, deliberate exceptions and blocked Android verification. Urdu font/locale changes remain in milestone 6. Notifications and backend deletion integration remain deferred. Approval does not establish the final privacy/support/deletion destination; that remains a release prerequisite.
