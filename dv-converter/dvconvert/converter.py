"""Dolby Vision Profile 7 -> 8.1 conversion pipeline (no GUI dependencies).

Steps:
  1. ffmpeg copies the HEVC video out of the container as an Annex B stream and
     pipes it into ``dovi_tool -m 2 convert --discard``, which rewrites every RPU
     to profile 8.1 and drops the enhancement layer.
  2. mkvmerge muxes the new video with the source's audio, subtitles, chapters
     and attachments.
  3. ffprobe checks that the result carries a profile 8 configuration record.
"""
from __future__ import annotations

import json
import os
import re
import shutil
import subprocess
import sys
import tempfile
import threading
from collections import deque
from dataclasses import dataclass
from pathlib import Path
from typing import Callable, Iterator

SUPPORTED_SUFFIXES = {".mkv", ".mp4", ".m4v", ".m2ts", ".ts"}

_NO_WINDOW = getattr(subprocess, "CREATE_NO_WINDOW", 0) if sys.platform == "win32" else 0

LogFn = Callable[[str], None]
ProgressFn = Callable[[float], None]


class ConversionError(Exception):
    """A conversion failed for a reason worth showing to the user."""


class Cancelled(Exception):
    """The user stopped the conversion."""


@dataclass
class ProbeResult:
    stream_index: int | None
    dv_profile: int | None
    dv_level: int | None
    el_present: bool
    bl_compat_id: int | None
    hevc_streams: int
    width: int | None
    height: int | None
    frame_rate: str | None
    duration: float | None
    track_profiles: tuple[int | None, ...] = ()  # Dolby Vision profile of every HEVC track

    @property
    def description(self) -> str:
        if self.dv_profile is None:
            return "No Dolby Vision"
        text = f"Dolby Vision Profile {self.dv_profile}"
        if self.bl_compat_id is not None and self.dv_profile == 8:
            text += f".{self.bl_compat_id}"
        return text

    @property
    def resolution(self) -> str:
        if not self.width or not self.height:
            return ""
        if self.width >= 3800:
            return "4K"
        if self.width >= 1900:
            return "1080p"
        return f"{self.width}×{self.height}"

    def unsupported_reason(self) -> str | None:
        """Why this file can't be converted, or None when it can."""
        if self.stream_index is None:
            return "This file has no HEVC video track."
        if self.dv_profile is None:
            return "This file has no Dolby Vision metadata."
        if self.dv_profile == 8 and 7 in self.track_profiles:
            return "This file already has both a Profile 8.1 and a Profile 7 video track."
        if self.dv_profile == 8:
            return "This file is already Dolby Vision Profile 8."
        if self.dv_profile != 7:
            return (f"This file is Dolby Vision Profile {self.dv_profile}. "
                    "Only Profile 7 files can be converted.")
        if self.hevc_streams > 1:
            return ("The enhancement layer is stored as a separate video track, which is common in "
                    "Blu-ray .m2ts files. Remux the disc with MakeMKV first, then convert the MKV.")
        return None


def _popen(cmd: list[str], **kwargs) -> subprocess.Popen:
    return subprocess.Popen(cmd, creationflags=_NO_WINDOW, **kwargs)


def _run(cmd: list[str]) -> subprocess.CompletedProcess:
    return subprocess.run(cmd, capture_output=True, creationflags=_NO_WINDOW)


def probe(ffprobe: str, path: str | Path) -> ProbeResult:
    """Read the stream layout and Dolby Vision configuration of ``path``."""
    proc = _run([ffprobe, "-v", "error", "-print_format", "json",
                 "-show_format", "-show_streams", str(path)])
    if proc.returncode != 0:
        msg = proc.stderr.decode(errors="replace").strip().splitlines()
        raise ConversionError(f"Couldn't read this file: {msg[-1] if msg else 'ffprobe failed'}")
    data = json.loads(proc.stdout or b"{}")
    streams = data.get("streams", [])

    hevc = [s for s in streams
            if s.get("codec_type") == "video" and s.get("codec_name") == "hevc"
            and not s.get("disposition", {}).get("attached_pic")]
    video = hevc[0] if hevc else None

    def _dovi(stream) -> dict:
        for sd in stream.get("side_data_list", []):
            if "DOVI" in sd.get("side_data_type", ""):
                return sd
        return {}

    def _num(value, kind=float):
        try:
            return kind(value)
        except (TypeError, ValueError):
            return None

    dovi = _dovi(video) if video else {}

    duration = _num(data.get("format", {}).get("duration"))

    rate = None
    if video:
        for key in ("r_frame_rate", "avg_frame_rate"):
            value = video.get(key)
            if value and value not in ("0/0", "0/1"):
                rate = value
                break

    return ProbeResult(
        stream_index=video["index"] if video else None,
        dv_profile=_num(dovi.get("dv_profile"), int),
        dv_level=_num(dovi.get("dv_level"), int),
        el_present=bool(dovi.get("el_present_flag")),
        bl_compat_id=_num(dovi.get("dv_bl_signal_compatibility_id"), int),
        hevc_streams=len(hevc),
        width=_num(video.get("width"), int) if video else None,
        height=_num(video.get("height"), int) if video else None,
        frame_rate=rate,
        duration=duration,
        track_profiles=tuple(_num(_dovi(s).get("dv_profile"), int) for s in hevc),
    )


def default_output_path(src: Path, out_dir: Path | None = None, keep_original: bool = False) -> Path:
    """``Movie.mkv`` -> ``Movie.P8.mkv`` (or ``Movie.P8+P7.mkv``), numbered if that name is taken."""
    folder = out_dir or src.parent
    tag = "P8+P7" if keep_original else "P8"
    candidate = folder / f"{src.stem}.{tag}.mkv"
    n = 2
    while candidate.exists():
        candidate = folder / f"{src.stem}.{tag} ({n}).mkv"
        n += 1
    return candidate


def _iter_output(stream) -> Iterator[str]:
    """Yield text lines from a byte stream, splitting on both \\r and \\n."""
    buf = b""
    while True:
        chunk = stream.read1(4096) if hasattr(stream, "read1") else stream.read(4096)
        if not chunk:
            break
        buf += chunk
        parts = re.split(rb"[\r\n]", buf)
        buf = parts.pop()
        for part in parts:
            if part.strip():
                yield part.decode(errors="replace").strip()
    if buf.strip():
        yield buf.decode(errors="replace").strip()


def _gb(n: float) -> str:
    return f"{n / 1e9:.1f} GB"


class Converter:
    """Runs one conversion. ``cancel()`` may be called from any thread."""

    EXTRACT_SHARE = 0.75  # share of the progress bar spent on step 1

    def __init__(self, tools: dict[str, str], log: LogFn | None = None,
                 progress: ProgressFn | None = None, keep_original: bool = False):
        self.tools = tools
        # Also copy the untouched Profile 7 video into the output as a second video track.
        self.keep_original = keep_original
        self._log = log or (lambda _msg: None)
        self._progress = progress or (lambda _f: None)
        self._cancel = threading.Event()
        self._procs: list[subprocess.Popen] = []
        self._lock = threading.Lock()

    def cancel(self) -> None:
        self._cancel.set()
        with self._lock:
            for p in self._procs:
                if p.poll() is None:
                    p.kill()

    # -- helpers ---------------------------------------------------------

    def _spawn(self, cmd: list[str], **kwargs) -> subprocess.Popen:
        with self._lock:
            if self._cancel.is_set():
                raise Cancelled()
            proc = _popen(cmd, **kwargs)
            self._procs.append(proc)
            return proc

    def _check_cancel(self) -> None:
        if self._cancel.is_set():
            raise Cancelled()

    # -- pipeline --------------------------------------------------------

    def run(self, src: Path, dst: Path, info: ProbeResult | None = None) -> bool:
        """Convert ``src`` into ``dst``. Returns True when the output was verified as Profile 8."""
        src, dst = Path(src), Path(dst)
        info = info or probe(self.tools["ffprobe"], src)
        reason = info.unsupported_reason()
        if reason:
            raise ConversionError(reason)
        if src.resolve() == dst.resolve():
            raise ConversionError("The output file can't be the same as the input file.")

        dst.parent.mkdir(parents=True, exist_ok=True)
        # temporary video stream + final file (which also holds the original video when kept)
        needed = src.stat().st_size * (2.95 if self.keep_original else 2.05)
        free = shutil.disk_usage(dst.parent).free
        if free < needed:
            raise ConversionError(
                f"Not enough free space in {dst.parent}. Needs about {_gb(needed)}, "
                f"{_gb(free)} is free.")

        work = Path(tempfile.mkdtemp(prefix=".dvconvert-", dir=dst.parent))
        partial = dst.with_name(dst.name + ".partial")
        try:
            video = work / "BL_RPU.hevc"
            self._extract_and_convert(src, video, info)
            self._mux(src, video, partial, info)
            self._check_cancel()
            verified = self._verify(partial)
            os.replace(partial, dst)
            self._progress(1.0)
            return verified
        finally:
            shutil.rmtree(work, ignore_errors=True)
            if partial.exists():
                try:
                    partial.unlink()
                except OSError:
                    pass

    def _extract_and_convert(self, src: Path, out: Path, info: ProbeResult) -> None:
        self._log("Step 1 of 3: converting the video stream to Profile 8.1")
        ffmpeg_cmd = [self.tools["ffmpeg"], "-nostdin", "-hide_banner", "-loglevel", "error",
                      "-nostats", "-progress", "pipe:2", "-i", str(src),
                      "-map", f"0:{info.stream_index}", "-c:v", "copy",
                      "-bsf:v", "hevc_mp4toannexb", "-f", "hevc", "-"]
        dovi_cmd = [self.tools["dovi_tool"], "-m", "2", "convert", "--discard", "-", "-o", str(out)]

        ff = self._spawn(ffmpeg_cmd, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
        dv = self._spawn(dovi_cmd, stdin=ff.stdout, stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
        ff.stdout.close()  # dovi_tool owns the read end now

        ff_errors: deque[str] = deque(maxlen=20)
        dv_output: deque[str] = deque(maxlen=20)
        total_us = (info.duration or 0) * 1e6

        def read_ffmpeg():
            for line in _iter_output(ff.stderr):
                m = re.match(r"out_time_us=(\d+)", line)
                if m:
                    if total_us > 0:
                        self._progress(min(int(m.group(1)) / total_us, 1.0) * self.EXTRACT_SHARE)
                elif "=" not in line:
                    ff_errors.append(line)

        def read_dovi():
            for line in _iter_output(dv.stdout):
                dv_output.append(line)

        readers = [threading.Thread(target=read_ffmpeg, daemon=True),
                   threading.Thread(target=read_dovi, daemon=True)]
        for t in readers:
            t.start()
        dv_code = dv.wait()
        ff_code = ff.wait()
        for t in readers:
            t.join(timeout=5)
        self._check_cancel()

        if dv_code != 0:
            for line in dv_output:
                self._log(f"  dovi_tool: {line}")
            raise ConversionError(f"dovi_tool failed (exit code {dv_code}). See the log for details.")
        if ff_code != 0:
            for line in ff_errors:
                self._log(f"  ffmpeg: {line}")
            raise ConversionError(f"ffmpeg couldn't read the video (exit code {ff_code}).")
        if not out.exists() or out.stat().st_size == 0:
            raise ConversionError("dovi_tool produced no output.")
        self._log(f"  Video stream ready ({_gb(out.stat().st_size)})")

    def _mux(self, src: Path, video: Path, out: Path, info: ProbeResult) -> None:
        self._log("Step 2 of 3: adding the original audio, subtitles and chapters")
        cmd = [self.tools["mkvmerge"], "-o", str(out)]
        if info.frame_rate:
            cmd += ["--default-duration", f"0:{info.frame_rate}p"]
        cmd += ["--track-name", "0:Dolby Vision Profile 8.1", "--default-track-flag", "0:yes", str(video)]
        if self.keep_original:
            vid = self._source_video_track(src)
            cmd += ["--video-tracks", str(vid), "--track-name", f"{vid}:Dolby Vision Profile 7",
                    "--default-track-flag", f"{vid}:no", str(src)]
        else:
            cmd += ["--no-video", str(src)]

        proc = self._spawn(cmd, stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
        messages: deque[str] = deque(maxlen=30)
        share = self.EXTRACT_SHARE
        for line in _iter_output(proc.stdout):
            m = re.search(r"Progress: (\d+)%", line)
            if m:
                self._progress(share + (1 - share) * 0.98 * int(m.group(1)) / 100)
            elif line.startswith(("Warning", "Error")):
                messages.append(line)
        code = proc.wait()
        self._check_cancel()
        for line in messages:
            self._log(f"  mkvmerge: {line}")
        if code >= 2:  # 1 means "finished with warnings"
            raise ConversionError(f"mkvmerge failed (exit code {code}). See the log for details.")

    def _source_video_track(self, src: Path) -> int:
        """mkvmerge's track ID for the source's HEVC video track."""
        proc = _run([self.tools["mkvmerge"], "-J", str(src)])
        try:
            tracks = json.loads(proc.stdout or b"{}").get("tracks", [])
        except json.JSONDecodeError:
            tracks = []
        for track in tracks:
            codec = f"{track.get('codec', '')} {track.get('properties', {}).get('codec_id', '')}"
            if track.get("type") == "video" and ("HEVC" in codec or "H.265" in codec):
                return int(track["id"])
        raise ConversionError("mkvmerge couldn't find the original video track to keep.")

    def _verify(self, path: Path) -> bool:
        self._log("Step 3 of 3: checking the result")
        try:
            result = probe(self.tools["ffprobe"], path)
        except ConversionError as exc:
            self._log(f"  Couldn't check the result: {exc}")
            return False
        if self.keep_original and result.dv_profile == 8:
            if 7 in result.track_profiles[1:]:
                self._log(f"  Verified: {result.description} (default) + Dolby Vision Profile 7")
                return True
            self._log("  Warning: the Profile 7 track isn't tagged as Dolby Vision in the output.")
            return False
        if result.dv_profile == 8:
            self._log(f"  Verified: {result.description}")
            return True
        self._log(f"  Warning: the output reports '{result.description}'. "
                  "Your mkvmerge may be too old to tag Dolby Vision; update MKVToolNix.")
        return False
