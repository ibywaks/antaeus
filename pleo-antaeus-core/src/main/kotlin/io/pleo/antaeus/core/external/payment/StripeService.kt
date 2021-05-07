package io.pleo.antaeus.core.external.payment

import io.pleo.antaeus.core.external.PaymentProvider
import com.stripe.Stripe
import com.stripe.exception.CardException
import com.stripe.exception.SignatureVerificationException
import com.stripe.model.Customer
import com.stripe.exception.StripeException
import com.stripe.model.Event
import com.stripe.model.PaymentIntent
import com.stripe.model.SetupIntent
import com.stripe.net.Webhook
import io.pleo.antaeus.core.services.CustomerService
import io.pleo.antaeus.core.services.InvoiceService
import io.pleo.antaeus.models.*
import java.lang.Exception
import java.math.BigDecimal

class StripeService(
    private val apiKey: String,
    private val webhookSecretKey: String,
    private val customerService: CustomerService,
    private val invoiceService: InvoiceService
): PaymentProvider {

    override fun charge(invoice: Invoice): Boolean {
        Stripe.apiKey = apiKey

        try {
            val pleoCustomerId = invoice.customerId
            val pleoCustomer = customerService.fetch(pleoCustomerId)

            if (pleoCustomer.defaultStripePaymentMethodId == null) {
                // trigger some event to notify customer to setup payment
                // throw a custom exception
                // throw Exception("no available payment method")
                return false
            }

            val invoiceAmount = invoice.amount.value * BigDecimal(100)
            val paymentPayload = mapOf(
                "amount" to invoiceAmount,
                "currency" to invoice.amount.currency,
                "confirm" to true,
                "customer" to pleoCustomer.stripeId,
                "payment_method" to pleoCustomer.defaultStripePaymentMethodId,
                "off_session" to true,
                "metadata" to mapOf("invoice_id" to invoice.id)
            )

            createPaymentIntent(paymentPayload)

            return true
        } catch (e: CardException) {
            // log issue
            return false
        }
    }

    fun createPaymentIntent(payload: Map<String, Any?>) {
        PaymentIntent.create(payload)
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
            Stripe.apiKey = apiKey
            return Webhook.constructEvent(payload, sigHeader, webhookSecretKey)
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
