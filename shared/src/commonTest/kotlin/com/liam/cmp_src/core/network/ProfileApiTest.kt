package com.liam.cmp_src.core.network

import com.example.api.ApiRoutes
import com.example.api.common.ErrorCode
import com.example.api.common.ErrorResponse
import com.example.api.common.FieldLimits
import com.example.api.user.UpdateProfileRequest
import com.example.api.user.UserResponse
import com.liam.cmp_src.feature.profile.data.remote.ProfileApi
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.plugins.logging.EMPTY
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Exercises the profile calls against a `MockEngine`, so they run on every target with no server
 * and no network.
 *
 * The multipart assertions read the encoded request body rather than the builder that produced it,
 * because the part name and the single-part shape are exactly what the server refuses when they
 * are wrong, and only the encoded form shows what it will actually see.
 */
class ProfileApiTest {

    @Test
    fun `updating a profile puts the contract request to the contract route`() = runTest {
        var seenPath: String? = null
        var seenMethod: HttpMethod? = null
        var seenBody: String? = null

        val api = profileApi { request ->
            seenPath = request.url.encodedPath
            seenMethod = request.method
            seenBody = request.bodyText()
            respondJson(json.encodeToString(USER_RESPONSE))
        }

        api.updateProfile(displayName = "Ada", avatarUrl = null)

        assertEquals(ApiRoutes.Users.ME, seenPath)
        assertEquals(HttpMethod.Put, seenMethod)
        assertEquals(
            UpdateProfileRequest(displayName = "Ada", avatarUrl = null),
            json.decodeFromString<UpdateProfileRequest>(seenBody.orEmpty()),
        )
    }

    @Test
    fun `updating a profile answers with the account the server now holds`() = runTest {
        val renamed = USER_RESPONSE.copy(displayName = "Ada")
        val api = profileApi { respondJson(json.encodeToString(renamed)) }

        val result = api.updateProfile(displayName = "Ada", avatarUrl = null)

        val success = assertIs<ApiResult.Success<UserResponse>>(result)
        assertEquals("Ada", success.data.displayName)
    }

    @Test
    fun `an upload posts one multipart part under the name the server reads`() = runTest {
        var seenPath: String? = null
        var seenMethod: HttpMethod? = null
        var seenBody: String? = null

        val api = profileApi { request ->
            seenPath = request.url.encodedPath
            seenMethod = request.method
            seenBody = request.bodyText()
            respondJson(json.encodeToString(USER_RESPONSE))
        }

        api.uploadAvatar(bytes = JPEG_BYTES, fileName = "face.jpg", contentType = "image/jpeg")

        assertEquals(ApiRoutes.Users.ME_AVATAR, seenPath)
        assertEquals(HttpMethod.Post, seenMethod)

        val body = seenBody.orEmpty()
        assertTrue(
            body.contains("name=${FieldLimits.AVATAR_PART_NAME}") ||
                body.contains("name=\"${FieldLimits.AVATAR_PART_NAME}\""),
            "the one part must carry the name the server reads, was: $body",
        )
        assertTrue(body.contains("face.jpg"), "the filename should travel with the part")
        assertEquals(
            1,
            Regex("Content-Disposition").findAll(body).count(),
            "the server refuses anything but a single part",
        )
    }

    @Test
    fun `a refused picture is reported as its own failure and not as a generic one`() = runTest {
        val api = profileApi {
            respondJson(
                body = json.encodeToString(
                    ErrorResponse(code = ErrorCode.UNSUPPORTED_IMAGE, message = "Not an image"),
                ),
                status = HttpStatusCode.UnprocessableEntity,
            )
        }

        val result = api.uploadAvatar(JPEG_BYTES, "face.jpg", "image/jpeg")

        val failure = assertIs<ApiResult.Failure>(result)
        val error = assertIs<ApiError.Http>(failure.error)
        assertEquals(ErrorCode.UNSUPPORTED_IMAGE, error.code)
    }

    /** A 204 has no body; decoding one would fail the call that actually worked. */
    @Test
    fun `removing an avatar succeeds on an empty response`() = runTest {
        var seenMethod: HttpMethod? = null
        val api = profileApi { request ->
            seenMethod = request.method
            respond(content = ByteArray(0), status = HttpStatusCode.NoContent)
        }

        val result = api.removeAvatar()

        assertIs<ApiResult.Success<Unit>>(result)
        assertEquals(HttpMethod.Delete, seenMethod)
    }

    // ---- fixtures -------------------------------------------------------------------------

    private fun profileApi(
        handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData,
    ): ProfileApi = ProfileApi(client = testClient(handler))

    private fun testClient(
        handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData,
    ): HttpClient = createHttpClient(
        tokenStore = InMemoryTokenStore(),
        config = ApiConfig(baseUrl = BASE_URL),
        engine = MockEngine(handler),
        logger = Logger.EMPTY,
        logLevel = LogLevel.NONE,
    )

    private fun MockRequestHandleScope.respondJson(
        body: String,
        status: HttpStatusCode = HttpStatusCode.OK,
    ) = respond(
        content = body,
        status = status,
        headers = headersOf(HttpHeaders.ContentType, "application/json"),
    )

    /**
     * The request as the server would receive it.
     *
     * A multipart body is only assembled on its way out, so reading the encoded bytes is the only
     * way to assert on what it actually says. Image bytes are not valid UTF-8 and decode to
     * replacement characters, which is harmless: every header this asserts on is ASCII.
     */
    private suspend fun HttpRequestData.bodyText(): String = body.toByteArray().decodeToString()

    private companion object {
        const val BASE_URL = "https://api.test"

        /** A real JPEG header, so the bytes are the shape the production path would send. */
        val JPEG_BYTES = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0x00, 0x01)

        val json = Json { ignoreUnknownKeys = true }

        val USER_RESPONSE = UserResponse(
            id = "u1",
            email = "demo@cmpsrc.dev",
            displayName = "Demo User",
            avatarUrl = null,
            isEmailVerified = true,
            createdAt = "2026-01-01T00:00:00Z",
            linkedProviders = emptyList(),
        )
    }
}
