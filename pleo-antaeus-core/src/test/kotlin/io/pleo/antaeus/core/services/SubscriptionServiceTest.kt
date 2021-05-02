package io.pleo.antaeus.core.services

import com.beust.klaxon.Klaxon
import io.mockk.every
import io.mockk.mockk
import io.pleo.antaeus.core.exceptions.SubscriptionNotCreatedException
import io.pleo.antaeus.core.exceptions.SubscriptionNotFoundException
import io.pleo.antaeus.data.AntaeusDal
import io.pleo.antaeus.models.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal

class SubscriptionServiceTest {
    private val dal = mockk<AntaeusDal>{
        val result1 = Klaxon().parse<Subscription>("""
            {
                "id": 200,
                "customerId": 12,
                "planId": 12,
                "amount": {
                    "value": 5000,
                    "currency": "NGN"
                },
                "createdAt": 1619725878925,
                "updatedAt": 1619725878925,
                "deletedAt": null
            }
        """.trimIndent())

        val result2 = Klaxon().parse<Subscription>("""
            {
                "id": 203,
                "customerId": 12,
                "planId": 12,
                "amount": {
                    "value": 5000,
                    "currency": "NGN"
                },
                "createdAt": 1619725878925,
                "updatedAt": 1619725878925,
                "deletedAt": 1619725878925
            }
        """.trimIndent())

        val subscriptionUpdate1 = SubscriptionUpdateSchema(
            amount = Money( value = BigDecimal(10000), currency = Currency.NGN)
        )
        val subscriptionUpdate2 = SubscriptionUpdateSchema(
            isDeleted = true
        )
        val result3 = Klaxon().parse<Subscription>("""
            {
                "id": 200,
                "customerId": 12,
                "planId": 12,
                "amount": {
                    "value": 10000,
                    "currency": "NGN"
                },
                "createdAt": 1619725878925,
                "updatedAt": 1619725878925,
                "deletedAt": null
            }
        """.trimIndent())

        val result4 = Klaxon().parse<Subscription>("""
            {
                "id": 200,
                "customerId": 12,
                "planId": 12,
                "amount": {
                    "value": 10000,
                    "currency": "NGN"
                },
                "createdAt": 1619725878925,
                "updatedAt": 1619725878925,
                "deletedAt": 1619725878925
            }
        """.trimIndent())

        val amount1 = Money(
            value = BigDecimal(5000),
            currency = Currency.NGN
        )
        val amount2 = Money(
            value = BigDecimal(15000),
            currency = Currency.NGN
        )
        val customer1 = Customer(
            id = 12,
            currency = Currency.NGN,
            createdAt = 1619725878925,
            updatedAt = 1619725878925
        )

        val subscriptionPlan = SubscriptionPlan(
            id = 12,
            name = "Basic plan",
            amount = Money(value = BigDecimal(5000), currency = Currency.NGN),
            createdAt = 1619725878925,
            updatedAt = 1619725878925
        )

        val subscriptionPlan2 = SubscriptionPlan(
            id = 12,
            name = "Basic plan",
            amount = Money(value = BigDecimal(15000), currency = Currency.NGN),
            createdAt = 1619725878925,
            updatedAt = 1619725878925
        )

        every { fetchSubscription(404) } returns null
        every { fetchSubscription(200) } returns result1
        every { fetchSubscription(203) } returns result2

        every { fetchSubscriptions(false) } returns listOf(result1 as Subscription)
        every { fetchSubscriptions(true) } returns listOf(result2 as Subscription)

        every { updateSubscription(200, subscriptionUpdate1) } returns result3
        every { updateSubscription(200, subscriptionUpdate2) } returns result4
        every { updateSubscription(251, subscriptionUpdate1) } returns null

        every { createSubscription(subscriptionPlan, amount1, customer1) } returns result1
        every { createSubscription(subscriptionPlan2, amount2, customer1) } returns null
    }

    private val subscriptionService = SubscriptionService(dal = dal)

    @Test
    fun `will throw when invoice not found`() {
        assertThrows<SubscriptionNotFoundException> {
            subscriptionService.fetch(404)
        }
    }

    @Test
    fun `will return a subscription`() {
        val result = subscriptionService.fetch(200)

        assertEquals(result.id, 200)
        assertNull(result.deletedAt)
    }

    @Test
    fun `will return a soft deleted subscription`() {
        val result = subscriptionService.fetch(203)

        assertEquals(result.id, 203)
        assertNotNull(result.deletedAt)
    }

    @Test
    fun `will return a list of undeleted subscriptions`() {
        val results = subscriptionService.fetchAll()

        assertEquals(results.size, 1)
        assertNull(results[0].deletedAt)
    }

    @Test
    fun `will return a list of deleted subscriptions`() {
        val results = subscriptionService.fetchAll(true)

        assertEquals(results.size, 1)
        assertNotNull(results[0].deletedAt)
    }

    @Test
    fun `will create a subscription for customer`() {
        val subscriptionPlan = SubscriptionPlan(
            id = 12,
            name = "Basic plan",
            amount = Money(value = BigDecimal(5000), currency = Currency.NGN),
            createdAt = 1619725878925,
            updatedAt = 1619725878925
        )

        val customer = Customer(
            id = 12,
            currency = Currency.NGN,
            createdAt = 1619725878925,
            updatedAt = 1619725878925
        )

        val result = subscriptionService.create(subscriptionPlan, customer)

        assertEquals(result.customerId, customer.id)
        assertEquals(result.amount.value, subscriptionPlan.amount.value)
        assertEquals(result.amount.currency, subscriptionPlan.amount.currency)
        assertNull(result.deletedAt)
    }

    @Test
    fun `will throw when unable to create subscription`() {
        val subscriptionPlan = SubscriptionPlan(
            id = 12,
            name = "Basic plan",
            amount = Money(value = BigDecimal(15000), currency = Currency.NGN),
            createdAt = 1619725878925,
            updatedAt = 1619725878925
        )

        val customer = Customer(
            id = 12,
            currency = Currency.NGN,
            createdAt = 1619725878925,
            updatedAt = 1619725878925
        )

        assertThrows<SubscriptionNotCreatedException> {
            subscriptionService.create(subscriptionPlan, customer)
        }
    }
}
