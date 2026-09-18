package com.hjinlabs.sengkode.core.export

import android.graphics.Bitmap
import com.hjinlabs.sengkode.core.model.EccLevel
import com.hjinlabs.sengkode.core.model.QrContent
import com.hjinlabs.sengkode.core.model.QrContentDefaults
import com.hjinlabs.sengkode.core.model.QrGenerationResult
import com.hjinlabs.sengkode.core.model.QrStyle
import com.hjinlabs.sengkode.core.qr.QrEngine
import com.hjinlabs.sengkode.core.style.LogoEccPolicy
import com.hjinlabs.sengkode.core.style.ScanSafety
import com.hjinlabs.sengkode.core.style.ScanSafetyPolicy
import java.io.BufferedOutputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Batch productivity workflow: many (content, style) snapshots ->
 * one ZIP of PNGs. Streams strictly ONE bitmap at a time
 * (generate -> render -> compress -> recycle) so a batch of 100
 * codes never holds 100 bitmaps in memory.
 *
 * Reuses the single existing pipeline only: engine (matrix) ->
 * safety policy (gate) -> QrBitmapRenderer (pixels). No duplicate
 * renderer, no duplicate encoder logic. Unsafe styles are skipped
 * and reported, never silently exported.
 */
class BatchPngExporter(
    private val engine: QrEngine,
    private val renderer: DrawListBitmapRenderer,
) {

    data class Result(val exportedCount: Int, val skipped: List<String>)

    suspend fun exportToZip(
        snapshots: List<Pair<QrContent, QrStyle>>,
        outputStream: OutputStream,
        sizePx: Int = DEFAULT_EXPORT_SIZE_PX,
    ): Result = withContext(Dispatchers.IO) {
        val skipped = mutableListOf<String>()
        var exported = 0

        ZipOutputStream(BufferedOutputStream(outputStream)).use { zip ->
            snapshots.forEachIndexed { index, (content, style) ->
                val title = QrContentDefaults.titleFor(content)
                val ecc = LogoEccPolicy.effectiveEcc(EccLevel.M, style.logo)
                val generated = engine.generate(content, ecc)
                val matrix = (generated as? QrGenerationResult.Success)?.matrix
                val verdict = matrix?.let {
                    ScanSafetyPolicy.evaluate(style, it.width)
                }
                if (matrix == null || verdict !is ScanSafety.Safe) {
                    skipped.add("${index + 1}. $title")
                    return@forEachIndexed
                }

                var bitmap: Bitmap? = null
                try {
                    bitmap = renderer.render(matrix, style, sizePx)
                    zip.putNextEntry(ZipEntry("%02d-%s.png".format(index + 1, slug(title))))
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, zip)
                    zip.closeEntry()
                    exported++
                } finally {
                    bitmap?.recycle()
                }
            }
        }

        Result(exportedCount = exported, skipped = skipped)
    }

    /** ZIP entry names stay portable: ASCII letters, digits, dash. */
    internal fun slug(title: String): String {
        val cleaned = title.map { ch ->
            if (ch in 'a'..'z' || ch in 'A'..'Z' || ch in '0'..'9' || ch == '-') ch
            else '-'
        }.joinToString("").trim('-').replace(Regex("-{2,}"), "-")
        return cleaned.ifBlank { "code" }.take(40)
    }

    companion object {
        const val DEFAULT_EXPORT_SIZE_PX = 1024
    }
}
