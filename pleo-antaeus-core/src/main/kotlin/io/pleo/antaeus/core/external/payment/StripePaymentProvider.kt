package io.pleo.antaeus.core.external.payment

import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.models.Invoice
import com.stripe.Stripe
import com.stripe.model.Customer
import com.stripe.exception.StripeException
import com.stripe.model.SetupIntent

class StripePaymentProvider(private val apiKey: String): PaymentProvider {

    override fun charge(invoice: Invoice): Boolean {
        Stripe.apiKey = apiKey
        return false
    }



}
