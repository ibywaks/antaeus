package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.core.external.payment.ChargePayload
import io.pleo.antaeus.core.external.payment.PaymentSetupDTO
import io.pleo.antaeus.core.external.payment.PaymentSetupObject
import io.pleo.antaeus.models.*
import java.math.BigDecimal
import java.util.*

class BillingService(
    private val paymentProvider: PaymentProvider,
    private val invoiceService: InvoiceService,
    private val customerService: CustomerService
) {
    fun initPaymentMethod(data: PaymentSetupDTO): PaymentSetupObject {
        return paymentProvider.initPaymentSetup(data)
    }

    private fun chargeInvoice(invoice: Invoice): Invoice {
        val customerId = invoice.customerId
        val customer = customerService.fetch(customerId)

        if (customer.defaultStripePaymentMethodId == null) {
            // trigger some event to notify customer to setup payment
            // throw a custom exception
             throw Exception("no available payment method")
        }

        val invoiceAmount = invoice.amount.value * BigDecimal(100)
        val payload = ChargePayload(
            amount = invoiceAmount.toLong(),
            currency = invoice.amount.currency,
            customerReference = customer.stripeId.toString(),
            paymentMethod = customer.defaultStripePaymentMethodId!!,
            invoiceId = invoice.id.toString()
        )

        val chargeExecuted = paymentProvider.charge(payload)

        if (chargeExecuted) return invoice

        val update = InvoiceUpdateSchema(
            status = InvoiceStatus.FAILED,
            numberOfFailedCharges = (invoice.numberOfFailedCharges ?: 0) + 1,
            lastFailedCharge = Date().time
        )

        return invoiceService.update(invoice.id, update)
    }

    fun chargeAllActiveInvoices() {
        val invoices = invoiceService.fetchAll()

        invoices.forEach { invoice ->
            chargeInvoice(invoice)
        }
    }

    fun retryFailedInvoices() {
        val failedInvoices = invoiceService.fetchAll(false, InvoiceStatus.FAILED)

        failedInvoices.forEach { invoice ->
            val updatedInvoice = chargeInvoice(invoice)

            // if exceeds retries disable customer
            if (updatedInvoice.numberOfFailedCharges!! >= invoiceService.MAX_CHARGE_RETRIES)
                customerService.update(invoice.customerId, CustomerUpdateSchema(CustomerStatus.INACTIVE))
        }
    }
}
