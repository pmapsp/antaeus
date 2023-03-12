package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.exceptions.*
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.models.FailedReason
import io.pleo.antaeus.models.InvoiceStatus
import io.pleo.antaeus.models.PaidPendingInvoicesResponse
import mu.KotlinLogging
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.util.*

class BillingService(
    private val paymentProvider: PaymentProvider,
    private val invoiceService: InvoiceService
) {

    private val logger = KotlinLogging.logger {}

    fun schedulePendingInvoicePayments(maximumNumberOfTries: Int){
        val timer = Timer()
        val firstOfMonth = LocalDate.now().plusMonths(1).withDayOfMonth(1)
        val firstOfMonthAtMidnight = LocalDateTime.of(firstOfMonth, LocalTime.MIDNIGHT)
        val delay = firstOfMonthAtMidnight.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() - System.currentTimeMillis()
        val every27Days = 1000L * 60L * 60L * 24L * 27L

        timer.schedule(object : TimerTask() {
            override fun run() {
                val payPendingInvoicesResponse = payPendingInvoices(maximumNumberOfTries = maximumNumberOfTries)
                logger.info("The following invoices were processed for payment: $payPendingInvoicesResponse")
            }
        }, delay, every27Days)
    }

    private fun payPendingInvoices(maximumNumberOfTries: Int): PaidPendingInvoicesResponse{
        val pendingInvoices =
            invoiceService.fetchAll(status = InvoiceStatus.PENDING.toString()).toMutableSet()
        val successfullyPaidInvoicesIds = mutableListOf<Int>()
        val failedPaidInvoices = mutableMapOf<Int, FailedReason>()
        var tries = 0

        while(pendingInvoices.isNotEmpty() && tries++ < maximumNumberOfTries) {
            if(tries > 0){
                //waiting 1 second before trying again
                Thread.sleep(1000)
            }

            pendingInvoices.forEach { invoice ->
                try {
                    if (paymentProvider.charge(invoice)) {
                        successfullyPaidInvoicesIds.add(invoice.id)
                        failedPaidInvoices.remove(invoice.id)
                    } else {
                        failedPaidInvoices[invoice.id] = FailedReason.INSUFFICIENT_BALANCE
                    }
                } catch (e: Exception) {
                    val failedReason = when (e) {
                        is CustomerNotFoundException -> FailedReason.CUSTOMER_NOT_FOUND
                        is CurrencyMismatchException -> FailedReason.CURRENCY_MISMATCH
                        is NetworkException -> FailedReason.NETWORK_FAILURE
                        else -> FailedReason.UNKNOWN
                    }
                    failedPaidInvoices[invoice.id] = failedReason
                }
            }
            pendingInvoices.removeIf { successfullyPaidInvoicesIds.contains(it.id) }
        }

        if(successfullyPaidInvoicesIds.isNotEmpty()) {
            invoiceService.bulkUpdateInvoicesToPaid(
                successfullyPaidInvoicesIds = successfullyPaidInvoicesIds,
                paidTimestamp = Instant.now()
            )
        }

        return PaidPendingInvoicesResponse(
            successfullyPaidInvoices = successfullyPaidInvoicesIds,
            failedPaidInvoices = failedPaidInvoices
        )
    }
}
