# RetroCompanion Privacy Policy

_Last updated: 18 November 2026_

RetroCompanion is a free Android app that shows your [RetroAchievements](https://retroachievements.org)
progress. It is an unofficial, fan-made app and is not affiliated with, endorsed by, or sponsored by
RetroAchievements. This policy explains what the app does with your information.

**In short:** the app has no accounts, no ads, no analytics and no tracking. The developer does not
receive any of your data. Everything is stored on your device, and the only place the app sends data
is RetroAchievements itself, to get your own achievement information.

## What is stored on your device

- **Your RetroAchievements username and Web API key.** You enter these in Settings. They are encrypted
  on the device (AES-GCM through the Tink library, with the key protected by the Android Keystore) and
  are never written to logs.
- **Your preferences and lists:** your favorite games, your wishlist, your theme choice and whether
  unlock notifications are turned on.
- **Cached data:** copies of what RetroAchievements returned (games, achievements, your progress) so
  the app can show something while offline or while refreshing.
- **Background-check state:** if you turn on unlock notifications, which unlocks the app has already
  told you about, so it doesn't notify you twice.

This data stays on your device. Android's backup may include your wishlist in your own device backup
to your Google account; your credentials and cached data are excluded from backup.

## What the app sends over the network

- **To RetroAchievements (`retroachievements.org`)**, over HTTPS: requests for your profile, recently
  played games, achievements and progress. Each request includes your username and your Web API key,
  which is how RetroAchievements knows whose data to return. If you turn on unlock notifications, the
  app makes one such request about every 15 minutes in the background.
- **To RetroAchievements' image server (`media.retroachievements.org`):** requests for game icons,
  achievement badges and avatars. Like any web request, this reveals your IP address to that server.

RetroAchievements handles the information you send it under its own terms and privacy practices; see
<https://retroachievements.org/terms>. The developer of RetroCompanion has no access to those requests
or to your RetroAchievements account.

## What the app does not do

- It does not collect analytics, crash reports or advertising identifiers.
- It does not show ads, sell data or share data with anyone other than RetroAchievements as described
  above.
- It does not read your contacts, location, photos, microphone or camera.
- It is not directed at children.

## Notifications and sharing

- **Notifications** are optional and off by default. The app asks for the notification permission
  only when you turn them on, and the notifications are created on your device.
- **Share cards** are images the app creates on your device only when you tap Share. They show a game
  or achievement (title, points, rarity, date) and do not contain your username or API key. You choose
  where they go through Android's share sheet.

## Deleting your data

- **In the app:** Settings → **Clear** removes your stored username and API key, favorites, wishlist,
  notification setting and cached data from the device.
- **Uninstalling** the app removes everything it stored.
- Your data on RetroAchievements itself is managed on that site; the app cannot change or delete it.
  You can also revoke or reset your Web API key at any time in your RetroAchievements settings.

## Changes to this policy

If the app's handling of data changes, this page will be updated and the date above changed.

## Contact

Questions or concerns: please open an issue at
<https://github.com/lipofefeyt/RetroCompanion/issues>.
