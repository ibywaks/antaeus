package io.pleo.antaeus.core.external.payment

import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.models.Invoice
import com.stripe.Stripe
import com.stripe.model.Customer
import com.stripe.exception.StripeException
import com.stripe.model.SetupIntent
import io.pleo.antaeus.core.services.CustomerService
import java.lang.Exception

class StripeService(private val apiKey: String, private val customerService: CustomerService): PaymentProvider {

    override fun charge(invoice: Invoice): Boolean {
        Stripe.apiKey = apiKey
        return false
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
                "payment_method_types" to paymentMethodTypes,
                "confirm" to true
            )

            return SetupIntent.create(intentParams)
        } catch (e: StripeException) {
            // throw custom error & do a log
            throw Exception("something went wrong")
        }
    }
}
