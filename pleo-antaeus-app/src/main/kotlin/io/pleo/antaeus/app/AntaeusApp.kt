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
import io.pleo.antaeus.data.AntaeusDal
import io.pleo.antaeus.data.CustomerTable
import io.pleo.antaeus.data.InvoiceTable
import io.pleo.antaeus.data.SubscriptionPlanTable
import io.pleo.antaeus.data.SubscriptionTable
import io.pleo.antaeus.rest.AntaeusRest
import io.pleo.antaeus.rest.controllers.*
import java.io.File
import java.nio.file.Paths
import java.sql.Connection
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import setupInitialData
/* ktlint-enable no-wildcard-imports */

fun main() {
    // Set up dotenv
    val dotenv = dotenv {
        directory = "../"
    }

    // The tables to create in the database.
    val tables = arrayOf(InvoiceTable, CustomerTable, SubscriptionTable, SubscriptionPlanTable)

    val appMode = dotenv["MODE"]
    val dbPath = dotenv["DB_FILE"]

    val path = Paths.get("../").toAbsolutePath().toString()
    val dbFile: File = File(path, dbPath)

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

                if (appMode != "production") {
                    // Drop all existing tables to ensure a clean slate on each run
                    SchemaUtils.drop(*tables)
                    // Create all tables
                    SchemaUtils.create(*tables)
                }
            }
        }

    // Set up data access layer.
    val dal = AntaeusDal(db = db)

    // Insert example data in the database.
    if (appMode != "production")
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

    // Initialize Controller classes
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
    val paymentController = PaymentController(
        customerService = customerService,
        billingService = billingService
    )

    // Create REST web service
    AntaeusRest(
        invoiceController = invoiceController,
        customerController = customerController,
        subscriptionController = subscriptionController,
        stripeWebhookController = stripeWebhookController,
        paymentController = paymentController
    ).run()
}
