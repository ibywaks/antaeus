package io.pleo.antaeus.core.exceptions

class NoCustomerSubscriptionException(customerId: Int): Exception("Customer $customerId has no active subscriptions")
