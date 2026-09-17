package com.liam.cmp_src.core.network

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Guards the one-line environment switch: a default [ApiConfig] must follow
 * [ApiEnvironment.ACTIVE], and an explicit environment or base URL must override it.
 */
class ApiConfigTest {

    @Test
    fun `default config follows the active environment`() {
        val config = ApiConfig()

        assertEquals(ApiEnvironment.ACTIVE, config.environment)
        assertEquals(ApiEnvironment.ACTIVE.baseUrl, config.baseUrl)
    }

    @Test
    fun `production is https and shared by every target`() {
        assertEquals(ApiConfig.PRODUCTION_BASE_URL, ApiEnvironment.PRODUCTION.baseUrl)
        assertTrue(ApiEnvironment.PRODUCTION.baseUrl.startsWith("https://"))
    }

    @Test
    fun `local points at this target's host on the contract port`() {
        assertEquals(
            "http://${localApiHost()}:${ApiConfig.LOCAL_PORT}",
            ApiEnvironment.LOCAL.baseUrl,
        )
    }

    @Test
    fun `base urls carry no trailing path`() {
        ApiEnvironment.entries.forEach { environment ->
            assertTrue(
                !environment.baseUrl.removePrefix("https://").removePrefix("http://").contains('/'),
                "${environment.name} base URL must be scheme, host and port only",
            )
        }
    }

    @Test
    fun `an explicit environment overrides the active one`() {
        assertEquals(
            ApiConfig.PRODUCTION_BASE_URL,
            ApiConfig(environment = ApiEnvironment.PRODUCTION).baseUrl,
        )
    }

    @Test
    fun `an explicit base url wins over the environment`() {
        val config = ApiConfig(baseUrl = "http://192.168.1.10:${ApiConfig.LOCAL_PORT}")

        assertEquals("http://192.168.1.10:${ApiConfig.LOCAL_PORT}", config.baseUrl)
    }
}
