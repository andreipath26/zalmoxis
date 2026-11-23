#!/usr/bin/env bash
# One-time setup for stable Android app-update signing. Run only on a trusted machine.
# Never commit, email, or paste the generated keystore or secrets into a chat.
set -euo pipefail
umask 077

repo="andreipath26/zalmoxis"
for tool in gh keytool openssl base64; do
  if ! command -v "$tool" >/dev/null 2>&1; then
    echo "Missing required command: $tool" >&2
    exit 1
  fi
done
gh auth status >/dev/null
storage="${XDG_CONFIG_HOME:-$HOME/.config}/zalmoxis/update-signing"
mkdir -p "$storage"
chmod 700 "$storage"
keystore="$storage/zalmoxis-update-signing.p12"
credentials="$storage/credentials.env"

if [[ -f "$keystore" && ! -f "$credentials" ]]; then
  echo "Existing keystore found without credentials: $keystore" >&2
  echo "Restore its original credentials; do not replace a key used for installed apps." >&2
  exit 1
fi
if [[ ! -f "$keystore" && -f "$credentials" ]]; then
  echo "Credentials exist but the keystore is missing. Restore the keystore from backup." >&2
  exit 1
fi

if [[ ! -f "$keystore" ]]; then
  echo "Generating a persistent app-update signing key. Keep this key and its credentials backed up."
  password="$(openssl rand -hex 32)"
  alias_name="zalmoxis-updates"
  keytool -genkeypair -noprompt -storetype PKCS12 \
    -keystore "$keystore" -alias "$alias_name" \
    -keyalg RSA -keysize 3072 -validity 36500 \
    -storepass "$password" -keypass "$password" \
    -dname "CN=Zalmoxis Android Update,O=Zalmoxis"
  printf 'ZALMOXIS_ALIAS=%q\nZALMOXIS_PASSWORD=%q\n' "$alias_name" "$password" > "$credentials"
  chmod 600 "$credentials" "$keystore"
  unset password
fi

# shellcheck disable=SC1090
source "$credentials"
[[ -n "$ZALMOXIS_ALIAS" && -n "$ZALMOXIS_PASSWORD" ]] || {
  echo "Signing credentials are incomplete." >&2
  exit 1
}

echo "Uploading signing data to GitHub Actions secrets (values will not be printed)."
base64 < "$keystore" | tr -d '\n' | gh secret set ZALMOXIS_SIGNING_KEYSTORE_B64 --repo "$repo"
printf '%s' "$ZALMOXIS_PASSWORD" | gh secret set ZALMOXIS_SIGNING_STORE_PASSWORD --repo "$repo"
printf '%s' "$ZALMOXIS_ALIAS" | gh secret set ZALMOXIS_SIGNING_KEY_ALIAS --repo "$repo"
printf '%s' "$ZALMOXIS_PASSWORD" | gh secret set ZALMOXIS_SIGNING_KEY_PASSWORD --repo "$repo"

echo "Signing secrets configured for $repo."
echo "Back up BOTH files outside the repository:"
echo "  $keystore"
echo "  $credentials"
echo "Retain them for the lifetime of installed Zalmoxis applications."
echo "On existing installations signed with another key, a one-time reinstall may be required."
