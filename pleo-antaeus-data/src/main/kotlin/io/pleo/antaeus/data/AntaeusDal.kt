/*
    Implements the data access layer (DAL).
    The data access layer generates and executes requests to the database.

    See the `mappings` module for the conversions between database rows and Kotlin objects.
 */

package io.pleo.antaeus.data

import io.pleo.antaeus.models.Subscription
import io.pleo.antaeus.models.Currency
import io.pleo.antaeus.models.Customer
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import io.pleo.antaeus.models.CustomerStatus
import io.pleo.antaeus.models.Money
import io.pleo.antaeus.models.CustomerUpdateSchema
import io.pleo.antaeus.models.InvoiceUpdateSchema
import io.pleo.antaeus.models.SubscriptionUpdateSchema
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.Date

class AntaeusDal(private val db: Database) {
    // Invoice CRUD Methods
    fun fetchInvoice(id: Int): Invoice? {
        // transaction(db) runs the internal query as a new database transaction.
        return transaction(db) {
            // Returns the first invoice with matching id.
            InvoiceTable
                .select { InvoiceTable.id.eq(id) }
                .firstOrNull()
                ?.toInvoice()
        }
    }

    fun fetchInvoices(isDeleted: Bool = false, status: InvoiceStatus?): List<Invoice> {
        return transaction(db) {
            val query = InvoiceTable.selectAll()

            if (status)
                query.andWhere{ InvoiceTable.status eq status.toString() }

            if (!isDeleted) {
                query.andWhere{ InvoiceTable.deletedAt.isNull() }
            }

            val results = query.map { it.toInvoice() }

            results
        }
    }

    fun updateInvoice(id: Int, updates: InvoiceUpdateSchema): Invoice? {
        val id = transaction(db) {
            // Update the invoice and return its id
            InvoiceTable.update({ InvoiceTable.id eq id }) {
                if (update.amount) {
                    it[value] = update.amount.value
                    it[currency] = update.amount.currency.toString()
                }

                if (updates.status)
                    it[status] = updates.status.toString()

                if (updates.paymentRef)
                    it[paymentRef] = updates.paymentRef

                if (updates.isDeleted)
                    it[deletedAt] = Date().getTime()
            } get InvoiceTable.id
        }

        return fetchInvoice(id)
    }

    fun createInvoice(amount: Money, customer: Customer, subscription: Subscription, status: InvoiceStatus = InvoiceStatus.PENDING): Invoice? {
        val id = transaction(db) {
            // Insert the invoice and returns its new id.
            InvoiceTable
                .insert {
                    it[this.value] = amount.value
                    it[this.currency] = amount.currency.toString()
                    it[this.status] = status.toString()
                    it[this.customerId] = customer.id
                    it[this.subscriptionId] = subscription.id
                } get InvoiceTable.id
        }

        return fetchInvoice(id)
    }
    // End

    // Subscription CRUD Methods
    fun fetchSubscription(id: Int): Subscription? {
        // transaction(db) runs the internal query as a new database transaction.
        return transaction(db) {
            // Returns the first invoice with matching id.
            SubscriptionTable
                .select { SubscriptionTable.id.eq(id) }
                .firstOrNull()
                ?.toSubscription()
        }
    }

    fun fetchSubscriptions(isDeleted: Bool = false): List<Subscription> {
        return transaction(db) {
            val query = SubscriptionTable.selectAll()

            if (!isDeleted) {
                query.andWhere{ SubscriptionTable.deletedAt.isNull() }
            }

            val results = query.map{ it.toSubscription() }

            results
        }
    }

    fun updateSubscription(id: Int, updates: SubscriptionUpdateSchema): Subscription {
        val id = transaction(db) {
            // Update subscription and return its id
            SubscriptionTable.update({ SubscriptionTable.id eq id }) {
                if (updates.amount) {
                    it[value] = updates.amount.value
                    it[currency] = updates.amount.currency.toString()
                }

                if (updates.isDeleted)
                    it[deletedAt] = Date().getTime()
            } get SubscriptionTable.id
        }

        return fetchSubscription(id)
    }

    fun createSubscription(amount: Money, customer: Customer): Subscription? {
        val id = transaction(db) {
            // Insert the subscription and return its new id.
            SubscriptionTable
                .insert {
                    it[this.value] = amount.value
                    it[this.currency] = amount.currency.toString()
                    it[this.customerId] = customer.id
                } get SubscriptionTable.id
        }

        return fetchSubscription(id)
    }
    // END

    // Customer CRUD Methods
    fun fetchCustomer(id: Int): Customer? {
        return transaction(db) {
            CustomerTable
                .select { CustomerTable.id.eq(id) }
                .firstOrNull()
                ?.toCustomer()
        }
    }

    fun fetchCustomers(isDeleted: Bool = false, status: CustomerStatus?): List<Customer> {
        return transaction(db) {
            val query = CustomerTable.selectAll()

            if (status)
                query.andWhere{ CustomerTable.status eq status.toString() }

            if (!isDeleted) {
                query.andWhere{ CustomerTable.deletedAt.isNull() }
            }
                
            val results = query.map { it.toCustomer() }

            results
        }
    }

    fun updateCustomer(id: Int, updates: CustomerUpdateSchema): Customer {
        val id = transaction(db) {
            // Update Customer table entry and return updated row's id
            CustomerTable.update({ CustomerTable.id.eq(id) }) {
                if (updates.status)
                    it[status] = updates.status.toString()

                if (update.isDeleted)
                    it[deletedAt] = Date().getTime()
            } get CustomerTable.id
        }

        return fetchCustomer(id)
    }

    fun createCustomer(currency: Currency): Customer? {
        val id = transaction(db) {
            // Insert the customer and return its new id.
            CustomerTable.insert {
                it[this.currency] = currency.toString()
            } get CustomerTable.id
        }

        return fetchCustomer(id)
    }
}
