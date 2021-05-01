package io.pleo.antaeus.core.services

import com.beust.klaxon.Klaxon
import io.mockk.every
import io.mockk.mockk
import io.pleo.antaeus.core.exceptions.SubscriptionPlanNotCreatedException
import io.pleo.antaeus.core.exceptions.SubscriptionPlanNotFoundException
import io.pleo.antaeus.data.AntaeusDal
import io.pleo.antaeus.models.Currency
import io.pleo.antaeus.models.Money
import io.pleo.antaeus.models.SubscriptionPlan
import io.pleo.antaeus.models.SubscriptionPlanUpdateSchema
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal

class SubscriptionPlanServiceTest {
    private val dal = mockk<AntaeusDal>{
        val result1 = Klaxon().parse<SubscriptionPlan>("""
            {
                "id": 200,
                "name": "Basic plan",
                amount: {
                    "value": 50,
                    "currency": "USD"
                },
                "createdAt": 1619725878925,
                "updatedAt": 1619725878925,
                "deletedAt": null
            }
        """.trimIndent())

        val result2 = Klaxon().parse<SubscriptionPlan>("""
            {
                "id": 203,
                "name": "Standard plan",
                amount: {
                    "value": 50,
                    "currency": "USD"
                },
                "createdAt": 1619725878925,
                "updatedAt": 1619725878925,
                "deletedAt": 1619725878925
            }
        """.trimIndent())

        val result3 = Klaxon().parse<SubscriptionPlan>("""
            {
                "id": 205,
                "name": "Premium plan",
                amount: {
                    "value": 50,
                    "currency": "USD"
                },
                "createdAt": 1619725878925,
                "updatedAt": 1619725878925,
                "deletedAt": null
            }
        """.trimIndent())

        val result4 = Klaxon().parse<SubscriptionPlan>("""
            {
                "id": 205,
                "name": "Premium plan",
                amount: {
                    "value": 100,
                    "currency": "USD"
                },
                "createdAt": 1619725878925,
                "updatedAt": 1619725878925,
                "deletedAt": null
            }
        """.trimIndent())

        val result5 = Klaxon().parse<SubscriptionPlan>("""
            {
                "id": 205,
                "name": "Zaposs Basic Plan",
                amount: {
                    "value": 50,
                    "currency": "USD"
                },
                "createdAt": 1619725878925,
                "updatedAt": 1619725878925,
                "deletedAt": null
            }
        """.trimIndent())

        val result6 = Klaxon().parse<SubscriptionPlan>("""
            {
                "id": 205,
                "name": "Premium plan",
                amount: {
                    "value": 50,
                    "currency": "USD"
                },
                "createdAt": 1619725878925,
                "updatedAt": 1619725878925,
                "deletedAt": 1619725878925
            }
        """.trimIndent())

        every { fetchSubscriptionPlan(404) } returns null
        every { fetchSubscriptionPlan(200) } returns result1
        every { fetchSubscriptionPlan(203) } returns result2

        every { fetchSubscriptionPlans() } returns listOf(
            result1 as SubscriptionPlan,
            result3 as SubscriptionPlan
        )
        every { fetchSubscriptionPlans(true) } returns listOf(
            result2 as SubscriptionPlan
        )

        val planAmount1 = Money(
            value = BigDecimal(50),
            currency = Currency.USD
        )
        val planAmount2 = Money(
            value = BigDecimal(100),
            currency = Currency.USD
        )
        every { createSubscriptionPlan("Basic plan", planAmount1 ) } returns result1
        every { createSubscriptionPlan("", planAmount2 ) } returns null

        val planUpdates1 = SubscriptionPlanUpdateSchema(
            amount = planAmount2
        )
        val planUpdates2 = SubscriptionPlanUpdateSchema(
            name = "Zaposs Basic Plan"
        )
        val planUpdates3 = SubscriptionPlanUpdateSchema(
            isDeleted = true
        )

        every { updateSubscriptionPlan(205, planUpdates1) } returns result4
        every { updateSubscriptionPlan(205, planUpdates2) } returns result5
        every { updateSubscriptionPlan(205, planUpdates3) } returns result6
        every { updateSubscriptionPlan(251, planUpdates1) } returns null
    }

    private val subscriptionPlanService = SubscriptionPlanService(dal)

    @Test
    fun `will throw when plan not found`() {
        assertThrows<SubscriptionPlanNotFoundException> {
            subscriptionPlanService.fetch(404)
        }
    }

    @Test
    fun `will return a valid plan`() {
        val result =  subscriptionPlanService.fetch(200)

        assertEquals(result.id, 200)
        assertNull(result.deletedAt)
    }

    @Test
    fun `will return a soft deleted plan`() {
        val result =  subscriptionPlanService.fetch(203)

        assertEquals(result.id, 203)
        assertNotNull(result.deletedAt)
    }

    @Test
    fun `will return a list of plans`() {
        val results =  subscriptionPlanService.fetchAll(false)

        assertEquals(results.size, 2)
        assertNull(results[0].deletedAt)
    }

    @Test
    fun `will return a list of soft deleted plans`() {
        val results =  subscriptionPlanService.fetchAll(true)

        assertEquals(results.size, 1)
        assertNotNull(results[0].deletedAt)
    }

    @Test
    fun `will create a plan`() {
        val planAmount = Money(
            value = BigDecimal(50),
            currency = Currency.USD
        )
        val result =  subscriptionPlanService.create("Basic plan", planAmount)

        assertEquals(result.id, 200)
        assertEquals(result.name, "Basic plan")
        assertNull(result.deletedAt)
    }

    @Test
    fun `will throw when unable to create plan`() {
        val planAmount = Money(
            value = BigDecimal(100),
            currency = Currency.USD
        )

        assertThrows<SubscriptionPlanNotCreatedException> {
            subscriptionPlanService.create("", planAmount)
        }
    }

    @Test
    fun `will update plan amount`() {
        val planAmount = Money(
            value = BigDecimal(100),
            currency = Currency.USD
        )
        val updates = SubscriptionPlanUpdateSchema(
            amount = planAmount
        )

        val result =  subscriptionPlanService.update(200, updates)

        assertEquals(result.id, 200)
        assertEquals(result.amount.value, planAmount.value)
        assertEquals(result.amount.currency, planAmount.currency)
        assertNull(result.deletedAt)
    }

    @Test
    fun `will update plan name`() {
        val updates = SubscriptionPlanUpdateSchema(
            name = "Zaposs Basic Plan"
        )

        val result =  subscriptionPlanService.update(200, updates)

        assertEquals(result.id, 200)
        assertEquals(result.name, "Zaposs Basic Plan")
        assertEquals(result.amount.value, BigDecimal(50))
        assertEquals(result.amount.currency, Currency.USD)
        assertNull(result.deletedAt)
    }

    @Test
    fun `will update soft delete plan`() {
        val updates = SubscriptionPlanUpdateSchema(
            isDeleted = true
        )

        val result =  subscriptionPlanService.update(200, updates)

        assertEquals(result.id, 200)
        assertEquals(result.name, "Basic Plan")
        assertEquals(result.amount.value, BigDecimal(50))
        assertEquals(result.amount.currency, Currency.USD)
        assertNotNull(result.deletedAt)
    }

    @Test
    fun `will throw when cannot update plan`() {
        val planAmount = Money(
            value = BigDecimal(100),
            currency = Currency.USD
        )
        val updates = SubscriptionPlanUpdateSchema(
            amount = planAmount
        )

        assertThrows<SubscriptionPlanNotFoundException> {
            subscriptionPlanService.update(251, updates)
        }
    }

}
