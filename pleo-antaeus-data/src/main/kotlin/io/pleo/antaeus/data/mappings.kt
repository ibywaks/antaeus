/*
    Defines mappings between database rows and Kotlin objects.
    To be used by `AntaeusDal`.
 */

package io.pleo.antaeus.data

import io.pleo.antaeus.models.Currency
import io.pleo.antaeus.models.Customer
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import io.pleo.antaeus.models.CustomerStatus
import io.pleo.antaeus.models.Subscription
import io.pleo.antaeus.models.Money
import org.jetbrains.exposed.sql.ResultRow

fun ResultRow.toInvoice(): Invoice = Invoice(
    id = this[InvoiceTable.id],
    amount = Money(
        value = this[InvoiceTable.value],
        currency = Currency.valueOf(this[InvoiceTable.currency])
    ),
    status = InvoiceStatus.valueOf(this[InvoiceTable.status]),
    customerId = this[InvoiceTable.customerId],
    subscriptionId = this[InvoiceTable.subscriptionId],
    paymentRef = this[InvoiceTable.paymentRef],
    createdAt = this[InvoiceTable.createdAt],
    updatedAt = this[InvoiceTable.updatedAt],
    deletedAt = this[InvoiceTable.deletedAt]
)

fun ResultRow.toCustomer(): Customer = Customer(
    id = this[CustomerTable.id],
    currency = Currency.valueOf(this[CustomerTable.currency]),
    status = CustomerStatus.valueOf(this[CustomerTable.status]),
    createdAt = this[CustomerTable.createdAt],
    updatedAt = this[CustomerTable.updatedAt],
    deletedAt = this[CustomerTable.deletedAt]
)

fun ResultRow.toSubscription(): Subscription = Subscription(
    id = this[SubscriptionTable.id],
    amount = Money(
        value = this[SubscriptionTable.value],
        currency = Currency.valueOf(this[SubscriptionTable.currency])
    ),
    customerId = this[SubscriptionTable.customerId],
    createdAt = this[SubscriptionTable.createdAt],
    updatedAt = this[SubscriptionTable.updatedAt],
    deletedAt = this[SubscriptionTable.deletedAt]
)
