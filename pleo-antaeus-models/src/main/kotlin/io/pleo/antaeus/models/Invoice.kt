package io.pleo.antaeus.models

data class Invoice(
    val id: Int,
    val customerId: Int,
    val subscriptionId: Int,
    val amount: Money,
    val status: InvoiceStatus,
    val paymentRef: String? = null,
    val description: String? = null,
    val chargeStartDate: Long,
    val chargeEndDate: Long,
    val numberOfFailedCharges: Int? = null,
    val lastFailedCharge: Long? = null,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null
)
