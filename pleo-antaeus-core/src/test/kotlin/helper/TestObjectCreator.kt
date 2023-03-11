package helper

import io.pleo.antaeus.models.Currency
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import io.pleo.antaeus.models.Money
import java.math.BigDecimal

fun createInvoice(): Invoice =
    Invoice(
        id = 1,
        customerId = 1,
        amount = Money(value =  BigDecimal.ONE, currency = Currency.EUR),
        status = InvoiceStatus.PENDING,
        paidTimestamp = null
    )