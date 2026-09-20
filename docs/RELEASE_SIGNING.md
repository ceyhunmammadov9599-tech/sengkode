# SENGKODE Release Signing Guide

The release build is wired to sign automatically when a local,
UNCOMMITTED `keystore.properties` file exists at the repository
root. No secrets are (or ever will be) in version control;
`keystore.properties`, `*.jks` and `*.keystore` are gitignored.

## 1. Generate a production keystore (one-time, on your machine)

keytool -genkeypair -v \
  -keystore sengkode-release.jks \
  -alias sengkode \
  -keyalg RSA -keysize 4096 -validity 10000 \
  -storetype PKCS12

Keep a safe offline backup of the keystore AND passwords.
Losing them means the app can never be updated under the same
Play Store listing.

## 2. Create keystore.properties (repository root, not committed)

storeFile=/absolute/path/to/sengkode-release.jks
storePassword=<keystore password>
keyAlias=sengkode
keyPassword=<key password>

## 3. Build

./gradlew assembleRelease --no-daemon

Output: app/build/outputs/apk/release/app-release.apk
(signed; the unsigned suffix disappears once signing succeeds).

## 4. Verify the artifact

keytool -printcert -jarfile app-release.apk
(for AAB: jarsigner -verify bundle.aab; or use apksigner from
build-tools: apksigner verify --verbose app-release.apk)

## Notes

- Play Store submission requires an AAB; `bundleRelease`
  reuses the same signing config.
- Play App Signing is recommended at listing setup: upload the
  AAB with this key once; Google then manages distribution keys.
- For CI later, the same properties can be injected as masked
  environment secrets instead of a file.
