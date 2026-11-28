#!/usr/bin/env bash
# Downloads the pinned sing-box-lx libbox AAR (AmneziaWG 1.x/2.0/3.x).
# The AAR is intentionally not stored in git (it is ~120 MB).
set -euo pipefail

VERSION="1.14.4-lx.37"
SHA256="bf4d0a8f60201315c5bd8b6835c66d895cf5a4c932d5d6853f84aa919b5aa687"
URL="https://github.com/Leadaxe/sing-box-lx/releases/download/v${VERSION}/libbox-${VERSION}.aar"

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DEST="$ROOT/ui/libs/libbox.aar"
mkdir -p "$(dirname "$DEST")"

if [ -f "$DEST" ] && echo "$SHA256  $DEST" | sha256sum -c - >/dev/null 2>&1; then
    echo "libbox ${VERSION} already present"
    exit 0
fi

echo "Downloading libbox ${VERSION}..."
curl -L --retry 3 --fail -o "$DEST.tmp" "$URL"
echo "$SHA256  $DEST.tmp" | sha256sum -c -
mv "$DEST.tmp" "$DEST"
echo "libbox ${VERSION} ready at $DEST"
