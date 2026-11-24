# RetroPocket

Pocket-handheld Android Libretro frontend for GB, GBA, NES, SNES, and PS1.

## Legal

RetroPocket **does not** ship copyrighted BIOS files or ROMs. You import your own game folder and, for PlayStation, a BIOS you own (for example `SCPH1001.bin`). Emulation cores are Libretro builds packaged with the app.

## Stack

- Kotlin + Jetpack Compose UI
- Room library + SAF folder scan
- NDK JNI Libretro bridge (`core-libretro`)
- Bundled cores: gambatte, mgba, fceumm, snes9x, pcsx_rearmed

## Modules

- `app` — Compose UI (home, library, detail, play, settings, BIOS)
- `core-data` — Room, ROM scanner, BIOS manager, prefs
- `core-libretro` — JNI session, audio/video/input, packaged `.so` cores

## Build

```bash
export ANDROID_HOME=~/Android/Sdk
./gradlew :app:assembleDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk`

## Use

1. Open RetroPocket → **Add games** → pick a ROM folder.
2. Browse the library, open a title, tap **Play**.
3. For PS1, open **Settings → BIOS Manager** and import your BIOS first.
