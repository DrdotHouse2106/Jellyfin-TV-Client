#!/usr/bin/env bash
# Quallenkino: libass-android (ass-kt + ass-media) selbst bauen – mit libass 0.17.5, FreeType 2.14.3 und
# FriBidi 1.0.18, nur arm64-v8a und armeabi-v7a, mit Grenzen für Schrift-Anhänge aus MKV-Dateien.
# Alle Quellen sind auf Commits gepinnt (siehe PINS). Läuft in CI (.github/workflows/libass-build.yaml);
# Ergebnis: ein Maven-Verzeichnis als ZIP (out/libass-android-maven.zip).
set -euo pipefail

HERE="$(cd "$(dirname "$0")" && pwd)"
# shellcheck source=PINS
source "$HERE/PINS"
WORK="${WORK:-$HERE/build}"
OUT="${OUT:-$HERE/out}"
REPO_LOCAL="$WORK/m2"

rm -rf "$WORK" "$OUT"
mkdir -p "$WORK" "$OUT"

pin() { # Verzeichnis, Remote, Commit
	git -C "$1" remote set-url origin "$2"
	git -C "$1" fetch --depth 1 origin "$3"
	git -C "$1" checkout --quiet "$3"
	test "$(git -C "$1" rev-parse HEAD)" = "$3"
}

git clone --quiet https://github.com/peerless2012/libass-android.git "$WORK/src"
cd "$WORK/src"
git checkout --quiet "$LIBASS_ANDROID_COMMIT"
git submodule update --init --quiet lib_ass/src/main/cpp/libass-cmake
CMAKE_DIR="$WORK/src/lib_ass/src/main/cpp/libass-cmake"
git -C "$CMAKE_DIR" checkout --quiet "$LIBASS_CMAKE_COMMIT"
# FreeType vom offiziellen GitHub-Spiegel (gitlab.freedesktop.org ist nicht überall erreichbar)
git -C "$CMAKE_DIR" submodule init --quiet
git -C "$CMAKE_DIR" config submodule.src/freetype.url https://github.com/freetype/freetype.git
if [ -n "${SKIP_FONTCONFIG_FETCH:-}" ]; then git -C "$CMAKE_DIR" config submodule.src/fontconfig.update none; fi
git -C "$CMAKE_DIR" submodule update --recursive --quiet

# Neuere Stände als upstream
pin "$CMAKE_DIR/src/ass" https://github.com/libass/libass.git "$LIBASS_COMMIT"
pin "$CMAKE_DIR/src/freetype" https://github.com/freetype/freetype.git "$FREETYPE_COMMIT"
pin "$CMAKE_DIR/src/fribidi" https://github.com/fribidi/fribidi.git "$FRIBIDI_COMMIT"

# libass-Version prüfen (Header, gegen den gebaut wird)
VERSION_LINE="$(grep -E '^#define LIBASS_VERSION ' "$CMAKE_DIR/src/ass/libass/ass.h")"
echo "libass: $VERSION_LINE"
test "$(echo "$VERSION_LINE" | awk '{print $3}')" = "$LIBASS_VERSION_EXPECTED"
# Upstream liefert eigene Kopien der Header mit (0.17.3) – durch die gebauten ersetzen
cp "$CMAKE_DIR/src/ass/libass/ass.h" "$CMAKE_DIR/src/ass/libass/ass_types.h" lib_ass/src/main/cpp/include/ass/

# Quallenkino-Änderungen
git apply "$HERE"/patches/*.patch
sed -i "s/^VERSION_NAME=.*/VERSION_NAME=$ARTIFACT_VERSION/" gradle.properties
# Ohne Maven-Central-Signatur (lokal veröffentlicht)
sed -i '/signAllPublications()/d' lib_ass/build.gradle.kts lib_ass_kt/build.gradle.kts lib_ass_media/build.gradle.kts
# Nur die Architekturen der Fernseher-Sticks
for module in lib_ass lib_ass_kt; do
	sed -i 's/^\(    defaultConfig {\)$/\1\n        ndk { abiFilters += listOf("arm64-v8a", "armeabi-v7a") }/' "$module/build.gradle.kts"
	grep -q 'abiFilters += listOf("arm64-v8a", "armeabi-v7a")' "$module/build.gradle.kts"
done

# Vorbereitung wie upstream (.github/actions/setup-libraries)
(cd "$CMAKE_DIR/src/unibreak" && NOCONFIGURE=1 ./autogen.sh)
(cd "$CMAKE_DIR/src/fribidi" && NOCONFIGURE=1 ./autogen.sh)
(cd "$CMAKE_DIR/src/fontconfig" && NOCONFIGURE=1 ./autogen.sh)
(cd "$CMAKE_DIR/src/ass" && ./autogen.sh)
(cd "$CMAKE_DIR/src/expat/expat" && ./buildconf.sh)

./gradlew --no-daemon --no-configuration-cache -Dmaven.repo.local="$REPO_LOCAL" \
	:lib_ass_kt:publishToMavenLocal :lib_ass_media:publishToMavenLocal

# Prüfen: nur zwei Architekturen, libass.so enthalten
KT_AAR="$REPO_LOCAL/io/github/peerless2012/ass-kt/$ARTIFACT_VERSION/ass-kt-$ARTIFACT_VERSION.aar"
unzip -l "$KT_AAR" | grep '\.so$'
test "$(unzip -l "$KT_AAR" | grep -c 'jni/.*/libass\.so$')" = 2
! unzip -l "$KT_AAR" | grep -qE 'jni/(x86|x86_64)/'
test -f "$REPO_LOCAL/io/github/peerless2012/ass-media/$ARTIFACT_VERSION/ass-media-$ARTIFACT_VERSION.aar"

# Quellstände für die Lizenzhinweise
{
	echo "libass-android $LIBASS_ANDROID_COMMIT (MIT)"
	echo "libass-cmake $LIBASS_CMAKE_COMMIT"
	for lib in ass freetype harfbuzz fribidi fontconfig expat unibreak; do
		echo "$lib $(git -C "$CMAKE_DIR/src/$lib" describe --tags --always 2>/dev/null) $(git -C "$CMAKE_DIR/src/$lib" rev-parse HEAD)"
	done
	echo "$VERSION_LINE"
} | tee "$OUT/SOURCES.txt"

(cd "$REPO_LOCAL" && zip -qr "$OUT/libass-android-maven.zip" io/github/peerless2012)
sha256sum "$OUT/libass-android-maven.zip" | tee "$OUT/libass-android-maven.zip.sha256"
