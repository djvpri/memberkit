package com.memberkit.app

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdaterTest {

    @Test
    fun detectsNewerVersions() {
        assertTrue(Updater.isNewer("1.1.0", "1.0.0"))
        assertTrue(Updater.isNewer("v1.2.0", "1.1.0"))
        assertTrue(Updater.isNewer("1.1.1", "1.1.0"))
        assertTrue(Updater.isNewer("2.0", "1.9.9"))
        assertTrue(Updater.isNewer("1.1.0.1", "1.1.0"))
    }

    @Test
    fun sameOrOlderIsNotNewer() {
        assertFalse(Updater.isNewer("1.1.0", "1.1.0"))
        assertFalse(Updater.isNewer("v1.1.0", "1.1.0"))
        assertFalse(Updater.isNewer("1.0.9", "1.1.0"))
        assertFalse(Updater.isNewer("0.9", "1.0"))
    }
}
