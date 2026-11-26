# VManager

**Advanced dual-pane file manager for Android** with modern Dark Glass UI.

## Features (v1.0.0)

- **Dual-pane mode** — view two folders side-by-side and transfer files easily
- **Tabs** — open multiple locations
- **Bookmarks** — quick access to favorite folders
- **Batch operations queue** — copy / move / delete with progress
- **ZIP** create & extract
- **Dark Glass UI** (Haze real blur) — can be toggled in Settings
- **Storage usage** indicator
- Material 3 + Jetpack Compose
- Full file access (MANAGE_EXTERNAL_STORAGE)

## Planned

- SFTP / FTP / SMB / WebDAV
- Cloud providers (Google Drive, Dropbox, Yandex Disk…)
- Root + Shizuku support
- 7z / RAR
- Hex editor & APK tools
- Secure vault & shredding
- Folder sync & scheduled backups

## Build

```bash
./gradlew assembleRelease
```

APK will be in `app/build/outputs/apk/release/`.

## Signing

Release builds are signed with a dedicated keystore.  
SHA-1: `15:5F:A7:DE:17:CC:F7:8A:5D:54:1E:33:C5:2C:DB:FE:3F:74:28:D5`  
SHA-256: `7F:83:7F:9E:13:9F:CF:1D:1C:AA:2D:EA:79:52:A9:07:44:BC:3E:DC:C1:C2:FD:4C:08:32:F8:EA:52:24:A1:BA`

## License

MIT
