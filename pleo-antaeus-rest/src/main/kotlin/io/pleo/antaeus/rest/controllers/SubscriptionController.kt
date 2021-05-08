package io.pleo.antaeus.rest.controllers

import io.javalin.http.Context
import io.pleo.antaeus.core.services.SubscriptionService
import io.pleo.antaeus.models.Currency
import io.pleo.antaeus.models.Money
import io.pleo.antaeus.models.SubscriptionUpdateSchema

class SubscriptionController(private val subscriptionService: SubscriptionService) {
    fun list(ctx: Context) {
        val isDeleted = ctx.queryParam("is_deleted") ?: false

        ctx.json(subscriptionService.fetchAll(isDeleted as Boolean))
    }

    fun index(ctx: Context) {
        ctx.json(subscriptionService.fetch(ctx.pathParam("id").toInt()))
    }

    fun edit(ctx: Context) {
        val id = ctx.pathParam("id").toInt()

        val amount = ctx.formParam("amount")?.toBigDecimal()
        val currency = ctx.formParam("currency")
        val isDeleted = ctx.formParam("is_deleted")?.toBoolean()

        var newAmount: Money? = null

        if (amount != null && currency != null) {
            newAmount = Money(
                value = amount,
                currency = Currency.valueOf(currency)
            )
        }

        ctx.json(
            subscriptionService.update(
                id,
                SubscriptionUpdateSchema(
                    amount = newAmount,
                    isDeleted = isDeleted as Boolean
                )
            )
        )
    }
}
