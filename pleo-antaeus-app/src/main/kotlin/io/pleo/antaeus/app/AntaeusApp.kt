/*
    Defines the main() entry point of the app.
    Configures the database and sets up the REST web service.
 */

@file:JvmName("AntaeusApp")

package io.pleo.antaeus.app

/* ktlint-disable no-wildcard-imports */
import io.github.cdimascio.dotenv.dotenv
import io.pleo.antaeus.core.external.payment.StripeService
import io.pleo.antaeus.core.services.*
import io.pleo.antaeus.data.SubscriptionPlanTable
import io.pleo.antaeus.data.SubscriptionTable
import io.pleo.antaeus.data.CustomerTable
import io.pleo.antaeus.data.InvoiceTable
import io.pleo.antaeus.data.AntaeusDal
import io.pleo.antaeus.rest.AntaeusRest
import io.pleo.antaeus.rest.controllers.CustomerController
import io.pleo.antaeus.rest.controllers.InvoiceController
import io.pleo.antaeus.rest.controllers.StripeWebhookController
import io.pleo.antaeus.rest.controllers.SubscriptionController
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import setupInitialData
import java.io.File
import java.sql.Connection
/* ktlint-enable no-wildcard-imports */

fun main() {
    // The tables to create in the database.
    val tables = arrayOf(InvoiceTable, CustomerTable, SubscriptionTable, SubscriptionPlanTable)

    val dbFile: File = File.createTempFile("antaeus-db", ".sqlite")
    // Connect to the database and create the needed tables. Drop any existing data.
    val db = Database
        .connect(url = "jdbc:sqlite:${dbFile.absolutePath}",
            driver = "org.sqlite.JDBC",
            user = "root",
            password = "")
        .also {
            TransactionManager.manager.defaultIsolationLevel = Connection.TRANSACTION_SERIALIZABLE
            transaction(it) {
                addLogger(StdOutSqlLogger)
                // Drop all existing tables to ensure a clean slate on each run
                SchemaUtils.drop(*tables)
                // Create all tables
                SchemaUtils.create(*tables)
            }
        }

    // Set up dotenv
    val dotenv = dotenv{
        directory = "../"
    }

    // Set up data access layer.
    val dal = AntaeusDal(db = db)

    // Insert example data in the database.
    setupInitialData(dal = dal)

    // Create core services
    val invoiceService = InvoiceService(dal = dal)
    val customerService = CustomerService(dal = dal)
    val subscriptionService = SubscriptionService(dal = dal)
    val subscriptionPlanService = SubscriptionPlanService(dal = dal)

    // Setting up the stripe service
    val stripeAPISecret = dotenv["STRIPE_SECRET_KEY"]
    val webhookSecretKey = dotenv["STRIPE_WEBHOOK_SECRET"]
    val stripeService = StripeService(
        stripeAPISecret,
        webhookSecretKey
    )

    // This is _your_ billing service to be included where you see fit
    val billingService = BillingService(
        paymentProvider = stripeService,
        invoiceService = invoiceService,
        customerService = customerService
    )

    // Cron service
    CronScheduleService(
        billingService = billingService
    ).run()

    //Initialize Controller classes
    val invoiceController = InvoiceController(
        invoiceService = invoiceService,
        customerService = customerService,
        subscriptionService = subscriptionService
    )
    val customerController = CustomerController(
        invoiceService = invoiceService,
        customerService = customerService,
        subscriptionService = subscriptionService,
        subscriptionPlanService = subscriptionPlanService
    )
    val subscriptionController = SubscriptionController(
        subscriptionService = subscriptionService
    )
    val stripeWebhookController = StripeWebhookController(
        invoiceService = invoiceService,
        customerService = customerService,
        stripeService = stripeService
    )

    // Create REST web service
    AntaeusRest(
        invoiceController = invoiceController,
        customerController = customerController,
        subscriptionController = subscriptionController,
        stripeWebhookController = stripeWebhookController
    ).run()
}
