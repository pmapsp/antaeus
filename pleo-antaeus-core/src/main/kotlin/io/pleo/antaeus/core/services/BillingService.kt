package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.exceptions.*
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.models.PaymentFailedReason
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
        //starting in the next month
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
        val failedPaidInvoices = mutableMapOf<Int, PaymentFailedReason>()
        var tries = 0

        while(pendingInvoices.isNotEmpty() && tries < maximumNumberOfTries) {
            if(tries > 0){
                //waiting 1 second before trying again
                Thread.sleep(1000)
            }
            pendingInvoices.removeIf { invoice ->
                try {
                    if (paymentProvider.charge(invoice)) {
                        successfullyPaidInvoicesIds.add(invoice.id)
                        failedPaidInvoices.remove(invoice.id)
                        true
                    } else {
                        failedPaidInvoices[invoice.id] = PaymentFailedReason.INSUFFICIENT_BALANCE
                        true
                    }
                } catch (e: Exception) {
                    val paymentFailedReason = when (e) {
                        is CustomerNotFoundException -> PaymentFailedReason.CUSTOMER_NOT_FOUND
                        is CurrencyMismatchException -> PaymentFailedReason.CURRENCY_MISMATCH
                        is NetworkException -> PaymentFailedReason.NETWORK_FAILURE
                        else -> PaymentFailedReason.UNKNOWN
                    }
                    failedPaidInvoices[invoice.id] = paymentFailedReason

                    paymentFailedReason == PaymentFailedReason.CUSTOMER_NOT_FOUND ||
                        paymentFailedReason == PaymentFailedReason.CURRENCY_MISMATCH
                }
            }
            tries++
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
