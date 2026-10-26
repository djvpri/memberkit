package com.memberkit.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

enum class Status(val label: String) { ACTIVE("Aktif"), SOON("Segera Habis"), EXPIRED("Expired") }

data class Member(
    val id: Long,
    val name: String,
    val app: String,
    val start: String,
    val end: String,
    val note: String,
    val price: Long = 0L
) {
    val endDate: LocalDate get() = Dates.parse(end)
    val startDate: LocalDate get() = Dates.parse(start)

    fun daysLeft(today: LocalDate = LocalDate.now()): Long =
        ChronoUnit.DAYS.between(today, endDate)

    fun status(today: LocalDate = LocalDate.now()): Status = when {
        daysLeft(today) < 0 -> Status.EXPIRED
        daysLeft(today) <= 7 -> Status.SOON
        else -> Status.ACTIVE
    }

    fun initials(): String = name.trim().split(" ").filter { it.isNotBlank() }
        .take(2).joinToString("") { it.first().uppercase() }

    fun toJson(): JSONObject = JSONObject()
        .put("id", id).put("name", name).put("app", app)
        .put("start", start).put("end", end).put("note", note).put("price", price)

    companion object {
        fun from(o: JSONObject) = Member(
            o.optLong("id"), o.optString("name"), o.optString("app"),
            o.optString("start"), o.optString("end"), o.optString("note"),
            o.optLong("price", 0L)
        )
    }
}

object Dates {
    private val out = DateTimeFormatter.ofPattern("d MMM yyyy", Locale("id", "ID"))
    fun parse(s: String): LocalDate = LocalDate.parse(s, DateTimeFormatter.ISO_LOCAL_DATE)
    fun fmt(d: LocalDate): String = d.format(out)
    fun iso(d: LocalDate): String = d.format(DateTimeFormatter.ISO_LOCAL_DATE)
}

class Store(ctx: Context) {
    private val file = File(ctx.filesDir, "members.json")

    fun all(): MutableList<Member> {
        if (!file.exists()) return mutableListOf()
        return runCatching {
            val arr = JSONArray(file.readText())
            MutableList(arr.length()) { Member.from(arr.getJSONObject(it)) }
        }.getOrElse { mutableListOf() }
    }

    fun save(list: List<Member>) {
        val arr = JSONArray()
        list.forEach { arr.put(it.toJson()) }
        file.writeText(arr.toString())
    }

    fun upsert(m: Member) {
        val list = all()
        val i = list.indexOfFirst { it.id == m.id }
        if (i >= 0) list[i] = m else list.add(m)
        save(list)
    }

    fun delete(id: Long) {
        save(all().filterNot { it.id == id })
    }

    fun nextId(): Long = (all().maxOfOrNull { it.id } ?: 0L) + 1L
}
