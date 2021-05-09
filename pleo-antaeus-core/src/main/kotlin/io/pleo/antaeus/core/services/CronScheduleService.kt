package io.pleo.antaeus.core.services

import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import java.util.*
import kotlin.concurrent.fixedRateTimer

class CronScheduleService(
    private val billingService: BillingService
) : Runnable {
    override fun run() {
        val start = LocalDate.now().with(TemporalAdjusters.firstDayOfMonth())
        val startDate = SimpleDateFormat("yyyy-mm-dd").parse(start.toString())

        // run schedule monthly starting from the first day of the month
        fixedRateTimer("charge-invoices", true, startDate, (2.628e+9).toLong()) {
            billingService.chargeAllActiveInvoices()
        }

        // run schedule to retry failed invoice charges
        fixedRateTimer("charge-invoices", true, 0.toLong(), (8.64e+7).toLong()) {
            billingService.retryFailedInvoices()
        }
    }
}
