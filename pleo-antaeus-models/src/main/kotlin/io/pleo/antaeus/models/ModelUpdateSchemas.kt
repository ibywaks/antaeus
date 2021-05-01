package io.pleo.antaeus.models

data class InvoiceUpdateSchema(
    val amount: Money? = null,
    val paymentRef: String? = null,
    val status: InvoiceStatus? = null,
    val isDeleted: Boolean = false
)

data class SubscriptionUpdateSchema(
    val amount: Money? = null,
    val isDeleted: Boolean = false
)

data class CustomerUpdateSchema(
    val status: CustomerStatus? = null,
    val isDeleted: Boolean = false
)