package com.hjinlabs.sengkode.core.style

import com.hjinlabs.sengkode.core.model.QrContent
import com.hjinlabs.sengkode.core.model.QrGenerationResult
import com.hjinlabs.sengkode.core.model.QrStyle
import com.hjinlabs.sengkode.core.qr.ZxingQrEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Contract tests for the template gallery: every built-in template
 * must be policy-safe AND its full style must survive a serialization
 * round trip (that is how templates persist) AND render+decode - the
 * same gate every shipped style passes.
 */
class BuiltInTemplatesTest {

    private val engine = ZxingQrEngine()
    private val renderer = QrStyleRenderer()

    @Test
    fun `every built-in template is policy safe at a typical symbol`() {
        BuiltInTemplates.ALL.forEach { template ->
            val verdict = ScanSafetyPolicy.evaluate(template.style, matrixWidth = 29)
            assertTrue(
                "Template ${template.name} must be safe, was: $verdict",
                verdict.isSafe,
            )
        }
    }

    @Test
    fun `every template style survives a serialization round trip`() {
        val json = kotlinx.serialization.json.Json
        BuiltInTemplates.ALL.forEach { template ->
            val encoded = json.encodeToString(QrStyle.serializer(), template.style)
            val decoded = json.decodeFromString(QrStyle.serializer(), encoded)
            assertEquals(template.style, decoded)
        }
    }

    @Test
    fun `applying each template and rendering decodes`() {
        val json = kotlinx.serialization.json.Json
        BuiltInTemplates.ALL.forEach { template ->
            // Persistence path: JSON round trip, then render -> decode.
            val encoded = json.encodeToString(QrStyle.serializer(), template.style)
            val style = json.decodeFromString(QrStyle.serializer(), encoded)
            val content = when (template.contentTypeHint) {
                com.hjinlabs.sengkode.core.model.QrContentType.TEXT ->
                    QrContent.Text("SENGKODE template ${template.name}")
                com.hjinlabs.sengkode.core.model.QrContentType.URL ->
                    QrContent.Url("https://hjinlabs.app/${template.name.lowercase()}")
                com.hjinlabs.sengkode.core.model.QrContentType.VCARD ->
                    QrContent.VCard(
                        "Jeyhun Mammadov", "HJIN Labs", "+994 50 123 45 67",
                        "jeyhun@hjinlabs.app", "https://hjinlabs.app", null,
                    )
                else -> QrContent.Text("SENGKODE template ${template.name}")
            }
            val result = engine.generate(
                content,
                com.hjinlabs.sengkode.core.model.EccLevel.M,
            )
            assertTrue(result is QrGenerationResult.Success)
            result as QrGenerationResult.Success

            // Reuse the styled round-trip harness pieces inline:
            val ops = renderer.render(result.matrix, style, 512)
            val decoded = RoundTripHarness.decode(RoundTripHarness.paint(ops, 512))
            assertEquals(result.payload, decoded)
        }
    }
}
