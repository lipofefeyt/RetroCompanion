# Play submission checklist

Steps only the account owner can do are marked **(you)**. Never put the API key, keystore or any
password in this repo; the Play "App access" form is where a test account goes.

## Already done
- Application ID `io.github.lipofefeyt.retrocompanion`, `versionCode 2` / `versionName 1.0.1`,
  `targetSdk 37`.
- Signed AAB built by the Release workflow and attached to the GitHub release `v1.0.1`
  (`RetroCompanion-v1.0.1.aab`, with `SHA256SUMS.txt`).
- Privacy policy live at <https://lipofefeyt.github.io/RetroCompanion/privacy>.
- Listing text (`listing.md`) and Data safety answers (`data-safety.md`) drafted and matched to the
  current app.

## Before uploading
1. **(you)** Create a Play Console developer account if there isn't one. Personal accounts created
   after November 2023 must run a **closed test with at least 12 testers opted in for 14 days**
   before they can apply for production access; check the Console's current requirement.
2. **(you)** Decide the signing route (see RELEASING.md). Use the **same** keystore as the GitHub
   builds only if you want GitHub and Play installs to update each other; otherwise enrol in Play App
   Signing so Google holds the app-signing key and the keystore is only the upload key.
3. **(you)** Install the signed release build on a device and run the "Testing the minified build"
   checklist in RELEASING.md (the 1.0.1 startup crash was an R8 problem a debug build hid).
4. **(you)** Produce the graphics: 512 x 512 icon (Android Studio, Image Asset, from the launcher
   icon), 1024 x 500 feature graphic, and at least 2 phone screenshots from an account you're happy to
   show. No RetroAchievements logo.

## In the Play Console
1. Create the app: name RetroCompanion, free, app (not game), default language.
2. Main store listing: paste from `listing.md`; add the graphics.
3. App content: privacy policy URL, ads (no), app access (supply a test username and key here),
   content rating questionnaire, target audience 13+ (not for children), Data safety form from
   `data-safety.md`, and the government/financial/health declarations (none apply).
4. Upload `RetroCompanion-v1.0.1.aab` (or a fresh `./gradlew bundleRelease`) to **Internal testing**
   first, add yourself as a tester and install from the Play link.
5. Promote to closed or production when the account's requirements allow.

## Each later release
Bump `versionCode` (it must increase on every upload), update `versionName`, follow RELEASING.md,
and refresh `listing.md` if features changed.
