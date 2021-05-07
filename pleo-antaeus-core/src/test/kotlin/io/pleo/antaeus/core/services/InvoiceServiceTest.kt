package io.pleo.antaeus.core.services

/* ktlint-disable no-wildcard-imports */
import io.mockk.every
import io.mockk.mockk
import io.pleo.antaeus.core.exceptions.InvoiceNotFoundException
import io.pleo.antaeus.core.exceptions.InvoiceNotCreatedException
import io.pleo.antaeus.data.AntaeusDal
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertNotNull
import com.beust.klaxon.Klaxon
import io.pleo.antaeus.models.*
import java.math.BigDecimal
/* ktlint-enable no-wildcard-imports */


class InvoiceServiceTest {
    private val dal = mockk<AntaeusDal> {
        val result1 = Klaxon().parse<Invoice>("""
            {
                "id": 200,
                "customerId": 12,
                "subscriptionId": 12,
                "amount": {
                    "value": 5000,
                    "currency": "NGN"
                },
                "status": "PENDING",
                "paymentRef": null,
                "lastFailedCharge": null,
                "numberOfFailedCharges": null,
                "description": "Initial invoice charge for 20days",
                "chargeStartDate": 1619725878925,
                "chargeEndDate": 1619725878925,
                "createdAt": 1619725878925,
                "updatedAt": 1619725878925,
                "deletedAt": null
            }
        """)

        val result2 = Klaxon().parse<Invoice>("""
            {
                "id": 203,
                "customerId": 12,
                "subscriptionId": 12,
                "amount": {
                    "value": 5000,
                    "currency": "NGN"
                },
                "status": "PENDING",
                "paymentRef": null,
                "lastFailedCharge": null,
                "numberOfFailedCharges": null,
                "description": "Initial invoice charge for 11days",
                "chargeStartDate": 1619725878925,
                "chargeEndDate": 1619725878925,
                "createdAt": 1619725878925,
                "updatedAt": 1619725878925,
                "deletedAt": 1619725878955
            }
        """)

        val result3 = Klaxon().parse<Invoice>("""
            {
                "id": 205,
                "customerId": 12,
                "subscriptionId": 12,
                "amount": {
                    "value": 5000,
                    "currency": "NGN"
                },
                "status": "PAID",
                "paymentRef": "ABCDEF12345",
                "lastFailedCharge": null,
                "numberOfFailedCharges": null,
                "description": null,
                "chargeStartDate": 1619725878925,
                "chargeEndDate": 1619725878925,
                "createdAt": 1619725878925,
                "updatedAt": 1619725878925,
                "deletedAt": null
            }
        """)

        val invoiceUpdates1 = InvoiceUpdateSchema(
            amount = Money(value = BigDecimal(6000), currency = Currency.USD)
        )

        val invoiceUpdates2 = InvoiceUpdateSchema(
            isDeleted = true
        )

        val result4 = Klaxon().parse<Invoice>("""
            {
                "id": 200,
                "customerId": 12,
                "subscriptionId": 12,
                "amount": {
                    "value": 6000,
                    "currency": "USD"
                },
                "status": "PENDING",
                "paymentRef": null,
                "lastFailedCharge": null,
                "numberOfFailedCharges": null,
                "description": "Initial invoice charge for 20days",
                "chargeStartDate": 1619725878925,
                "chargeEndDate": 1619725878925,
                "createdAt": 1619725878925,
                "updatedAt": 1619725878925,
                "deletedAt": null
            }
        """)

        val result5 = Klaxon().parse<Invoice>("""
            {
                "id": 200,
                "customerId": 12,
                "subscriptionId": 12,
                "amount": {
                    "value": 5000,
                    "currency": "NGN"
                },
                "status": "PENDING",
                "paymentRef": null,
                "lastFailedCharge": null,
                "numberOfFailedCharges": null,
                "description": "Initial invoice charge for 20days",
                "chargeStartDate": 1619725878925,
                "chargeEndDate": 1619725878925,
                "createdAt": 1619725878925,
                "updatedAt": 1619725878925,
                "deletedAt": 1619725878925
            }
        """)

        val result6 = Klaxon().parse<Invoice>("""
            {
                "id": 200,
                "customerId": 12,
                "subscriptionId": 12,
                "amount": {
                    "value": 10000,
                    "currency": "NGN"
                },
                "status": "PENDING",
                "paymentRef": null,
                "lastFailedCharge": null,
                "numberOfFailedCharges": null,
                "description": "Initial invoice charge for 20days",
                "chargeStartDate": 1619725878925,
                "chargeEndDate": 1619725878925,
                "createdAt": 1619725878925,
                "updatedAt": 1619725878925,
                "deletedAt": null
            }
        """)

        val customer1 = Customer(
            id = 12,
            currency = Currency.NGN,
            createdAt = 1619725878925,
            updatedAt = 1619725878925
        )

        val subscription1 = Subscription(
            id = 12,
            planId = 12,
            customerId = customer1.id,
            amount = Money(value = BigDecimal(5000), currency = Currency.NGN),
            createdAt = 1619725878925,
            updatedAt = 1619725878925
        )
        val customAmount1 = Money(value = BigDecimal(10000), currency = Currency.NGN)
        val customAmount2 = Money(value = BigDecimal(10), currency = Currency.NGN)

        every { fetchInvoice(404) } returns null
        every { fetchInvoice(200) } returns result1
        every { fetchInvoice(203) } returns result2
        every { fetchInvoice(205) } returns result3

        every { fetchInvoices(false, null) } returns listOf((result1 as Invoice), (result3 as Invoice))
        every { fetchInvoices(true, null) } returns listOf((result2 as Invoice))
        every { fetchInvoices(false, InvoiceStatus.PAID) } returns listOf(result3)

        
        every { updateInvoice(200, invoiceUpdates1) } returns result4
        every { updateInvoice(200, invoiceUpdates2) } returns result5
        every { updateInvoice(220, invoiceUpdates1) } returns null

        every { createInvoice(subscription1.amount, customer1, subscription1) } returns result1
        every { createInvoice(customAmount1, customer1, subscription1) } returns result6
        every { createInvoice(customAmount2, customer1, subscription1) } returns null

    }

    private val invoiceService = InvoiceService(dal = dal)

    @Test
    fun `will throw if invoice is not found`() {

        assertThrows<InvoiceNotFoundException> {
            invoiceService.fetch(404)
        }
    }

    @Test
    fun `will return a pending invoice`() {
        val result = invoiceService.fetch(200)


        assertEquals(result.id, 200)
        assertEquals(result.status, InvoiceStatus.PENDING)
        assertNull(result.deletedAt)
    }

    @Test
    fun `will return a soft deleted invoice`() {
        val result = invoiceService.fetch(203)

        assertEquals(result.id, 203)
        assertEquals(result.status, InvoiceStatus.PENDING)
        assertNotNull(result.deletedAt)
    }

    @Test
    fun `will return a paid invoice with reference 'ABCDEF12345'`() {
        val result = invoiceService.fetch(205)

        assertEquals(result.id, 205)
        assertEquals(result.paymentRef, "ABCDEF12345")
        assertEquals(result.status, InvoiceStatus.PAID)
        assertNull(result.deletedAt)
    }

    // fetch invoices test
    @Test
    fun `will get 1 paid invoice`() {
        val result = invoiceService.fetchAll(false, InvoiceStatus.PAID)

        assertEquals(result.size, 1)
        assertEquals(result[0].status, InvoiceStatus.PAID)
    }

    @Test
    fun `will get undeleted invoices`() {
        val result = invoiceService.fetchAll(false)

        assertEquals(result.size, 2)
        assertNull(result[0].deletedAt)
        assertNull(result[1].deletedAt)
    }

    @Test
    fun `will get soft deleted invoices`() {
        val result = invoiceService.fetchAll(true)

        assertEquals(result.size, 1)
        assertNotNull(result[0].deletedAt)
    }

    // update invoices Test
    @Test
    fun `will update invoice amount`() {
        val updates = InvoiceUpdateSchema(
            amount = Money(value = BigDecimal(6000), currency = Currency.USD)
        )

        val result = invoiceService.update(200, updates)

        assertEquals(result.amount.value, BigDecimal(6000))
        assertEquals(result.amount.currency, Currency.USD)
        assertNull(result.deletedAt)
    }

    @Test
    fun `will soft delete an invoice`() {
        val updates = InvoiceUpdateSchema(
            isDeleted = true
        )

        val result = invoiceService.update(200, updates)

        assertEquals(result.amount.value, BigDecimal(5000))
        assertEquals(result.amount.currency, Currency.NGN)
        assertNotNull(result.deletedAt)
    }

    @Test
    fun `will throw if invoice does not exist`() {
        val updates = InvoiceUpdateSchema(
            amount = Money(value = BigDecimal(6000), currency = Currency.USD)
        )

        assertThrows<InvoiceNotFoundException> {
            invoiceService.update(220, updates)
        }
    }

    // create invoice test
    @Test
    fun `will create a new invoice with subscription amount`() {
        val customer = Customer(
            id = 12,
            currency = Currency.NGN,
            createdAt = 1619725878925,
            updatedAt = 1619725878925
        )
        val subscription = Subscription(
            id = 12,
            planId = 12,
            customerId = customer.id,
            amount = Money(value = BigDecimal(5000), currency = Currency.NGN),
            createdAt = 1619725878925,
            updatedAt = 1619725878925
        )

        val result = invoiceService.create(customer, subscription)

        assertEquals(result.status, InvoiceStatus.PENDING)
        assertEquals(result.customerId, customer.id)
        assertEquals(result.subscriptionId, subscription.id)
        assertEquals(result.amount.value, subscription.amount.value)
        assertEquals(result.amount.currency, subscription.amount.currency)
        assertNull(result.deletedAt)
    }

    @Test
    fun `will create a new invoice with custom amount`() {
        val customer = Customer(
            id = 12,
            currency = Currency.NGN,
            createdAt = 1619725878925,
            updatedAt = 1619725878925
        )
        val subscription = Subscription(
            id = 12,
            planId = 12,
            customerId = customer.id,
            amount = Money(value = BigDecimal(5000), currency = Currency.NGN),
            createdAt = 1619725878925,
            updatedAt = 1619725878925
        )
        val customAmount = Money(value = BigDecimal(10000), currency = Currency.NGN)

        val result = invoiceService.create(customer, subscription, customAmount)

        assertEquals(result.status, InvoiceStatus.PENDING)
        assertEquals(result.customerId, customer.id)
        assertEquals(result.subscriptionId, subscription.id)
        assertEquals(result.amount.value, customAmount.value)
        assertEquals(result.amount.currency, customAmount.currency)
        assertNull(result.deletedAt)
    }

    @Test
    fun `will throw if invoice not created`() {
        val customer = Customer(
            id = 12,
            currency = Currency.NGN,
            createdAt = 1619725878925,
            updatedAt = 1619725878925
        )
        val subscription = Subscription(
            id = 12,
            planId = 12,
            customerId = customer.id,
            amount = Money(value = BigDecimal(5000), currency = Currency.NGN),
            createdAt = 1619725878925,
            updatedAt = 1619725878925
        )
        val customAmount = Money(value = BigDecimal(10), currency = Currency.NGN)

        assertThrows<InvoiceNotCreatedException> {
            invoiceService.create(customer, subscription, customAmount)
        }
    }
}
