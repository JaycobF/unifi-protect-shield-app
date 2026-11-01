#!/usr/bin/env bash
# Package "dist/DV Converter.app" into a drag-to-Applications disk image.
# Usage (from dv-converter/): installer/macos/make-dmg.sh 1.0.0
set -euo pipefail

version=${1:?Usage: $0 VERSION}
app="dist/DV Converter.app"
dmg="dist/DV-Converter-${version}.dmg"

[[ -d $app ]] || { echo "Build the app first: $app not found" >&2; exit 1; }

# The app isn't notarized; an ad-hoc signature over the whole bundle (including
# the bundled dovi_tool) lets Apple silicon Macs run it after the user approves it.
codesign --force --deep --sign - "$app"

stage=$(mktemp -d)
trap 'rm -rf "$stage"' EXIT
cp -R "$app" "$stage/"
ln -s /Applications "$stage/Applications"

rm -f "$dmg"
# hdiutil occasionally reports "Resource busy" on CI machines; retry a few times.
for attempt in 1 2 3 4 5; do
  if hdiutil create -volname "DV Converter" -srcfolder "$stage" -ov -format UDZO "$dmg"; then
    echo "Created $dmg"
    exit 0
  fi
  echo "hdiutil failed (attempt $attempt), retrying..." >&2
  sleep $((attempt * 3))
done
exit 1
