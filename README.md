# MemberKit

App Android native (Kotlin) untuk manage member langganan aplikasi: catat nama member, nama app, tanggal mulai, tanggal selesai, dan keterangan.

## Fitur

- List member dengan status otomatis: **Aktif**, **Segera Habis** (≤ 7 hari), **Expired**
- Cari nama / aplikasi / keterangan
- Filter cepat: Semua, Aktif, Segera Habis, Expired
- Tambah, edit, hapus member (long-press item untuk hapus)
- Preset durasi 1 / 3 / 6 / 12 bulan → tanggal selesai otomatis
- Data tersimpan lokal (JSON di internal storage) — offline-first, tanpa izin jaringan
- Validasi form inline: nama & app wajib, tanggal selesai harus setelah tanggal mulai
- **Nota gaya minimarket** (32 kolom monospace, 384 px = 58 mm @203dpi): pratinjau, cetak/PDF via Android Print, simpan ke Galeri, kirim sebagai gambar PNG ke WhatsApp

## Build

Butuh JDK 17 dan Android SDK (compileSdk 34).

```bash
export JAVA_HOME=/path/to/jdk17
./gradlew assembleDebug
# hasil: app/build/outputs/apk/debug/app-debug.apk
```

Test logic:

```bash
./gradlew testDebugUnitTest
```

## Struktur

```
app/src/main/java/com/memberkit/app/
  Member.kt          model + Store (JSON) + helper tanggal
  MainActivity.kt    list, search, filter
  MemberAdapter.kt   RecyclerView adapter
  EditActivity.kt    form tambah/edit + date picker
  Nota.kt            render nota monospace ke Bitmap, share PNG, cetak PDF
  NotaActivity.kt    pratinjau + tombol Simpan / Cetak / Kirim WA
```

## Nota

Format 32 kolom monospace, lebar gambar **384 px** (pas untuk printer thermal 58 mm @203 dpi).
Isi: header MEMBERKIT, No. nota (`MK-yyMMdd-id`), tanggal & kasir, data member + aplikasi,
periode mulai/selesai/durasi, baris item + harga, TOTAL/BAYAR/KEMBALI, stempel LUNAS,
catatan, footer. Kirim via WhatsApp memakai `FileProvider` + `ACTION_SEND` (`image/png`).

## Desain

Arah visual: Swiss Minimal (teal). Ikon SVG, target sentuh ≥ 48dp, kontras teks ≥ 4.5:1, spacing 4/8dp.
