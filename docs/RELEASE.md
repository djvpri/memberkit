# Release

## v1.1.0 (rilis pertama)

- `versionCode` 2, `versionName` 1.1.0
- R8 minify + resource shrink: APK 5,72 MB → **1,67 MB**
- Signed dengan keystore release (RSA 2048, valid 30 tahun)
- APK: https://github.com/djvpri/memberkit/releases/download/v1.1.0/memberkit-1.1.0.apk
- SHA-256: `2a08c0b31cff6cb2869a1448bb02de20cb184164f5cc2278665f9f8f032bb548`
- Sertifikat SHA-256: `61c1c49528a0d4a895979a787590bcf68f4be42801efcaebdb1e9f544db96886`

### Signing

Keystore **tidak** di-commit (`.gitignore` → `keystore/`). Salinan tersimpan di:

```
C:\Users\Anjuv\AppData\Local\hermes\keystores\memberkit\
  memberkit-release.jks
  password.txt
```

Build release butuh dua file itu ada di `keystore/` root project. Alias: `memberkit`.

**Simpan backup keystore + password.** Kalau hilang, update APK tidak bisa dipasang di atas versi lama.

### Build

```bash
export JAVA_HOME=C:/Users/Anjuv/AppData/Local/Temp/jdk-17.0.20.1+1   # JDK 17, bukan JBR Studio (25)
./gradlew assembleRelease
# app/build/outputs/apk/release/app-release.apk
```

### Rilis berikutnya

1. naikkan `versionCode` (+1) dan `versionName`
2. `./gradlew assembleRelease`
3. buat tag `vX.Y.Z` + upload APK ke GitHub Releases
