package io.pleo.antaeus.models

data class PaidPendingInvoicesResponse(
    val successfullyPaidInvoices: List<Int>,
    val failedPaidInvoices: Map<Int, PaymentFailedReason>
)

enum class PaymentFailedReason {
    INSUFFICIENT_BALANCE,
    CUSTOMER_NOT_FOUND,
    CURRENCY_MISMATCH,
    NETWORK_FAILURE,
    UNKNOWN
}
