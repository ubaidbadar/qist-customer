# Qist customer app plan

Status: approved. Milestones 1–3 are on `customer-app`; branch release validation is recorded below.
Discovery baseline: `main` at `c78798db648279f3319ca4b6824d92b964213f9d` (4 November 2026). Recheck the default-branch HEAD before implementation.

## Structure and isolation

Create `customer-app` from the default branch after approval. Add independent Android application module `:customer` in `customer/`; register it in `settings.gradle.kts`. Application ID and namespace: `pk.qist.device.customer`; launcher label: `Qist`. Keep `:app`, its icons, signing configuration, versions, and both existing workflows unchanged. No dependency on the locker module. Never push to main, force-push, change repository settings/secrets/protection, or open a PR targeting main during this experiment.

Use existing Kotlin, Jetpack Compose, Material 3, Gradle wrapper, version catalog, and Java configuration. Add only customer-required dependencies. Minimal launcher manifest; no networking, device administration, accessibility, overlays, boot receivers, Firebase, or locking capabilities. Inspect merged manifests, including dependency-added permissions.

Separate customer data models, repository interface, deterministic fake repository, screen state, reusable components, and navigation. Authentication is explicitly a demo: no SMS or backend traffic; document a fixed test code. Keep fake customer data recognisably fictional.

## Workflows and signing

Create `.github/workflows/customer-build-apk.yml` and `customer-release.yml`, copied respectively from `build-apk.yml` and `release.yml`. Preserve action versions, Temurin JDK 17 setup, Gradle setup/caching, step order where practical, signing-secret names, and cleanup.

Both customer copies trigger on pushes to `customer-app` and tags `customer-v*`; retain manual dispatch as optional, not required. Use separate customer concurrency groups. Debug task: `:customer:assembleDebug`; release task: `:customer:assembleRelease`. Customer module uses the same `qist.signing.*` Gradle properties for release signing. Debug uses ordinary Android debug signing, matching the existing debug workflow. Branch pushes also run the release copy, producing a keystore-signed APK.

Name artifacts `qist-customer-debug-apk` and `qist-customer-release-apk`; APK filenames `qist-customer-debug.apk` and `qist-customer-release.apk`. Debug publication uses the separate `customer-latest` GitHub Release on branch pushes. Existing release workflow produces no GitHub Release or AAB; preserve that behavior in its copy. No new secrets required.

Necessary exceptions to “change only” the listed workflow fields, included for approval:

- Change the copied R2 destination from `android/qist.apk` to `android/qist-customer.apk`, including its summary. Reusing the original destination would overwrite the locker download. Branch pushes will publish this customer-only object.
- Replace copied locker QR-provisioning instructions/checksum summary with customer installation information; customer APKs must never be used for device-owner enrollment.
- Adjust output filenames/paths and branch conditions consistently; avoid echoing API secret values. Customer has no API calls and needs no API URL build setting.

Existing locker push filters already exclude the customer branch/tags. A PR into main would trigger locker debug CI. Its unqualified `assembleDebug` would also build the customer module if that module were included on the tested ref; therefore do not open/merge such a PR. Root-wide local tasks likewise cover both modules; use qualified tasks.

## Versioning and assets

Start customer `versionCode=1`, `versionName="0.1.0"`, independent of locker `2` / `0.2.0`. Increment code for each distributed update; customer release tags must match the declared name (for example `customer-v0.1.0`). Tags do not automatically update Gradle versions.

Copy supplied adaptive background, foreground, monochrome, adaptive wrappers, and all five densities of regular/round PNG launcher icons into customer resources only. Put `qist-play-store-512.png` in `store-assets/customer/`; never commit the ZIP. Bundle Inter with its license and an appropriate Urdu fallback if Urdu is enabled.

## Screens and visual behavior

1. Welcome: navy background, gradient white-keyhole badge, Qist wordmark, Urdu language control, heading/subtitle, cyan Get started pill and retailer guidance.
2. Phone number: light background, back control, fixed +92 prefix, validated Pakistani mobile input, Send code pill, registration help. Normalize pasted local/international formats.
3. Verify code: six digit cells, paste/backspace support, displayed phone number, 30-second resend timer, validation and Verify action.
4. Home: navy greeting/header/avatar; white next-installment card with amount, due date, progress and How to pay; phone/status card; recent payments; Home/Payments/Plan/Profile bottom navigation. Payments and Plan reuse the sample records, and Profile shows a sample identity. Payment help explains how the demo differs from real payments.

Use specified brand colors, Inter typography, rounded cards, minimum 48dp pill buttons and touch targets. Support scrolling, keyboard/system insets, narrow devices, font scaling, accessible labels and contrast. Proposed first batch includes English/Urdu strings for these screens so the language control works, with RTL checks.

## Milestones and verification

1. Branch, plan commit as part of milestone, standalone hello module, signing, customer workflows. Validate YAML with actionlint if available; run `./gradlew :customer:assembleDebug`; inspect branch-triggered artifacts and signed release output.
2. Theme/tokens, licensed fonts, supplied icons, navigation shell. Review icon masks, typography, insets and navigation.
3. Four screens and fake repository. Test phone/code validation, resend, navigation/back, progress calculations, recreation, keyboard, large fonts and screenshot fidelity.
4. Run customer tag release end to end, verify signature/application ID/minimal permissions, install alongside locker, verify customer R2 object and artifacts. Run locker build/check tasks and compare original workflow/app files against baseline. Report unavailable emulator/device checks honestly.

Commit each milestone separately; report changed files, review targets, commands and actual results. Build risks: pinned SDK 37/AGP 9.4.1/Gradle 9.8.0 availability, Gradle daemon requesting Java 25 despite workflow JDK 17, signing/R2 secret availability, and runner/network/tool access. Do not upgrade shared tooling silently.

## How to release

On `customer-app`, increment customer versions, commit and push; both customer workflows run. After successful validation, tag that commit `customer-v<versionName>` and push only that tag. Download customer APK artifacts; the signed APK is also published to `android/qist-customer.apk` in the existing R2 bucket. Never use locker `v*` tags or its R2 object path.

## Test the demo

Run `./gradlew :customer:installDebug` with a connected Android device, or download the customer debug APK from Actions. Enter any ten-digit sample number starting with `3`, then enter `123456`. The debug and release builds use different signing certificates, so Android may require uninstalling the debug customer app before switching to the signed release. The locker has its own package ID.

## Verification record

- Customer branch pushes ran both customer workflows. The screen commit's debug job built and uploaded its APK. The signed release workflow verifies the APK with `apksigner` before publishing to the customer R2 object. See [customer Actions runs](https://github.com/ubaidbadar/qist-android-locker/actions?query=branch%3Acustomer-app).
- The branch diff contains no locker module files or existing locker workflows. Both locker workflows previously passed on the baseline commit.
- YAML parsing and push filter checks passed locally. Local Android builds, emulator and device checks were unavailable in this environment. The `customer-v*` filters were checked statically and still need a live tag push.

From an authenticated clone, after checking out the desired customer commit:

```bash
git tag customer-v0.1.0
git push origin refs/tags/customer-v0.1.0
```
