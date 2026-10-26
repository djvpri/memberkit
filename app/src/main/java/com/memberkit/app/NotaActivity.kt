package com.memberkit.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import java.io.FileOutputStream

class NotaActivity : AppCompatActivity() {

    private lateinit var member: Member
    private lateinit var png: java.io.File

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_nota)

        member = Store(this).all().firstOrNull { it.id == intent.getLongExtra("id", -1L) }
            ?: run { finish(); return }

        val bmp = Nota.build(member)
        findViewById<ImageView>(R.id.preview).setImageBitmap(bmp)

        val file = Nota.png(this, bmp, "nota-${member.id}.png")
        png = file

        findViewById<TextView>(R.id.back).apply {
            setOnClickListener { finish() }
        }
        findViewById<TextView>(R.id.cetak).setOnClickListener {
            Nota.print(this, bmp, "Nota ${member.name}")
        }
        findViewById<TextView>(R.id.wa).setOnClickListener {
            Nota.share(this, file, toWhatsapp = true)
        }
        findViewById<TextView>(R.id.simpan).setOnClickListener { saveToGallery(file) }
    }

    /** ponytail: copy ke Pictures lewat MediaStore; tak butuh izin di API 29+. */
    private fun saveToGallery(file: java.io.File) {
        runCatching {
            val values = android.content.ContentValues().apply {
                put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, file.name)
                put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "image/png")
                if (android.os.Build.VERSION.SDK_INT >= 29) {
                    put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/MemberKit")
                }
            }
            val uri: Uri? = contentResolver.insert(
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values
            )
            uri?.let { u ->
                contentResolver.openOutputStream(u)?.use { out ->
                    file.inputStream().use { it.copyTo(out) }
                }
                Toast.makeText(this, "Nota disimpan ke Galeri", Toast.LENGTH_SHORT).show()
            } ?: throw IllegalStateException("MediaStore null")
        }.onFailure {
            runCatching {
                val dst = java.io.File(
                    getExternalFilesDir(null), file.name
                )
                file.inputStream().use { i -> FileOutputStream(dst).use { o -> i.copyTo(o) } }
                Toast.makeText(this, "Tersimpan: ${dst.absolutePath}", Toast.LENGTH_LONG).show()
            }.onFailure { e ->
                Toast.makeText(this, "Gagal simpan: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}
