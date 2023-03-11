package io.pleo.antaeus.data

import io.pleo.antaeus.models.Currency
import io.pleo.antaeus.models.InvoiceStatus
import io.pleo.antaeus.models.Money
import org.jetbrains.exposed.sql.*
import org.junit.jupiter.api.Test
import org.jetbrains.exposed.sql.SchemaUtils.create
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import java.io.File
import java.math.BigDecimal
import java.time.Clock
import java.time.LocalDateTime
import java.time.ZoneOffset

class AntaeusDalTest {

    private val dbFile: File = File.createTempFile("antaeus-test-db", ".sqlite")
    private var db = Database
        .connect(url = "jdbc:sqlite:${dbFile.absolutePath}",
            driver = "org.sqlite.JDBC")
    private lateinit var dal: AntaeusDal
    private val tables = arrayOf(InvoiceTable, CustomerTable)

    private val currentTime = LocalDateTime.of(2023, 3, 10, 14, 0, 0, 0)
        .toInstant(ZoneOffset.UTC)
    val testClock: Clock = Clock.fixed(currentTime, ZoneOffset.UTC)

    @BeforeEach
    fun setup(){
        transaction(db) {
            create(*tables)
        }

        dal = AntaeusDal(db)
    }

    @AfterEach
    fun dropTables(){
        transaction(db) {
            SchemaUtils.drop(*tables)
        }
    }

    @Test
    fun `test create customer will return new customer`() {
        val expectedCurrency = Currency.EUR
        val newCustomer = dal.createCustomer(currency = Currency.EUR)
        assertNotNull(newCustomer)
        assertEquals(expectedCurrency, newCustomer!!.currency)
    }

    @Test
    fun `test fetch customer will return customer when customer exists`() {
        val expectedCurrency = Currency.EUR
        val newCustomer = dal.createCustomer(currency = Currency.EUR)
        assertNotNull(newCustomer)
        assertEquals(expectedCurrency, newCustomer!!.currency)

        val fetchedCustomer = dal.fetchCustomer(id = newCustomer.id)
        assertNotNull(fetchedCustomer)
        assertEquals(expectedCurrency, fetchedCustomer!!.currency)
    }

    @Test
    fun `test fetch customer will null when customer doesnt exist`() {
        val fetchedCustomer = dal.fetchCustomer(id = 1)
        assertNull(fetchedCustomer)
    }

    @Test
    fun `test fetch customers will return a list of customers when customers exists`() {
        val currency = Currency.EUR
        dal.createCustomer(currency = currency)
        dal.createCustomer(currency = currency)

        val fetchedCustomers = dal.fetchCustomers()
        assertNotNull(fetchedCustomers)
        assertEquals(2, fetchedCustomers.size)
    }

    @Test
    fun `test fetch customers will return an empty list of customers when no customer exist`() {
        val fetchedCustomers = dal.fetchCustomers()
        assertTrue(fetchedCustomers.isEmpty())
    }

    @Test
    fun `test create invoice will return new invoice`() {
        val currency = Currency.EUR
        val value = BigDecimal.ONE
        val newCustomer = dal.createCustomer(currency = Currency.EUR)
        assertNotNull(newCustomer)
        assertEquals(newCustomer!!.currency, currency)

        val newInvoice = dal.createInvoice(amount = Money(value, currency), customer = newCustomer)
        assertNotNull(newInvoice)
        assertEquals(newInvoice!!.amount.value, value)
        assertEquals(newInvoice.amount.currency, currency)
        assertEquals(newInvoice.customerId, newCustomer.id)
        assertEquals(newInvoice.status, InvoiceStatus.PENDING)
    }

    @Test
    fun `test fetch invoice will return the invoice when it exists`() {
        val currency = Currency.EUR
        val value = BigDecimal.ONE
        val newCustomer = dal.createCustomer(currency = Currency.EUR)
        assertNotNull(newCustomer)
        assertEquals(newCustomer!!.currency, currency)

        val newInvoice = dal.createInvoice(amount = Money(value, currency), customer = newCustomer)
        assertNotNull(newInvoice)
        assertEquals(newInvoice!!.amount.value, value)
        assertEquals(newInvoice.amount.currency, currency)
        assertEquals(newInvoice.customerId, newCustomer.id)
        assertEquals(newInvoice.status, InvoiceStatus.PENDING)

        val fetchedInvoice = dal.fetchInvoice(id = newInvoice.id)
        assertNotNull(fetchedInvoice)
        assertEquals(fetchedInvoice!!.amount.value, value)
        assertEquals(fetchedInvoice.amount.currency, currency)
        assertEquals(fetchedInvoice.customerId, newCustomer.id)
        assertEquals(fetchedInvoice.status, InvoiceStatus.PENDING)
    }

    @Test
    fun `test fetch invoice will return null when it doesnt exist`() {
        val fetchedInvoice = dal.fetchInvoice(id = 1)
        assertNull(fetchedInvoice)
    }

    @Test
    fun `test fetch invoices will return a list of invoices when invoices exist`() {
        val currency = Currency.EUR
        val value = BigDecimal.ONE
        val newCustomer = dal.createCustomer(currency = Currency.EUR)
        assertNotNull(newCustomer)
        assertEquals(newCustomer!!.currency, currency)

        dal.createInvoice(amount = Money(value, currency), customer = newCustomer)
        dal.createInvoice(amount = Money(value, currency), customer = newCustomer)

        val fetchedInvoices = dal.fetchInvoices()
        assertNotNull(fetchedInvoices)
        assertEquals(2, fetchedInvoices.size)
    }

    @Test
    fun `test fetch invoices will return an empty list of invoices when no invoices exist`() {
        val fetchedInvoices = dal.fetchInvoices()
        assertNotNull(fetchedInvoices)
        assertTrue(fetchedInvoices.isEmpty())
    }

    @Test
    fun `test fetch pending invoices will return a list of invoices when there are pening invoices`() {
        val currency = Currency.EUR
        val value = BigDecimal.ONE
        val newCustomer = dal.createCustomer(currency = Currency.EUR)
        assertNotNull(newCustomer)
        assertEquals(newCustomer!!.currency, currency)

        dal.createInvoice(amount = Money(value, currency), customer = newCustomer)
        dal.createInvoice(amount = Money(value, currency), customer = newCustomer)
        dal.createInvoice(amount = Money(value, currency), customer = newCustomer, status = InvoiceStatus.PAID)

        val fetchedInvoices = dal.fetchInvoicesByStatus(status = InvoiceStatus.PENDING.toString())
        assertNotNull(fetchedInvoices)
        assertEquals(2, fetchedInvoices.size)
    }

    @Test
    fun `test fetch pending invoices will return an empty list when there are no pending invoices`() {
        val currency = Currency.EUR
        val value = BigDecimal.ONE
        val newCustomer = dal.createCustomer(currency = Currency.EUR)
        assertNotNull(newCustomer)
        assertEquals(newCustomer!!.currency, currency)

        dal.createInvoice(amount = Money(value, currency), customer = newCustomer, status = InvoiceStatus.PAID)
        dal.createInvoice(amount = Money(value, currency), customer = newCustomer, status = InvoiceStatus.PAID)

        val fetchedInvoices = dal.fetchInvoicesByStatus(status = InvoiceStatus.PENDING.toString())
        assertNotNull(fetchedInvoices)
        assertTrue(fetchedInvoices.isEmpty())
    }

    @Test
    fun `test fetch paid invoices will return a list of invoices when there are paid invoices`() {
        val currency = Currency.EUR
        val value = BigDecimal.ONE
        val newCustomer = dal.createCustomer(currency = Currency.EUR)
        assertNotNull(newCustomer)
        assertEquals(newCustomer!!.currency, currency)

        dal.createInvoice(amount = Money(value, currency), customer = newCustomer)
        dal.createInvoice(amount = Money(value, currency), customer = newCustomer)
        dal.createInvoice(amount = Money(value, currency), customer = newCustomer, status = InvoiceStatus.PAID)

        val fetchedInvoices = dal.fetchInvoicesByStatus(status = InvoiceStatus.PAID.toString())
        assertNotNull(fetchedInvoices)
        assertEquals(1, fetchedInvoices.size)
    }

    @Test
    fun `test fetch paid invoices will return an empty list when there are no paid invoices`() {
        val currency = Currency.EUR
        val value = BigDecimal.ONE
        val newCustomer = dal.createCustomer(currency = Currency.EUR)
        assertNotNull(newCustomer)
        assertEquals(newCustomer!!.currency, currency)

        dal.createInvoice(amount = Money(value, currency), customer = newCustomer, status = InvoiceStatus.PENDING)
        dal.createInvoice(amount = Money(value, currency), customer = newCustomer, status = InvoiceStatus.PENDING)

        val fetchedInvoices = dal.fetchInvoicesByStatus(status = InvoiceStatus.PAID.toString())
        assertNotNull(fetchedInvoices)
        assertTrue(fetchedInvoices.isEmpty())
    }

    @Test
    fun `test bulk update invoices to paid will update pending invoices to paid`() {
        val currency = Currency.EUR
        val value = BigDecimal.ONE
        val newCustomer = dal.createCustomer(currency = Currency.EUR)
        assertNotNull(newCustomer)
        assertEquals(newCustomer!!.currency, currency)

        val invoice1 = dal.createInvoice(amount = Money(value, currency), customer = newCustomer)
        val invoice2 = dal.createInvoice(amount = Money(value, currency), customer = newCustomer)
        val invoice3 = dal.createInvoice(amount = Money(value, currency), customer = newCustomer)

        val fetchedInvoices1 = dal.fetchInvoicesByStatus(status =InvoiceStatus.PENDING.toString())
        assertNotNull(fetchedInvoices1)
        assertEquals(3, fetchedInvoices1.size)

        dal.bulkUpdateInvoicesToPaid(listOf(invoice1!!.id, invoice2!!.id), testClock.instant())

        val fetchedInvoices2 = dal.fetchInvoices()
        assertNotNull(fetchedInvoices2)
        assertEquals(3, fetchedInvoices2.size)
        assertEquals(2, fetchedInvoices2.filter { it.status == InvoiceStatus.PAID }.size)
        assertEquals(1, fetchedInvoices2.filter { it.status == InvoiceStatus.PENDING }.size)
        assertTrue(fetchedInvoices2.filter { it.status == InvoiceStatus.PAID && it.paidTimestamp == testClock.instant()}.any { it.id == invoice1.id })
        assertTrue(fetchedInvoices2.filter { it.status == InvoiceStatus.PAID && it.paidTimestamp == testClock.instant()}.any { it.id == invoice2.id })
        assertTrue(fetchedInvoices2.filter { it.status == InvoiceStatus.PENDING && it.paidTimestamp == null}.any { it.id == invoice3!!.id })
    }
}
