package com.hjinlabs.sengkode.core.qr

import com.hjinlabs.sengkode.core.model.EccLevel
import com.hjinlabs.sengkode.core.model.QrContent
import com.hjinlabs.sengkode.core.model.QrError
import com.hjinlabs.sengkode.core.model.QrGenerationResult
import com.hjinlabs.sengkode.core.model.QrMatrix
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.WriterException
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

/**
 * The engine facade: content in, scannable matrix out. Pure JVM, no
 * Android, no I/O - fully unit-testable.
 */
interface QrEngine {
    fun generate(content: QrContent, ecc: EccLevel = EccLevel.DEFAULT): QrGenerationResult
}

/**
 * ZXing-backed implementation. Design notes:
 * - The writer is asked for MARGIN 0; the quiet zone is the renderer's
 *   responsibility (QrStyle.quietZoneModules), which keeps the matrix
 *   pure and re-renderable at any size.
 * - A capacity pre-check runs BEFORE the writer so oversized payloads
 *   get a typed, user-presentable error instead of a WriterException.
 * - UTF-8 is forced so any content encodes identically everywhere.
 */
class ZxingQrEngine(
    private val registry: QrEncoderRegistry = QrEncoderRegistry(),
) : QrEngine {

    override fun generate(content: QrContent, ecc: EccLevel): QrGenerationResult {
        val encoder = registry.encoderFor(content)
        val outcome = encoder.encode(content)
        if (outcome is EncoderOutcome.Invalid) {
            return QrGenerationResult.Failure(
                outcome.errors.first().let { QrError.Validation(it.userMessage) },
            )
        }
        val payload = (outcome as EncoderOutcome.Payload).value

        val capacityReason = Capacity.check(payload, ecc)
        if (capacityReason != null) {
            return QrGenerationResult.Failure(
                QrError.Capacity(
                    userMessage = capacityReason,
                    payloadBytes = payload.toByteArray(Charsets.UTF_8).size,
                    maxBytes = Capacity.maxPayloadBytes(ecc),
                    eccLevel = ecc,
                ),
            )
        }

        return try {
            val bitMatrix = QRCodeWriter().encode(
                payload,
                BarcodeFormat.QR_CODE,
                QUIET_ZONE_FREE_SIZE,
                QUIET_ZONE_FREE_SIZE,
                mapOf(
                    EncodeHintType.ERROR_CORRECTION to ecc.toZxing(),
                    EncodeHintType.MARGIN to 0,
                    EncodeHintType.CHARACTER_SET to Charsets.UTF_8.name(),
                ),
            )
            val width = bitMatrix.width
            val height = bitMatrix.height
            val bits = BooleanArray(width * height) { i ->
                bitMatrix.get(i % width, i / width)
            }
            QrGenerationResult.Success(
                payload = payload,
                matrix = QrMatrix(width = width, height = height, bits = bits),
                eccLevel = ecc,
                version = (width - 17) / 4,
            )
        } catch (e: WriterException) {
            QrGenerationResult.Failure(
                QrError.Encoding(
                    "The content could not be encoded into a QR code. " +
                        "Try shortening it or simplifying special characters.",
                ),
            )
        }
    }

    private fun EccLevel.toZxing(): ErrorCorrectionLevel = when (this) {
        EccLevel.L -> ErrorCorrectionLevel.L
        EccLevel.M -> ErrorCorrectionLevel.M
        EccLevel.Q -> ErrorCorrectionLevel.Q
        EccLevel.H -> ErrorCorrectionLevel.H
    }

    companion object {
        /**
         * Hint size for the writer. ZXing picks the symbol version from
         * the payload, so this only steers internal scaling; the matrix
         * always comes back at true module dimensions.
         */
        private const val QUIET_ZONE_FREE_SIZE = 1
    }
}
