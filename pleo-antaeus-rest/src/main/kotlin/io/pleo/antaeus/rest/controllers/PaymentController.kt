package io.pleo.antaeus.rest.controllers

import io.javalin.http.Context
import io.pleo.antaeus.core.external.payment.PaymentSetupDTO
import io.pleo.antaeus.core.services.BillingService
import io.pleo.antaeus.core.services.CustomerService
import java.lang.Exception

class PaymentController(
    private val customerService: CustomerService,
    private val billingService: BillingService
) {
    fun initPaymentMethod(ctx: Context) {
        try {
            val customerId = ctx.queryParam("customer_id")

            if (customerId == null)
                ctx.status(400).result("'customer_id' required to initialize payment method")

            val customer = customerService.fetch(customerId!!.toInt())
            val payload = PaymentSetupDTO(customer = customer)

            ctx.json(billingService.initPaymentMethod(payload))
        } catch (e: Exception) {
            ctx.status(400).result(e.message ?: "An error occurred during payment initialization")
        }
    }
}
