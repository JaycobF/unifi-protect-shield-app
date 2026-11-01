# DV Converter

A desktop app for Windows and macOS that converts **Dolby Vision Profile 7** video
files (UHD Blu-ray rips, usually from MakeMKV) to **Profile 8.1**. Profile 8.1
plays on the Nvidia Shield, Apple TV, Fire TV, Plex, Infuse and most TVs.

Drop files into the window and click **Convert**. Each file is saved next to the
original as `Name.P8.mkv`, or in a folder you choose. Audio, subtitles and
chapters are copied unchanged.

## Keeping Profile 7 in the same file

Tick **Keep Profile 7 in the same file** before converting to get one file
(`Name.P8+P7.mkv`) with two video tracks:

1. **Dolby Vision Profile 8.1**: the default track. The Shield, Apple TV, Fire TV,
   Plex, Infuse and most TVs play this one.
2. **Dolby Vision Profile 7**: the original, untouched video with its
   enhancement layer. Players that support Profile 7 (Kodi on CoreELEC, for
   example) can switch to it in their video-track menu. Players that can't
   switch video tracks ignore it.

The file is almost twice the size of a Profile 8.1-only file, because it holds
both full video streams. From the command line, use
`python -m dvconvert.cli --keep-p7 Movie.mkv`.

## Install

Installers come from the **Build DV Converter** workflow on GitHub Actions:

1. Open the repository's **Actions** tab and choose the latest successful
   **Build DV Converter** run.
2. Download `DV-Converter-Windows` or `DV-Converter-macOS-AppleSilicon` and unzip it.
3. Install:
   - **Windows:** run `DV-Converter-Setup-<version>.exe`. It installs for your
     user account without asking for admin rights, adds a Start menu shortcut
     and an uninstaller, and offers to open the MKVToolNix download page if
     MKVToolNix isn't installed. Because the installer isn't signed,
     SmartScreen may warn about an unrecognized app: click
     **More info → Run anyway**.
   - **macOS:** open `DV-Converter-<version>.dmg` and drag **DV Converter** onto
     **Applications**. The app isn't notarized by Apple, so the first time you
     open it macOS blocks it. Go to **System Settings → Privacy & Security**,
     scroll down and click **Open Anyway** next to DV Converter.

Each build also publishes both installers to the GitHub release
`dv-converter-v<version>` (the version is in `dvconvert/__init__.py`). Raise the
version to start a new release; builds of the same version replace its installers.

## Tools it needs

The conversion is done by free command-line tools. The app finds them
automatically and shows **Tools ready** in the top-right corner when everything
is in place. Click that button to see what's missing or to point to a tool by hand.

| Tool | Windows build | macOS build |
| --- | --- | --- |
| dovi_tool | included | included |
| ffmpeg / ffprobe | included | `brew install ffmpeg` |
| mkvmerge | install [MKVToolNix](https://mkvtoolnix.download/downloads.html#windows) or `winget install MoritzBunkus.MKVToolNix` | `brew install mkvtoolnix` or the [MKVToolNix app](https://mkvtoolnix.download/downloads.html#macosx) |

## What happens to the video

1. `ffmpeg` copies the HEVC video stream out of the file without re-encoding it.
2. `dovi_tool -m 2 convert --discard` rewrites the Dolby Vision metadata (RPU)
   to Profile 8.1 and removes the enhancement layer.
3. `mkvmerge` puts the new video together with the original audio, subtitles
   and chapters.
4. `ffprobe` checks that the new file is tagged as Dolby Vision Profile 8.

There's no re-encoding, so a 4K film takes a few minutes and picture quality
doesn't change. On MEL (minimal enhancement layer) discs the result looks the
same as the original. On FEL (full enhancement layer) discs, the extra detail
in the enhancement layer is dropped. That's the usual trade-off for playing
these files on streaming devices.

You need free disk space of about twice the source file size while a file is
converting.

**Not supported:** files whose enhancement layer is a separate video track (some
raw Blu-ray `.m2ts` files). Remux the disc with MakeMKV first, then convert the MKV.

## Running from source

```bash
cd dv-converter
pip install -r requirements.txt
python main.py                              # desktop app
python -m dvconvert.cli Movie.mkv           # command line
```

To build the app yourself, put `dovi_tool` (and optionally `ffmpeg`/`ffprobe`)
in `dv-converter/bin/`, then run `pip install pyinstaller` and
`pyinstaller --noconfirm dv_converter.spec`. The app appears in `dist/`.
Then make the installer:

- **Windows:** install [Inno Setup 6](https://jrsoftware.org/isdl.php) and run
  `iscc /DAppVersion=1.0.0 installer\windows\dv-converter.iss`.
- **macOS:** run `installer/macos/make-dmg.sh 1.0.0`.

Both write the installer to `dist/`.
