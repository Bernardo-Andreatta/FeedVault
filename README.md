# FeedVault

A private, local media gallery for Android with tag-based organization, custom feeds, clip management, and a password-locked vault. No cloud. No accounts. Your files stay on your device.

## Features

- **Private Vault** — move photos, videos and GIFs into the app's private storage, locked behind a password or biometrics (see below)
- **Private gallery** — media stays local on your device; nothing is uploaded
- **Tag & people system** — tag any photo or video, assign people labels, filter instantly
- **Custom feeds** — browse by tag, person, or favorites; shuffle or sort by date/name
- **Clips** — trim and save named segments from videos with their own tag system
- **Grid & feed views** — toggle between scrollable feed and grid layout
- **Batch operations** — select multiple items to tag, assign people, or delete
- **Import from anywhere** — pick a folder or import individual files via SAF
- **Export metadata** — back up all tags, people, and clip data as JSON
- **Fullscreen playback** — tap any video to go fullscreen with mute/unmute and seek

## Private Vault

The Vault is a private mirror of the gallery — same feed, grid, clips, favorites, tags, people, search and sort — but the media is moved **into the app's private storage** and locked behind a password, instead of being visible in the system gallery.

### How it works

1. **Enable** — tap the lock icon in the drawer and set a password.
2. **Add media** — inside the Vault, tap **+** (files) or the folder icon, and pick photos, videos or GIFs. Each file is moved into the app's private storage and the **original is deleted from the device**.
3. **View** — unlock with your password or biometrics; media plays straight from private storage, no copies made.
4. **Restore** — select items and tap **Restore** to move them back into the system gallery.

### What protects your files

The Vault relies on Android's app sandbox rather than per-file encryption:

- **App-private storage** (`filesDir`) — vault media lives in storage that **other apps cannot read**, that **does not show up over USB/MTP** when the phone is plugged into a computer, and that is **not in the system gallery** or any media scan.
- **Encrypted at rest by the OS** — Android's file-based encryption protects this storage whenever the device is locked.
- **Access gate** — entering the Vault requires your **password** (**PBKDF2-HMAC-SHA256**, 120,000 iterations, random 16-byte salt) or **biometrics** (`BiometricPrompt`, `BIOMETRIC_STRONG`, optionally hardware-backed via Android Keystore). The unlock key is held **only in memory** while the Vault is open.

This makes the Vault fast (no decrypt step — even hundreds of large videos open instantly) and lossless (files are byte-for-byte the originals).

### Additional protections

- **Auto-lock** when the app goes to the background (screen off, recents, app switch).
- **`FLAG_SECURE`** while in the Vault: media is excluded from the recent-apps preview and from screenshots.
- Fully offline; nothing leaves the device.

### Important — threat model

- **What it stops:** other apps, the system gallery, and a computer browsing the phone over USB. Good for everyday privacy (a friend or family member scrolling your gallery, files surfacing in another app).
- **What it does *not* stop:** the vault files are stored **unencrypted** inside the app sandbox. Someone with **root access, a custom recovery, or forensic tools on an unlocked/exploited device** could read them. If you need protection against that, do not rely on this Vault alone.
- **The password cannot be recovered.** If you forget it you lose access to the Vault in the app (the files themselves stay in private storage).
- Adding to the Vault **deletes the original** from the device (use Restore to reverse it).
- Metadata (file names, tags, people, dates) is stored in the local database in plain text.

## Screenshots

> Coming soon

## Requirements

- Android 7.0 (API 24) or higher
- No internet permission required

## Building

```bash
git clone https://github.com/Bernardo-Andreatta/FeedVault.git
cd FeedVault
./gradlew assembleRelease
```

Or open in Android Studio and run directly.

## Play Store

Coming Soon

## License

Private source. All rights reserved.
