package io.pleo.antaeus.models

data class InvoiceUpdateSchema(
    val amount: Money?,
    val paymentRef: String?,
    val status: InvoiceStatus?,
    val isDeleted: Bool
)

data class SubscriptionUpdateSchema(
    val amount: Money?,
    val isDeleted: Bool?
)

data class CustomerUpdateSchema(
    val status: CustomerStatus?,
    val isDeleted: Bool?
)