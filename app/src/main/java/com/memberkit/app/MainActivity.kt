package com.memberkit.app

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MainActivity : AppCompatActivity() {

    private lateinit var store: Store
    private lateinit var adapter: MemberAdapter
    private var filter = "all"
    private var query = ""

    private val editLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        render()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        store = Store(this)

        findViewById<TextView>(R.id.version).text = "v${BuildConfig.VERSION_NAME}"

        val list = findViewById<RecyclerView>(R.id.list)
        list.layoutManager = LinearLayoutManager(this)
        adapter = MemberAdapter(
            onClick = { m -> openEdit(m.id) },
            onLongClick = { m -> confirmDelete(m) },
            onNota = { m -> openNota(m.id) }
        )
        list.adapter = adapter

        findViewById<ImageButton>(R.id.fab).setOnClickListener { openEdit(null) }

        findViewById<TextView>(R.id.cekUpdate).setOnClickListener {
            startActivity(Intent(this, UpdateActivity::class.java))
        }

        findViewById<EditText>(R.id.search).addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { query = s?.toString()?.trim() ?: ""; render() }
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
        })

        val chips = mapOf(
            "all" to findViewById<TextView>(R.id.chipAll),
            "active" to findViewById<TextView>(R.id.chipActive),
            "soon" to findViewById<TextView>(R.id.chipSoon),
            "expired" to findViewById<TextView>(R.id.chipExpired)
        )
        chips.forEach { (key, view) ->
            view.setOnClickListener {
                filter = key
                render()
            }
        }

        render()
    }

    override fun onResume() {
        super.onResume()
        render()
        // kirim sisa log dari sesi sebelumnya + backup berkala
        Backend.backupAsync(this)
    }

    private fun openEdit(id: Long?) {
        val i = Intent(this, EditActivity::class.java)
        i.putExtra("id", id ?: -1L)
        editLauncher.launch(i)
    }

    private fun openNota(id: Long) {
        startActivity(Intent(this, NotaActivity::class.java).putExtra("id", id))
    }

    private fun confirmDelete(m: Member) {
        AlertDialog.Builder(this)
            .setTitle("Hapus member?")
            .setMessage("${m.name} — ${m.app}\nData akan dihapus permanen.")
            .setNegativeButton("Batal", null)
            .setPositiveButton("Hapus") { _, _ ->
                store.delete(m.id)
                render()
                Backend.backupAsync(this)
            }
            .show()
    }

    private fun render() {
        val today = java.time.LocalDate.now()
        val all = store.all()
        val visible = all.asSequence()
            .filter { m ->
                when (filter) {
                    "active" -> m.status(today) == Status.ACTIVE
                    "soon" -> m.status(today) == Status.SOON
                    "expired" -> m.status(today) == Status.EXPIRED
                    else -> true
                }
            }
            .filter { m ->
                query.isEmpty() ||
                    m.name.contains(query, true) ||
                    m.app.contains(query, true) ||
                    m.note.contains(query, true)
            }
            .sortedBy { it.endDate }
            .toList()

        adapter.submit(visible)

        findViewById<TextView>(R.id.subtitle).text =
            "${all.size} member · ${all.map { it.app }.distinct().size} app"

        findViewById<View>(R.id.empty).visibility =
            if (visible.isEmpty()) View.VISIBLE else View.GONE

        val keys = listOf("all", "active", "soon", "expired")
        val chipIds = intArrayOf(R.id.chipAll, R.id.chipActive, R.id.chipSoon, R.id.chipExpired)
        keys.forEachIndexed { idx, key ->
            val v = findViewById<TextView>(chipIds[idx])
            v.isSelected = (filter == key)
            v.setTextColor(getColor(if (filter == key) android.R.color.white else R.color.fg2))
        }
    }
}
