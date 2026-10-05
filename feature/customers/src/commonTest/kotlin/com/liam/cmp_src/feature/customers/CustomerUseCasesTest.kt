package com.liam.cmp_src.feature.customers

import com.example.api.customer.CustomerStatus
import com.liam.cmp_src.feature.customers.domain.model.CustomerDraft
import com.liam.cmp_src.feature.customers.domain.model.SyncOutcome
import com.liam.cmp_src.feature.customers.domain.usecase.DeleteCustomerUseCase
import com.liam.cmp_src.feature.customers.domain.usecase.GetCustomerUseCase
import com.liam.cmp_src.feature.customers.domain.usecase.ObserveCustomersUseCase
import com.liam.cmp_src.feature.customers.domain.usecase.ObservePendingChangesUseCase
import com.liam.cmp_src.feature.customers.domain.usecase.SaveCustomerUseCase
import com.liam.cmp_src.feature.customers.domain.usecase.SyncCustomersUseCase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** The thin use cases: what each passes on, and which of them ask for a sync. */
class CustomerUseCasesTest {

    private val repository = FakeCustomerRepository(sampleCustomer(id = "c1"))
    private val scheduler = FakeSyncScheduler()

    @Test
    fun `saving a new customer creates it and asks for a sync`() = runTest {
        val saved = SaveCustomerUseCase(repository, scheduler)(OWNER, id = null, draft = DRAFT)

        assertTrue(saved)
        assertEquals(listOf(DRAFT), repository.created)
        assertEquals(listOf(OWNER), scheduler.requests)
    }

    @Test
    fun `saving an existing customer updates it and asks for a sync`() = runTest {
        SaveCustomerUseCase(repository, scheduler)(OWNER, id = "c1", draft = DRAFT)

        assertEquals(listOf("c1" to DRAFT), repository.updated)
        assertEquals(listOf(OWNER), scheduler.requests)
    }

    @Test
    fun `saving a customer that has gone asks for nothing`() = runTest {
        val saved = SaveCustomerUseCase(repository, scheduler)(OWNER, id = "gone", draft = DRAFT)

        assertFalse(saved)
        assertTrue(scheduler.requests.isEmpty())
    }

    @Test
    fun `deleting asks for a sync`() = runTest {
        DeleteCustomerUseCase(repository, scheduler)(OWNER, "c1")

        assertEquals(listOf("c1"), repository.deleted)
        assertEquals(listOf(OWNER), scheduler.requests)
    }

    @Test
    fun `a search is trimmed before it reaches the table`() = runTest {
        ObserveCustomersUseCase(repository)(OWNER, "  ad ").first()

        assertEquals(listOf("ad"), repository.searches)
    }

    @Test
    fun `reads and syncs pass straight through`() = runTest {
        repository.pending.value = 2
        repository.syncOutcome = SyncOutcome.Offline

        assertEquals("c1", GetCustomerUseCase(repository)("c1")?.id)
        assertEquals(2, ObservePendingChangesUseCase(repository)(OWNER).first())
        assertEquals(SyncOutcome.Offline, SyncCustomersUseCase(repository)(OWNER))
    }

    private companion object {
        const val OWNER = "owner-1"
        val DRAFT = CustomerDraft(firstName = "Ada", status = CustomerStatus.ACTIVE)
    }
}
