package com.hjinlabs.sengkode.core.export

import android.graphics.Bitmap
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader

/**
 * Runtime scannability gate (Phase 4 logo workflow): decode what was
 * actually rendered and require the EXACT original payload. This is
 * the same generate -> decode -> assert contract as the JVM
 * round-trip suite, executed on device at the moment the user adds a
 * logo. Static policies stay first (they gate rendering); this is
 * the final "it really scans" check.
 */
object BitmapScannabilityVerifier {

    fun isScannable(bitmap: Bitmap, expectedPayload: String): Boolean {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        val source = RGBLuminanceSource(width, height, pixels)
        val binary = BinaryBitmap(HybridBinarizer(source))
        val hints = mapOf(DecodeHintType.CHARACTER_SET to Charsets.UTF_8.name())
        val decoded = runCatching {
            QRCodeReader().decode(binary, hints).text
        }.getOrNull()
        return decoded == expectedPayload
    }
}
