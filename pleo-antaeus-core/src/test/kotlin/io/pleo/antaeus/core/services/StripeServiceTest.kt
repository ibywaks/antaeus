package io.pleo.antaeus.core.services

import io.mockk.justRun
import io.mockk.mockk
import io.mockk.spyk
import io.pleo.antaeus.core.external.payment.ChargePayload
import io.pleo.antaeus.core.external.payment.PaymentSetupDTO
import io.pleo.antaeus.core.external.payment.StripeService
import io.pleo.antaeus.data.AntaeusDal
import io.pleo.antaeus.models.*
import java.math.BigDecimal
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class StripeServiceTest {
    private val testApiKey = "sk_test_51ImgwpBaFgF9u9b7FUhawKwVfN7yldjzfbgRS27zHkmjHK0Ioo3oPcABKvoEAcSgMvNkxesDtagQOWb1wy61V6px00AVJpf1UL"

    private val stripeService = spyk(StripeService(
        testApiKey,
        ""
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
        assertNotNull(intentObj.secretKey)
    }

    @Test
    fun `successfully charges payload`() {
        val amount = Money(
            value = BigDecimal(5000),
            currency = Currency.NGN
        )

        val payload = ChargePayload(
            amount = amount.value.toLong(),
            currency = amount.currency,
            customerReference = "CUS_1234",
            paymentMethod = "PM_1234",
            invoiceId = "101"
        )

        val result = stripeService.charge(payload)

        assertEquals(result, true)
    }
}
