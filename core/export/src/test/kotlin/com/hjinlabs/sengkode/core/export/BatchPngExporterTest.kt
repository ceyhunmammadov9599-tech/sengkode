package com.hjinlabs.sengkode.core.export

import android.graphics.BitmapFactory
import com.hjinlabs.sengkode.core.model.QrContent
import com.hjinlabs.sengkode.core.model.QrStyle
import com.hjinlabs.sengkode.core.qr.ZxingQrEngine
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipInputStream
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Batch contract under a real (Robolectric) Android pipeline:
 * the ZIP contains one scannable PNG per safe snapshot, unsafe
 * styles are skipped and reported, and every PNG decodes back to
 * its EXACT original payload - the round-trip requirement applied
 * to the batch workflow.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class BatchPngExporterTest {

    private val engine = ZxingQrEngine()
    private val renderer = DrawListBitmapRenderer()

    @Test
    fun `batch exports every safe snapshot and decodes each png`() = runTest {
        val snapshots = listOf(
            QrContent.Text("batch one") to QrStyle(),
            QrContent.Url("https://hjinlabs.app/batch") to QrStyle(
                moduleShape = com.hjinlabs.sengkode.core.model.ModuleShape.ROUNDED,
                eyeShape = com.hjinlabs.sengkode.core.model.EyeShape.ROUNDED,
            ),
            QrContent.Wifi(
                "Net", "batchpass1",
                com.hjinlabs.sengkode.core.model.WifiEncryption.WPA, false,
            ) to QrStyle(
                moduleShape = com.hjinlabs.sengkode.core.model.ModuleShape.ROUNDED,
            ),
            QrContent.Text(
                "dot modules travel with a longer payload " +
                    "so the symbol is large enough for the shape policy",
            ) to QrStyle(
                moduleShape = com.hjinlabs.sengkode.core.model.ModuleShape.DOT,
            ),
        )

        val bytes = ByteArrayOutputStream()
        val result = BatchPngExporter(engine, renderer)
            .exportToZip(snapshots, bytes)

        assertEquals(4, result.exportedCount)
        assertTrue(result.skipped.isEmpty())

        val entries = zipEntries(bytes.toByteArray())
        assertEquals(4, entries.size)

        // Round-trip: every exported PNG decodes to its exact payload.
        val payloads = snapshots.map { (content, _) ->
            (engine.generate(content) as com.hjinlabs.sengkode.core.model.QrGenerationResult.Success)
                .payload
        }
        entries.forEachIndexed { index, pngBytes ->
            val bitmap = BitmapFactory.decodeStream(ByteArrayInputStream(pngBytes))
            assertTrue("entry $index must be a valid bitmap", bitmap != null)
            assertTrue(
                "entry $index must decode back to its original payload",
                BitmapScannabilityVerifier.isScannable(bitmap!!, payloads[index]),
            )
        }
    }

    @Test
    fun `unsafe styles are skipped and reported`() = runTest {
        val unsafeStyle = QrStyle(
            foregroundArgb = 0xFF999999L,
            backgroundArgb = 0xFF999999L,
        )
        val snapshots = listOf(
            QrContent.Text("safe") to QrStyle(),
            QrContent.Text("risky") to unsafeStyle,
        )

        val bytes = ByteArrayOutputStream()
        val result = BatchPngExporter(engine, renderer).exportToZip(snapshots, bytes)

        assertEquals(1, result.exportedCount)
        assertEquals(listOf("2. risky"), result.skipped)
        assertEquals(1, zipEntries(bytes.toByteArray()).size)
    }

    @Test
    fun `empty batch produces an empty valid zip`() = runTest {
        val bytes = ByteArrayOutputStream()
        val result = BatchPngExporter(engine, renderer).exportToZip(emptyList(), bytes)
        assertEquals(0, result.exportedCount)
        assertTrue(result.skipped.isEmpty())
        assertTrue(zipEntries(bytes.toByteArray()).isEmpty())
    }

    @Test
    fun `entry names are portable ascii slugs`() {
        val exporter = BatchPngExporter(engine, renderer)
        assertEquals("My-Best-Code", exporter.slug("My Best Code!"))
        assertEquals("code", exporter.slug("###"))
        assertEquals("A", exporter.slug("A"))
        assertEquals("Wi-Fi-HomeNet", exporter.slug("Wi-Fi HomeNet"))
    }

    private fun zipEntries(zipBytes: ByteArray): List<ByteArray> {
        val entries = mutableListOf<ByteArray>()
        ZipInputStream(ByteArrayInputStream(zipBytes)).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                entries += zip.readBytes()
                entry = zip.nextEntry
            }
        }
        return entries
    }
}
