package com.hjinlabs.sengkode.app

import kotlinx.serialization.Serializable

/**
 * Type-safe navigation routes (kotlinx.serialization). Each destination is
 * a serializable object; Navigation Compose resolves these types directly,
 * so adding a parameter in a later phase is a compile-time change, never a
 * string-contract change.
 */
@Serializable
data object GeneratorRoute

@Serializable
data object HistoryRoute

@Serializable
data class HistoryDetailRoute(val itemId: Long)

@Serializable
data object TemplatesRoute
