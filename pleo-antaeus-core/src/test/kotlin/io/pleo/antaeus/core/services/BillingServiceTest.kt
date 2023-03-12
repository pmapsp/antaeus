package io.pleo.antaeus.core.services

import helper.createInvoice
import io.mockk.*
import io.pleo.antaeus.core.exceptions.CurrencyMismatchException
import io.pleo.antaeus.core.exceptions.CustomerNotFoundException
import io.pleo.antaeus.core.exceptions.NetworkException
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.models.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import java.lang.NullPointerException
import java.util.*

class BillingServiceTest {

    @Test
    fun `test payPendingInvoices will return successful invoices`() {
        val paymentProviderMock = mockk<PaymentProvider> {
            every { charge(any()) } returns true
        }

        val invoiceServiceMock = mockk<InvoiceService> {
            every { fetchAll(InvoiceStatus.PENDING.toString()) } returns
                listOf(
                    createInvoice(),
                    createInvoice().copy(id = 2)
                )

            every { bulkUpdateInvoicesToPaid(any(), any()) } just runs
        }

        val billingService = BillingService(paymentProvider = paymentProviderMock, invoiceService = invoiceServiceMock)
        val method = billingService::class.java.getDeclaredMethod("payPendingInvoices", Int::class.javaPrimitiveType)
        method.isAccessible = true

        val result = method.invoke(billingService,5) as PaidPendingInvoicesResponse
        assertTrue(result.failedPaidInvoices.isEmpty())
        assertEquals(2, result.successfullyPaidInvoices.size)

        //we are trying just 1 time for each invoice
        verify(exactly = 2) { paymentProviderMock.charge(any()) }
        verify(exactly = 1) { invoiceServiceMock.fetchAll(any()) }
        verify(exactly = 1) { invoiceServiceMock.bulkUpdateInvoicesToPaid(any(), any()) }
    }

    @Test
    fun `test payPendingInvoices will return failed invoices with reason insufficient balance`() {
        val paymentProviderMock = mockk<PaymentProvider> {
            every { charge(any()) } returns false
        }

        val invoiceServiceMock = mockk<InvoiceService> {
            every { fetchAll(InvoiceStatus.PENDING.toString()) } returns
                listOf(
                    createInvoice(),
                    createInvoice().copy(id = 2)
                )
        }

        val billingService = BillingService(paymentProvider = paymentProviderMock, invoiceService = invoiceServiceMock)
        val method = billingService::class.java.getDeclaredMethod("payPendingInvoices", Int::class.javaPrimitiveType)
        method.isAccessible = true

        val result = method.invoke(billingService, 5) as PaidPendingInvoicesResponse
        assertEquals(2, result.failedPaidInvoices.size)
        assertEquals(FailedReason.INSUFFICIENT_BALANCE, result.failedPaidInvoices[1])
        assertTrue(result.successfullyPaidInvoices.isEmpty())

        //we are trying 5 times for each invoice
        verify(exactly = 10) { paymentProviderMock.charge(any()) }
        verify(exactly = 1) { invoiceServiceMock.fetchAll(any()) }
        verify(exactly = 0) { invoiceServiceMock.bulkUpdateInvoicesToPaid(any(), any()) }
    }

    @Test
    fun `test payPendingInvoices will return failed invoices with reason customer not found`() {
        val paymentProviderMock = mockk<PaymentProvider> {
            every { charge(any()) } throws CustomerNotFoundException(1)
        }

        val invoiceServiceMock = mockk<InvoiceService> {
            every { fetchAll(InvoiceStatus.PENDING.toString()) } returns
                listOf(
                    createInvoice(),
                    createInvoice().copy(id = 2)
                )
        }

        val billingService = BillingService(paymentProvider = paymentProviderMock, invoiceService = invoiceServiceMock)
        val method = billingService::class.java.getDeclaredMethod("payPendingInvoices", Int::class.javaPrimitiveType)
        method.isAccessible = true

        val result = method.invoke(billingService, 5) as PaidPendingInvoicesResponse
        assertEquals(2, result.failedPaidInvoices.size)
        assertEquals(FailedReason.CUSTOMER_NOT_FOUND, result.failedPaidInvoices[1])
        assertTrue(result.successfullyPaidInvoices.isEmpty())

        //we are trying 5 times for each invoice
        verify(exactly = 10) { paymentProviderMock.charge(any()) }
        verify(exactly = 1) { invoiceServiceMock.fetchAll(any()) }
        verify(exactly = 0) { invoiceServiceMock.bulkUpdateInvoicesToPaid(any(), any()) }
    }

    @Test
    fun `test payPendingInvoices will return failed invoices with reason currency mismatch`() {
        val paymentProviderMock = mockk<PaymentProvider> {
            every { charge(any()) } throws CurrencyMismatchException(1,1)
        }

        val invoiceServiceMock = mockk<InvoiceService> {
            every { fetchAll(InvoiceStatus.PENDING.toString()) } returns
                listOf(
                    createInvoice(),
                    createInvoice().copy(id = 2)
                )
        }

        val billingService = BillingService(paymentProvider = paymentProviderMock, invoiceService = invoiceServiceMock)
        val method = billingService::class.java.getDeclaredMethod("payPendingInvoices", Int::class.javaPrimitiveType)
        method.isAccessible = true

        val result = method.invoke(billingService, 5) as PaidPendingInvoicesResponse
        assertEquals(2, result.failedPaidInvoices.size)
        assertEquals(FailedReason.CURRENCY_MISMATCH, result.failedPaidInvoices[1])
        assertTrue(result.successfullyPaidInvoices.isEmpty())

        //we are trying 5 times for each invoice
        verify(exactly = 10) { paymentProviderMock.charge(any()) }
        verify(exactly = 1) { invoiceServiceMock.fetchAll(any()) }
        verify(exactly = 0) { invoiceServiceMock.bulkUpdateInvoicesToPaid(any(), any()) }
    }

    @Test
    fun `test payPendingInvoices will return failed invoices with reason network failure`() {
        val paymentProviderMock = mockk<PaymentProvider> {
            every { charge(any()) } throws NetworkException()
        }

        val invoiceServiceMock = mockk<InvoiceService> {
            every { fetchAll(InvoiceStatus.PENDING.toString()) } returns
                listOf(
                    createInvoice(),
                    createInvoice().copy(id = 2)
                )
        }

        val billingService = BillingService(paymentProvider = paymentProviderMock, invoiceService = invoiceServiceMock)
        val method = billingService::class.java.getDeclaredMethod("payPendingInvoices", Int::class.javaPrimitiveType)
        method.isAccessible = true

        val result = method.invoke(billingService, 5) as PaidPendingInvoicesResponse
        assertEquals(2, result.failedPaidInvoices.size)
        assertEquals(FailedReason.NETWORK_FAILURE, result.failedPaidInvoices[1])
        assertTrue(result.successfullyPaidInvoices.isEmpty())

        //we are trying 5 times for each invoice
        verify(exactly = 10) { paymentProviderMock.charge(any()) }
        verify(exactly = 1) { invoiceServiceMock.fetchAll(any()) }
        verify(exactly = 0) { invoiceServiceMock.bulkUpdateInvoicesToPaid(any(), any()) }
    }

    @Test
    fun `test payPendingInvoices will return failed invoices with reason unknown`() {
        val paymentProviderMock = mockk<PaymentProvider> {
            every { charge(any()) } throws NullPointerException()
        }

        val invoiceServiceMock = mockk<InvoiceService> {
            every { fetchAll(InvoiceStatus.PENDING.toString()) } returns
                listOf(
                    createInvoice(),
                    createInvoice().copy(id = 2)
                )
        }

        val billingService = BillingService(paymentProvider = paymentProviderMock, invoiceService = invoiceServiceMock)
        val method = billingService::class.java.getDeclaredMethod("payPendingInvoices", Int::class.javaPrimitiveType)
        method.isAccessible = true

        val result = method.invoke(billingService, 5) as PaidPendingInvoicesResponse
        assertEquals(2, result.failedPaidInvoices.size)
        assertEquals(FailedReason.UNKNOWN, result.failedPaidInvoices[1])
        assertTrue(result.successfullyPaidInvoices.isEmpty())

        //we are trying 5 times for each invoice
        verify(exactly = 10) { paymentProviderMock.charge(any()) }
        verify(exactly = 1) { invoiceServiceMock.fetchAll(any()) }
        verify(exactly = 0) { invoiceServiceMock.bulkUpdateInvoicesToPaid(any(), any()) }
    }
}
