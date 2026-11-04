# Qist device app (Android)

The small app that locks or unlocks a financed phone when the portal says so. It runs as **Device Owner**, set
during phone setup by scanning the provisioning QR the portal shows for a sold phone. Not on Google Play: we host
the APK ourselves. This folder is a separate Gradle project, not part of the bun workspaces.

## Open and build

- Open the `android/` folder in Android Studio (it uses its own bundled JDK; nothing else to install).
- From a terminal: `./gradlew check` runs unit tests, Compose UI tests (on the JVM), ktlint and Android Lint.
  `./gradlew assembleDebug` builds `app/build/outputs/apk/debug/app-debug.apk`.
- `./gradlew ktlintFormat` fixes formatting.

## Settings (no URLs or secrets in code)

`gradle.properties` holds `qist.apiUrl.debug` and `qist.apiUrl.release`. Override them on your machine in
`~/.gradle/gradle.properties` or with `-Pqist.apiUrl.release=https://api.<domain>/api/v1`. A release build refuses
anything that is not `https://`. Debug builds may use plain HTTP to `10.0.2.2` (the host computer, seen from the
emulator) only.

## Setup QR (server settings)

The portal's QR tells a factory-fresh phone where to download this app and how to recognise it. The API needs:
- `DEVICE_APP_APK_URL`: the https link to the signed release APK;
- `DEVICE_APP_SIGNATURE_CHECKSUM`: the SHA-256 of the signing certificate in URL-safe base64. It does not change
  when you publish a new version, only if the signing key changes. Compute it from the release keystore with:

```bash
keytool -exportcert -alias qist -keystore qist-release.jks | openssl dgst -binary -sha256 | openssl base64 | tr '+/' '-_' | tr -d '='
```

## Testing on the emulator (no QR needed)

On a fresh emulator with no Google account added, make the debug build Device Owner directly:

```bash
adb shell dpm set-device-owner pk.qist.device/.deviceadmin.QistAdminReceiver
```

## Layout (`app/src/main/kotlin/pk/qist/device/`)

| Package | What |
|---|---|
| `data/` | API models, the HTTP client (`HttpDeviceApi`), the encrypted state file (`DeviceState`) |
| `deviceadmin/` | the Device Owner receiver and the policies it applies |
| `ui/theme/` | Qist colours, light and dark |
| `AppContainer` | builds every object by hand and passes it in (no DI framework) |
# qist-android-locker
# qist-customer
