package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.exceptions.SubscriptionNotCreatedException
import io.pleo.antaeus.core.exceptions.SubscriptionPlanNotFoundException
import io.pleo.antaeus.data.AntaeusDal
import io.pleo.antaeus.models.Money
import io.pleo.antaeus.models.SubscriptionPlan
import io.pleo.antaeus.models.SubscriptionPlanUpdateSchema

class SubscriptionPlanService(private val dal: AntaeusDal) {
    fun create(name: String, amount: Money): SubscriptionPlan {
        return dal.createSubscriptionPlan(name, amount) ?: throw SubscriptionNotCreatedException()
    }

    fun fetch(id: Int): SubscriptionPlan {
        return dal.fetchSubscriptionPlan(id) ?: throw SubscriptionPlanNotFoundException(id)
    }

    fun fetchAll(isDeleted: Boolean = false): List<SubscriptionPlan> {
        return dal.fetchSubscriptionPlans(isDeleted)
    }

    fun update(id: Int, updates: SubscriptionPlanUpdateSchema): SubscriptionPlan {
        return dal.updateSubscriptionPlan(id, updates) ?: throw SubscriptionPlanNotFoundException(id)
    }
}
