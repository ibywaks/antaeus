package io.pleo.antaeus.rest.controllers

import cc.vileda.openapi.dsl.mediaType
import io.javalin.http.Context
import io.javalin.http.Handler
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
        val isDeleted = ctx.queryParam("is_deleted") ?: false
        val status = ctx.queryParam("status")

        var selectedStatus: InvoiceStatus? = null

        if (status != null) {
            selectedStatus = InvoiceStatus.valueOf(status)
        }

        ctx.json(invoiceService.fetchAll(isDeleted as Boolean, selectedStatus))
    }

    @OpenApi(
        description = "Invoice Update",
        tags = ["invoice"],
        pathParams = [
            OpenApiParam(
                name = "id",
                description = "Invoice ID"
            )
        ],
        requestBody = OpenApiRequestBody(
            description = "object containing invoice edit params",
            content = [
                OpenApiContent(
                    type = "application/json",
                    from = InvoiceUpdateSchema::class
                )
            ]
        ),
        responses = [
            OpenApiResponse(
                status = "200",
                content = [OpenApiContent(from = Invoice::class)]
            )
        ]
    )
    fun edit(ctx: Context) {
        val id = ctx.pathParam("id").toInt()

        try {
            val amount = ctx.formParam("amount")?.toBigDecimal()
            val currency = ctx.formParam("currency")
            val status = ctx.formParam("status")
            val paymentRef = ctx.formParam("payment_reference")
            val isDeleted = ctx.formParam("is_deleted")?.toBoolean()

            var newStatus: InvoiceStatus? = null
            var newAmount: Money? = null

            if (amount != null && currency != null) {
                newAmount = Money(
                    value = amount,
                    currency = Currency.valueOf(currency)
                )
            }

            if (status != null) {
                newStatus = InvoiceStatus.valueOf(status)
            }

            ctx.json(invoiceService.update(
                id,
                InvoiceUpdateSchema(
                    amount = newAmount,
                    status = newStatus,
                    isDeleted = isDeleted as Boolean,
                    paymentRef = paymentRef
                )
            ))
        } catch (e: Exception) { ctx.status(400).result("unable to update invoice $id")}
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
                currency = Currency.valueOf(currency)
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
