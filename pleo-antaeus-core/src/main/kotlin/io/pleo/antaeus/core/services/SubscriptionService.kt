package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.exceptions.SubscriptionNotCreatedException
import io.pleo.antaeus.core.exceptions.SubscriptionNotFoundException
import io.pleo.antaeus.data.AntaeusDal
import io.pleo.antaeus.models.*

class SubscriptionService(private val dal: AntaeusDal) {
    fun fetch(id: Int): Subscription {
        return dal.fetchSubscription(id) ?: throw SubscriptionNotFoundException(id)
    }

    fun fetchAll(isDeleted: Boolean = false, customer: Customer? = null): List<Subscription> {
        return dal.fetchSubscriptions(isDeleted, customer)
    }

    fun update(id: Int, updates: SubscriptionUpdateSchema): Subscription {
        return dal.updateSubscription(id, updates) ?: throw SubscriptionNotFoundException(id)
    }

    fun create(plan: SubscriptionPlan, customer: Customer, amount: Money): Subscription {
        return dal.createSubscription(plan, amount, customer) ?: throw SubscriptionNotCreatedException()
    }
}
