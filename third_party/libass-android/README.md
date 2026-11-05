# libass-android – Eigenbau für Quallenkino

Die ASS/SSA-Untertitel-Effekte nutzen [libass-android](https://github.com/peerless2012/libass-android) (MIT). Die veröffentlichte Fassung 0.5.1 bringt libass 0.17.3 mit (CVE-2026-61626/-61627). Deshalb baut Quallenkino sie selbst:

- **libass 0.17.5**, **FreeType 2.14.3**, **FriBidi 1.0.18**. HarfBuzz 11.3.3, fontconfig, expat 2.7.1 und libunibreak 6.1 bleiben wie in libass-cmake gepinnt. Alle Quellen sind auf Commits festgelegt ([PINS](PINS)).
- Nur **arm64-v8a** und **armeabi-v7a**.
- **Grenzen für Schrift-Anhänge aus MKV-Dateien:** höchstens 20 MB je Schrift, 64 Schriften und 128 MB zusammen; Anhänge ohne Namen oder Typ werden übersprungen statt die Wiedergabe abzubrechen ([patches](patches)).

## Bauen

Über GitHub Actions: Workflow **„libass / Build“** (`.github/workflows/libass-build.yaml`). Er führt [build.sh](build.sh) aus, prüft `LIBASS_VERSION` (0x01705000) und die Architekturen und legt das Release `v0.0.0-libass-<Version>` mit dem Maven-Verzeichnis als ZIP an. Die App-Builds laden dieses ZIP und prüfen es gegen [maven.sha256](maven.sha256).

Lokal (Linux mit Android-SDK/NDK, gperf, gettext, autopoint, libtool, autoconf, ninja): `third_party/libass-android/build.sh`.

## Lizenzen

libass (ISC), libass-android und libass-cmake (MIT), FreeType (FreeType-Lizenz), HarfBuzz (MIT), FriBidi (LGPL-2.1), fontconfig (MIT-artig), expat (MIT), libunibreak (zlib). Siehe NOTICE im Hauptverzeichnis.
