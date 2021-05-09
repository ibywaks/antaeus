package io.pleo.antaeus.core.exceptions

class NoPaymentMethodException(val customerId: Int) : Exception("No payment method found for Customer $customerId")
