package io.pleo.antaeus.models

data class SubscriptionPlan(
    val id: Int,
    val name: String,
    val amount: Money,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null
)
