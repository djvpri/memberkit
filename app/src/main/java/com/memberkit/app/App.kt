package com.memberkit.app

import android.app.Application
import android.content.Context
import android.util.Log
import java.io.PrintWriter
import java.io.StringWriter

/**
 * Menangkap semua uncaught exception, menyimpannya, mengirim ke backend
 * (best-effort, sinkron dengan timeout pendek), lalu meneruskan ke handler
 * bawaan supaya perilaku crash Android tetap normal.
 */
class App : Application() {

    override fun onCreate() {
        super.onCreate()

        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            runCatching { report(applicationContext, thread, error) }
            previous?.uncaughtException(thread, error)
                ?: run {
                    android.os.Process.killProcess(android.os.Process.myPid())
                    kotlin.system.exitProcess(10)
                }
        }

        // kirim sisa log dari sesi sebelumnya + backup awal
        Thread {
            runCatching { Backend.flushQueueBlocking(applicationContext) }
            runCatching { Backend.backupAsync(applicationContext) }
        }.apply { isDaemon = true }.start()
    }

    private fun report(ctx: Context, thread: Thread, error: Throwable) {
        val sw = StringWriter()
        error.printStackTrace(PrintWriter(sw))
        val stack = sw.toString().take(20_000)
        val message = "${error.javaClass.name}: ${error.message ?: "(no message)"}"

        Log.e("MemberKit", "FATAL di thread ${thread.name}", error)

        Backend.log(
            ctx,
            level = "FATAL",
            kind = "crash",
            tag = thread.name,
            message = "$message\nDevice: ${Backend.deviceSummary()}",
            stack = stack
        )
        // kirim segera; kalau gagal, file tetap di antrean dan dikirim saat app dibuka lagi
        Backend.flushQueueBlocking(ctx)
    }
}
