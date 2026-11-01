"""Locate the external command-line tools the converter drives."""
from __future__ import annotations

import glob
import os
import shutil
import sys
from pathlib import Path

TOOL_NAMES = ("ffmpeg", "ffprobe", "dovi_tool", "mkvmerge")

IS_WINDOWS = sys.platform == "win32"
IS_MAC = sys.platform == "darwin"


def _exe(name: str) -> str:
    return f"{name}.exe" if IS_WINDOWS else name


def bundled_bin_dir() -> Path:
    """Directory holding tools shipped inside the packaged app (may not exist)."""
    if getattr(sys, "frozen", False):
        return Path(getattr(sys, "_MEIPASS", Path(sys.executable).parent)) / "bin"
    return Path(__file__).resolve().parent.parent / "bin"


def _search_dirs(name: str) -> list[Path]:
    """Common install locations that a GUI app's PATH usually misses."""
    home = Path.home()
    if IS_MAC:
        dirs = [Path("/opt/homebrew/bin"), Path("/usr/local/bin"), home / ".cargo/bin"]
        if name == "mkvmerge":
            dirs += [Path(p) for p in sorted(glob.glob("/Applications/MKVToolNix*.app/Contents/MacOS"), reverse=True)]
        return dirs
    if IS_WINDOWS:
        local = Path(os.environ.get("LOCALAPPDATA", home / "AppData/Local"))
        pf = [Path(os.environ.get(v, "")) for v in ("ProgramFiles", "ProgramFiles(x86)") if os.environ.get(v)]
        dirs = [local / "Microsoft/WinGet/Links", home / "scoop/shims", Path("C:/ProgramData/chocolatey/bin"),
                Path("C:/ffmpeg/bin"), home / ".cargo/bin"]
        dirs += [p / "MKVToolNix" for p in pf]
        dirs += [Path(p) for p in glob.glob(str(local / "Microsoft/WinGet/Packages/Gyan.FFmpeg*/*/bin"))]
        return dirs
    return [Path("/usr/local/bin"), Path("/usr/bin"), home / ".cargo/bin", home / ".local/bin"]


def _usable(path: Path) -> bool:
    return path.is_file() and os.access(path, os.X_OK)


def find_tool(name: str, override: str | None = None) -> str | None:
    """Return the path to ``name``, preferring a user override, then the bundled copy."""
    if override and _usable(Path(override)):
        return override
    bundled = bundled_bin_dir() / _exe(name)
    if _usable(bundled):
        return str(bundled)
    found = shutil.which(name)
    if found:
        return found
    for d in _search_dirs(name):
        candidate = d / _exe(name)
        if _usable(candidate):
            return str(candidate)
    return None


def find_all(overrides: dict[str, str] | None = None) -> dict[str, str | None]:
    overrides = overrides or {}
    return {name: find_tool(name, overrides.get(name)) for name in TOOL_NAMES}


def install_hint(name: str) -> str:
    """Short, platform-specific instructions for getting a missing tool (HTML)."""
    dovi = '<a href="https://github.com/quietvoid/dovi_tool/releases">Download dovi_tool</a>'
    if IS_MAC:
        hints = {
            "ffmpeg": "Run <code>brew install ffmpeg</code> in Terminal.",
            "ffprobe": "Comes with ffmpeg: <code>brew install ffmpeg</code>.",
            "mkvmerge": 'Run <code>brew install mkvtoolnix</code>, or install the '
                        '<a href="https://mkvtoolnix.download/downloads.html#macosx">MKVToolNix app</a>.',
            "dovi_tool": f"{dovi} (the macOS zip), then click Locate.",
        }
    elif IS_WINDOWS:
        hints = {
            "ffmpeg": "Run <code>winget install Gyan.FFmpeg</code>, then restart this app.",
            "ffprobe": "Comes with ffmpeg: <code>winget install Gyan.FFmpeg</code>.",
            "mkvmerge": 'Install <a href="https://mkvtoolnix.download/downloads.html#windows">MKVToolNix</a> '
                        "or run <code>winget install MoritzBunkus.MKVToolNix</code>.",
            "dovi_tool": f"{dovi} (the windows zip), then click Locate.",
        }
    else:
        hints = {
            "ffmpeg": "Install ffmpeg from your package manager.",
            "ffprobe": "Comes with ffmpeg.",
            "mkvmerge": "Install mkvtoolnix from your package manager.",
            "dovi_tool": f"{dovi}, then click Locate.",
        }
    return hints[name]
