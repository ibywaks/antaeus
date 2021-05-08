/*
    Configures the rest app along with basic exception handling and URL endpoints.
 */

package io.pleo.antaeus.rest

/* ktlint-disable no-wildcard-imports */
import io.javalin.Javalin
import io.javalin.apibuilder.ApiBuilder.*
import io.javalin.plugin.openapi.OpenApiOptions
import io.javalin.plugin.openapi.OpenApiPlugin
import io.javalin.plugin.openapi.annotations.OpenApi
import io.javalin.plugin.openapi.annotations.OpenApiContent
import io.javalin.plugin.openapi.annotations.OpenApiParam
import io.javalin.plugin.openapi.annotations.OpenApiResponse
import io.javalin.plugin.openapi.dsl.document
import io.javalin.plugin.openapi.dsl.documented
import io.javalin.plugin.openapi.ui.SwaggerOptions
import io.pleo.antaeus.core.exceptions.*
import io.pleo.antaeus.core.external.payment.StripeService
import io.pleo.antaeus.core.helpers.calculatePartialPlanAmount
import io.pleo.antaeus.core.services.CustomerService
import io.pleo.antaeus.core.services.InvoiceService
import io.pleo.antaeus.core.services.SubscriptionPlanService
import io.pleo.antaeus.core.services.SubscriptionService
import io.pleo.antaeus.models.*
import io.pleo.antaeus.rest.controllers.InvoiceController
import mu.KotlinLogging
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import java.util.Date
import java.util.concurrent.TimeUnit
import io.swagger.v3.oas.models.info.Info
import java.math.BigDecimal

/* ktlint-enable no-wildcard-imports */

private val logger = KotlinLogging.logger {}
private val thisFile: () -> Unit = {}

class AntaeusRest(
    private val invoiceService: InvoiceService,
    private val customerService: CustomerService,
    private val subscriptionService: SubscriptionService,
    private val subscriptionPlanService: SubscriptionPlanService,
    private val stripeService: StripeService,
    private val invoiceController: InvoiceController
) : Runnable {

    override fun run() {
        app.start(7000)
    }

    private fun getOpenApiOptions(): OpenApiOptions {
        val applicationInfo: Info = Info()
            .version("1.0")
            .description("Pleo Invoicing and Billing API")
        return OpenApiOptions(applicationInfo)
            .path("/swagger-docs")
            .swagger(SwaggerOptions("/swagger-ui"))
    }

    // Set up Javalin rest app
    private val app = Javalin
        .create { config ->
            config.registerPlugin(OpenApiPlugin(getOpenApiOptions()))
        }
        .apply {
            // InvoiceNotFoundException: return 404 HTTP status code
            exception(EntityNotFoundException::class.java) { _, ctx ->
                ctx.status(404)
            }
            // Unexpected exception: return HTTP 500
            exception(Exception::class.java) { e, _ ->
                logger.error(e) { "Internal server error" }
            }
            // On 404: return message
            error(404) { ctx -> ctx.json("not found") }
        }

    init {
        // Set up URL endpoints for the rest app
        app.routes {
            get("/") {
                it.result("Welcome to Antaeus! see AntaeusRest class for routes")
            }
            path("rest") {
                // Route to check whether the app is running
                // URL: /rest/health
                get("health") {
                    it.json("ok")
                }

                // V1
                path("v1") {
                    path("invoices") {
                        // URL: /rest/v1/invoices
                        val listInvoiceDoc = document()
                            .operation {
                                it.description("Invoice List")
                                it.addTagsItem("invoice")
                            }
                            .queryParam<Boolean>("is_deleted") { it.description("Filters invoice list based on deletedAt attribute") }
                            .queryParam<String>("status") { it.description("Filters invoice list based on status") }
                            .jsonArray<Invoice>("200")
                        get("", documented(listInvoiceDoc) { ctx -> invoiceController.list(ctx) })

                        // URL: /rest/v1/invoices/{:id}
                        val fetchInvoiceDoc = document()
                            .operation {
                                it.description("Invoice Fetch")
                                it.addTagsItem("invoice")
                            }
                            .pathParam<Int>("id") { it.description("Invoice ID") }
                            .json<Invoice>("200")
                        get(":id", documented(fetchInvoiceDoc) {
                            invoiceController.index(it)
                        })

                        //URL: /rest/v1/invoices
                        val createInvoiceDoc = document()
                            .operation {
                                it.description("Invoice Create")
                                it.addTagsItem("invoice")
                            }
                            .formParam<Int>("customer_id", true)
                            .formParam<BigDecimal>("amount", false)
                            .formParam<String>("currency", true)
                            .json<Invoice>("200")
                        post("", documented(createInvoiceDoc) {
                            invoiceController.create(it)
                        })

                        //URL: /rest/v1/invoices/{:id}
                        val editInvoiceDoc = document()
                            .operation {
                                it.description("Invoice Edit")
                                it.addTagsItem("invoice")
                            }
                            .formParam<String>("status", false)
                            .formParam<String>("payment_reference", false)
                            .formParam<Boolean>("is_deleted", false)
                            .formParam<BigDecimal>("amount", false)
                            .formParam<String>("currency", false)
                            .json<Invoice>("200")
                        put(":id", documented(editInvoiceDoc) {
                            invoiceController.edit(it)
                        })
                    }

                    path("customers") {
                        // URL: /rest/v1/customers
                        get {
                            val isDeleted = it.queryParam("is_deleted")?.toBoolean()
                            val status = it.queryParam("status")

                            var selectedStatus: CustomerStatus? = null

                            if (status != null) {
                                selectedStatus = CustomerStatus.valueOf(status)
                            }

                            it.json(customerService.fetchAll(isDeleted as Boolean, selectedStatus))
                        }

                        // URL: /rest/v1/customers/{:id}
                        get(":id") {
                            it.json(customerService.fetch(it.pathParam("id").toInt()))
                        }

                        // URL: /rest/v1/customers
                        post {
                            val currency = it.formParam("currency")
                            val planId = it.formParam("plan_id")?.toInt()

                            var customerCurrency: Currency = Currency.USD
                            if (currency != null) {
                                customerCurrency = Currency.valueOf(currency)
                            }

                            try {
                                val subscriptionPlan = subscriptionPlanService.fetch(planId as Int)
                                val customer = customerService.create(customerCurrency)
                                val subscription = subscriptionService.create(subscriptionPlan, customer)

                                val startDate = Date().time
                                val end = LocalDate.now().with(TemporalAdjusters.lastDayOfMonth())
                                val endDate = (SimpleDateFormat("yyyy-mm-dd").parse(end.toString())).time

                                invoiceService.create(customer, subscription, null, startDate, endDate)

                                it.json(customer)
                            } catch (e: CustomerNotCreatedException) {
                                it.status(400).result(e.message ?: "unable to create new customer")
                            } catch (e: SubscriptionNotCreatedException) {
                                it.status(400).result(e.message ?: "unable to create customer subscription")
                            } catch (e: InvoiceNotCreatedException) {
                                it.status(400).result(e.message ?: "unable to create customer invoice")
                            } catch (e: Exception) {
                                it.status(400).result(e.message ?: "unable to register new customer")
                            }
                        }

                        //URL: /rest/v1/customers/{:id}
                        put(":id") {
                            val id = it.pathParam("id").toInt()

                            val status = it.formParam("status")
                            val isDeleted = it.formParam("is_deleted")?.toBoolean()

                            var newStatus : CustomerStatus? = null
                            if (status != null) {
                                newStatus = CustomerStatus.valueOf(status)
                            }

                            try {
                                it.json(
                                    customerService.update(
                                        id,
                                        CustomerUpdateSchema(
                                            status = newStatus,
                                            isDeleted = isDeleted as Boolean
                                        )
                                    )
                                )
                            } catch (e: Exception) {
                                it.status(400).result(e.message ?: "unable to update customer $id")
                            }
                        }

                        //URL: /rest/v1/customer/{:id}
                        delete(":id") {
                            val id = it.pathParam("id").toInt()

                            val customer = customerService.update(id, CustomerUpdateSchema(isDeleted = true))
                            val activeInvoice = (invoiceService.fetchAll(false, InvoiceStatus.PENDING, customer))[0]

                            subscriptionService.update(activeInvoice.subscriptionId, SubscriptionUpdateSchema(isDeleted = true))

                            val end = LocalDate.now().with(TemporalAdjusters.lastDayOfMonth())
                            val endDate = (SimpleDateFormat("yyyy-mm-dd").parse(end.toString())).time

                            val diff = endDate - activeInvoice.chargeStartDate
                            val days = TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS)

                            val proratedAmount = calculatePartialPlanAmount(activeInvoice.amount, days)

                            invoiceService.update(activeInvoice.id, InvoiceUpdateSchema( endDate = endDate, amount = proratedAmount ))
                        }
                    }

                    path("subscriptions") {
                        // URL: /rest/v1/subscriptions
                        get {
                            val isDeleted = it.queryParam("is_deleted") ?: false

                            it.json(subscriptionService.fetchAll(isDeleted as Boolean))
                        }

                        //URL: /rest/v1/subscriptions/{:id}
                        get(":id") {
                            it.json(subscriptionService.fetch(it.pathParam("id").toInt()))
                        }

                        //URL: /rest/v1/subscriptions/{:id}
                        put(":id") {
                            val id = it.pathParam("id").toInt()

                            val amount = it.formParam("amount")?.toBigDecimal()
                            val currency = it.formParam("currency")
                            val isDeleted = it.formParam("is_deleted")?.toBoolean()

                            var newAmount: Money? = null

                            if (amount != null && currency != null) {
                                newAmount = Money(
                                    value = amount,
                                    currency = Currency.valueOf(currency)
                                )
                            }

                            it.json(
                                subscriptionService.update(
                                    id,
                                    SubscriptionUpdateSchema(
                                        amount = newAmount,
                                        isDeleted = isDeleted as Boolean
                                    )
                                )
                            )
                        }
                    }
                }
            }
            path("webhooks") {
               post("stripe") {
                   val secretSignature = it.header("stripe-signature")
                   val requestBody = it.body()

                   try {
                       if (secretSignature != null) {
                           val event = stripeService.generateEvent(requestBody, secretSignature)

                           stripeService.handleWebhookEvent(event)
                           it.status(200)
                       }
                   } catch (e: Exception) {
                       it.status(400)
                   }
               }
            }
        }
    }
}
