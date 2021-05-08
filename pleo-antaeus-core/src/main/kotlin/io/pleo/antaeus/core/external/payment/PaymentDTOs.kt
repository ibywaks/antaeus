package io.pleo.antaeus.core.external.payment

import io.pleo.antaeus.models.Currency
import io.pleo.antaeus.models.Customer

data class PaymentSetupDTO(
    val customer: Customer
)

data class PaymentSetupObject(
    val reference: String,
    val secretKey: String? = null
)

data class ChargePayload(
    val amount: Long,
    val currency: Currency,
    val customerReference: String,
    val paymentMethod: String,
    val invoiceId: String
)
