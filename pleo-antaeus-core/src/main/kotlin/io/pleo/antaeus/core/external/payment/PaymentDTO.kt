package io.pleo.antaeus.core.external.payment

import io.pleo.antaeus.models.Customer
import io.pleo.antaeus.models.Money

data class PaymentSetupDTO(
    val customer: Customer,
    val amount: Money
)
