# Milestone 1 — theme and icon foundation

Status: implementation prepared for review; Android verification blocked. Base: `2e18985555465cc5dd57f8fa36afe149a16e1013` on `customer-app`; work branch `ui-polish`. The user approved the plan's recommended defaults on 7 November 2026. No merge, deployment or Play upload is part of this milestone.

## Changes

- Split the customer theme into palette, shape/dimension tokens, typography and Material role wiring. Preserve all existing data/state/repository behavior.
- Correct success/warning/danger/destructive/divider/input values. Add missing text, disabled, neutral and dark-surface tokens; use stronger input outline #8795B2 (3.01:1 on white) for fields/OTP.
- Define every Material text role using bundled Inter: tab28/34, pushed/auth24/30, amount34/42, section17/24, body15/22, secondary14/20, caption13/18.
- Correct card radius22→20, app pill54→52, auth54→56, OTP56→60 high; nominal OTP width48 adapts to available width. Tokenize existing placeholder geometry without claiming the skeleton milestone is complete.
- Replace named color and numeric dp/sp literals in customer UI with theme roles; convert sized badge/skeleton helpers to Dp instead of integer-to-dp conversions.
- Unify sheet scrim, decorative blue tint and icon sizes. Use two-unit round outline strokes and XML color resources. Replace auth/FAQ text arrows with outline vectors; the back vector retains RTL mirroring.
- Add design rules to AGENTS.md, preserve approved plan/audit in the repository, and record the review gates.

## Files to review

- `customer/src/main/kotlin/pk/qist/customer/ui/theme/`: palette, geometry, text and Material roles.
- `ui/auth/Authentication.kt`, `ui/auth/PrivacyConsent.kt`, `ui/components/CustomerComponents.kt`: title hierarchy, input outline, pill/OTP geometry, helper sizes.
- `ui/dashboard/`, `ui/plan/`, `ui/phones/`, `ui/payments/`, `ui/notifications/`, `ui/profile/`: adoption of tokens in existing screens.
- `customer/src/main/res/drawable/` and `res/values/colors.xml`: shared outline stroke/color and directional icons; logo colors moved to resources without changing artwork.
- `MainActivity.kt`: transparent system-bar value now references the theme; behavior is unchanged.
- `AGENTS.md`, `UI_POLISH_PLAN.md`, `DESIGN_AUDIT.md`: rules, progress, before/after evidence.

## Verification

| Check | Result |
| --- | --- |
| Source retrieval | All 184 repository blobs matched their original Git object hashes; baseline tree matches 8f63a6d438664164479ae4dc7d43124f739c2c6c |
| Diff whitespace | `git diff --check` passed |
| UI token references | Static scan passed: all QistShapes/QistSize/QistSpacing/QistSkeletonSize members resolve to declarations |
| Literal counts | Customer UI numeric dp/sp: 167→0 outside theme. Named Color.White/Transparent: 31→0 outside theme. Inline hexadecimal Compose colors remain zero outside theme |
| Vector/XML checks | Parsed all customer XML; color/drawable references resolved; non-launcher outlines stroke2 with round caps/joins |
| Protected paths | Locker, repository/data classes, CustomerViewModel, API contracts, tests, Gradle/build/signing/version/manifest/workflows unchanged |
| Android build/lint/unit tests | Attempted `./gradlew :customer:assembleDebug :customer:lintDebug :customer:testDebugUnitTest`; wrapper download failed with `Network is unreachable` before Gradle started. None of the three tasks ran |
| Screenshots/install/RTL/TalkBack/200%/emulator | Not run: Android SDK/emulator unavailable; no runtime visual fidelity claim |

The static checks are source checks, not Kotlin compilation or Android lint. No dependencies or toolchain versions were changed to bypass the environment.

## Deliberate exceptions and remaining work

- Stronger field boundary than the pale reference border, as approved for accessibility. Decorative input-border token remains #C4CFE6.
- Navy logos, consent56, receipt/logout64, avatar52 and logo104 are distinct reference roles; UI icon glyphs themselves remain within16–24dp.
- Material bottom navigation preserves its own accessibility minimums and safe insets. 76dp is a floor, not a forced total height.
- Nominal OTP48 width shrinks on narrow phones; text/button regions may grow for accessibility.
- Urdu Nastaliq font, per-app locale and 200% line-height verification remain in milestone6; this milestone does not claim Urdu typography is finished.
- Generic skeleton timing/layout, persistent selection/offline cache, consent order, logout failure behavior, refresh accessibility, complete component states and shared component consolidation remain in their planned milestones.
- Notifications and backend-dependent deletion integration remain explicitly deferred. Final privacy/support/deletion destinations still need confirmation before release.
- No screenshot state is marked visually verified. Review auth, Home amount/card, Plan schedule, My phones, language sheet and logout dialog in an Android build when the toolchain is available.

Stop here for the required milestone1 review. Milestone2 is shared components plus debug gallery/previews.
