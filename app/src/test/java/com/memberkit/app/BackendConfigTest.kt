package com.memberkit.app

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BackendConfigTest {

    /** Build harus mengisi konfigurasi backend, kalau tidak fitur backup/log mati diam-diam. */
    @Test
    fun backendConfiguredAtBuildTime() {
        assertTrue("BACKEND_URL kosong", BuildConfig.BACKEND_URL.isNotBlank())
        assertTrue("BACKEND_URL harus https", BuildConfig.BACKEND_URL.startsWith("https://"))
        assertTrue("BACKEND_TOKEN kosong", BuildConfig.BACKEND_TOKEN.isNotBlank())
        assertFalse("token terlalu pendek", BuildConfig.BACKEND_TOKEN.length < 20)
    }

    @Test
    fun tokenNotHardcodedInSources() {
        // token hanya boleh datang dari BuildConfig, bukan literal di kode
        val dir = java.io.File("src/main/java/com/memberkit/app")
        dir.listFiles { f -> f.name.endsWith(".kt") }!!.forEach { f ->
            val t = f.readText()
            assertFalse("${f.name}: token backend ditulis langsung di kode", t.contains(BuildConfig.BACKEND_TOKEN))
        }
    }
}
