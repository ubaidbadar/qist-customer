# Customer app CI and Google Play releases

| Branch | Purpose | On push |
| --- | --- | --- |
| dev | Development | Build APK/AAB, Android Lint, unit-test tasks |
| stage | Internal testing | CI, then signed AAB upload to Play internal track |
| main | Live release | CI, then signed AAB upload to Play production track |

Open pull requests from dev to stage, then stage to main. Pull requests run CI without release credentials. Each release builds the exact pushed commit and waits for CI. Google Play review, account eligibility and Managed Publishing can delay availability even after upload succeeds. Production uses a completed release, not a percentage rollout.

The same package ID, pk.qist.device.customer, and upload key are used on both tracks. AABs are Play upload files. Install debug APK artifacts directly on a test device.

## Required GitHub Actions secrets

Add these to **ubaidbadar/qist-customer**. Repository secrets are not copied from another repository.

- ANDROID_KEYSTORE_BASE64 — base64 of the customer app's registered Play upload keystore.
- ANDROID_KEYSTORE_PASSWORD — keystore password.
- ANDROID_KEY_ALIAS — upload key alias.
- ANDROID_KEY_PASSWORD — key password.
- PLAY_SERVICE_ACCOUNT_JSON — complete Google Cloud service-account JSON authorised for this app in Play Console.

Use the existing key only if it matches the upload certificate registered for this Play app. Keep credentials in GitHub secrets. The workflow does not need Cloudflare R2 secrets.

Enable Google Play Android Developer API in the service account's Cloud project. Grant the service account access to this specific app in Play Console, including testing-track and production-release permissions.

Before the first automated publication, create the Play app, configure Play App Signing, complete the required store listing and declarations, and upload the first signed AAB manually if the API requires initial app setup. Download the signed AAB from the CD run artifacts if publishing fails after the build. A draft app may require completing its initial draft release in Play Console before completed releases are accepted.

## Versioning

The user-visible versionName stays in customer/build.gradle.kts. Update it deliberately for each product release.

CD sets versionCode through qist.customer.versionCode using seconds since 2020-01-01 UTC plus 1,000,000. Uploads are serialised across stage and main. A rerun generates a fresh code. Check that the generated code is higher than any version already in Play before the first upload. Local builds default to code 1. This avoids separate branch counters colliding in the same Play app.

Main rebuilds a release AAB with a newer code rather than promoting the exact internal-track binary. Merge the tested stage changes into main to retain the same source.

## Checks and limitations

CI runs assembleDebug, bundleRelease, lintDebug, lintRelease, the test aggregate task for :customer. A unit-test task reporting NO-SOURCE means there are no tests for that variant, not that test assertions passed. CI does not run emulator tests.

Missing signing or Play secrets fail the publish job with a clear error after CI. Debug artifacts and check reports remain available. Publishing is not verified until a real Play upload succeeds.

The app currently uses sample data and demo sign-in. Store-readiness, privacy declarations and real backend integration remain separate product work.

## References

- https://developers.google.com/android-publisher/getting_started
- https://developer.android.com/studio/publish/versioning
- https://github.com/r0adkll/upload-google-play
