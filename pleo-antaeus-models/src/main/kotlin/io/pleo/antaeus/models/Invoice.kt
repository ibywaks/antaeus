package io.pleo.antaeus.models

data class Invoice(
    val id: Int,
    val customerId: Int,
    val subscriptionId: Int,
    val amount: Money,
    val status: InvoiceStatus,
    val paymentRef: String?,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long?
)
