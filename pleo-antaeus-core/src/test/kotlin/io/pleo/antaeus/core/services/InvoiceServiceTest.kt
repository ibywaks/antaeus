package io.pleo.antaeus.core.services

import io.mockk.every
import io.mockk.mockk
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import io.pleo.antaeus.core.exceptions.InvoiceNotFoundException
import io.pleo.antaeus.data.AntaeusDal
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertNotNull
import com.beust.klaxon.Klaxon


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
                "createdAt": 1619725878925,
                "updatedAt": 1619725878925,
                "deletedAt": null
            }
        """)

        every { fetchInvoice(404) } returns null
        every { fetchInvoice(200) } returns result1
        every { fetchInvoice(203) } returns result2
        every { fetchInvoice(205) } returns result3

        every { fetchInvoices() } returns listOf(result1, result3)
        every { fetchInvoices(true) } returns listOf(result2)
        every { fetchInvoices(false, InvoiceStatus.PAID) } returns listOf(result3)

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
}
