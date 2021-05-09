package io.pleo.antaeus.rest.controllers

import com.stripe.model.Customer
import com.stripe.model.Event
import com.stripe.model.PaymentIntent
import com.stripe.model.SetupIntent
import io.javalin.http.Context
import io.pleo.antaeus.core.external.payment.StripeService
import io.pleo.antaeus.core.services.CustomerService
import io.pleo.antaeus.core.services.InvoiceService
import io.pleo.antaeus.models.CustomerStatus
import io.pleo.antaeus.models.CustomerUpdateSchema
import io.pleo.antaeus.models.InvoiceStatus
import io.pleo.antaeus.models.InvoiceUpdateSchema

class StripeWebhookController(
    private val invoiceService: InvoiceService,
    private val customerService: CustomerService,
    private val stripeService: StripeService
) {
    fun handleWebhookEvent(ctx: Context) {
        val secretSignature = ctx.header("stripe-signature")
        val requestBody = ctx.body()

        try {
            if (secretSignature != null) {
                val event = stripeService.generateEvent(requestBody, secretSignature)

                handleEvents(event)
                ctx.status(200)
            }
        } catch (e: Exception) {
            ctx.status(400)
        }
    }

    private fun handleEvents(event: Event) {
        val dataObjectDeserializer = event.dataObjectDeserializer
        if (!dataObjectDeserializer.`object`.isPresent) {
            throw java.lang.Exception("something happened")
        }

        val stripeObject = dataObjectDeserializer.`object`.get()

        when (event.type) {
            "customer.created" -> {
                val customerData = stripeObject as Customer

                val stripeCustomerId = customerData.id
                val pleoCustomerId = customerData.metadata["pleo_id"]

                if (pleoCustomerId == null) {
                    // log unrecognised customer created
                    // keep it moving
                    return
                }

                val update = CustomerUpdateSchema(stripeId = stripeCustomerId)
                customerService.update(pleoCustomerId.toInt(), update)
            }
            "payment_intent.succeeded" -> {
                val paymentIntentData = stripeObject as PaymentIntent

                val paymentIntentId = paymentIntentData.id
                val pleoInvoiceId = paymentIntentData.metadata["pleo_invoice_id"]

                if (pleoInvoiceId == null) {
                    // log unrecognised payment
                    // keep it moving
                    return
                }

                val invoiceUpdate = InvoiceUpdateSchema(
                    paymentRef = paymentIntentId,
                    status = InvoiceStatus.PAID
                )
                val invoice = invoiceService.update(pleoInvoiceId.toInt(), invoiceUpdate)

                val customerUpdate = CustomerUpdateSchema(
                    status = CustomerStatus.ACTIVE
                )
                customerService.update(invoice.customerId, customerUpdate)
            }
            "payment_intent.processing" -> {
                val paymentIntentData = stripeObject as PaymentIntent

                val paymentIntentId = paymentIntentData.id
                val pleoInvoiceId = paymentIntentData.metadata["pleo_invoice_id"]

                if (pleoInvoiceId == null) {
                    // log unrecognised payment
                    // keep it moving
                    return
                }

                val invoiceUpdate = InvoiceUpdateSchema(
                    paymentRef = paymentIntentId,
                    status = InvoiceStatus.PROCESSING
                )
                invoiceService.update(pleoInvoiceId.toInt(), invoiceUpdate)
            }
            "setup_intent.succeeded" -> {
                val setupIntentData = stripeObject as SetupIntent

                val paymentMethodId = setupIntentData.paymentMethod
                val customerId = setupIntentData.customer

                val customer = Customer.retrieve(customerId)
                if (customer == null) {
                    // log unrecognised payment setup
                    // keep it moving
                    return
                }

                val pleoCustomerId = customer.metadata["pleo_id"]

                if (pleoCustomerId == null) {
                    // log unrecognised customer created
                    // keep it moving
                    return
                }

                val update = CustomerUpdateSchema(defaultStripePaymentMethodId = paymentMethodId)
                customerService.update(pleoCustomerId.toInt(), update)
            }
        }
    }
}
