# Releasing

How RetroCompanion is built and signed for release. Nothing in this file is secret, and nothing
secret should ever be added to the repo: **never commit a keystore, `keystore.properties`, or any
password.**

## Application ID

The app is published as **`io.github.lipofefeyt.retrocompanion`** (`applicationId` in
`app/build.gradle.kts`). This is permanent once the app is on Play. The source `namespace` stays
`com.example.retrocompanion` on purpose: it only names the code's package and R class, and moving
it would touch every file for no benefit. Because the ID changed from the original
`com.example.retrocompanion`, a device that has the old debug build gets this as a **separate app**
with its own empty data (re-enter your credentials), and the old one can be uninstalled.

## One-time: create the upload keystore

Do this yourself, in your own terminal, so the passwords are typed only by you. Keep the keystore
**outside the repo** and back it up (a password manager and an offline copy). Losing the upload key
is recoverable through Play Console support, but it is slow and painful.

```sh
# Windows (Android Studio's bundled JDK):
"C:\Program Files\Android\Android Studio\jbr\bin\keytool.exe" -genkeypair -v -keystore C:\Users\<you>\keystores\retrocompanion-upload.jks -alias upload -keyalg RSA -keysize 2048 -validity 10000
```

`keytool` prompts for the keystore password, the key password and your details. Then:

1. Copy `keystore.properties.example` to `keystore.properties` (git-ignored) in the repo root.
2. Fill in the path, alias and the two passwords.
3. `./gradlew bundleRelease` (or `assembleRelease`) now produces a **signed** build. Without
   `keystore.properties` the release build is simply unsigned, which is what CI produces.

`keyPassword` is the answer you gave to keytool's "key password" prompt. If you pressed Enter there,
it is the same as `storePassword` (modern PKCS12 keystores use one password for both).

**GitHub Releases only (no Play):** this keystore is the real signing key. Phones check every
update against it, so if it is lost nobody can update in place (they must uninstall and lose their
data). Keep a backup.

**Google Play only:** this is the *upload* key. When you first upload, enroll in **Play App
Signing** in the Play Console: Google then holds the real app-signing key and re-signs what you
upload, so a lost upload key can be reset. An app already installed from GitHub can't update to a
Play build signed with a different key, so decide up front which route a given key is for.

## Each release (Google Play)

1. **Bump the version** in `app/build.gradle.kts` (`defaultConfig`): `versionCode` must increase on
   every upload to Play (it is an integer, never reused); `versionName` is the human-readable one
   shown to users (e.g. `1.0`, `1.1`).
2. Make sure `master` is green in CI and `./gradlew testDebugUnitTest lintDebug lintRelease` pass.
3. `./gradlew bundleRelease` (with `keystore.properties` present) produces the signed
   **`app/build/outputs/bundle/release/app-release.aab`**. That is what you upload to Play.
   For a sideloadable APK use `./gradlew assembleRelease` (`app/build/outputs/apk/release/`).
4. Test the minified build on a device (checklist below).
5. Upload the AAB to a Play testing track (internal first). Play uses the R8 mapping file inside
   the bundle to de-obfuscate crash reports.
6. Tag the release (`git tag v1.0 && git push origin v1.0`) so the code for each upload can be found.

CI builds an **unsigned** release bundle and runs release lint on every push, so R8 and
release-only problems are caught before this step.

## Publishing on GitHub Releases (no Play account needed)

Pushing a tag like `v1.0` runs `.github/workflows/release.yml`, which builds a **signed** APK and
AAB and attaches them, with a `SHA256SUMS.txt`, to a new GitHub Release. The tag must equal
`v` + `versionName` (tag `v1.0` needs `versionName = "1.0"`), otherwise the workflow fails before
building. Users can sideload the APK, or follow the repo's releases with Obtainium.

One-time setup, in the repo's **Settings > Secrets and variables > Actions**, add four repository
secrets (type them yourself; they are never printed or stored in the repo):

| Secret              | Value                                                                  |
| ------------------- | ---------------------------------------------------------------------- |
| `KEYSTORE_BASE64`   | the keystore file, base64-encoded (see below)                          |
| `KEYSTORE_PASSWORD` | the keystore password                                                  |
| `KEY_ALIAS`         | the key alias (`upload` if you used the command above)                 |
| `KEY_PASSWORD`      | the key password                                                       |

Encode the keystore without line breaks (PowerShell):

```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("C:\Users\<you>\keystores\retrocompanion-upload.jks")) | Set-Clipboard
```

(Git Bash / WSL: `base64 -w0 retrocompanion-upload.jks`.) Avoid backslashes in the passwords, since
they are written to a `.properties` file on the runner.

Each release: bump `versionCode`/`versionName`, commit and push, then
`git tag v1.1 && git push origin v1.1`. Keep using the same keystore for every release: Android only
installs an update over an existing install if it is signed with the same key.

## Minification (R8)

Release builds run R8, but `optimization.packageScope` in `app/build.gradle.kts` limits it to
`androidx.**` and `kotlinx.**`. App code, the Kotlin standard library, Retrofit, OkHttp, Gson, Tink
and Coil are outside that scope, so they are **not renamed or shrunk** (confirm in
`app/build/outputs/mapping/release/mapping.txt`: it should contain no `com.example` classes).

That matters because several classes are saved as JSON by Gson using their field names, with no
`@SerializedName`: `WishlistEntry`, `UnlockStamp`, `AchievementOfTheWeek`, `PollingState`,
`SeenUnlock`. Renaming those fields would silently lose saved data on update.
`app/src/main/keepRules/persisted-models.keep` keeps them explicitly so that stays true even if the
scope is widened. Notes:

- Don't add `kotlin.**` to the scope: R8 then renamed standard library classes that `androidx`
  classes extend, and the release app crashed at startup with `IllegalAccessError` in
  `WorkManagerInitializer`.
- AGP 9 reads keep rules from the `keepRules` source folder, and files there must end in `.keep`
  (a `.pro` file fails the build with a clear message).
- If you add a class that is persisted through Gson without `@SerializedName`, list it in that file.
- After any change to R8 scope or rules, build `assembleRelease`, check `configuration.txt` in the
  same mapping folder shows your rules, and test the minified build on a device (see below).

## Testing the minified build

Debug builds don't exercise R8. Before every upload, install a signed release build and check:
first-run credentials, all six tabs, Game Details, the wishlist (add, restart the app, import),
favorites, Stats heatmap, Achievement of the Week, a share card, notifications (permission and a
real unlock), clearing credentials; then install a newer build over it and confirm saved data
survives.
