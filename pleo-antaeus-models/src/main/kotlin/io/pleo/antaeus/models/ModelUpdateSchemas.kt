package io.pleo.antaeus.models

data class InvoiceUpdateSchema(
    val amount: Money?,
    val paymentRef: String?,
    val status: InvoiceStatus?,
    val isDeleted: Boolean = false
)

data class SubscriptionUpdateSchema(
    val amount: Money?,
    val isDeleted: Boolean = false
)

data class CustomerUpdateSchema(
    val status: CustomerStatus?,
    val isDeleted: Boolean = false
)