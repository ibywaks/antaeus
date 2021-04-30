/*
    Implements endpoints related to customers.
 */

package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.exceptions.CustomerNotFoundException
import io.pleo.antaeus.data.AntaeusDal
import io.pleo.antaeus.models.Customer
import io.pleo.antaeus.models.CustomerStatus

class CustomerService(private val dal: AntaeusDal) {
    fun fetchAll(isDeleted: Boolean = false, status: CustomerStatus?): List<Customer> {
        return dal.fetchCustomers(isDeleted, status)
    }

    fun fetch(id: Int): Customer {
        return dal.fetchCustomer(id) ?: throw CustomerNotFoundException(id)
    }
}
