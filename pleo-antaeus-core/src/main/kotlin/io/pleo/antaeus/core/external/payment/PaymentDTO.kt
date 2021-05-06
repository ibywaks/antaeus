package io.pleo.antaeus.core.external.payment

import io.pleo.antaeus.models.Customer

data class PaymentSetupDTO(
    val customer: Customer
)
