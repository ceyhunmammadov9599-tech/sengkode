package com.hjinlabs.sengkode.app

import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Navigation contract test: every type-safe route must survive a
 * kotlinx.serialization round trip (encode -> decode -> equality).
 * Navigation Compose serializes routes under the hood, so this pins the
 * contract at the JVM level without an emulator.
 */
class RouteContractTest {

    @Test
    fun `routes round-trip through serialization`() {
        val routes = listOf(
            GeneratorRoute,
            HistoryRoute,
            HistoryDetailRoute(itemId = 42L),
            TemplatesRoute,
        )
        assertTrue(routes.all { route -> roundTrip(route) == route })
    }

    /**
     * Verified behavior (Phase 0 finding): kotlinx.serialization encodes
     * every @Serializable data object as "{}" - no class discriminator.
     * Navigation Compose therefore identifies destinations by the route's
     * qualified class name, which is exactly what this asserts: the real
     * navigation contract, pinned as a JVM test.
     */
    @Test
    fun `distinct routes resolve to distinct navigation identities`() {
        val names = listOf(GeneratorRoute, HistoryRoute, TemplatesRoute)
            .map { it::class.qualifiedName }
        assertTrue(names.none { it.isNullOrBlank() })
        assertEquals(3, names.toSet().size)
        assertTrue(HistoryDetailRoute::class.qualifiedName != HistoryRoute::class.qualifiedName)
    }

    // -- helpers -----------------------------------------------------------

    private fun encode(route: Any): String = when (route) {
        is GeneratorRoute -> Json.encodeToString(serializer<GeneratorRoute>(), route)
        is HistoryRoute -> Json.encodeToString(serializer<HistoryRoute>(), route)
        is HistoryDetailRoute -> Json.encodeToString(
            serializer<HistoryDetailRoute>(), route,
        )
        is TemplatesRoute -> Json.encodeToString(serializer<TemplatesRoute>(), route)
        else -> error("Unknown route type: $route")
    }

    private fun decode(routeType: String, payload: String): Any = when (routeType) {
        HistoryDetailRoute::class.qualifiedName!! ->
            Json.decodeFromString(serializer<HistoryDetailRoute>(), payload)
        GeneratorRoute::class.qualifiedName!! ->
            Json.decodeFromString(serializer<GeneratorRoute>(), payload)
        HistoryRoute::class.qualifiedName!! ->
            Json.decodeFromString(serializer<HistoryRoute>(), payload)
        TemplatesRoute::class.qualifiedName!! ->
            Json.decodeFromString(serializer<TemplatesRoute>(), payload)
        else -> error("Unknown route type: $routeType")
    }

    private fun roundTrip(route: Any): Any {
        val payload = encode(route)
        return decode(route::class.qualifiedName!!, payload)
    }
}
