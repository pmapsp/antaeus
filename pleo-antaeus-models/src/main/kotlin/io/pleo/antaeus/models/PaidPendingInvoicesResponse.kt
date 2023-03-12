package io.pleo.antaeus.models

data class PaidPendingInvoicesResponse(
    val successfullyPaidInvoices: List<Int>,
    val failedPaidInvoices: Map<Int, FailedReason>
)

enum class FailedReason {
    INSUFFICIENT_BALANCE,
    CUSTOMER_NOT_FOUND,
    CURRENCY_MISMATCH,
    NETWORK_FAILURE,
    UNKNOWN
}
