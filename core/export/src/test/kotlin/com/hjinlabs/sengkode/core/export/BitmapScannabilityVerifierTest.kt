package com.hjinlabs.sengkode.core.export

import com.hjinlabs.sengkode.core.model.EccLevel
import com.hjinlabs.sengkode.core.model.QrContent
import com.hjinlabs.sengkode.core.model.QrGenerationResult
import com.hjinlabs.sengkode.core.model.QrStyle
import com.hjinlabs.sengkode.core.qr.ZxingQrEngine
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class BitmapScannabilityVerifierTest {

    private val engine = ZxingQrEngine()
    private val renderer = DrawListBitmapRenderer()

    @Test
    fun `a rendered code verifies against its own payload`() {
        val result = engine.generate(QrContent.Text("verify me"), EccLevel.M)
            as QrGenerationResult.Success
        val bitmap = renderer.render(result.matrix, QrStyle(), 512)
        assertTrue(BitmapScannabilityVerifier.isScannable(bitmap, result.payload))
    }

    @Test
    fun `verification fails on a different payload`() {
        val result = engine.generate(QrContent.Text("something else"), EccLevel.M)
            as QrGenerationResult.Success
        val bitmap = renderer.render(result.matrix, QrStyle(), 512)
        assertFalse(BitmapScannabilityVerifier.isScannable(bitmap, "not the payload"))
    }

    @Test
    fun `verification fails on a blank bitmap`() {
        val blank = android.graphics.Bitmap.createBitmap(64, 64, android.graphics.Bitmap.Config.ARGB_8888)
        blank.eraseColor(0xFFFFFFFF.toInt())
        assertFalse(BitmapScannabilityVerifier.isScannable(blank, "anything"))
    }
}
