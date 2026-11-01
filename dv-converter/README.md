# DV Converter

A desktop app for Windows and macOS that converts **Dolby Vision Profile 7** video
files (UHD Blu-ray rips, usually from MakeMKV) to **Profile 8.1**. Profile 8.1
plays on the Nvidia Shield, Apple TV, Fire TV, Plex, Infuse and most TVs.

Drop files into the window and click **Convert**. Each file is saved next to the
original as `Name.P8.mkv`, or in a folder you choose. Audio, subtitles and
chapters are copied unchanged.

## Download

Builds come from the **Build DV Converter** workflow on GitHub Actions:

1. Open the repository's **Actions** tab and choose the latest successful
   **Build DV Converter** run.
2. Download `DV-Converter-Windows` or `DV-Converter-macOS-AppleSilicon`.
3. Unzip it.
   - **Windows:** open the `DV Converter` folder and run `DV Converter.exe`. If
     SmartScreen warns about an unrecognized app, click **More info → Run anyway**.
   - **macOS:** move `DV Converter.app` to Applications. The app isn't signed, so
     the first time, right-click it and choose **Open**, then **Open** again.

Pushing a tag such as `dv-converter-v1.0.0` also attaches both builds to a GitHub
release.

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
