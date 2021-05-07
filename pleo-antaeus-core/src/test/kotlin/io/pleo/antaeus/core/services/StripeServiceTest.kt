package io.pleo.antaeus.core.services

import com.beust.klaxon.Klaxon
import io.mockk.justRun
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.pleo.antaeus.core.external.payment.PaymentSetupDTO
import io.pleo.antaeus.data.AntaeusDal
import io.pleo.antaeus.core.external.payment.StripeService
import io.pleo.antaeus.models.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class StripeServiceTest {
    // mock dal
    private val dal = mockk<AntaeusDal>{
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

        every { fetchCustomer(200) } returns result1
        every { fetchCustomer(205) } returns result2
    }

    private val testApiKey = "sk_test_51ImgwpBaFgF9u9b7FUhawKwVfN7yldjzfbgRS27zHkmjHK0Ioo3oPcABKvoEAcSgMvNkxesDtagQOWb1wy61V6px00AVJpf1UL"
    private val customerService = CustomerService(dal = dal)
    private val invoiceService = InvoiceService(dal = dal)

    private val stripeService = spyk(StripeService(
        testApiKey,
        "",
        customerService,
        invoiceService
    ), recordPrivateCalls = true)

    @BeforeEach
    internal fun setUp() {
        justRun { stripeService.createPaymentIntent(any()) }
    }

    @Test
    fun `should return a valid payment setup intent`() {
        val customer = Customer(
            id = 200,
            currency = Currency.NGN,
            createdAt = 1619725878925,
            updatedAt = 1619725878925
        )
        val setupData = PaymentSetupDTO(customer)

        val intentObj = stripeService.initPaymentSetup(setupData)

        assertNotNull(intentObj)
        assertNotNull(intentObj?.clientSecret)
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

        val result = stripeService.charge(invoice)

        assertEquals( false, result)
    }

    @Test
    fun `successfully charges invoice`() {
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

        val result = stripeService.charge(invoice)

        assertEquals(result, true)
    }

}
