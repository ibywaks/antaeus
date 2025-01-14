package io.pleo.antaeus.models

data class Subscription(
    val id: Int,
    val planId: Int,
    val customerId: Int,
    val amount: Money,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null
)
