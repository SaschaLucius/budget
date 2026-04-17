# FinanzApp

Eine Android-App zur Budgetverwaltung.

## Release-Workflow

Beim Pushen eines Tags mit dem Prefix `v` (z. B. `v1.0.0`) wird automatisch ein signiertes Release-APK gebaut und als GitHub Release veröffentlicht.

```bash
git tag v1.0.0
git push origin v1.0.0
```

## Benötigte GitHub Secrets

| Secret              | Beschreibung                                         |
| ------------------- | ---------------------------------------------------- |
| `KEYSTORE_BASE64`   | Base64-kodierter Keystore (`base64 -i keystore.jks`) |
| `KEYSTORE_PASSWORD` | Keystore-Passwort                                    |
| `KEY_ALIAS`         | Key-Alias                                            |
| `KEY_PASSWORD`      | Key-Passwort                                         |

### Keystore erstellen

```bash
keytool -genkeypair \
  -alias release \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000 \
  -keystore release.keystore \
  -storepass <KEYSTORE_PASSWORD> \
  -keypass <KEY_PASSWORD>
```

### Keystore als Base64 kodieren

```bash
base64 -i release.keystore | pbcopy
```

Der Inhalt befindet sich jetzt in der Zwischenablage.

### Secrets in GitHub hinterlegen

1. Repository auf GitHub öffnen
2. **Settings → Secrets and variables → Actions → New repository secret**
3. Folgende Secrets anlegen:
   - `KEYSTORE_BASE64` — Einfügen des Base64-Strings aus der Zwischenablage
   - `KEYSTORE_PASSWORD` — Das beim `keytool`-Befehl verwendete Storepass
   - `KEY_ALIAS` — Der gewählte Alias (z. B. `release`)
   - `KEY_PASSWORD` — Das beim `keytool`-Befehl verwendete Keypass
