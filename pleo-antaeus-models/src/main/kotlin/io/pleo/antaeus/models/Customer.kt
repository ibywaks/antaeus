package io.pleo.antaeus.models

data class Customer(
    val id: Int,
    val currency: Currency,
    val status: CustomerStatus,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long
)
