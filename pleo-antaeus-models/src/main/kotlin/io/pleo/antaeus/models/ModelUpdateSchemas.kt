package io.pleo.antaeus.models

data class InvoiceUpdateSchema(
    val amount: Money? = null,
    val paymentRef: String? = null,
    val status: InvoiceStatus? = null,
    val isDeleted: Boolean = false,
    val startDate: Long? = null,
    val endDate: Long? = null
)

data class SubscriptionUpdateSchema(
    val amount: Money? = null,
    val isDeleted: Boolean = false,
    val plan: SubscriptionPlan? = null
)

data class CustomerUpdateSchema(
    val status: CustomerStatus? = null,
    val stripeId: String? = null,
    val defaultStripePaymentMethodId: String? = null,
    val isDeleted: Boolean = false
)

data class SubscriptionPlanUpdateSchema(
    val name: String? = null,
    val amount: Money? = null,
    val isDeleted: Boolean = false
)
