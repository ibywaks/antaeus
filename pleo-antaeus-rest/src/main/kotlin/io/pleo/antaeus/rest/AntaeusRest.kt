/*
    Configures the rest app along with basic exception handling and URL endpoints.
 */

package io.pleo.antaeus.rest

/* ktlint-disable no-wildcard-imports */
import io.javalin.Javalin
import io.javalin.apibuilder.ApiBuilder.*
import io.javalin.plugin.openapi.OpenApiOptions
import io.javalin.plugin.openapi.OpenApiPlugin
import io.javalin.plugin.openapi.dsl.document
import io.javalin.plugin.openapi.dsl.documented
import io.javalin.plugin.openapi.ui.SwaggerOptions
import io.pleo.antaeus.core.exceptions.*
import io.pleo.antaeus.core.external.payment.PaymentSetupObject
import io.pleo.antaeus.models.*
import io.pleo.antaeus.rest.controllers.*
import io.swagger.v3.oas.models.info.Info
import java.math.BigDecimal
import mu.KotlinLogging

/* ktlint-enable no-wildcard-imports */

private val logger = KotlinLogging.logger {}
private val thisFile: () -> Unit = {}

class AntaeusRest(
    private val invoiceController: InvoiceController,
    private val customerController: CustomerController,
    private val subscriptionController: SubscriptionController,
    private val stripeWebhookController: StripeWebhookController,
    private val paymentController: PaymentController
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
            val welcomeDoc = document()
                .operation {
                    it.description("API Root")
                }
            get("/", documented(welcomeDoc) {
                it.result("Welcome to Antaeus! see AntaeusRest class for routes")
            })
            path("rest") {
                // Route to check whether the app is running
                // URL: /rest/health
                val healthDoc = document()
                    .operation {
                        it.description("API Health")
                    }
                get("health", documented(healthDoc) {
                    it.json("ok")
                })

                // V1
                path("v1") {
                    path("invoices") {
                        // URL: /rest/v1/invoices
                        val listInvoiceDoc = document()
                            .operation {
                                it.description("Invoice List")
                                it.addTagsItem("invoices")
                            }
                            .queryParam<Boolean>("is_deleted") { it.description("Filters invoice list based on deletedAt attribute") }
                            .queryParam<String>("status") { it.description("Filters invoice list based on status") }
                            .queryParam<String>("customer_id") { it.description("Filters invoice list based on customer") }
                            .jsonArray<Invoice>("200")
                        get("", documented(listInvoiceDoc) { ctx -> invoiceController.list(ctx) })

                        // URL: /rest/v1/invoices/{:id}
                        val fetchInvoiceDoc = document()
                            .operation {
                                it.description("Invoice Fetch")
                                it.addTagsItem("invoices")
                            }
                            .pathParam<Int>("id") { it.description("Invoice ID") }
                            .json<Invoice>("200")
                        get(":id", documented(fetchInvoiceDoc) {
                            invoiceController.index(it)
                        })

                        // URL: /rest/v1/invoices
                        val createInvoiceDoc = document()
                            .operation {
                                it.description("Invoice Create")
                                it.addTagsItem("invoices")
                            }
                            .formParam<Int>("customer_id", true)
                            .formParam<BigDecimal>("amount", false)
                            .formParam<String>("currency", true)
                            .json<Invoice>("200")
                        post("", documented(createInvoiceDoc) {
                            invoiceController.create(it)
                        })

                        // URL: /rest/v1/invoices/{:id}
                        val editInvoiceDoc = document()
                            .operation {
                                it.description("Invoice Edit")
                                it.addTagsItem("invoices")
                            }
                            .pathParam<Int>("id") { it.description("Invoice ID") }
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
                        val listCustomersDoc = document()
                            .operation {
                                it.description("Customer List")
                                it.addTagsItem("customers")
                            }
                            .queryParam<Boolean>("is_deleted") { it.description("Filters invoice list based on deletedAt attribute") }
                            .queryParam<String>("status") { it.description("Filters invoice list based on status") }
                            .jsonArray<Customer>("200")
                        get("", documented(listCustomersDoc) {
                            customerController.list(it)
                        })

                        // URL: /rest/v1/customers/{:id}
                        val fetchCustomerDoc = document()
                            .operation {
                                it.description("Customer Fetch")
                                it.addTagsItem("customers")
                            }
                            .pathParam<Int>("id") { it.description("Customer ID") }
                            .json<Customer>("200")
                        get(":id", documented(fetchCustomerDoc) {
                            customerController.index(it)
                        })

                        // URL: /rest/v1/customers
                        val createCustomerDoc = document()
                            .operation {
                                it.description("Customer Create")
                                it.addTagsItem("customers")
                            }
                            .formParam<Int>("plan_id", true)
                            .formParam<String>("currency", true)
                            .json<Customer>("200")
                        post("", documented(createCustomerDoc) {
                            customerController.create(it)
                        })

                        // URL: /rest/v1/customers/{:id}
                        val editCustomerDoc = document()
                            .operation {
                                it.description("Customer Edit")
                                it.addTagsItem("customers")
                            }
                            .pathParam<Int>("id") { it.description("Customer ID") }
                            .formParam<String>("status", false)
                            .formParam<String>("currency", false)
                            .json<Customer>("200")
                        put(":id", documented(editCustomerDoc) {
                            customerController.edit(it)
                        })

                        // URL: /rest/v1/customer/{:id}
                        val deleteCustomerDoc = document()
                            .operation {
                                it.description("Customer Delete")
                                it.addTagsItem("customers")
                            }
                            .pathParam<Int>("id") { it.description("Customer ID") }
                            .result<Int>("200")
                        delete(":id", documented(deleteCustomerDoc) {
                            customerController.delete(it)
                        })
                    }

                    path("subscriptions") {
                        // URL: /rest/v1/subscriptions
                        val listSubscriptionsDoc = document()
                            .operation {
                                it.description("Subscription List")
                                it.addTagsItem("subscriptions")
                            }
                            .queryParam<Boolean>("is_deleted") { it.description("Filters subscription list based on deletedAt attribute") }
                            .jsonArray<Subscription>("200")
                        get("", documented(listSubscriptionsDoc) {
                            subscriptionController.list(it)
                        })

                        // URL: /rest/v1/subscriptions/{:id}
                        val fetchSubscriptionDoc = document()
                            .operation {
                                it.description("Subscription Fetch")
                                it.addTagsItem("subscriptions")
                            }
                            .pathParam<Int>("id") { it.description("Subscription ID") }
                            .json<Subscription>("200")
                        get(":id", documented(fetchSubscriptionDoc) {
                            subscriptionController.index(it)
                        })

                        // URL: /rest/v1/subscriptions/{:id}
                        val editSubscriptionDoc = document()
                            .operation {
                                it.description("Subscription Edit")
                                it.addTagsItem("subscriptions")
                            }
                            .pathParam<Int>("id") { it.description("Subscription ID") }
                            .formParam<Boolean>("is_deleted", false)
                            .formParam<BigDecimal>("amount", false)
                            .formParam<String>("currency", false)
                            .json<Customer>("200")
                        put(":id", documented(editSubscriptionDoc) {
                            subscriptionController.edit(it)
                        })
                    }

                    val paymentInitDoc = document()
                        .operation {
                            it.description("Endpoint to initialize adding payment method")
                            it.addTagsItem("payment")
                        }
                        .queryParam<String>("customer_id") { it.description("The customer's payment method being initialised") }
                        .json<PaymentSetupObject>("200")
                    get("payment/init", documented(paymentInitDoc) {
                        paymentController.initPaymentMethod(it)
                    })
                }
            }
            path("webhooks") {
                val stripeWebhookDoc = document()
                    .operation {
                        it.description("Stripe Webhook")
                    }
                    .header<String>("stripe-signature")
                    .ignore(true)
                post("stripe", documented(stripeWebhookDoc) {
                    stripeWebhookController.handleWebhookEvent(it)
                })
            }
        }
    }
}
