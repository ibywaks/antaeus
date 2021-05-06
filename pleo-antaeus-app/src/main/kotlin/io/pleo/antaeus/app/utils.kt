
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.data.AntaeusDal
import io.pleo.antaeus.models.Currency
import io.pleo.antaeus.models.Money
import io.pleo.antaeus.models.SubscriptionPlan
import io.pleo.antaeus.models.InvoiceStatus
import io.pleo.antaeus.models.Invoice
import java.math.BigDecimal
import kotlin.random.Random

// This will create all schemas and setup initial data
internal fun setupInitialData(dal: AntaeusDal) {
    val subscriptionPlan = dal.createSubscriptionPlan("Basic Plan", Money(
            value = BigDecimal(50),
            currency = Currency.USD
        )
    )
    val customers = (1..100).mapNotNull {
        dal.createCustomer(
            currency = Currency.values()[Random.nextInt(0, Currency.values().size)]
        )
    }

    val subscriptions = customers.mapNotNull { customer ->
        dal.createSubscription(
            plan = subscriptionPlan as SubscriptionPlan,
            amount = Money(
                value = BigDecimal(Random.nextDouble(10.0, 500.0)),
                currency = customer.currency
            ),
            customer = customer
        )
    }

    subscriptions.forEach { subscription ->
        val subscriptionCustomer = customers.find{c -> c.id == subscription.customerId}

        if (subscriptionCustomer != null) {
            (1..10).forEach {
                dal.createInvoice(
                    amount = subscription.amount,
                    customer = subscriptionCustomer,
                    subscription = subscription,
                    status = if (it == 1) InvoiceStatus.PENDING else InvoiceStatus.PAID
                )
            }
        }
    }

    // customers.forEach { customer ->
    //     (1..10).forEach {
    //         dal.createInvoice(
    //             amount = Money(
    //                 value = BigDecimal(Random.nextDouble(10.0, 500.0)),
    //                 currency = customer.currency
    //             ),
    //             customer = customer,
    //             status = if (it == 1) InvoiceStatus.PENDING else InvoiceStatus.PAID
    //         )
    //     }
    // }
}

// This is the mocked instance of the payment provider
internal fun getPaymentProvider(): PaymentProvider {
    return object : PaymentProvider {
        override fun charge(invoice: Invoice): Boolean {
                return Random.nextBoolean()
        }
    }
}
