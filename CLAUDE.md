# Working rules for this project

- **Never request, print, or commit the real RetroAchievements API key.** It's entered by the user
  at runtime into the Settings screen and stored encrypted on-device; it never belongs in source,
  commit messages, issues, or test files. Tests must use sample/fake data only — no real key, no
  real account data.
- **Don't commit `local.properties`** (it's git-ignored; holds a machine-local `sdk.dir`) or
  line-ending-only diffs.
- **This repo may be worked on from WSL while the Android SDK/Android Studio live on Windows.**
  Don't change `sdk.dir` to "fix" a WSL build failure. If `./gradlew` can't resolve the SDK from
  WSL, use `cmd.exe /c "gradlew.bat <task> --console=plain"` from the project's `/mnt/c/...` path
  instead. If neither works, stop and say so rather than editing SDK paths.
- **Check the current RetroAchievements API docs** (https://api-docs.retroachievements.org) before
  relying on an endpoint path, query parameter name, or response field name — several have changed
  or been mis-copied in the past in this repo (e.g. the recently-played-games endpoint name and its
  count parameter).
- **Don't use `EncryptedSharedPreferences`** for credential storage; use DataStore with Tink AEAD
  encryption, keyset protected by Android Keystore (see `data/local/CredentialsCipher.kt`).
- **One commit per GitHub issue** when doing issue-driven work; use `Closes #N` only when every
  acceptance criterion in that issue is met, `Refs #N` (issue stays open) when a criterion needs a
  real device/emulator or a real RetroAchievements account to verify.
