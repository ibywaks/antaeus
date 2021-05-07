/*
    Defines mappings between database rows and Kotlin objects.
    To be used by `AntaeusDal`.
 */

package io.pleo.antaeus.data

/* ktlint-disable no-wildcard-imports */
import io.pleo.antaeus.models.*
import org.jetbrains.exposed.sql.ResultRow
/* ktlint-enable no-wildcard-imports */

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
    description = this[InvoiceTable.description],
    chargeStartDate = this[InvoiceTable.chargeStartDate],
    chargeEndDate = this[InvoiceTable.chargeEndDate],
    lastFailedCharge = this[InvoiceTable.lastFailedCharge],
    numberOfFailedCharges = this[InvoiceTable.numberOfFailedCharges],
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
    planId = this[SubscriptionTable.planId],
    customerId = this[SubscriptionTable.customerId],
    createdAt = this[SubscriptionTable.createdAt],
    updatedAt = this[SubscriptionTable.updatedAt],
    deletedAt = this[SubscriptionTable.deletedAt]
)

fun ResultRow.toSubscriptionPlan(): SubscriptionPlan = SubscriptionPlan(
    id = this[SubscriptionPlanTable.id],
    amount = Money(
        value = this[SubscriptionPlanTable.value],
        currency = Currency.valueOf(this[SubscriptionPlanTable.currency])
    ),
    name = this[SubscriptionPlanTable.name],
    createdAt = this[SubscriptionPlanTable.createdAt],
    updatedAt = this[SubscriptionPlanTable.updatedAt],
    deletedAt = this[SubscriptionPlanTable.deletedAt]
)
