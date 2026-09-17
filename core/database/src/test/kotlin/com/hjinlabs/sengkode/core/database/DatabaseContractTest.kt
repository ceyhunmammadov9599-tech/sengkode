package com.hjinlabs.sengkode.core.database

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.hjinlabs.sengkode.core.model.EccLevel
import com.hjinlabs.sengkode.core.model.QrContent
import com.hjinlabs.sengkode.core.model.QrGenerationResult
import com.hjinlabs.sengkode.core.model.QrStyle
import com.hjinlabs.sengkode.core.model.WifiEncryption
import com.hjinlabs.sengkode.core.qr.ZxingQrEngine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Real-DB contract tests (in-memory Room under Robolectric): insert,
 * query, update, delete, seeding, and the save -> reload ->
 * regenerate-identical history workflow through the ACTUAL repository.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DatabaseContractTest {

    private lateinit var db: AppDatabase
    private lateinit var history: HistoryRepositoryImpl
    private lateinit var templates: TemplateRepositoryImpl

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java,
        ).allowMainThreadQueries().build()
        history = HistoryRepositoryImpl(db.historyDao()).apply { clock = { FIXED_NOW } }
        templates = TemplateRepositoryImpl(db.templateDao())
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `insert then query returns the same snapshot`() = runTest {
        val content = QrContent.Wifi("HomeNet", "secret123", WifiEncryption.WPA, true)
        val style = QrStyle()
        val id = history.save(content, style, "Home Wi-Fi")

        val loaded = history.get(id)!!
        assertEquals("Home Wi-Fi", loaded.title)
        assertEquals(content, loaded.content)
        assertEquals(style, loaded.style)
        assertEquals(FIXED_NOW, loaded.createdAtEpochMs)
        assertEquals(false, loaded.isFavorite)
    }

    @Test
    fun `favorites sort to the top of the list`() = runTest {
        val older = history.save(QrContent.Text("older"), QrStyle(), "Older")
        val newer = history.save(QrContent.Text("newer"), QrStyle(), "Newer")
        history.setFavorite(older, true)

        val list = history.observeAll().first()
        assertEquals(listOf("Older", "Newer"), list.map { it.title })
    }

    @Test
    fun `update favorite persists`() = runTest {
        val id = history.save(QrContent.Text("x"), QrStyle(), "X")
        history.setFavorite(id, true)
        assertEquals(true, history.get(id)!!.isFavorite)
        history.setFavorite(id, false)
        assertEquals(false, history.get(id)!!.isFavorite)
    }

    @Test
    fun `delete removes only the target row`() = runTest {
        val keep = history.save(QrContent.Text("keep"), QrStyle(), "Keep")
        val drop = history.save(QrContent.Text("drop"), QrStyle(), "Drop")
        history.delete(drop)
        assertNull(history.get(drop))
        assertTrue(history.get(keep) != null)
        assertEquals(1, history.observeAll().first().size)
    }

    @Test
    fun `clear all wipes history (privacy action)`() = runTest {
        history.save(QrContent.Text("a"), QrStyle(), "A")
        history.save(QrContent.Text("b"), QrStyle(), "B")
        history.clearAll()
        assertTrue(history.observeAll().first().isEmpty())
    }

    @Test
    fun `save reload and regenerate produces the identical qr`() = runTest {
        val engine = ZxingQrEngine()
        val content = QrContent.VCard(
            "Jeyhun Mammadov", "HJIN Labs", "+994 50 123 45 67",
            "jeyhun@hjinlabs.app", "https://hjinlabs.app", "Note",
        )
        val style = QrStyle(
            moduleShape = com.hjinlabs.sengkode.core.model.ModuleShape.ROUNDED,
            eyeShape = com.hjinlabs.sengkode.core.model.EyeShape.ROUNDED,
        )
        val original = engine.generate(content, EccLevel.M) as QrGenerationResult.Success

        val id = history.save(content, style, "Contact")
        val reloaded = history.get(id)!!

        val regenerated = engine.generate(reloaded.content, EccLevel.M)
                as QrGenerationResult.Success

        assertEquals(original.payload, regenerated.payload)
        assertEquals(original.matrix, regenerated.matrix)
        assertEquals(style, reloaded.style)
    }

    // -- templates --------------------------------------------------------

    @Test
    fun `seed is idempotent and inserts all built-ins`() = runTest {
        templates.ensureSeeded()
        templates.ensureSeeded() // second call must not duplicate
        val all = templates.observeAll().first()
        assertEquals(com.hjinlabs.sengkode.core.style.BuiltInTemplates.ALL.size, all.size)
        assertTrue(all.all { it.isBuiltIn })
    }

    @Test
    fun `custom template save query and delete`() = runTest {
        templates.ensureSeeded()
        val style = QrStyle(
            foregroundArgb = 0xFF00695CL,
            moduleShape = com.hjinlabs.sengkode.core.model.ModuleShape.DOT,
        )
        val id = templates.saveCustom("My Style", "Custom dots", style,
            com.hjinlabs.sengkode.core.model.QrContentType.URL)

        val custom = templates.observeAll().first().first { it.id == id }
        assertEquals("My Style", custom.name)
        assertEquals(style, custom.style)
        assertEquals(false, custom.isBuiltIn)

        templates.deleteCustom(id)
        assertTrue(templates.observeAll().first().none { it.id == id })
    }

    @Test
    fun `built-in templates are not deletable`() = runTest {
        templates.ensureSeeded()
        val builtIn = templates.observeAll().first().first { it.isBuiltIn }
        var threw = false
        try {
            templates.deleteCustom(builtIn.id)
        } catch (e: IllegalStateException) {
            threw = true
        }
        assertTrue(threw)
        assertTrue(templates.observeAll().first().any { it.id == builtIn.id })
    }

    companion object {
        const val FIXED_NOW = 1_726_500_000_000L
    }
}
