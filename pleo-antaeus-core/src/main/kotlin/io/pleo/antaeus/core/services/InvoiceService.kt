/*
    Implements endpoints related to invoices.
 */

package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.exceptions.InvoiceNotCreatedException
import io.pleo.antaeus.core.exceptions.InvoiceNotFoundException
import io.pleo.antaeus.core.helpers.calculatePartialPlanAmount
import io.pleo.antaeus.data.AntaeusDal
import io.pleo.antaeus.models.Customer
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import io.pleo.antaeus.models.InvoiceUpdateSchema
import io.pleo.antaeus.models.Money
import io.pleo.antaeus.models.Subscription
import java.util.concurrent.TimeUnit

class InvoiceService(private val dal: AntaeusDal) {
    val MAX_CHARGE_RETRIES = 3
    fun fetchAll(isDeleted: Boolean = false, status: InvoiceStatus? = null, customer: Customer? = null): List<Invoice> {
        return dal.fetchInvoices(isDeleted, status, customer)
    }

    fun fetch(id: Int): Invoice {
        return dal.fetchInvoice(id) ?: throw InvoiceNotFoundException(id)
    }

    fun update(id: Int, updates: InvoiceUpdateSchema): Invoice {
        return dal.updateInvoice(id, updates) ?: throw InvoiceNotFoundException(id)
    }

    fun create(
        customer: Customer,
        subscription: Subscription,
        customAmount: Money? = null,
        startDate: Long? = null,
        endDate: Long? = null
    ): Invoice {
        if (customAmount != null)
            return dal.createInvoice(customAmount, customer, subscription) ?: throw InvoiceNotCreatedException()

        if (startDate != null && endDate != null && startDate <= endDate) {
            val diff = endDate - startDate
            val days = TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS)

            var description = "Partial subscription charge for $days day"
            if (days > 1) description += "s"

            val invoiceAmount = calculatePartialPlanAmount(subscription.amount, days)
            return dal.createInvoice(invoiceAmount, customer, subscription, description, startDate, endDate) ?: throw InvoiceNotCreatedException()
        }

        return dal.createInvoice(subscription.amount, customer, subscription) ?: throw InvoiceNotCreatedException()
    }
}
