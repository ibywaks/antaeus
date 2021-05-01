package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.exceptions.SubscriptionNotCreatedException
import io.pleo.antaeus.core.exceptions.SubscriptionNotFoundException
import io.pleo.antaeus.data.AntaeusDal
import io.pleo.antaeus.models.Customer
import io.pleo.antaeus.models.Money
import io.pleo.antaeus.models.Subscription
import io.pleo.antaeus.models.SubscriptionUpdateSchema

class SubscriptionService(private val dal: AntaeusDal) {
    fun fetch(id: Int): Subscription {
        return dal.fetchSubscription(id) ?: throw SubscriptionNotFoundException(id)
    }

    fun fetchAll(isDeleted: Boolean = false): List<Subscription> {
        return dal.fetchSubscriptions(isDeleted)
    }

    fun update(id: Int, updates: SubscriptionUpdateSchema): Subscription {
        return dal.updateSubscription(id, updates) ?: throw SubscriptionNotFoundException(id)
    }

    fun create(customer: Customer, amount: Money): Subscription {
        return dal.createSubscription(amount, customer) ?: throw SubscriptionNotCreatedException()
    }
}
