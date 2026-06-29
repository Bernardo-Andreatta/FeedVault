# FeedVault

A private, local media gallery for Android with tag-based organization, custom feeds, clip management, and an encrypted vault. No cloud. No accounts. Your files stay on your device.

## Features

- **Encrypted Vault** — move photos, videos and GIFs into an AES-256 encrypted vault unlocked by password or biometrics (see below)
- **Private gallery** — media stays local on your device; nothing is uploaded
- **Tag & people system** — tag any photo or video, assign people labels, filter instantly
- **Custom feeds** — browse by tag, person, or favorites; shuffle or sort by date/name
- **Clips** — trim and save named segments from videos with their own tag system
- **Grid & feed views** — toggle between scrollable feed and grid layout
- **Batch operations** — select multiple items to tag, assign people, or delete
- **Import from anywhere** — pick a folder or import individual files via SAF
- **Export metadata** — back up all tags, people, and clip data as JSON
- **Fullscreen playback** — tap any video to go fullscreen with mute/unmute and seek

## Encrypted Vault

The Vault is an encrypted mirror of the gallery — same feed, grid, clips, favorites, tags, people, search and sort — but the media is stored **encrypted inside the app's private storage** instead of being visible in the system gallery.

### How it works

1. **Enable** — tap the lock icon in the drawer and set a password.
2. **Add media** — inside the Vault, tap **+** and pick photos, videos or GIFs. Each file is encrypted into the app and the **original is deleted from the device**.
3. **View** — unlock with your password or biometrics; media is decrypted to a temporary cache only while unlocked.
4. **Restore** — select items and tap **Restore** to decrypt them back into the system gallery.

### Encryption

- **Cipher:** AES-256 in **GCM** mode (`AES/GCM/NoPadding`) — authenticated, with a random IV per file and a 128-bit tag.
- **No quality loss:** the original bytes are encrypted as a stream, so a restored file is **bit-for-bit identical** to the original (photos, videos and GIFs alike).
- **Envelope encryption:**
  - A random AES-256 **Data Encryption Key (DEK)** encrypts every file.
  - The DEK is wrapped by a key derived from your **password** via **PBKDF2-HMAC-SHA256**, 120,000 iterations, with a random 16-byte salt.
  - Optionally it is also wrapped by an **Android Keystore** key (hardware-backed when available) gated by **biometrics** (`BiometricPrompt`, `BIOMETRIC_STRONG`, authentication required on every use).
  - The password is always the recovery path; biometrics are only a convenience.
- The DEK exists **only in memory** while the Vault is unlocked.

### Additional protections

- **Auto-lock** when the app goes to the background (screen off, recents, app switch).
- **`FLAG_SECURE`** while in the Vault: media is excluded from the recent-apps preview and from screenshots.
- Decrypted temporary files are wiped on lock.
- Fully offline; nothing leaves the device.

### Important

- **The password cannot be recovered.** If you forget it, the Vault's files are lost permanently.
- Adding to the Vault **deletes the original** from the device (use Restore to reverse it).
- Metadata (file names, tags, people, dates) is stored in the local database in plain text — only the media bytes are encrypted.

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

Coming Soon!

## License

Private source. All rights reserved.
