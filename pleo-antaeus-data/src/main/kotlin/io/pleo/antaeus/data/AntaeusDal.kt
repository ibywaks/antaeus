/*
    Implements the data access layer (DAL).
    The data access layer generates and executes requests to the database.

    See the `mappings` module for the conversions between database rows and Kotlin objects.
 */

package io.pleo.antaeus.data

import io.pleo.antaeus.models.*
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.update
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.andWhere
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

    fun fetchInvoices(isDeleted: Boolean = false, status: InvoiceStatus?): List<Invoice> {
        return transaction(db) {
            val query = InvoiceTable.selectAll()

            if (status != null)
                query.andWhere{ InvoiceTable.status.eq(status.toString()) }

            if (!isDeleted) {
                query.andWhere{ InvoiceTable.deletedAt.isNull() }
            }

            val results = query.map { it.toInvoice() }

            results
        }
    }

    fun updateInvoice(id: Int, updates: InvoiceUpdateSchema): Invoice? {
        transaction(db) {
            // Update the invoice and return its id
            InvoiceTable.update({ InvoiceTable.id.eq(id) }) {
                val amount = updates.amount
                if (amount != null) {
                    it[value] = amount.value
                    it[currency] = amount.currency.toString()
                }

                if (updates.status != null)
                    it[status] = updates.status.toString()

                if (updates.paymentRef != null)
                    it[paymentRef] = updates.paymentRef

                if (updates.isDeleted)
                    it[deletedAt] = Date().getTime()
            }
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

    fun fetchSubscriptions(isDeleted: Boolean = false, customer: Customer? = null): List<Subscription> {
        return transaction(db) {
            val query = SubscriptionTable.selectAll()

            if (!isDeleted) {
                query.andWhere{ SubscriptionTable.deletedAt.isNull() }
            }

            if (customer != null) {
                query.andWhere { SubscriptionTable.customerId.eq(customer.id) }
            }

            val results = query.map{ it.toSubscription() }

            results
        }
    }

    fun updateSubscription(id: Int, updates: SubscriptionUpdateSchema): Subscription? {
        transaction(db) {
            // Update subscription and return its id
            SubscriptionTable.update({ SubscriptionTable.id eq id }) {
                val amount = updates.amount
                if (amount != null) {
                    it[value] = amount.value
                    it[currency] = amount.currency.toString()
                }

                if (updates.isDeleted)
                    it[deletedAt] = Date().getTime()
            }
        }

        return fetchSubscription(id)
    }

    fun createSubscription(plan: SubscriptionPlan, amount: Money, customer: Customer): Subscription? {
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

    fun fetchCustomers(isDeleted: Boolean = false, status: CustomerStatus?): List<Customer> {
        return transaction(db) {
            val query = CustomerTable.selectAll()

            if (status != null)
                query.andWhere{ CustomerTable.status eq status.toString() }

            if (!isDeleted) {
                query.andWhere{ CustomerTable.deletedAt.isNull() }
            }
                
            val results = query.map { it.toCustomer() }

            results
        }
    }

    fun updateCustomer(id: Int, updates: CustomerUpdateSchema): Customer? {
        transaction(db) {
            // Update Customer table entry and return updated row's id
            CustomerTable.update({ CustomerTable.id.eq(id) }) {
                if (updates.status != null)
                    it[status] = updates.status.toString()

                if (updates.isDeleted)
                    it[deletedAt] = Date().getTime()
            }
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

    fun fetchSubscriptionPlan(id: Int): SubscriptionPlan? {
        return transaction(db) {
            SubscriptionPlanTable
                .select { SubscriptionPlanTable.id.eq(id) }
                .firstOrNull()
                ?.toSubscriptionPlan()
        }
    }

    fun createSubscriptionPlan(name: String, amount: Money): SubscriptionPlan? {
        val id = transaction(db) {
            SubscriptionPlanTable.insert {
                it[this.name] = name
                it[this.value] = amount.value
                it[this.currency] = amount.currency.toString()
            } get SubscriptionPlanTable.id
        }

        fetchSubscriptionPlan(id)
    }
}
