package com.hjinlabs.sengkode.feature.generator

import com.hjinlabs.sengkode.core.model.QrContent
import com.hjinlabs.sengkode.core.model.QrStyle
import com.hjinlabs.sengkode.core.model.repository.StudioRestoreStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Share-intake contract: URLs become URL content, everything else
 * plain text; blank and oversized payloads are rejected; accepted
 * payloads are staged through the existing session restore store
 * (which the studio consumes through its tested exactly-once path).
 */
class ShareIntakeTest {

    private class RecordingRestoreStore : StudioRestoreStore {
        private var pending: Pair<QrContent, QrStyle>? = null
        val puts = mutableListOf<Pair<QrContent, QrStyle>>()

        override fun put(content: QrContent, style: QrStyle) {
            pending = content to style
            puts.add(pending!!)
        }

        override fun consume(): Pair<QrContent, QrStyle>? = pending.also { pending = null }
    }

    @Test
    fun `urls map to url content`() {
        assertEquals(
            QrContent.Url("https://hjinlabs.app"),
            ShareIntake.parse("https://hjinlabs.app"),
        )
        assertEquals(
            QrContent.Url("http://example.org/a/b?c=1"),
            ShareIntake.parse("http://example.org/a/b?c=1"),
        )
        assertEquals(
            QrContent.Url("https://EXAMPLE.COM/Path"),
            ShareIntake.parse("https://EXAMPLE.COM/Path"),
        )
    }

    @Test
    fun `plain text maps to text content and is trimmed`() {
        assertEquals(QrContent.Text("hello sengkode"), ShareIntake.parse("  hello sengkode  "))
    }

    @Test
    fun `blank and null shares are rejected`() {
        assertNull(ShareIntake.parse(null))
        assertNull(ShareIntake.parse(""))
        assertNull(ShareIntake.parse("   "))
    }

    @Test
    fun `oversized shares are rejected`() {
        val tooLong = "x".repeat(ShareIntake.MAX_SHARE_LENGTH + 1)
        assertNull(ShareIntake.parse(tooLong))
    }

    @Test
    fun `text that merely contains a url stays text`() {
        assertEquals(
            QrContent.Text("see https://hjinlabs.app for details"),
            ShareIntake.parse("see https://hjinlabs.app for details"),
        )
    }

    @Test
    fun `staging hands the content to the restore store with a default style`() {
        val store = RecordingRestoreStore()
        val accepted = ShareIntake.stageInto(ShareIntake.parse("https://hjinlabs.app"), store)
        assertTrue(accepted)
        assertEquals(1, store.puts.size)
        assertEquals(QrContent.Url("https://hjinlabs.app"), store.puts[0].first)
        assertEquals(QrStyle(), store.puts[0].second)
    }

    @Test
    fun `staging nothing is a no-op`() {
        val store = RecordingRestoreStore()
        assertFalse(ShareIntake.stageInto(null, store))
        assertTrue(store.puts.isEmpty())
    }
}
