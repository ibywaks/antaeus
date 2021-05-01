/*
    Implements endpoints related to invoices.
 */

package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.exceptions.InvoiceNotFoundException
import io.pleo.antaeus.core.exceptions.InvoiceNotCreatedException
import io.pleo.antaeus.data.AntaeusDal
import io.pleo.antaeus.models.Money
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.Customer
import io.pleo.antaeus.models.Subscription
import io.pleo.antaeus.models.InvoiceStatus
import io.pleo.antaeus.models.InvoiceUpdateSchema

class InvoiceService(private val dal: AntaeusDal) {
    fun fetchAll(isDeleted: Boolean = false, status: InvoiceStatus? = null): List<Invoice> {
        return dal.fetchInvoices(isDeleted, status)
    }

    fun fetch(id: Int): Invoice {
        return dal.fetchInvoice(id) ?: throw InvoiceNotFoundException(id)
    }

    fun update(id: Int, updates:InvoiceUpdateSchema): Invoice {
        return dal.updateInvoice(id, updates) ?: throw InvoiceNotFoundException(id)
    }

    fun create(customer: Customer, subscription: Subscription, customAmount: Money? = null): Invoice {
        if (customAmount != null)
            return dal.createInvoice(customAmount, customer, subscription) ?: throw InvoiceNotCreatedException()
            
        return dal.createInvoice(subscription.amount, customer, subscription) ?: throw InvoiceNotCreatedException()
    }
}
