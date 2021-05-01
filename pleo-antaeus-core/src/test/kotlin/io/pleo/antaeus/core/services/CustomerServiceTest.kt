package io.pleo.antaeus.core.services

import io.mockk.every
import io.mockk.mockk
import io.pleo.antaeus.models.Customer
import io.pleo.antaeus.core.exceptions.CustomerNotFoundException
import io.pleo.antaeus.data.AntaeusDal
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import com.beust.klaxon.Klaxon
import io.pleo.antaeus.core.exceptions.CustomerNotCreatedException
import io.pleo.antaeus.models.Currency
import io.pleo.antaeus.models.CustomerStatus
import io.pleo.antaeus.models.CustomerUpdateSchema
import org.junit.jupiter.api.Assertions.*

class CustomerServiceTest {
    private val dal = mockk<AntaeusDal> {
        val result1 = Klaxon().parse<Customer>(
            """
            {
                "id": 200,
                "currency": "NGN",
                "status": "ACTIVE",
                "createdAt": 1619725878925,
                "updatedAt": 1619725878925,
                "deletedAt": null
            }
        """
        )

        val result2 = Klaxon().parse<Customer>("""
            {
                "id": 203,
                "currency": "NGN",
                "status": "ACTIVE",
                "createdAt": 1619725878925,
                "updatedAt": 1619725878925,
                "deletedAt": 1619725878925
            }
        """.trimIndent())

        val result3 = Klaxon().parse<Customer>("""
            {
                "id": 205,
                "currency": "NGN",
                "status": "INACTIVE",
                "createdAt": 1619725878925,
                "updatedAt": 1619725878925,
                "deletedAt": null
            }
        """.trimIndent())

        val result4 = Klaxon().parse<Customer>("""
            {
                "id": 203,
                "currency": "NGN",
                "status": "INACTIVE",
                "createdAt": 1619725878925,
                "updatedAt": 1619725878925,
                "deletedAt": null
            }
        """.trimIndent())

        val result5 = Klaxon().parse<Customer>("""
            {
                "id": 203,
                "currency": "NGN",
                "status": "ACTIVE",
                "createdAt": 1619725878925,
                "updatedAt": 1619725878925,
                "deletedAt": 1619725878925
            }
        """.trimIndent())

        val customerUpdates1 = CustomerUpdateSchema(status = CustomerStatus.INACTIVE)
        val customerUpdates2 = CustomerUpdateSchema(isDeleted = true)


        every { fetchCustomer(404) } returns null

        every { fetchCustomer(200) } returns result1
        every { fetchCustomer(203) } returns result2
        every { fetchCustomer(205) } returns result3

        every { fetchCustomers(true, null) } returns listOf((result2 as Customer))
        every { fetchCustomers(false, null) } returns listOf((result1 as Customer), (result3 as Customer))
        every { fetchCustomers(false, CustomerStatus.INACTIVE ) } returns listOf(result3)

        every { updateCustomer(251, customerUpdates1) } returns null
        every { updateCustomer(203, customerUpdates1) } returns result4
        every { updateCustomer(203, customerUpdates2) } returns result5

        every { createCustomer(Currency.NGN) } returns result1
        every { createCustomer(Currency.SEK) } returns null
    }

    private val customerService = CustomerService(dal = dal)

    @Test
    fun `will throw if customer is not found`() {
        assertThrows<CustomerNotFoundException> {
            customerService.fetch(404)
        }
    }

    @Test
    fun `will return an active customer`() {
        val result = customerService.fetch(200)

        assertEquals(result.id, 200)
        assertEquals(result.status, CustomerStatus.ACTIVE)
        assertNull(result.deletedAt)
    }

    @Test
    fun `will return an inactive customer`() {
        val result = customerService.fetch(205)

        assertEquals(result.id, 205)
        assertEquals(result.status, CustomerStatus.INACTIVE)
        assertNull(result.deletedAt)
    }

    @Test
    fun `will return a soft deleted customer`() {
        val result = customerService.fetch(203)

        assertEquals(result.id, 203)
        assertEquals(result.status, CustomerStatus.ACTIVE)
        assertNotNull(result.deletedAt)
    }

    @Test
    fun `will return a list of undeleted customers`() {
        val results = customerService.fetchAll(false, null)

        assertEquals(results.size, 2)
        assertNull(results[0].deletedAt)
        assertNull(results[1].deletedAt)
    }

    @Test
    fun `will return a list of soft deleted customers`() {
        val results = customerService.fetchAll(true, null)

        println(results)
        assertEquals(results.size, 1)
        assertNotNull(results[0].deletedAt)
    }

    @Test
    fun `will return a list of inactive customers`() {
        val results = customerService.fetchAll(false, CustomerStatus.INACTIVE)

        assertEquals(results.size, 1)
        assertNull(results[0].deletedAt)
    }

    @Test
    fun `will update the status of a customer`() {
        val updates = CustomerUpdateSchema(status = CustomerStatus.INACTIVE)
        val result = customerService.update(203, updates)

        assertEquals(result.id, 203)
        assertEquals(result.status, CustomerStatus.INACTIVE)
        assertNull(result.deletedAt)
    }

    @Test
    fun `will soft delete a customer`() {
        val updates = CustomerUpdateSchema(isDeleted = true)
        val result = customerService.update(203, updates)

        assertEquals(result.id, 203)
        assertNotNull(result.deletedAt)
    }

    @Test
    fun `will throw when customer does not exist`() {
        val updates = CustomerUpdateSchema(status = CustomerStatus.INACTIVE)

        assertThrows<CustomerNotFoundException> {
            customerService.update(251, updates)
        }
    }

    @Test
    fun `will create an active customer`() {
        val result = customerService.create(Currency.NGN)

        assertEquals(result.status, CustomerStatus.ACTIVE)
        assertNull(result.deletedAt)
    }

    @Test
    fun `will throw when unable to create customer`() {
        assertThrows<CustomerNotCreatedException> {
            customerService.create(Currency.SEK)
        }
    }
}
