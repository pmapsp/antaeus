/*
    Implements endpoints related to invoices.
 */

package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.exceptions.InvoiceNotFoundException
import io.pleo.antaeus.data.AntaeusDal
import io.pleo.antaeus.models.Invoice
import java.time.Instant

class InvoiceService(private val dal: AntaeusDal) {
    fun fetchAll(status: String?): List<Invoice> {
        if(status != null) {
            return dal.fetchInvoicesByStatus(status = status)
        }
        return dal.fetchInvoices()
    }

    fun fetch(id: Int): Invoice {
        return dal.fetchInvoice(id) ?: throw InvoiceNotFoundException(id)
    }

    fun bulkUpdateInvoicesToPaid(successfullyPaidInvoicesIds: List<Int>, paidTimestamp: Instant){
        dal.bulkUpdateInvoicesToPaid(invoiceIds = successfullyPaidInvoicesIds, paidTimestampInstant = paidTimestamp)
    }
}
