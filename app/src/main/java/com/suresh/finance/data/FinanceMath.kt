package com.suresh.finance.data

import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.ceil
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min

/** Plain calculations with no Android dependencies, so they can be unit tested. */
object FinanceMath {

    /** Interest charged for one month on [balance] at [annualRatePct] percent per year. */
    fun monthlyInterest(balance: Double, annualRatePct: Double): Double =
        balance * annualRatePct / 1200.0

    /**
     * New balance after a payment. For loans, mortgages and payment plans the month's interest is taken
     * out of the payment first and the rest reduces the principal. A credit card payment reduces the
     * balance directly, because card interest shows up on the statement balance you enter.
     */
    fun applyPayment(balance: Double, annualRatePct: Double, payment: Double, kind: DebtKind): Double {
        val reduction = if (kind == DebtKind.CREDIT_CARD) payment
        else payment - monthlyInterest(balance, annualRatePct)
        return max(0.0, balance - reduction)
    }

    /** Months until paid off at a fixed monthly payment, or null if the payment never clears it. */
    fun monthsToPayoff(balance: Double, annualRatePct: Double, monthlyPayment: Double): Int? {
        if (balance <= 0.0) return 0
        if (monthlyPayment <= 0.0) return null
        val r = annualRatePct / 1200.0
        if (r == 0.0) return ceil(balance / monthlyPayment).toInt()
        if (monthlyPayment <= balance * r) return null
        val n = -ln(1 - r * balance / monthlyPayment) / ln(1 + r)
        return ceil(n - 1e-9).toInt()
    }

    /** Interest still to be paid if the balance is cleared at the given monthly payment. */
    fun remainingInterest(balance: Double, annualRatePct: Double, monthlyPayment: Double): Double? {
        val months = monthsToPayoff(balance, annualRatePct, monthlyPayment) ?: return null
        var b = balance
        var interest = 0.0
        repeat(months) {
            val i = monthlyInterest(b, annualRatePct)
            interest += i
            b = max(0.0, b + i - monthlyPayment)
        }
        return interest
    }

    /** Standard EMI for a loan of [principal] over [months] months. */
    fun emi(principal: Double, annualRatePct: Double, months: Int): Double {
        if (months <= 0) return 0.0
        val r = annualRatePct / 1200.0
        if (r == 0.0) return principal / months
        val f = Math.pow(1 + r, months.toDouble())
        return principal * r * f / (f - 1)
    }

    /** Share of the original amount already repaid, 0..1. */
    fun progress(original: Double, balance: Double): Float =
        if (original <= 0.0) 0f else ((original - balance) / original).coerceIn(0.0, 1.0).toFloat()

    /** The due date for a monthly [day] within [month], clamped to the month's last day. */
    fun dueDate(month: YearMonth, day: Int): LocalDate =
        month.atDay(min(max(day, 1), month.lengthOfMonth()))

    fun ym(month: YearMonth): Int = month.year * 100 + month.monthValue
}
