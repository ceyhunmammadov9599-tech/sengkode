package com.hjinlabs.sengkode.core.export

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.core.content.FileProvider
import com.hjinlabs.sengkode.core.model.ExportSpec
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/**
 * PNG export and share workflow. Privacy rules enforced here:
 * - Android 10+ saves go through MediaStore - NO storage permission.
 * - On Android 8.x/9 the bitmap is saved to the app-owned external
 *   files dir (still no permission) instead of requesting the legacy
 *   WRITE_EXTERNAL_STORAGE grant.
 * - Sharing always uses FileProvider with a narrow per-export grant.
 *
 * The provider authority must be declared by the app:
 * "${applicationId}.fileprovider" with cache-path "exports/".
 */
class QrFileExporter(private val context: Context) {

    private val appAuthority: String
        get() = "${context.packageName}.fileprovider"

    /**
     * Saves the PNG into the system gallery. Returns the Uri on
     * Android 10+ (MediaStore) or the app-owned file Uri on older
     * versions; a Result failure explains any I/O problem.
     */
    fun saveToGallery(bitmap: Bitmap, spec: ExportSpec): Result<Uri> = runCatching {
        val file = File(context.cacheDir, "exports").apply { mkdirs() }
            .resolve("${spec.displayName}.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, PNG_QUALITY, out)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            insertIntoMediaStore(file, spec)
        } else {
            saveToLegacyAppStorage(file, spec)
        }
    }

    /**
     * Prepares and launches a share sheet with the PNG attached via
     * FileProvider (read grant limited to this export's Uri).
     */
    fun sharePng(bitmap: Bitmap, spec: ExportSpec) {
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = dir.resolve("${spec.displayName}.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, PNG_QUALITY, out)
        }
        val uri = FileProvider.getUriForFile(context, appAuthority, file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(
            Intent.createChooser(intent, "Share QR code"),
        )
    }

    // -- internals -------------------------------------------------------

    private fun insertIntoMediaStore(file: File, spec: ExportSpec): Uri {
        val values = ContentValues().apply {
            put(MediaStore_DISPLAY_NAME, "${spec.displayName}.png")
            put(MediaStore_MIME_TYPE, "image/png")
            put(MediaStore_RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/SENGKODE")
            put(MediaStore_IS_PENDING, 1)
        }
        val resolver = context.contentResolver
        val uri = resolver.insert(IMAGE_COLLECTION_URI, values)
            ?: throw IOException("MediaStore rejected the insert")
        try {
            resolver.openOutputStream(uri)?.use { out ->
                file.inputStream().copyTo(out)
            } ?: throw IOException("Could not open MediaStore stream")
        } catch (e: Exception) {
            resolver.delete(uri, null, null)
            throw e
        }
        values.clear()
        values.put(MediaStore_IS_PENDING, 0)
        resolver.update(uri, values, null, null)
        file.delete()
        return uri
    }

    private fun saveToLegacyAppStorage(file: File, spec: ExportSpec): Uri {
        // App-owned external storage: visible to file managers, requires
        // no permission (privacy contract holds on pre-Q devices too).
        val dir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            ?: throw IOException("External files dir unavailable")
        val target = File(dir, "${spec.displayName}.png")
        file.copyTo(target, overwrite = true)
        file.delete()
        return FileProvider.getUriForFile(context, appAuthority, target)
    }

    companion object {
        private const val PNG_QUALITY = 100

        // Named indirection keeps API-level constants readable in one place.
        private const val MediaStore_DISPLAY_NAME = android.provider.MediaStore.MediaColumns.DISPLAY_NAME
        private const val MediaStore_MIME_TYPE = android.provider.MediaStore.MediaColumns.MIME_TYPE
        private const val MediaStore_RELATIVE_PATH = android.provider.MediaStore.MediaColumns.RELATIVE_PATH
        private const val MediaStore_IS_PENDING = android.provider.MediaStore.MediaColumns.IS_PENDING

        private val IMAGE_COLLECTION_URI: Uri
            get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                android.provider.MediaStore.Images.Media.getContentUri(
                    android.provider.MediaStore.VOLUME_EXTERNAL_PRIMARY,
                )
            } else {
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            }
    }
}
