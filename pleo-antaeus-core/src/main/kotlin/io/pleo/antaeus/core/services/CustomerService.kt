/*
    Implements endpoints related to customers.
 */

package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.exceptions.CustomerNotCreatedException
import io.pleo.antaeus.core.exceptions.CustomerNotFoundException
import io.pleo.antaeus.data.AntaeusDal
import io.pleo.antaeus.models.Currency
import io.pleo.antaeus.models.Customer
import io.pleo.antaeus.models.CustomerStatus
import io.pleo.antaeus.models.CustomerUpdateSchema

class CustomerService(private val dal: AntaeusDal) {
    fun fetchAll(isDeleted: Boolean = false, status: CustomerStatus? = null): List<Customer> {
        return dal.fetchCustomers(isDeleted, status)
    }

    fun fetch(id: Int): Customer {
        return dal.fetchCustomer(id) ?: throw CustomerNotFoundException(id)
    }

    fun update(id: Int, updates: CustomerUpdateSchema): Customer {
        return dal.updateCustomer(id, updates) ?: throw CustomerNotFoundException(id)
    }

    fun create(currency: Currency): Customer {
        return dal.createCustomer(currency) ?: throw CustomerNotCreatedException()
    }
}
