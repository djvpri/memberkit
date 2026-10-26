package com.memberkit.app

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import java.time.LocalDate
import java.util.Calendar

class EditActivity : AppCompatActivity() {

    private lateinit var store: Store
    private var id: Long = -1
    private var startDate: LocalDate = LocalDate.now()
    private var endDate: LocalDate = LocalDate.now().plusMonths(1)

    private lateinit var fName: EditText
    private lateinit var fApp: EditText
    private lateinit var fNote: EditText
    private lateinit var fStart: TextView
    private lateinit var fEnd: TextView
    private lateinit var fDuration: TextView
    private lateinit var fPrice: EditText
    private lateinit var fError: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit)
        store = Store(this)
        id = intent.getLongExtra("id", -1L)

        fName = findViewById(R.id.name)
        fApp = findViewById(R.id.app)
        fNote = findViewById(R.id.note)
        fStart = findViewById(R.id.start)
        fEnd = findViewById(R.id.end)
        fDuration = findViewById(R.id.duration)
        fPrice = findViewById(R.id.price)
        fError = findViewById(R.id.error)

        val existing = if (id > 0) store.all().firstOrNull { it.id == id } else null
        if (existing != null) {
            findViewById<TextView>(R.id.title).text = "Edit Member"
            fName.setText(existing.name)
            fApp.setText(existing.app)
            fNote.setText(existing.note)
            if (existing.price > 0) fPrice.setText(existing.price.toString())
            startDate = existing.startDate
            endDate = existing.endDate
            findViewById<View>(R.id.delete).visibility = View.VISIBLE
            findViewById<TextView>(R.id.delete).setOnClickListener { confirmDelete(existing) }
        }

        fStart.setOnClickListener { pick(startDate) { startDate = it; syncDates(startMoved = true); paint() } }
        fEnd.setOnClickListener { pick(endDate) { endDate = it; paint() } }
        findViewById<View>(R.id.back).setOnClickListener { finish() }
        findViewById<View>(R.id.save).setOnClickListener { save() }

        listOf(
            R.id.q1 to 1L, R.id.q3 to 3L, R.id.q6 to 6L, R.id.q12 to 12L
        ).forEach { (viewId, months) ->
            findViewById<View>(viewId).setOnClickListener {
                endDate = startDate.plusMonths(months)
                paint()
            }
        }

        paint()
    }

    private fun syncDates(startMoved: Boolean) {
        if (startMoved && endDate.isBefore(startDate)) endDate = startDate.plusMonths(1)
    }

    private fun pick(current: LocalDate, onPicked: (LocalDate) -> Unit) {
        val c = Calendar.getInstance().apply {
            set(current.year, current.monthValue - 1, current.dayOfMonth)
        }
        DatePickerDialog(this, { _, y, m, d -> onPicked(LocalDate.of(y, m + 1, d)) },
            c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun paint() {
        fStart.text = Dates.fmt(startDate)
        fEnd.text = Dates.fmt(endDate)
        val days = java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate)
        fDuration.text = "Durasi terhitung: $days hari"
    }

    private fun save() {
        val name = fName.text.toString().trim()
        val app = fApp.text.toString().trim()
        val price = fPrice.text.toString().filter { it.isDigit() }.toLongOrNull() ?: 0L

        when {
            name.isEmpty() -> return fail("Nama member wajib diisi.", fName)
            app.isEmpty() -> return fail("Nama aplikasi wajib diisi.", fApp)
            !endDate.isAfter(startDate) -> return fail("Tanggal selesai harus setelah tanggal mulai.", fEnd)
        }

        fError.visibility = View.GONE
        val m = Member(
            id = if (id > 0) id else store.nextId(),
            name = name,
            app = app,
            start = Dates.iso(startDate),
            end = Dates.iso(endDate),
            note = fNote.text.toString().trim(),
            price = price
        )
        store.upsert(m)
        finish()
    }

    private fun fail(msg: String, field: View) {
        fError.text = msg
        fError.visibility = View.VISIBLE
        field.requestFocus()
    }

    private fun confirmDelete(m: Member) {
        AlertDialog.Builder(this)
            .setTitle("Hapus member?")
            .setMessage("${m.name} — ${m.app}")
            .setNegativeButton("Batal", null)
            .setPositiveButton("Hapus") { _, _ -> store.delete(m.id); finish() }
            .show()
    }
}
