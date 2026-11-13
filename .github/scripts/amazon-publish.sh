#!/usr/bin/env bash
# Quallenkino: Amazon-Fassung über die Amazon Appstore App Submission API veröffentlichen.
# Aufruf: amazon-publish.sh <APK> <Version> <commit:true|false>
# Umgebung (GitHub-Secrets): AMAZON_CLIENT_ID, AMAZON_CLIENT_SECRET, AMAZON_APP_ID
# Ablauf: Token holen → offenes Edit nehmen oder anlegen → APK ersetzen → Versionshinweise → prüfen → (einreichen)
set -euo pipefail
APK="$1"; VERSION="$2"; COMMIT="${3:-false}"
: "${AMAZON_CLIENT_ID:?fehlt}" "${AMAZON_CLIENT_SECRET:?fehlt}" "${AMAZON_APP_ID:?fehlt}"
# Nur die Amazon-Fassung hochladen: richtige Paket-ID, ohne Selbst-Update-Berechtigung
AAPT2="$(ls -d "$ANDROID_HOME"/build-tools/* | sort -V | tail -1)/aapt2"
BADGING="$("$AAPT2" dump badging "$APK")"
grep -q "^package: name='de.drdothouse.jellyfintv'" <<<"$BADGING" || { echo "::error::Falsche Paket-ID"; exit 1; }
! grep -q "REQUEST_INSTALL_PACKAGES" <<<"$BADGING" || { echo "::error::$APK ist nicht die Amazon-Fassung (enthält REQUEST_INSTALL_PACKAGES)"; exit 1; }
CODE_NEW="$(sed -n "s/^package: .*versionCode='\([0-9]*\)'.*/\1/p" <<<"$BADGING")"
echo "APK $APK: Version $VERSION, versionCode $CODE_NEW"

API="https://developer.amazon.com/api/appstore/v1/applications/$AMAZON_APP_ID"
H="$(mktemp)"

TOKEN="$(curl -fsS https://api.amazon.com/auth/o2/token \
	--data-urlencode grant_type=client_credentials \
	--data-urlencode "client_id=$AMAZON_CLIENT_ID" \
	--data-urlencode "client_secret=$AMAZON_CLIENT_SECRET" \
	--data-urlencode scope=appstore::apps:readwrite | jq -r .access_token)"
echo "::add-mask::$TOKEN"

# api METHODE PFAD [curl-Optionen…] – Antwort auf stdout, Kopfzeilen in $H
api() { local m="$1" p="$2"; shift 2; curl -fsS -X "$m" -D "$H" -H "Authorization: Bearer $TOKEN" "$@" "$API$p"; }
etag() { api GET "$1" -o /dev/null; tr -d '\r' < "$H" | awk 'tolower($1)=="etag:"{print $2}'; }

EDIT="$(api GET /edits | jq -r '.id // empty' || true)"
if [ -z "$EDIT" ]; then EDIT="$(api POST /edits | jq -r .id)"; echo "Neues Edit $EDIT"; else echo "Offenes Edit $EDIT wird weiterverwendet"; fi

# APK ersetzen (Gerätezuordnung bleibt erhalten); gleiche Version nicht doppelt hochladen
APKS="$(api GET "/edits/$EDIT/apks")"
[ "$(jq length <<<"$APKS")" -eq 1 ] || { echo "::error::Erwartet genau eine APK im Edit, gefunden: $(jq length <<<"$APKS")"; exit 1; }
APK_ID="$(jq -r '.[0].id' <<<"$APKS")"
CODE_OLD="$(jq -r '.[0].versionCode' <<<"$APKS")"
if [ "$CODE_NEW" = "$CODE_OLD" ]; then
	echo "Version $VERSION ($CODE_NEW) liegt schon im Edit – kein erneuter Upload"
else
	api PUT "/edits/$EDIT/apks/$APK_ID/replace" -H "If-Match: $(etag "/edits/$EDIT/apks/$APK_ID")" \
		-H "Content-Type: application/vnd.android.package-archive" --data-binary "@$APK" | jq -c '{id, versionCode, name}'
fi

# Versionshinweise: Abschnitt dieser Version aus CHANGELOG-TV.md (ohne Markdown, gekürzt)
NOTES_DE="$(awk -v v="## $VERSION " 'index($0,v)==1{f=1;next} /^## /{f=0} f' CHANGELOG-TV.md \
	| sed -E 's/\*\*//g; s/`//g; s/^- /• /' | sed '/^$/d')"
[ -n "$NOTES_DE" ] || NOTES_DE="Fehlerbehebungen und Verbesserungen."
NOTES_EN="Bug fixes and improvements. Details: https://github.com/DrdotHouse2106/Quallenkino-AndroidTV/blob/main/CHANGELOG-TV.md"
for LANG_CODE in $(api GET "/edits/$EDIT/listings" | jq -r '.listings | keys[]'); do
	case "$LANG_CODE" in de*) NOTES="$NOTES_DE" ;; *) NOTES="$NOTES_EN" ;; esac
	P="/edits/$EDIT/listings/$LANG_CODE"
	TAG="$(etag "$P")"
	api GET "$P" | jq --arg n "$NOTES" '.recentChanges = $n[0:1200]' \
		| api PUT "$P" -H "If-Match: $TAG" -H "Content-Type: application/json" --data-binary @- -o /dev/null
	echo "Versionshinweise gesetzt: $LANG_CODE"
done

api POST "/edits/$EDIT/validate" | jq .
if [ "$COMMIT" = "true" ]; then
	api POST "/edits/$EDIT/commit" -H "If-Match: $(etag "/edits/$EDIT")" | jq .
	echo "Eingereicht: Quallenkino $VERSION (Edit $EDIT)"
else
	echo "::notice::Edit $EDIT vorbereitet, aber NICHT eingereicht – bitte in der Amazon-Developer-Konsole prüfen."
fi
