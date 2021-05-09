package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.exceptions.SubscriptionNotCreatedException
import io.pleo.antaeus.core.exceptions.SubscriptionNotFoundException
import io.pleo.antaeus.core.helpers.convertCurrency
import io.pleo.antaeus.data.AntaeusDal
import io.pleo.antaeus.models.Customer
import io.pleo.antaeus.models.Money
import io.pleo.antaeus.models.Subscription
import io.pleo.antaeus.models.SubscriptionPlan
import io.pleo.antaeus.models.SubscriptionUpdateSchema

class SubscriptionService(private val dal: AntaeusDal) {
    fun fetch(id: Int): Subscription {
        return dal.fetchSubscription(id) ?: throw SubscriptionNotFoundException(id)
    }

    fun fetchAll(isDeleted: Boolean = false, customer: Customer? = null, plan: SubscriptionPlan? = null): List<Subscription> {
        return dal.fetchSubscriptions(isDeleted, customer, plan)
    }

    fun update(id: Int, updates: SubscriptionUpdateSchema): Subscription {
        return dal.updateSubscription(id, updates) ?: throw SubscriptionNotFoundException(id)
    }

    fun create(plan: SubscriptionPlan, customer: Customer): Subscription {
        val amount = Money(
            currency = customer.currency,
            value = convertCurrency(plan.amount, customer.currency)
        )
        return dal.createSubscription(plan, amount, customer) ?: throw SubscriptionNotCreatedException()
    }
}
