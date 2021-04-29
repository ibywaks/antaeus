/*
    Defines database tables and their schemas.
    To be used by `AntaeusDal`.
 */

package io.pleo.antaeus.data

import org.jetbrains.exposed.sql.Table
import io.pleo.antaeus.models.CustomerStatus
import io.pleo.antaeus.models.InvoiceStatus

object InvoiceTable : Table() {
    val id = integer("id").autoIncrement().primaryKey()
    val currency = varchar("currency", 3)
    val value = decimal("value", 1000, 2)
    val customerId = reference("customer_id", CustomerTable.id)
    val subscriptionId = reference("subscription_id", SubscriptionTable.id)
    val status = varchar("status", 10).default(InvoiceStatus.PENDING.toString())
    val paymentRef = varchar("payment_ref", 20).nullable()
    val createdAt = long("created_at").clientDefault { System.currentTimeMillis() }
    val updatedAt = long("updated_at").clientDefault { System.currentTimeMillis() }
    val deletedAt = long("deleted_at").nullable()
}

object CustomerTable : Table() {
    val id = integer("id").autoIncrement().primaryKey()
    val currency = varchar("currency", 3)
    val status = varchar("status", 10).default(CustomerStatus.ACTIVE.toString())
    val createdAt = long("created_at").clientDefault { System.currentTimeMillis() }
    val updatedAt = long("updated_at").clientDefault { System.currentTimeMillis() }
    val deletedAt = long("deleted_at").nullable()
}

object SubscriptionTable : Table() {
    val id = integer("id").autoIncrement().primaryKey()
    val currency = varchar("currency", 3)
    val value = decimal("value", 1000, 2) 
    val customerId = reference("customer_id", CustomerTable.id)
    val createdAt = long("created_at").clientDefault { System.currentTimeMillis() }
    val updatedAt = long("updated_at").clientDefault { System.currentTimeMillis() }
    val deletedAt = long("deleted_at").nullable()
}
