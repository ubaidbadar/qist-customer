# Qist customer app

Follow `.agent/skills/kotlin-android-excellence/SKILL.md` and the approved `UI_POLISH_PLAN.md`.

## Scope and review

- Customer UI work is on `ui-polish`, based on `customer-app` at the reviewed `dev` commit `2e18985555465cc5dd57f8fa36afe149a16e1013`.
- Change only the customer module and its documentation. Preserve locker `:app`, backend contracts, DTOs, financial calculations, signing, versions and deployment configuration.
- One commit per milestone. Stop for review after milestones 1 and 2. Never merge or deploy without a separate instruction.
- Keep phone-first password/WhatsApp OTP routing. Notifications and backend-dependent deletion work remain deferred and must not be simulated.

## Design rules

- Customer colors live in `ui/theme/QistColors.kt`; dimensions/shapes in `QistTokens.kt`; text styles in `QistTypography.kt`; Material roles in `QistTheme.kt`.
- Use semantic tokens, not literal colors, dp/sp, local alpha variants or hidden integer size arguments. Drawable coordinate paths, 24-unit viewports and two-unit outline strokes are resource asset geometry, not layout tokens.
- Mirror the brand/white colors in customer XML color resources only where Android drawable resources require them; verify equality with the Compose palette. Launcher/logo artwork is exempt from the outline-only rule.
- Use Inter 400–700: tab headings 28/34, pushed/auth titles 24/30, amounts 34/42, section titles 17/24, body 15/22, secondary 14/20, captions 13/18. Urdu font/line-height and per-app locale verification belong to milestone 6; do not claim current Urdu typography is complete.
- Cards 20dp; input/compact variants 14–16dp; sheet/dialog 28dp. App pills at least 52dp, auth/onboarding 56dp, touch regions at least 48dp. Increase height for text scaling instead of clipping.
- OTP cells are nominally 48×60dp, shrinking horizontally to fit narrow screens. Keep the underlying editable field and digits LTR. Preserve system insets; bottom-nav 76dp is a minimum, with Material safe-inset and accessibility sizing retained.
- Outline icons use round joins/caps, stroke 2, and only 16/18/20/22/24dp sizes. Use auto-mirrored directional assets, not text arrows. Icon circles are 36/40/44dp; consent56, receipt/logout hero64, avatar52 and logo104 are documented reference-art exceptions.
- Inputs use the stronger `InputOutline` boundary for accessible contrast; `InputBorder` retains the reference decorative border value. Use Danger for error text/status and Destructive for destructive actions.
- Consolidate into shared components during milestone 2 rather than duplicating variants. Do not invent screens or copy; propose missing product states before implementing them.
- Data UI must distinguish initial loading, retained refresh, empty/error/offline and per-action loading. Reuse existing state ownership; no calls during composition, duplicate fetches, fake data, polling or new API contracts.
- Never save passwords/OTP in saved state; keep financial caches account-bound and clear them on approved logout/session transitions.

## Verification

Run customer-qualified build/lint/tests after each code milestone:

```bash
./gradlew :customer:assembleDebug :customer:lintDebug :customer:testDebugUnitTest
```

Record unavailable toolchain/network/device checks as blocked, not passed. Runtime screenshot, TalkBack, RTL, 200% text, keyboard/system-inset and 16KB checks need real build/device evidence. Static literal/resource checks alone do not prove compilation or visual fidelity.
