package com.suresh.finance.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class FinanceMathTest {

    @Test
    fun emiMatchesStandardFormula() {
        // 100,000 over 12 months at 12% a year is about 8,884.88 a month.
        assertEquals(8884.88, FinanceMath.emi(100_000.0, 12.0, 12), 0.01)
        assertEquals(1000.0, FinanceMath.emi(12_000.0, 0.0, 12), 0.0001)
    }

    @Test
    fun loanPaymentTakesInterestFirst() {
        // 1% monthly interest on 10,000 is 100, so a 1,000 payment cuts 900 off the principal.
        val newBalance = FinanceMath.applyPayment(10_000.0, 12.0, 1_000.0, DebtKind.LOAN)
        assertEquals(9_100.0, newBalance, 0.0001)
    }

    @Test
    fun creditCardPaymentReducesBalanceDirectly() {
        assertEquals(4_000.0, FinanceMath.applyPayment(5_000.0, 36.0, 1_000.0, DebtKind.CREDIT_CARD), 0.0001)
        assertEquals(0.0, FinanceMath.applyPayment(500.0, 36.0, 1_000.0, DebtKind.CREDIT_CARD), 0.0001)
    }

    @Test
    fun monthsToPayoffMatchesEmiTerm() {
        val emi = FinanceMath.emi(100_000.0, 12.0, 24)
        assertEquals(24, FinanceMath.monthsToPayoff(100_000.0, 12.0, emi))
        assertEquals(10, FinanceMath.monthsToPayoff(10_000.0, 0.0, 1_000.0))
        assertEquals(0, FinanceMath.monthsToPayoff(0.0, 10.0, 100.0))
    }

    @Test
    fun paymentBelowInterestNeverPaysOff() {
        assertNull(FinanceMath.monthsToPayoff(10_000.0, 24.0, 150.0))
        assertNull(FinanceMath.remainingInterest(10_000.0, 24.0, 150.0))
    }

    @Test
    fun remainingInterestIsPositiveForInterestBearingLoan() {
        val emi = FinanceMath.emi(100_000.0, 12.0, 12)
        val interest = FinanceMath.remainingInterest(100_000.0, 12.0, emi)!!
        assertEquals(emi * 12 - 100_000.0, interest, 1.0)
    }

    @Test
    fun dueDateClampsToMonthEnd() {
        assertEquals(LocalDate.of(2026, 2, 28), FinanceMath.dueDate(YearMonth.of(2026, 2), 31))
        assertEquals(LocalDate.of(2026, 3, 1), FinanceMath.dueDate(YearMonth.of(2026, 3), 0))
    }

    @Test
    fun progressIsBetweenZeroAndOne() {
        assertEquals(0.25f, FinanceMath.progress(1_000.0, 750.0), 0.0001f)
        assertEquals(1f, FinanceMath.progress(1_000.0, -5.0), 0.0001f)
        assertEquals(0f, FinanceMath.progress(0.0, 100.0), 0.0001f)
    }

    @Test
    fun duesMarkPaidItems() {
        val month = YearMonth.of(2026, 10)
        val bill = Bill(id = 1, title = "School fees", amount = 2_000.0, dueDay = 5, lastPaidYm = 202610)
        val unpaidBill = Bill(id = 2, title = "Phone", amount = 200.0, dueDay = 20)
        val debt = Debt(id = 3, name = "Car loan", kind = DebtKind.LOAN, originalAmount = 50_000.0, balance = 30_000.0,
            monthlyPayment = 1_500.0, dueDay = 10, startDate = 0)
        val lease = Lease(id = 4, propertyId = 9, tenantName = "Ali", startDate = 0, monthlyRent = 5_000.0, rentDueDay = 1)
        val day = month.atDay(3).toEpochDay()
        val txns = listOf(
            Txn(type = TxType.EXPENSE, amount = 1_500.0, categoryId = 1, date = day, debtId = 3),
            Txn(type = TxType.INCOME, amount = 4_000.0, categoryId = 2, date = day, propertyId = 9),
        )
        val dues = Dues.forMonth(month, listOf(bill, unpaidBill), listOf(debt), listOf(lease), emptyList(), txns)
        val byKey = dues.associateBy { it.source to it.sourceId }

        assertTrue(byKey.getValue(DueSource.BILL to 1L).done)
        assertFalse(byKey.getValue(DueSource.BILL to 2L).done)
        assertTrue(byKey.getValue(DueSource.DEBT to 3L).done)
        // Only 4,000 of 5,000 rent came in, so it is still outstanding.
        assertFalse(byKey.getValue(DueSource.RENT to 4L).done)
        // Outstanding items come first.
        assertFalse(dues.first().done)
    }
}
