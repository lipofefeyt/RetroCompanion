# Releasing

How RetroCompanion is built and signed for release. Nothing in this file is secret, and nothing
secret should ever be added to the repo: **never commit a keystore, `keystore.properties`, or any
password.**

## One-time: create the upload keystore

Do this yourself, in your own terminal, so the passwords are typed only by you. Keep the keystore
**outside the repo** and back it up (a password manager and an offline copy). Losing the upload key
is recoverable through Play Console support, but it is slow and painful.

```sh
# Windows (Android Studio's bundled JDK):
"C:\Program Files\Android\Android Studio\jbr\bin\keytool.exe" -genkeypair -v ^
  -keystore C:\Users\<you>\keystores\retrocompanion-upload.jks ^
  -alias upload -keyalg RSA -keysize 2048 -validity 10000
```

`keytool` prompts for the keystore password, the key password and your details. Then:

1. Copy `keystore.properties.example` to `keystore.properties` (git-ignored) in the repo root.
2. Fill in the path, alias and the two passwords.
3. `./gradlew bundleRelease` (or `assembleRelease`) now produces a **signed** build. Without
   `keystore.properties` the release build is simply unsigned, which is what CI produces.

This is the *upload* key. Enroll in **Play App Signing** in the Play Console: Google holds the real
app-signing key and re-signs what you upload, so a lost upload key can be reset.
