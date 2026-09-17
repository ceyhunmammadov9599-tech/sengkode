package com.hjinlabs.sengkode.feature.generator

import com.hjinlabs.sengkode.core.model.QrContent
import com.hjinlabs.sengkode.core.model.QrStyle
import com.hjinlabs.sengkode.core.model.repository.StudioRestoreStore
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Session-scope handoff between features (history regenerate,
 * template apply -> studio). Exactly-once consumption, thread-safe.
 */
@Singleton
class InMemoryStudioRestoreStore @Inject constructor() : StudioRestoreStore {

    private var pending: Pair<QrContent, QrStyle>? = null

    @Synchronized
    override fun put(content: QrContent, style: QrStyle) {
        pending = content to style
    }

    @Synchronized
    override fun consume(): Pair<QrContent, QrStyle>? =
        pending.also { pending = null }
}
