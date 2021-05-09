package io.pleo.antaeus.rest.controllers

import io.javalin.http.Context
import io.javalin.plugin.openapi.annotations.*
import io.pleo.antaeus.core.exceptions.CustomerNotFoundException
import io.pleo.antaeus.core.exceptions.InvoiceNotCreatedException
import io.pleo.antaeus.core.exceptions.NoCustomerSubscriptionException
import io.pleo.antaeus.core.services.CustomerService
import io.pleo.antaeus.core.services.InvoiceService
import io.pleo.antaeus.core.services.SubscriptionService
import io.pleo.antaeus.models.*

class InvoiceController(
    private val invoiceService: InvoiceService,
    private val customerService: CustomerService,
    private val subscriptionService: SubscriptionService
) {
    fun list(ctx: Context) {
        val isDeleted = ctx.queryParam("is_deleted") == "true"
        val status = ctx.queryParam("status")
        val customerId = ctx.queryParam("customer_id")

        var selectedStatus: InvoiceStatus? = null
        var customer: Customer? = null

        if (status != null) {
            selectedStatus = InvoiceStatus.valueOf(status.toUpperCase())
        }

        if (customerId != null) {
            customer = customerService.fetch(customerId.toInt())
        }

        ctx.json(invoiceService.fetchAll(isDeleted, selectedStatus, customer))
    }

    fun edit(ctx: Context) {
        val id = ctx.pathParam("id").toInt()

        try {
            val amount = ctx.formParam("amount")?.toBigDecimal()
            val currency = ctx.formParam("currency")
            val status = ctx.formParam("status")
            val paymentRef = ctx.formParam("payment_reference")
            val isDeleted = ctx.formParam("is_deleted") == "true"

            var newStatus: InvoiceStatus? = null
            var newAmount: Money? = null

            if (amount != null && currency != null) {
                newAmount = Money(
                    value = amount,
                    currency = Currency.valueOf(currency.toUpperCase())
                )
            }

            if (status != null) {
                newStatus = InvoiceStatus.valueOf(status.toUpperCase())
            }

            ctx.json(invoiceService.update(
                id,
                InvoiceUpdateSchema(
                    amount = newAmount,
                    status = newStatus,
                    isDeleted = isDeleted,
                    paymentRef = paymentRef
                )
            ))
        } catch (e: Exception) { ctx.status(400).result("unable to update invoice $id") }
    }

    fun index(ctx: Context) {
        ctx.json(invoiceService.fetch(ctx.pathParam("id").toInt()))
    }

    fun create(ctx: Context) {
        val customerId = ctx.formParam("customer_id")?.toInt()
        val amount = ctx.formParam("amount")?.toBigDecimal()
        val currency = ctx.formParam("currency")

        var customAmount: Money? = null
        if (amount != null && currency != null) {
            customAmount = Money(
                value = amount,
                currency = Currency.valueOf(currency.toUpperCase())
            )
        }

        try {
            val customer = customerService.fetch(customerId as Int)

            val subscriptions = subscriptionService.fetchAll(false, customer)

            if (subscriptions.isEmpty()) throw NoCustomerSubscriptionException(customerId)

            ctx.json(invoiceService.create(
                customer,
                subscriptions[0],
                customAmount
            ))
        } catch (e: CustomerNotFoundException) {
            ctx.status(400).result(e.message ?: "Customer $customerId not found")
        } catch (e: InvoiceNotCreatedException) {
            ctx.status(400).result(e.message ?: "Unable to create invoice for $customerId")
        } catch (e: NoCustomerSubscriptionException) {
            ctx.status(400).result(e.message ?: "Customer $customerId has no active subscriptions")
        } catch (e: Exception) {
            ctx.status(400).result(e.message ?: "Unable to create invoice for $customerId")
        }
    }
}
