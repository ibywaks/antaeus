package io.pleo.antaeus.core.external.payment

import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.models.Invoice
import com.stripe.Stripe
import com.stripe.exception.SignatureVerificationException
import com.stripe.model.Customer
import com.stripe.exception.StripeException
import com.stripe.model.Event
import com.stripe.model.PaymentIntent
import com.stripe.model.SetupIntent
import com.stripe.net.Webhook
import io.github.cdimascio.dotenv.dotenv
import io.pleo.antaeus.core.services.CustomerService
import io.pleo.antaeus.core.services.InvoiceService
import io.pleo.antaeus.models.CustomerUpdateSchema
import io.pleo.antaeus.models.InvoiceStatus
import io.pleo.antaeus.models.InvoiceUpdateSchema
import java.lang.Exception

class StripeService(
    private val apiKey: String,
    private val customerService: CustomerService,
    private val invoiceService: InvoiceService
): PaymentProvider {

    override fun charge(invoice: Invoice): Boolean {
        Stripe.apiKey = apiKey
        return true
    }

    fun initPaymentSetup(data: PaymentSetupDTO): SetupIntent? {
        Stripe.apiKey = apiKey

        try {
            val stripeCustomer = if (data.customer.stripeId !== null) {
                Customer.retrieve(data.customer.stripeId)
            } else {
                val metadata = mapOf("pleo_id" to data.customer.id)
                val params = mapOf("metadata" to metadata)
                Customer.create(params)
            }

            //@todo customer created event

            val paymentMethodTypes = listOf("card")
            val intentParams = mutableMapOf(
                "customer" to stripeCustomer.id,
                "payment_method_types" to paymentMethodTypes
            )

            return SetupIntent.create(intentParams)
        } catch (e: StripeException) {
            // throw custom error & do a log
            // ${e.code} ${e.message}
            throw Exception("something went wrong")
        }
    }

    fun generateEvent(payload: String, sigHeader: String): Event {
        try {
            val dotenv = dotenv()
            val webhookSecret = dotenv["STRIPE_WEBHOOK_SECRET"]

            Stripe.apiKey = apiKey
            return Webhook.constructEvent(payload, sigHeader, webhookSecret)
        } catch (e: SignatureVerificationException) {
            // log and return a custom exception
            throw Exception("something went wrong")
        }
    }

    fun handleWebhookEvent(event: Event) {
        val dataObjectDeserializer = event.dataObjectDeserializer
        if (!dataObjectDeserializer.`object`.isPresent) {
            throw Exception("something happened")
        }

        val stripeObject = dataObjectDeserializer.`object`.get()

        when(event.type) {
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
                customerService.update(pleoCustomerId as Int, update)
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
                invoiceService.update(pleoInvoiceId as Int, invoiceUpdate)
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
                invoiceService.update(pleoInvoiceId as Int, invoiceUpdate)
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
                customerService.update(pleoCustomerId as Int, update)
            }
        }
    }
}
