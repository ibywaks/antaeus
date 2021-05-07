package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.external.payment.StripeService
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import kotlin.concurrent.fixedRateTimer

class CronScheduleService(
    private val invoiceService: InvoiceService,
    private val stripeService: StripeService
): Runnable {
    override fun run() {
        val start = LocalDate.now().with(TemporalAdjusters.firstDayOfMonth())
        val startDate = SimpleDateFormat("yyyy-mm-dd").parse(start.toString())

        // run schedule monthly starting from the first day of the month
        fixedRateTimer("charge-invoices", true,  startDate, (2.628e+9).toLong()) {
            val invoices = invoiceService.fetchAll()

            invoices.forEach { invoice ->
                stripeService.charge(invoice)
            }
        }

    }
}
