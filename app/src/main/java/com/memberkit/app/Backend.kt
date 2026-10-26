package com.memberkit.app

import android.content.Context
import android.os.Build
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID
import java.util.concurrent.Executors

/**
 * Klien backend MemberKit: backup otomatis + kirim log error/crash.
 * Tanpa dependensi baru — HttpURLConnection saja.
 * Konfigurasi dari BuildConfig (BACKEND_URL, BACKEND_TOKEN).
 */
object Backend {

    private val io = Executors.newSingleThreadExecutor { r ->
        Thread(r, "memberkit-backend").apply { isDaemon = true }
    }

    private const val TIMEOUT = 12_000

    val enabled: Boolean
        get() = BuildConfig.BACKEND_URL.isNotBlank() && BuildConfig.BACKEND_TOKEN.isNotBlank()

    /** UUID stabil per instalasi, disimpan di prefs. */
    fun deviceId(ctx: Context): String {
        val p = ctx.getSharedPreferences("memberkit", Context.MODE_PRIVATE)
        var id = p.getString("device_id", null)
        if (id == null) {
            id = UUID.randomUUID().toString()
            p.edit().putString("device_id", id).apply()
        }
        return id
    }

    private fun queueDir(ctx: Context) = File(ctx.filesDir, "logqueue").apply { mkdirs() }

    /** Kirim backup. Panggil dari background thread. */
    fun backupNow(ctx: Context, members: List<Member>): Boolean {
        if (!enabled) return false
        val arr = JSONArray()
        members.forEach { arr.put(it.toJson()) }
        val body = JSONObject()
            .put("deviceId", deviceId(ctx))
            .put("appVersion", BuildConfig.VERSION_NAME)
            .put("members", arr)
        return post(ctx, "/backup", body) != null
    }

    /** Jadwalkan backup di background (tidak memblok UI). */
    fun backupAsync(ctx: Context) {
        if (!enabled) return
        val app = ctx.applicationContext
        io.execute {
            runCatching { backupNow(app, Store(app).all()) }
            flushQueue(app)
        }
    }

    /**
     * Catat log ke antrean lalu coba kirim. aman dipanggil dari mana saja,
     * termasuk dari crash handler (sinkron, timeout pendek).
     */
    fun log(ctx: Context, level: String, kind: String, tag: String?, message: String?, stack: String?) {
        if (!enabled) return
        val entry = JSONObject()
            .put("level", level)
            .put("kind", kind)
            .put("tag", tag ?: "")
            .put("message", message ?: "")
            .put("stack", stack ?: "")
        val f = File(queueDir(ctx), "${System.currentTimeMillis()}-${UUID.randomUUID().toString().take(8)}.json")
        runCatching { f.writeText(entry.toString()) }
    }

    /** Kirim semua log di antrean. Panggil di background. */
    fun flushQueue(ctx: Context): Int {
        if (!enabled) return 0
        val dir = queueDir(ctx)
        val files = dir.listFiles()?.sortedBy { it.name } ?: return 0
        if (files.isEmpty()) return 0

        var sent = 0
        for (chunk in files.chunked(20)) {
            val arr = JSONArray()
            chunk.forEach { f ->
                runCatching { arr.put(JSONObject(f.readText())) }
            }
            if (arr.length() == 0) {
                chunk.forEach { it.delete() }
                continue
            }
            val body = JSONObject()
                .put("deviceId", deviceId(ctx))
                .put("appVersion", BuildConfig.VERSION_NAME)
                .put("logs", arr)
            val resp = post(ctx, "/logs", body)
            if (resp != null) {
                chunk.forEach { it.delete() }
                sent += arr.length()
            } else {
                break // jaringan gagal, sisakan sisa antrean
            }
        }
        return sent
    }

    /** Dipakai crash handler: sinkron, tanpa executor (proses bisa mati kapan saja). */
    fun flushQueueBlocking(ctx: Context) {
        if (!enabled) return
        runCatching { flushQueue(ctx) }
    }

    fun deviceSummary(): String = Build.MANUFACTURER + " " + Build.MODEL + " / Android " + Build.VERSION.RELEASE

    private fun post(ctx: Context, path: String, body: JSONObject): String? {
        var conn: HttpURLConnection? = null
        return try {
            conn = (URL(BuildConfig.BACKEND_URL.trimEnd('/') + path).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = TIMEOUT
                readTimeout = TIMEOUT
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                setRequestProperty("X-Device-Token", BuildConfig.BACKEND_TOKEN)
                setRequestProperty("User-Agent", "MemberKit/${BuildConfig.VERSION_NAME}")
            }
            conn.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val text = stream?.bufferedReader()?.use { it.readText() } ?: ""
            if (code in 200..299) text else null
        } catch (e: Exception) {
            null
        } finally {
            conn?.disconnect()
        }
    }
}
