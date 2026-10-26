# Release

## v1.3.0

- `versionCode` 6, `versionName` 1.3.0
- **Backup otomatis** ke backend `memberkit-api` (Postgres di VPS Coolify) setiap app dibuka dan setiap member disimpan/dihapus
- **Log error otomatis**: `App` memasang `Thread.setDefaultUncaughtExceptionHandler`, tangkap stack trace, simpan ke antrean, kirim ke backend segera; kalau offline dikirim saat app dibuka lagi
- Layar Update menampilkan status backup terakhir
- Konfigurasi `BACKEND_URL` / `BACKEND_TOKEN` masuk lewat `~/.gradle/gradle.properties` → `buildConfigField`, tidak di-commit
- APK: https://github.com/djvpri/memberkit/releases/download/v1.3.0/memberkit-1.3.0.apk

## v1.2.2

- `versionCode` 5, `versionName` 1.2.2
- **Fix crash**: layar Nota menutup app karena `R.id.back` di-cast ke `TextView` padahal `ImageButton` (`ClassCastException`)
- Tambah `LayoutIdGuardTest` — memverifikasi setiap `findViewById<T>` cocok dengan tag layout; menangkap bug sejenis tanpa emulator
- APK: https://github.com/djvpri/memberkit/releases/download/v1.2.2/memberkit-1.2.2.apk
- SHA-256: `16614d8d219c057bc67b1b83258a9bc50320d67fa9b679705dfedc4efc22b7f7`

## v1.2.1

- `versionCode` 4, `versionName` 1.2.1
- Badge versi (`v1.2.1`) tampil di beranda, di samping jumlah member — sumbernya `BuildConfig.VERSION_NAME`, otomatis ikut naik
- APK: https://github.com/djvpri/memberkit/releases/download/v1.2.1/memberkit-1.2.1.apk
- SHA-256: `f4a49993348fa5e66e407cb2aa0d2c6ea4deb78d292b786928fa39e55d3c0abc`

## v1.2.0

- `versionCode` 3, `versionName` 1.2.0
- Auto update dari GitHub Releases (`releases/latest`, tanpa token)
- Tombol **Cek update** di header layar Member → `UpdateActivity`
- Izin baru: `INTERNET`, `REQUEST_INSTALL_PACKAGES`
- APK: https://github.com/djvpri/memberkit/releases/download/v1.2.0/memberkit-1.2.0.apk
- SHA-256: `69f5a44cc6a5c2e70d5d02eb7ac690246d33090520a0018779b6f08f38964b38`

### Cara update bekerja

1. `GET https://api.github.com/repos/djvpri/memberkit/releases/latest`
2. Bandingkan `tag_name` vs `BuildConfig.VERSION_NAME` (semver, toleran `v`)
3. Kalau lebih baru → unduh asset `.apk` ke `cacheDir/update/` dengan progress
4. Buka installer sistem via `FileProvider` + `ACTION_VIEW`

**Rilis baru otomatis terdeteksi cukup dengan upload APK ke GitHub Releases** — tidak perlu ubah kode.
Syarat di HP: izinkan "Install unknown apps" untuk MemberKit (sekali saja, diminta sistem saat instalasi pertama).

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
