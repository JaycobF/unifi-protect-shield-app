#!/usr/bin/env bash
# Convert a Dolby Vision Profile 7 (dual-layer, BL+EL) MKV into Profile 8.1
# (single-layer HDR10-compatible base layer + RPU). Audio, subtitles and
# chapters are copied unchanged; the enhancement layer is discarded.
#
# Requires: ffmpeg, ffprobe, dovi_tool (github.com/quietvoid/dovi_tool), mkvmerge
#
# Usage: dv-p7-to-p81.sh input.mkv [output.mkv]
set -euo pipefail

if [[ $# -lt 1 || $# -gt 2 ]]; then
  echo "Usage: $0 input.mkv [output.mkv]" >&2
  exit 1
fi

in=$1
out=${2:-"${in%.*}.P8.mkv"}

for tool in ffmpeg ffprobe dovi_tool mkvmerge; do
  command -v "$tool" >/dev/null || { echo "Missing required tool: $tool" >&2; exit 1; }
done

[[ -f $in ]] || { echo "Input not found: $in" >&2; exit 1; }
[[ $in -ef $out ]] && { echo "Output must differ from input" >&2; exit 1; }

work=$(mktemp -d "${TMPDIR:-/tmp}/dvconv.XXXXXX")
trap 'rm -rf "$work"' EXIT

fps=$(ffprobe -v error -select_streams v:0 -show_entries stream=r_frame_rate \
  -of default=nw=1:nk=1 "$in")

echo "==> Extracting HEVC stream and converting RPU to profile 8.1"
# Mode 2 rewrites the RPU to 8.1; --discard drops the enhancement layer NALs.
ffmpeg -nostdin -loglevel error -i "$in" -map 0:v:0 -c:v copy \
  -bsf:v hevc_mp4toannexb -f hevc - \
  | dovi_tool -m 2 convert --discard - -o "$work/BL_RPU.hevc"

echo "==> Verifying RPU profile"
dovi_tool extract-rpu "$work/BL_RPU.hevc" -o "$work/RPU.bin"
dovi_tool info -i "$work/RPU.bin" -f 0 | grep -i profile || true

echo "==> Remuxing with original audio, subtitles and chapters"
mkvmerge -q -o "$out" \
  --default-duration "0:${fps}p" "$work/BL_RPU.hevc" \
  --no-video "$in"

echo "Done: $out"
