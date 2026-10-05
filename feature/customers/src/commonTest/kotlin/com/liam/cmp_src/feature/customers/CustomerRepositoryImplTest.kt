package com.liam.cmp_src.feature.customers

import com.example.api.ApiRoutes
import com.example.api.common.ErrorCode
import com.example.api.common.ErrorResponse
import com.example.api.common.PageQuery
import com.example.api.common.PageResponse
import com.example.api.customer.CustomerRequest
import com.example.api.customer.CustomerResponse
import com.example.api.customer.CustomerStatus
import com.example.api.user.UserResponse
import com.liam.cmp_src.core.database.entities.CustomerSyncState
import com.liam.cmp_src.core.network.ApiConfig
import com.liam.cmp_src.core.network.InMemoryTokenStore
import com.liam.cmp_src.core.network.createHttpClient
import com.liam.cmp_src.feature.customers.data.CustomerRepositoryImpl
import com.liam.cmp_src.feature.customers.data.remote.CustomerApi
import com.liam.cmp_src.feature.customers.domain.model.CustomerDraft
import com.liam.cmp_src.feature.customers.domain.model.SyncOutcome
import com.liam.cmp_src.feature.customers.domain.model.SyncState
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.io.IOException
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import com.liam.cmp_src.core.database.entities.Customer as CustomerEntity

/**
 * Runs the offline-first repository against an in-memory table ([FakeCustomerDao]) and a scripted
 * server behind a `MockEngine` ([FakeServer]), so the whole push-then-pull runs with no device.
 */
@OptIn(ExperimentalTime::class)
class CustomerRepositoryImplTest {

    private val dispatcher = StandardTestDispatcher()

    // ---- local writes ---------------------------------------------------------------------

    @Test
    fun `a new customer is stored at once and waits for the server`() = runTest(dispatcher) {
        val repository = repository(FakeCustomerDao(), FakeServer())

        val id = repository.create(OWNER, DRAFT)

        val saved = assertNotNull(repository.customer(id))
        assertEquals(DRAFT, saved.details)
        assertEquals(SyncState.PENDING, saved.syncState)
        assertEquals(1, repository.pendingChanges(OWNER).first())
    }

    @Test
    fun `a search reaches the table`() = runTest(dispatcher) {
        val dao = FakeCustomerDao(settledRow("a", firstName = "Ada"), settledRow("g", firstName = "Grace"))
        val repository = repository(dao, FakeServer())

        val found = repository.customers(OWNER, "gr").first()

        assertEquals(listOf("g"), found.map { it.id })
    }

    @Test
    fun `updating a customer that was deleted reports it`() = runTest(dispatcher) {
        val repository = repository(FakeCustomerDao(), FakeServer())

        assertEquals(false, repository.update("missing", DRAFT))
    }

    // ---- sync: push -----------------------------------------------------------------------

    @Test
    fun `a customer created offline is posted and then settles under the same local id`() = runTest(dispatcher) {
        val server = FakeServer()
        val dao = FakeCustomerDao()
        val repository = repository(dao, server)
        val id = repository.create(OWNER, DRAFT)

        val outcome = repository.sync(OWNER)

        assertEquals(SyncOutcome.Synced, outcome)
        assertEquals(1, server.creates.size)
        val row = assertNotNull(dao.rowOf(id))
        assertEquals(CustomerSyncState.SYNCED, row.syncState)
        assertEquals(server.customers.keys.single(), row.remoteId)
        assertEquals(1, dao.rows.value.size, "the pull must not add the customer a second time")
    }

    @Test
    fun `an edit to a synced customer is put to its server id`() = runTest(dispatcher) {
        val server = FakeServer(response("server-1"))
        val dao = FakeCustomerDao(settledRow("local-1", remoteId = "server-1"))
        val repository = repository(dao, server)

        repository.update("local-1", DRAFT.copy(firstName = "Augusta"))
        repository.sync(OWNER)

        assertEquals(listOf("server-1"), server.replaces.map { it.first })
        assertEquals("Augusta", server.replaces.single().second.firstName)
        assertEquals(CustomerSyncState.SYNCED, dao.rowOf("local-1")?.syncState)
    }

    @Test
    fun `a delete reaches the server and the row leaves the device`() = runTest(dispatcher) {
        val server = FakeServer(response("server-1"))
        val dao = FakeCustomerDao(settledRow("local-1", remoteId = "server-1"))
        val repository = repository(dao, server)

        repository.delete("local-1")
        repository.sync(OWNER)

        assertEquals(listOf("server-1"), server.deletes)
        assertNull(dao.rowOf("local-1"))
    }

    @Test
    fun `a customer created and deleted offline never reaches the server`() = runTest(dispatcher) {
        val server = FakeServer()
        val dao = FakeCustomerDao()
        val repository = repository(dao, server)
        val id = repository.create(OWNER, DRAFT)

        repository.delete(id)
        repository.sync(OWNER)

        assertTrue(server.creates.isEmpty() && server.deletes.isEmpty())
        assertNull(dao.rowOf(id))
    }

    @Test
    fun `a change the server refuses is parked and the rest still go up`() = runTest(dispatcher) {
        val server = FakeServer().apply { refuseCreatesNamed = "Bad" }
        val dao = FakeCustomerDao()
        val repository = repository(dao, server)
        val bad = repository.create(OWNER, DRAFT.copy(firstName = "Bad"))
        val good = repository.create(OWNER, DRAFT.copy(firstName = "Good"))

        val outcome = repository.sync(OWNER)

        assertEquals(SyncOutcome.Synced, outcome)
        assertEquals(CustomerSyncState.REJECTED, dao.rowOf(bad)?.syncState)
        assertEquals(CustomerSyncState.SYNCED, dao.rowOf(good)?.syncState)
        assertEquals(0, repository.pendingChanges(OWNER).first(), "a refused change is not retried")
    }

    @Test
    fun `an edit to a customer deleted on another device gives way to the deletion`() = runTest(dispatcher) {
        val server = FakeServer()
        val dao = FakeCustomerDao(settledRow("local-1", remoteId = "server-1"))
        val repository = repository(dao, server)

        repository.update("local-1", DRAFT)
        repository.sync(OWNER)

        assertNull(dao.rowOf("local-1"))
    }

    // ---- sync: failures -------------------------------------------------------------------

    @Test
    fun `offline the change stays queued and the sync says so`() = runTest(dispatcher) {
        val server = FakeServer().apply { offline = true }
        val dao = FakeCustomerDao()
        val repository = repository(dao, server)
        val id = repository.create(OWNER, DRAFT)

        val outcome = repository.sync(OWNER)

        assertEquals(SyncOutcome.Offline, outcome)
        assertEquals(CustomerSyncState.PENDING, dao.rowOf(id)?.syncState)
    }

    @Test
    fun `another account's session sends nothing`() = runTest(dispatcher) {
        val server = FakeServer().apply { signedInAs = "someone-else" }
        val repository = repository(FakeCustomerDao(), server)
        repository.create(OWNER, DRAFT)

        val outcome = repository.sync(OWNER)

        assertEquals(SyncOutcome.SessionMismatch, outcome)
        assertTrue(server.creates.isEmpty())
    }

    @Test
    fun `a struggling server stops the push for a later retry`() = runTest(dispatcher) {
        val server = FakeServer().apply { failWrites = HttpStatusCode.ServiceUnavailable }
        val dao = FakeCustomerDao()
        val repository = repository(dao, server)
        val id = repository.create(OWNER, DRAFT)

        assertEquals(SyncOutcome.Failed, repository.sync(OWNER))
        assertEquals(CustomerSyncState.PENDING, dao.rowOf(id)?.syncState)
    }

    // ---- sync: pull -----------------------------------------------------------------------

    @Test
    fun `the pull walks every page into the table`() = runTest(dispatcher) {
        val server = FakeServer(response("s1"), response("s2"), response("s3")).apply { pageSize = 2 }
        val dao = FakeCustomerDao()
        val repository = repository(dao, server)

        repository.sync(OWNER)

        assertEquals(setOf("s1", "s2", "s3"), repository.customers(OWNER, "").first().map { it.id }.toSet())
        assertEquals(2, server.listCalls)
    }

    @Test
    fun `a customer deleted on another device leaves this one too`() = runTest(dispatcher) {
        val dao = FakeCustomerDao(settledRow("local-1", remoteId = "gone"))
        val repository = repository(dao, FakeServer())

        repository.sync(OWNER)

        assertNull(dao.rowOf("local-1"))
    }

    @Test
    fun `the pull does not overwrite a change the server refused`() = runTest(dispatcher) {
        val server = FakeServer(response("server-1")).apply { failWrites = HttpStatusCode.Conflict }
        val dao = FakeCustomerDao(settledRow("local-1", remoteId = "server-1"))
        val repository = repository(dao, server)
        repository.update("local-1", DRAFT.copy(firstName = "Mine"))

        repository.sync(OWNER)

        assertEquals("Mine", dao.rowOf("local-1")?.firstName)
    }

    // ---- fixtures -------------------------------------------------------------------------

    private fun repository(dao: FakeCustomerDao, server: FakeServer) = CustomerRepositoryImpl(
        api = CustomerApi(
            createHttpClient(
                tokenStore = InMemoryTokenStore(),
                config = ApiConfig(baseUrl = BASE_URL),
                engine = MockEngine { server.handle(this, it) },
                logger = Logger.EMPTY,
                logLevel = LogLevel.NONE,
            ),
        ),
        dao = dao,
        dispatcher = dispatcher,
        clock = TickingClock(),
        newId = IdSequence("local-new-")::next,
    )

    /** Every reading a second later than the last, so each write has its own `updatedAt`. */
    private class TickingClock : Clock {
        private var now = Instant.parse("2026-09-20T00:00:00Z")
        override fun now(): Instant = now.also { now += 1.seconds }
    }

    private class IdSequence(private val prefix: String) {
        private var next = 0
        fun next(): String = "$prefix${next++}"
    }

    /**
     * Just enough of the server to sync against: the customer routes over a map, and the owner
     * check. Its switches reproduce the failures the sync must survive.
     */
    private class FakeServer(vararg seeded: CustomerResponse) {
        val customers = seeded.associateBy { it.id }.toMutableMap()
        val creates = mutableListOf<CustomerRequest>()
        val replaces = mutableListOf<Pair<String, CustomerRequest>>()
        val deletes = mutableListOf<String>()
        var listCalls = 0
        var offline = false
        var signedInAs = OWNER
        var refuseCreatesNamed: String? = null
        var failWrites: HttpStatusCode? = null
        var pageSize = Int.MAX_VALUE
        private var nextId = 0

        suspend fun handle(scope: MockRequestHandleScope, request: HttpRequestData): HttpResponseData {
            if (offline) throw IOException("offline")
            val path = request.url.encodedPath
            val byIdPrefix = ApiRoutes.Customers.PATH + "/"
            return when {
                path == ApiRoutes.Users.ME -> scope.json(json.encodeToString(user(signedInAs)))
                path == ApiRoutes.Customers.PATH && request.method == HttpMethod.Get -> scope.list(request)
                path == ApiRoutes.Customers.PATH && request.method == HttpMethod.Post -> scope.create(request)
                path.startsWith(byIdPrefix) -> scope.byId(path.removePrefix(byIdPrefix), request)
                else -> scope.respond("", HttpStatusCode.NotFound)
            }
        }

        private fun MockRequestHandleScope.list(request: HttpRequestData): HttpResponseData {
            listCalls++
            val start = request.url.parameters[PageQuery.CURSOR]?.toInt() ?: 0
            val all = customers.values.sortedBy { it.id }
            val end = minOf(start + pageSize, all.size)
            val page = PageResponse(all.subList(start, end), nextCursor = end.takeIf { it < all.size }?.toString())
            return json(json.encodeToString(page))
        }

        private suspend fun MockRequestHandleScope.create(request: HttpRequestData): HttpResponseData {
            failWrites?.let { return error(it) }
            val body = json.decodeFromString<CustomerRequest>(request.body.toByteArray().decodeToString())
            if (body.firstName == refuseCreatesNamed) return error(HttpStatusCode.BadRequest)
            creates += body
            val created = body.toResponse("server-${nextId++}")
            customers[created.id] = created
            return json(json.encodeToString(created))
        }

        private suspend fun MockRequestHandleScope.byId(id: String, request: HttpRequestData): HttpResponseData {
            failWrites?.let { return error(it) }
            if (id !in customers) return error(HttpStatusCode.NotFound)
            return when (request.method) {
                HttpMethod.Delete -> {
                    deletes += id
                    customers -= id
                    respond(ByteArray(0), HttpStatusCode.NoContent)
                }
                else -> {
                    val body = json.decodeFromString<CustomerRequest>(request.body.toByteArray().decodeToString())
                    replaces += id to body
                    val replaced = body.toResponse(id)
                    customers[id] = replaced
                    json(json.encodeToString(replaced))
                }
            }
        }

        private fun MockRequestHandleScope.json(body: String) = respond(
            content = body,
            status = HttpStatusCode.OK,
            headers = headersOf(HttpHeaders.ContentType, "application/json"),
        )

        private fun MockRequestHandleScope.error(status: HttpStatusCode) = respond(
            content = json.encodeToString(ErrorResponse(ErrorCode.VALIDATION_ERROR, "no")),
            status = status,
            headers = headersOf(HttpHeaders.ContentType, "application/json"),
        )
    }

    private companion object {
        const val OWNER = "owner-1"
        const val BASE_URL = "https://api.test"
        val json = Json { ignoreUnknownKeys = true }

        val DRAFT = CustomerDraft(firstName = "Ada", lastName = "Lovelace", status = CustomerStatus.ACTIVE)

        fun user(id: String) = UserResponse(
            id = id, email = null, displayName = null, avatarUrl = null,
            isEmailVerified = true, createdAt = "2026-01-01T00:00:00Z", linkedProviders = emptyList(),
        )

        fun response(id: String) = CustomerResponse(
            id = id, firstName = "Server", lastName = null, companyName = null, email = null,
            phone = null, address = null, notes = null, status = CustomerStatus.ACTIVE.key,
            createdAt = "2026-09-01T00:00:00Z", updatedAt = "2026-09-01T00:00:00Z",
        )

        fun CustomerRequest.toResponse(id: String) = CustomerResponse(
            id = id, firstName = firstName, lastName = lastName, companyName = companyName,
            email = email, phone = phone, address = address, notes = notes, status = status.key,
            createdAt = "2026-09-01T00:00:00Z", updatedAt = "2026-09-01T00:00:01Z",
        )

        fun settledRow(id: String, remoteId: String = id, firstName: String = "Ada") = CustomerEntity(
            id = id, ownerId = OWNER, firstName = firstName, status = CustomerStatus.ACTIVE.key,
            createdAt = "2026-09-01T00:00:00Z", updatedAt = "2026-09-01T00:00:00Z",
            deletedAt = null, remoteId = remoteId, syncState = CustomerSyncState.SYNCED,
        )
    }
}
