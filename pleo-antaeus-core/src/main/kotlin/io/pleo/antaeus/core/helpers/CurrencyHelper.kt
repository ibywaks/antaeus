package io.pleo.antaeus.core.helpers

import io.pleo.antaeus.core.exceptions.CurrencyExchangeRateNotAvailableException
import io.pleo.antaeus.models.Currency
import io.pleo.antaeus.models.Money
import java.math.BigDecimal

private val baseCurrency = Currency.USD

private data class ExchangeRate(
    val currency: Currency,
    val rate: BigDecimal
)

private val availableExchangeRates = listOf<ExchangeRate>(
    ExchangeRate(Currency.NGN, BigDecimal(380.50)),
    ExchangeRate(Currency.SEK, BigDecimal(8.64)),
    ExchangeRate(Currency.DKK, BigDecimal(6.19)),
    ExchangeRate(Currency.EUR, BigDecimal(0.83)),
    ExchangeRate(Currency.GBP, BigDecimal(0.72))
)

fun convertCurrency(amount: Money, targetCurrency: Currency): BigDecimal {
    if (amount.currency == targetCurrency)
        return amount.value

    val targetRate = availableExchangeRates.find { r -> r.currency == targetCurrency } ?: throw CurrencyExchangeRateNotAvailableException(targetCurrency)

    var baseAmount = amount.value

    if (amount.currency != baseCurrency) {
        val firstRate = availableExchangeRates.find { r -> r.currency == amount.currency } ?: throw CurrencyExchangeRateNotAvailableException(targetCurrency)

        baseAmount = amount.value / firstRate.rate
    }

    return baseAmount * targetRate.rate
}
