package io.pleo.antaeus.core.helpers

import io.pleo.antaeus.models.Money
import java.math.BigDecimal

fun calculatePartialPlanAmount(amount: Money, days: Long): Money {
    //@todo use number of days in month
    val dailyValue = amount.value / BigDecimal(30)

    return Money(
        value = dailyValue * BigDecimal(days),
        currency = amount.currency
    )
}
