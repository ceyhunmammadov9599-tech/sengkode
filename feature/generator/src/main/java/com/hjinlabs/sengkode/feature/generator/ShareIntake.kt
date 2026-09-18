package com.hjinlabs.sengkode.feature.generator

import com.hjinlabs.sengkode.core.model.QrContent
import com.hjinlabs.sengkode.core.model.QrStyle
import com.hjinlabs.sengkode.core.model.repository.StudioRestoreStore

/**
 * ACTION_SEND intake (Phase 4): turns shared text into studio
 * content. Pure and JVM-testable; the Activity only bridges the
 * Intent extras into here and hands the result to the existing
 * session restore store - the studio then runs its normal pipeline
 * (generation, safety policy, rendering). No QR logic is duplicated
 * and no safety check is bypassed: the content flows through the
 * exact same validation as typed content.
 */
object ShareIntake {

    /** Generous but bounded: QR-M holds ~1000 chars; more is a mistake. */
    const val MAX_SHARE_LENGTH = 1000

    private val URL_PATTERN = Regex("(?i)^https?://\\S+$")

    /** Null means "nothing useful shared" - the app just opens normally. */
    fun parse(sharedText: String?): QrContent? {
        val trimmed = sharedText?.trim() ?: return null
        if (trimmed.isEmpty()) return null
        if (trimmed.length > MAX_SHARE_LENGTH) return null
        return if (URL_PATTERN.matches(trimmed)) {
            QrContent.Url(trimmed)
        } else {
            QrContent.Text(trimmed)
        }
    }

    /** Returns true when the share was accepted and staged for the studio. */
    fun stageInto(content: QrContent?, restoreStore: StudioRestoreStore): Boolean {
        if (content == null) return false
        restoreStore.put(content, QrStyle())
        return true
    }
}
