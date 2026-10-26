package com.memberkit.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.print.pdf.PrintedPdfDocument
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Nota gaya minimarket: 32 kolom monospace, lebar 384 px (58 mm @203dpi).
 * Render ke Bitmap → dipakai untuk share PNG ke WhatsApp dan untuk cetak PDF.
 */
object Nota {

    private const val W = 384
    private const val PAD = 14
    private const val COLS = 32
    private const val LINE_H = 26
    private const val TEXT = 19f
    private const val TOP = 18
    private const val BOTTOM = 22

    private fun center(s: String): String {
        val t = s.take(COLS)
        val off = (COLS - t.length) / 2
        return " ".repeat(maxOf(0, off)) + t
    }

    private fun row(l: String, r: String): String {
        val space = COLS - l.length - r.length
        return if (space >= 1) l + " ".repeat(space) + r
        else l.take(maxOf(0, COLS - r.length - 1)) + " " + r
    }

    private fun wrap(s: String, width: Int = COLS): List<String> {
        if (s.isBlank()) return emptyList()
        val out = mutableListOf<String>()
        var cur = StringBuilder()
        for (word in s.trim().split(" ")) {
            if (cur.isEmpty()) {
                if (word.length <= width) cur.append(word) else {
                    var rest = word
                    while (rest.length > width) { out += rest.substring(0, width); rest = rest.substring(width) }
                    cur.append(rest)
                }
            } else if (cur.length + 1 + word.length <= width) {
                cur.append(' ').append(word)
            } else {
                out += cur.toString(); cur = StringBuilder(word)
            }
        }
        if (cur.isNotEmpty()) out += cur.toString()
        return out
    }

    fun money(v: Long): String =
        String.format(Locale("id", "ID"), "%,d", v).replace(',', '.')

    fun receiptNo(m: Member): String =
        "MK-${m.endDate.format(DateTimeFormatter.ofPattern("yyMMdd"))}-${"%03d".format(m.id)}"

    fun lines(m: Member): List<String> {
        val sep = "-".repeat(COLS)
        val now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
        val days = java.time.temporal.ChronoUnit.DAYS.between(m.startDate, m.endDate)
        val fmt = DateTimeFormatter.ofPattern("dd/MM/yy", Locale("id", "ID"))

        val b = mutableListOf<String>()
        b += center("MEMBERKIT")
        b += center("NOTA LANGGANAN")
        b += sep
        b += row("No.", receiptNo(m))
        b += row("Tgl", now)
        b += row("Kasir", "ADMIN")
        b += sep
        b += wrap("Member : ${m.name}")
        b += wrap("Aplikasi: ${m.app}")
        b += row("Mulai", m.startDate.format(fmt))
        b += row("Selesai", m.endDate.format(fmt))
        b += row("Durasi", "$days hari")
        b += sep
        b += wrap("1 x ${m.app}")
        b += row("  $days hari", money(m.price))
        b += sep
        b += row("TOTAL", "Rp " + money(m.price))
        b += row("BAYAR", "Rp " + money(m.price))
        b += row("KEMBALI", "Rp 0")
        b += center("LUNAS")
        if (m.note.isNotBlank()) {
            b += sep
            b += wrap("Catatan: ${m.note}")
        }
        b += sep
        b += center("Terima kasih")
        b += center("Simpan nota ini")
        b += center("sebagai bukti aktif")
        b += sep
        b += center("* memberkit *")
        return b
    }

    fun build(m: Member): Bitmap {
        val rows = lines(m)
        val h = TOP + rows.size * LINE_H + BOTTOM
        val bmp = Bitmap.createBitmap(W, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.drawColor(Color.WHITE)

        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = TEXT
            typeface = Typeface.MONOSPACE
        }
        rows.forEachIndexed { i, line ->
            c.drawText(line, PAD.toFloat(), (TOP + (i + 1) * LINE_H - 7).toFloat(), p)
        }
        return bmp
    }

    fun png(ctx: Context, bmp: Bitmap, name: String): File {
        val dir = File(ctx.cacheDir, "nota").apply { mkdirs() }
        val f = File(dir, name)
        FileOutputStream(f).use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        return f
    }

    fun uri(ctx: Context, f: File): Uri =
        FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", f)

    fun share(ctx: Context, f: File, toWhatsapp: Boolean) {
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri(ctx, f))
            putExtra(Intent.EXTRA_TEXT, "Nota langganan kamu")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        if (toWhatsapp) {
            val wa = Intent(send).setPackage("com.whatsapp")
            runCatching { ctx.startActivity(wa) }
                .onFailure { ctx.startActivity(Intent.createChooser(send, "Kirim nota")) }
        } else {
            ctx.startActivity(Intent.createChooser(send, "Kirim nota"))
        }
    }

    fun print(ctx: Context, bmp: Bitmap, jobName: String) {
        val pm = ctx.getSystemService(Context.PRINT_SERVICE) as PrintManager
        val attrs = PrintAttributes.Builder()
            .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
            .setMinMargins(PrintAttributes.Margins(0, 0, 0, 0))
            .build()

        pm.print(jobName, object : PrintDocumentAdapter() {
            override fun onLayout(
                old: PrintAttributes?,
                new: PrintAttributes?,
                cancel: CancellationSignal?,
                cb: LayoutResultCallback,
                extras: Bundle?
            ) {
                cb.onLayoutFinished(
                    PrintDocumentInfo.Builder("$jobName.pdf")
                        .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                        .build(), true
                )
            }

            override fun onWrite(
                pages: Array<out PageRange>?,
                dest: ParcelFileDescriptor,
                cancel: CancellationSignal?,
                cb: WriteResultCallback
            ) {
                val pdf = PrintedPdfDocument(ctx, attrs)
                try {
                    val page = pdf.startPage(0)
                    val cv = page.canvas
                    val margin = 24f
                    val availW = cv.width - margin * 2
                    val scale = minOf(1f, availW / bmp.width)
                    val dw = bmp.width * scale
                    val dh = bmp.height * scale
                    val dst = android.graphics.RectF(
                        (cv.width - dw) / 2f, margin, (cv.width + dw) / 2f, margin + dh
                    )
                    cv.drawBitmap(bmp, null, dst, Paint(Paint.FILTER_BITMAP_FLAG))
                    pdf.finishPage(page)
                    FileOutputStream(dest.fileDescriptor).use { pdf.writeTo(it) }
                    cb.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
                } catch (e: Exception) {
                    cb.onWriteFailed(e.message)
                } finally {
                    pdf.close()
                }
            }
        }, attrs)
    }
}
