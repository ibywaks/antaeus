package io.pleo.antaeus.core.external.payment

import com.stripe.Stripe
import com.stripe.exception.CardException
import com.stripe.exception.SignatureVerificationException
import com.stripe.exception.StripeException
import com.stripe.model.Customer
import com.stripe.model.Event
import com.stripe.model.PaymentIntent
import com.stripe.model.SetupIntent
import com.stripe.net.Webhook
import com.stripe.param.PaymentIntentCreateParams
import com.stripe.param.SetupIntentCreateParams
import io.pleo.antaeus.core.exceptions.PaymentInitializationException
import io.pleo.antaeus.core.exceptions.StripeWebhookEventConstructionException
import io.pleo.antaeus.core.external.PaymentProvider
import mu.KotlinLogging
import java.lang.Exception

private val logger = KotlinLogging.logger {}

class StripeService(
    private val apiKey: String,
    private val webhookSecretKey: String
) : PaymentProvider {

    override fun charge(payload: ChargePayload): Boolean {
        Stripe.apiKey = apiKey

        try {
            val params = PaymentIntentCreateParams.builder()
                .setPaymentMethod(payload.paymentMethod)
                .setAmount(payload.amount)
                .setCurrency(payload.currency.toString())
                .setCustomer(payload.customerReference)
                .putMetadata("invoice_id", payload.invoiceId)
                .setConfirm(true)
                .setOffSession(true).build()

            createPaymentIntent(params)

            return true
        } catch (e: CardException) {
            // log issue
            logger.error("Error at Stripe charge - payment intent creation", e.code, e.message)
            return false
        }
    }

    fun createPaymentIntent(params: PaymentIntentCreateParams) {
        PaymentIntent.create(params)
    }

    override fun initPaymentSetup(data: PaymentSetupDTO): PaymentSetupObject {
        Stripe.apiKey = apiKey

        try {
            val stripeCustomer = if (data.customer.stripeId !== null) {
                Customer.retrieve(data.customer.stripeId)
            } else {
                val metadata = mapOf("pleo_id" to data.customer.id)
                val params = mapOf("metadata" to metadata)
                Customer.create(params)
            }

            val intentParams = SetupIntentCreateParams.builder()
                .setCustomer(stripeCustomer.id)
                .addPaymentMethodType("card")
                .build()

            val intent = SetupIntent.create(intentParams)

            return PaymentSetupObject(reference = intent.id, secretKey = intent.clientSecret)
        } catch (e: StripeException) {
            // throw custom error & do a log
            logger.error(
                "Error at Stripe payment method initialization - create setup intent",
                e.code,
                e.message
            )
            throw PaymentInitializationException()
        }
    }

    fun generateEvent(payload: String, sigHeader: String): Event {
        try {
            Stripe.apiKey = apiKey
            return Webhook.constructEvent(payload, sigHeader, webhookSecretKey)
        } catch (e: SignatureVerificationException) {
            // log and return a custom exception
            logger.error("Error at Stripe webhook event construction", e.code, e.message)
            throw StripeWebhookEventConstructionException()
        }
    }
}
