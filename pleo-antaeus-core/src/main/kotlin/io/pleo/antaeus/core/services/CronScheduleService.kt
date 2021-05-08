package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.external.payment.StripeService
import io.pleo.antaeus.models.CustomerStatus
import io.pleo.antaeus.models.CustomerUpdateSchema
import io.pleo.antaeus.models.InvoiceStatus
import io.pleo.antaeus.models.InvoiceUpdateSchema
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.temporal.TemporalAdjuster
import java.time.temporal.TemporalAdjusters
import java.util.*
import kotlin.concurrent.fixedRateTimer

class CronScheduleService(
    private val invoiceService: InvoiceService,
    private val customerService: CustomerService,
    private val stripeService: StripeService
): Runnable {
    override fun run() {
        val start = LocalDate.now().with(TemporalAdjusters.firstDayOfMonth())
        val startDate = SimpleDateFormat("yyyy-mm-dd").parse(start.toString())

        // run schedule monthly starting from the first day of the month
        fixedRateTimer("charge-invoices", true,  startDate, (2.628e+9).toLong()) {
            val invoices = invoiceService.fetchAll()

            invoices.forEach { invoice ->
                val chargeExecuted = stripeService.charge(invoice)

                if (chargeExecuted) return@forEach

                val update = InvoiceUpdateSchema(
                    status = InvoiceStatus.FAILED,
                    numberOfFailedCharges = (invoice.numberOfFailedCharges ?: 0) + 1,
                    lastFailedCharge = Date().time
                )
                invoiceService.update(invoice.id, update)
            }
        }

        // run schedule to retry failed invoice charges
        fixedRateTimer("charge-invoices", true, 0.toLong(), (8.64e+7).toLong()) {
            val failedInvoices = invoiceService.fetchAll(false, InvoiceStatus.FAILED)

            failedInvoices.forEach { invoice ->
                val chargeExecuted = stripeService.charge(invoice)

                if (chargeExecuted) return@forEach

                val update = InvoiceUpdateSchema(
                    numberOfFailedCharges = (invoice.numberOfFailedCharges ?: 0) + 1,
                    lastFailedCharge = Date().time
                )

                val updatedInvoice = invoiceService.update(invoice.id, update)

                if (updatedInvoice.numberOfFailedCharges!! < invoiceService.MAX_CHARGE_RETRIES) return@forEach

                // if exceeds retries disable customer
                customerService.update(invoice.customerId, CustomerUpdateSchema(CustomerStatus.INACTIVE))
            }
        }


    }
}
