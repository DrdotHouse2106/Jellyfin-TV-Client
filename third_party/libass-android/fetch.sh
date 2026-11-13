#!/usr/bin/env bash
# Quallenkino: den libass-Eigenbau (Release „v0.0.0-libass-<Version>“) laden, gegen maven.sha256 prüfen und nach
# third_party/maven entpacken. Gradle nimmt io.github.peerless2012 nur von dort (settings.gradle.kts).
set -euo pipefail
HERE="$(cd "$(dirname "$0")" && pwd)"
# shellcheck source=PINS
source "$HERE/PINS"
TARGET="$HERE/../maven"
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT
if [ ! -s "$HERE/maven.sha256" ]; then
	echo "libass-Eigenbau: noch keine Prüfsumme (maven.sha256) – es bleibt die Fassung von Maven Central" >&2
	exit 0
fi
URL="https://github.com/DrdotHouse2106/Quallenkino-AndroidTV/releases/download/v0.0.0-libass-$ARTIFACT_VERSION/libass-android-maven.zip"
curl -fsSL --retry 3 -o "$TMP/libass-android-maven.zip" "$URL"
EXPECTED="$(awk '{print $1}' "$HERE/maven.sha256")"
ACTUAL="$(sha256sum "$TMP/libass-android-maven.zip" | awk '{print $1}')"
if [ "$EXPECTED" != "$ACTUAL" ]; then
	echo "libass-Eigenbau: Prüfsumme passt nicht (erwartet $EXPECTED, erhalten $ACTUAL)" >&2
	exit 1
fi
rm -rf "$TARGET"
mkdir -p "$TARGET"
unzip -q "$TMP/libass-android-maven.zip" -d "$TARGET"
echo "libass-Eigenbau $ARTIFACT_VERSION bereit in third_party/maven"
