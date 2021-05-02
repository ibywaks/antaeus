package io.pleo.antaeus.core.exceptions

import io.pleo.antaeus.models.Currency
import java.lang.Exception

class CurrencyExchangeRateNotAvailableException (currency: Currency): Exception("$currency exchange rate is unavailable")
