package io.pleo.antaeus.core.services

import com.beust.klaxon.Klaxon
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.spyk
import io.pleo.antaeus.core.exceptions.NoPaymentMethodException
import io.pleo.antaeus.core.external.payment.ChargePayload
import io.pleo.antaeus.core.external.payment.StripeService
import io.pleo.antaeus.data.AntaeusDal
import io.pleo.antaeus.models.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal

class BillingServiceTest {
    // mock dal
    private val dal = mockk<AntaeusDal> {
        val result1 = Klaxon().parse<Customer>(
            """
            {
                "id": 200,
                "currency": "NGN",
                "status": "ACTIVE",
                "stripeId": "CUS_1234",
                "defaultStripePaymentMethodId": "PM_1234",
                "createdAt": 1619725878925,
                "updatedAt": 1619725878925,
                "deletedAt": null
            }
            """.trimIndent()
        )

        val result2 = Klaxon().parse<Customer>(
            """
            {
                "id": 205,
                "currency": "NGN",
                "status": "ACTIVE",
                "stripeId": "CUS_123456",
                "defaultStripePaymentMethodId": null,
                "createdAt": 1619725878925,
                "updatedAt": 1619725878925,
                "deletedAt": null
            }
        """.trimIndent()
        )

        val result3 = Klaxon().parse<Invoice>("""
            {
                "id": 203,
                "customerId": 200,
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

        val result4 = Klaxon().parse<Invoice>("""
            {
                "id": 205,
                "customerId": 210,
                "subscriptionId": 12,
                "amount": {
                    "value": 5000,
                    "currency": "NGN"
                },
                "status": "FAILED",
                "paymentRef": null,
                "lastFailedCharge": 1619725878925,
                "numberOfFailedCharges": 2,
                "description": null,
                "chargeStartDate": 1619725878925,
                "chargeEndDate": 1619725878925,
                "createdAt": 1619725878925,
                "updatedAt": 1619725878925,
                "deletedAt": null
            }
        """)

        val result5 = Klaxon().parse<Invoice>("""
            {
                "id": 203,
                "customerId": 200,
                "subscriptionId": 12,
                "amount": {
                    "value": 5000,
                    "currency": "NGN"
                },
                "status": "FAILED",
                "paymentRef": null,
                "lastFailedCharge": 1619725878925,
                "numberOfFailedCharges": 1,
                "description": "Initial invoice charge for 11days",
                "chargeStartDate": 1619725878925,
                "chargeEndDate": 1619725878925,
                "createdAt": 1619725878925,
                "updatedAt": 1619725878925,
                "deletedAt": 1619725878955
            }
        """)

        val result6 = Klaxon().parse<Invoice>("""
            {
                "id": 205,
                "customerId": 210,
                "subscriptionId": 12,
                "amount": {
                    "value": 5000,
                    "currency": "NGN"
                },
                "status": "FAILED",
                "paymentRef": null,
                "lastFailedCharge": 1619725878925,
                "numberOfFailedCharges": 3,
                "description": null,
                "chargeStartDate": 1619725878925,
                "chargeEndDate": 1619725878925,
                "createdAt": 1619725878925,
                "updatedAt": 1619725878925,
                "deletedAt": null
            }
        """)

        val result7 = Klaxon().parse<Customer>("""
            {
                "id": 210,
                "currency": "NGN",
                "status": "INACTIVE",
                "stripeId": "CUS_1234",
                "defaultStripePaymentMethodId": "PM_1234",
                "createdAt": 1619725878925,
                "updatedAt": 1619725878925,
                "deletedAt": null
            }
        """.trimIndent())

        every { fetchCustomer(200) } returns result1
        every { fetchCustomer(205) } returns result2
        every { fetchCustomer(210) } returns result7

        every { fetchInvoices(false, null, null) } returns listOf((result3 as Invoice))
        every { fetchInvoices(false, InvoiceStatus.FAILED, null) } returns listOf((result4 as Invoice))

        every { fetchInvoice(203) } returns result5
        every { fetchInvoice(205) } returns result5

        every { updateCustomer(210, any()) } returns result7

        every { updateInvoice(203, any()) } returns result5
        every { updateInvoice(205, any()) } returns result6
    }

    private val testApiKey = "sk_test_51ImgwpBaFgF9u9b7FUhawKwVfN7yldjzfbgRS27zHkmjHK0Ioo3oPcABKvoEAcSgMvNkxesDtagQOWb1wy61V6px00AVJpf1UL"
    private val customerService = CustomerService(dal = dal)
    private val invoiceService = InvoiceService(dal = dal)

    private val stripeService = spyk(StripeService(
        apiKey = testApiKey,
        webhookSecretKey = "",
        customerService = customerService,
        invoiceService = invoiceService
    ), recordPrivateCalls = true)

    @BeforeEach
    internal fun setUp() {
        justRun { stripeService.createPaymentIntent(any()) }

        val payload1 = Klaxon().parse<ChargePayload>("""
            {
                "amount": 500000,
                "currency": "NGN",
                "customerReference": "CUS_1234",
                "paymentMethod": "PM_1234",
                "invoiceId": "203"
            }
        """.trimIndent())

        val payload2 = Klaxon().parse<ChargePayload>("""
            {
                "amount": 500000,
                "currency": "NGN",
                "customerReference": "CUS_1234",
                "paymentMethod": "PM_1234",
                "invoiceId": "205"
            }
        """.trimIndent())

        every { stripeService.charge(payload1 as ChargePayload) } returns false
        every { stripeService.charge(payload2 as ChargePayload) } returns false
    }

    private val billingService = BillingService(
        paymentProvider = stripeService,
        invoiceService = invoiceService,
        customerService = customerService
    )

    @Test
    fun `should successfully charge a single invoice`() {
        val amount = Money(
            value = BigDecimal(5000),
            currency = Currency.NGN
        )
        val invoice = Invoice(
            id = 101,
            subscriptionId = 101,
            customerId = 200,
            amount = amount,
            status = InvoiceStatus.PENDING,
            chargeStartDate = 1619725878925,
            chargeEndDate = 1619725878925,
            createdAt = 1619725878925,
            updatedAt = 1619725878925
        )

        assertDoesNotThrow {
            billingService.chargeSingleInvoice(invoice = invoice)
        }
    }

    @Test
    fun `should not charge invoice if user has no payment method`() {
        val amount = Money(
            value = BigDecimal(5000),
            currency = Currency.NGN
        )
        val invoice = Invoice(
            id = 101,
            subscriptionId = 101,
            customerId = 205,
            amount = amount,
            status = InvoiceStatus.PENDING,
            chargeStartDate = 1619725878925,
            chargeEndDate = 1619725878925,
            createdAt = 1619725878925,
            updatedAt = 1619725878925
        )

        assertThrows<NoPaymentMethodException> {
            billingService.chargeSingleInvoice(invoice)
        }
    }

    @Test
    fun `should successfully run charge for all active invoices`() {
        assertDoesNotThrow {
            billingService.chargeAllActiveInvoices()
        }
    }

    @Test
    fun `should successfully retry failed invoices`() {
        assertDoesNotThrow {
            billingService.retryFailedInvoices()
        }
    }

    @Test
    fun `should mark invoice as failed if charge fails`() {
        val invoices = billingService.chargeAllActiveInvoices()

        assertEquals(1, invoices[0].numberOfFailedCharges)
        assertEquals(InvoiceStatus.FAILED, invoices[0].status)
    }

    @Test
    fun `should make customer status inactive if failed invoice passes max threshold`() {
        billingService.retryFailedInvoices()

        val failedInvoices = invoiceService.fetchAll(false, InvoiceStatus.FAILED)
        val customer = customerService.fetch(failedInvoices[0].customerId)

        assertEquals(CustomerStatus.INACTIVE, customer.status)
    }

}
