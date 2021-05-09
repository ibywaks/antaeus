package io.pleo.antaeus.rest.controllers

import io.javalin.http.Context
import io.pleo.antaeus.core.exceptions.CustomerNotCreatedException
import io.pleo.antaeus.core.exceptions.InvoiceNotCreatedException
import io.pleo.antaeus.core.exceptions.SubscriptionNotCreatedException
import io.pleo.antaeus.core.helpers.calculatePartialPlanAmount
import io.pleo.antaeus.core.services.CustomerService
import io.pleo.antaeus.core.services.InvoiceService
import io.pleo.antaeus.core.services.SubscriptionPlanService
import io.pleo.antaeus.core.services.SubscriptionService
import io.pleo.antaeus.models.*
import io.pleo.antaeus.models.Currency
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import java.util.*
import java.util.concurrent.TimeUnit

class CustomerController(
    private val invoiceService: InvoiceService,
    private val customerService: CustomerService,
    private val subscriptionService: SubscriptionService,
    private val subscriptionPlanService: SubscriptionPlanService
) {
    fun list(ctx: Context) {
        val isDeleted = ctx.queryParam("is_deleted") == "true"
        val status = ctx.queryParam("status")

        var selectedStatus: CustomerStatus? = null

        if (status != null) {
            selectedStatus = CustomerStatus.valueOf(status.toUpperCase())
        }

        ctx.json(customerService.fetchAll(isDeleted, selectedStatus))
    }

    fun index(ctx: Context) {
        ctx.json(customerService.fetch(ctx.pathParam("id").toInt()))
    }

    fun create(ctx: Context) {
        val currency = ctx.formParam("currency")
        val planId = ctx.formParam("plan_id")?.toInt()

        var customerCurrency: Currency = Currency.USD
        if (currency != null) {
            customerCurrency = Currency.valueOf(currency.toUpperCase())
        }

        try {
            val subscriptionPlan = subscriptionPlanService.fetch(planId as Int)
            val customer = customerService.create(customerCurrency)
            val subscription = subscriptionService.create(subscriptionPlan, customer)

            val startDate = Date().time
            val end = LocalDate.now().with(TemporalAdjusters.lastDayOfMonth())
            val endDate = (SimpleDateFormat("yyyy-mm-dd").parse(end.toString())).time

            invoiceService.create(customer, subscription, null, startDate, endDate)

            ctx.json(customer)
        } catch (e: CustomerNotCreatedException) {
            ctx.status(400).result(e.message ?: "unable to create new customer")
        } catch (e: SubscriptionNotCreatedException) {
            ctx.status(400).result(e.message ?: "unable to create customer subscription")
        } catch (e: InvoiceNotCreatedException) {
            ctx.status(400).result(e.message ?: "unable to create customer invoice")
        } catch (e: Exception) {
            ctx.status(400).result(e.message ?: "unable to register new customer")
        }
    }

    fun edit(ctx: Context) {
        val id = ctx.pathParam("id").toInt()

        val status = ctx.formParam("status")
        val currency = ctx.formParam("currency")

        var newStatus: CustomerStatus? = null
        var selectedCurrency: Currency? = null

        if (status != null) {
            newStatus = CustomerStatus.valueOf(status.toUpperCase())
        }

        if (currency != null) {
            selectedCurrency = Currency.valueOf(currency.toUpperCase())
        }

        try {
            ctx.json(
                customerService.update(
                    id,
                    CustomerUpdateSchema(
                        status = newStatus,
                        currency = selectedCurrency
                    )
                )
            )
        } catch (e: Exception) {
            ctx.status(400).result(e.message ?: "unable to update customer $id")
        }
    }

    fun delete(ctx: Context) {
        val id = ctx.pathParam("id").toInt()

        val customer = customerService.update(id, CustomerUpdateSchema(isDeleted = true))
        val activeInvoice = (invoiceService.fetchAll(false, InvoiceStatus.PENDING, customer))[0]

        subscriptionService.update(activeInvoice.subscriptionId, SubscriptionUpdateSchema(isDeleted = true))

        val end = LocalDate.now().with(TemporalAdjusters.lastDayOfMonth())
        val endDate = (SimpleDateFormat("yyyy-mm-dd").parse(end.toString())).time

        val diff = endDate - activeInvoice.chargeStartDate
        val days = TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS)

        val proratedAmount = calculatePartialPlanAmount(activeInvoice.amount, days)

        invoiceService.update(activeInvoice.id, InvoiceUpdateSchema(endDate = endDate, amount = proratedAmount))
        ctx.status(200)
    }
}
