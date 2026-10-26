package com.memberkit.app

import android.app.Activity
import android.app.ProgressDialog
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import java.util.concurrent.Executors

class UpdateActivity : AppCompatActivity() {

    private val io = Executors.newSingleThreadExecutor()
    private var latest: Updater.Release? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_update)

        findViewById<View>(R.id.back).setOnClickListener { finish() }
        findViewById<TextView>(R.id.current).text = "Versi terpasang: ${BuildConfig.VERSION_NAME}"

        findViewById<View>(R.id.checkBtn).setOnClickListener { check(manual = true) }
        check(manual = false)
        refreshBackend()
    }

    private fun check(manual: Boolean) {
        val status = findViewById<TextView>(R.id.status)
        val action = findViewById<TextView>(R.id.action)
        status.text = "Memeriksa update…"
        action.visibility = View.GONE

        io.execute {
            val r = Updater.fetchLatest()
            runOnUiThread {
                if (isFinishing) return@runOnUiThread
                if (r == null) {
                    status.text = "Gagal cek update. Periksa koneksi internet."
                    if (manual) toast("Gagal cek update")
                    return@runOnUiThread
                }
                latest = r
                if (Updater.isNewer(r.version, BuildConfig.VERSION_NAME)) {
                    status.text = "Versi baru tersedia: ${r.version}"
                    action.visibility = View.VISIBLE
                    action.text = "Unduh & pasang"
                    action.setOnClickListener { confirmDownload(r) }
                } else {
                    status.text = "Aplikasi sudah versi terbaru (${r.version})."
                    if (manual) toast("Sudah versi terbaru")
                }
                findViewById<TextView>(R.id.notes).text =
                    r.notes.take(600).ifBlank { "Tidak ada catatan rilis." }
            }
        }
    }

    private fun confirmDownload(r: Updater.Release) {
        AlertDialog.Builder(this)
            .setTitle("Update ke ${r.version}?")
            .setMessage("APK diunduh dari GitHub Releases (${r.apkUrl.substringAfterLast('/')}). Setelah selesai, izinkan pemasangan dari MemberKit.")
            .setNegativeButton("Batal", null)
            .setPositiveButton("Unduh") { _, _ -> download(r) }
            .show()
    }

    private fun download(r: Updater.Release) {
        val pd = ProgressDialog(this).apply {
            setTitle("Mengunduh update")
            setMessage("0%")
            setCancelable(false)
            show()
        }
        io.execute {
            val f = Updater.download(this, r) { pct ->
                runOnUiThread { pd.setMessage("$pct%") }
            }
            runOnUiThread {
                if (isFinishing) return@runOnUiThread
                pd.dismiss()
                if (f == null) {
                    toast("Unduhan gagal")
                } else {
                    Updater.install(this, f)
                }
            }
        }
    }

    private fun refreshBackend() {
        val info = findViewById<TextView>(R.id.backupInfo)
        if (!Backend.enabled) {
            info.text = "Backup: nonaktif (URL/token backend belum diisi saat build)."
            return
        }
        info.text = "Backup: mengirim…"
        io.execute {
            val members = Store(this).all()
            val ok = Backend.backupNow(this, members)
            val logs = Backend.flushQueue(this)
            runOnUiThread {
                if (isFinishing) return@runOnUiThread
                info.text = if (ok)
                    "Backup: tersimpan (${members.size} member) · log terkirim: $logs"
                else
                    "Backup: gagal terkirim, akan dicoba lagi otomatis."
            }
        }
    }

    private fun toast(s: String) = Toast.makeText(this, s, Toast.LENGTH_SHORT).show()

    override fun onDestroy() {
        io.shutdownNow()
        super.onDestroy()
    }
}
