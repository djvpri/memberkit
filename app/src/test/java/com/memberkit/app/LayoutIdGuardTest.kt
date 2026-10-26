package com.memberkit.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guard: setiap id yang dipakai findViewById di Activity harus ada di layout-nya
 * dengan tipe yang benar. Menangkap ClassCastException seperti R.id.back
 * (ImageButton di-cast ke TextView) tanpa perlu emulator.
 */
class LayoutIdGuardTest {

    private fun layout(name: String): String =
        java.io.File("src/main/res/layout/$name").readText()

    private fun activity(name: String): String =
        java.io.File("src/main/java/com/memberkit/app/$name").readText()

    /** id yang di-cast eksplisit ke Tipe di Activity harus Tipe itu juga di layout. */
    private fun assertCast(activityFile: String, layoutFile: String, id: String, xmlTag: String) {
        val a = activity(activityFile)
        val l = layout(layoutFile)
        val tag = xmlTag.substringAfterLast('.')
        val declared = Regex("""<${Regex.escape(xmlTag)}\b[^>]*@\+id/${Regex.escape(id)}""").containsMatchIn(l)
        assertTrue("$layoutFile: @+id/$id bukan <$xmlTag>", declared)

        val typed = Regex("""findViewById<([A-Za-z.]+)>\(R\.id\.${Regex.escape(id)}\)""").find(a)
        if (typed != null) {
            val castTo = typed.groupValues[1].substringAfterLast('.')
            assertEquals("$activityFile: findViewById<$castTo>(R.id.$id) tapi layout <$xmlTag>", tag, castTo)
        } else {
            assertTrue("$activityFile: R.id.$id tidak dipakai", a.contains("R.id.$id"))
        }
    }

    @Test
    fun notaActivityCastsMatchLayout() {
        assertCast("NotaActivity.kt", "activity_nota.xml", "back", "ImageButton")
        assertCast("NotaActivity.kt", "activity_nota.xml", "cetak", "TextView")
        assertCast("NotaActivity.kt", "activity_nota.xml", "wa", "TextView")
        assertCast("NotaActivity.kt", "activity_nota.xml", "simpan", "TextView")
        assertCast("NotaActivity.kt", "activity_nota.xml", "preview", "ImageView")
    }

    @Test
    fun editActivityCastsMatchLayout() {
        assertCast("EditActivity.kt", "activity_edit.xml", "name", "EditText")
        assertCast("EditActivity.kt", "activity_edit.xml", "app", "EditText")
        assertCast("EditActivity.kt", "activity_edit.xml", "note", "EditText")
        assertCast("EditActivity.kt", "activity_edit.xml", "price", "EditText")
        assertCast("EditActivity.kt", "activity_edit.xml", "start", "TextView")
        assertCast("EditActivity.kt", "activity_edit.xml", "end", "TextView")
    }

    @Test
    fun mainActivityCastsMatchLayout() {
        assertCast("MainActivity.kt", "activity_main.xml", "list", "androidx.recyclerview.widget.RecyclerView")
        assertCast("MainActivity.kt", "activity_main.xml", "fab", "ImageButton")
        assertCast("MainActivity.kt", "activity_main.xml", "search", "EditText")
        assertCast("MainActivity.kt", "activity_main.xml", "chipAll", "TextView")
        assertCast("MainActivity.kt", "activity_main.xml", "cekUpdate", "TextView")
        assertCast("MainActivity.kt", "activity_main.xml", "version", "TextView")
        assertCast("MainActivity.kt", "activity_main.xml", "subtitle", "TextView")
    }

    @Test
    fun itemMemberIdsExist() {
        val l = layout("item_member.xml")
        listOf("avatar", "name", "sub", "pill", "nota").forEach {
            assertTrue("item_member.xml: @+id/$it hilang", l.contains("@+id/$it"))
        }
    }

    @Test
    fun everyLayoutParsesAndIdsAreUniquePerFile() {
        val dir = java.io.File("src/main/res/layout")
        dir.listFiles { f -> f.name.endsWith(".xml") }!!.forEach { f ->
            val text = f.readText()
            val ids = Regex("""@\+id/([A-Za-z0-9_]+)""").findAll(text).map { it.groupValues[1] }.toList()
            assertEquals("${f.name}: id duplikat di satu layout", ids.size, ids.toSet().size)
            assertTrue("${f.name}: tag tidak seimbang", text.count { it == '<' } >= text.count { it == '>' } - 1)
        }
    }
}
