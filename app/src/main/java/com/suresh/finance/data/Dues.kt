package com.suresh.finance.data

import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

enum class DueSource { BILL, DEBT, RENT }

/** Something due this month: a bill to pay, a debt payment, or rent to collect. */
data class DueItem(
    val key: String,
    val source: DueSource,
    val sourceId: Long,
    val title: String,
    val subtitle: String,
    val amount: Double,
    val dueDate: LocalDate,
    val done: Boolean,
) {
    fun daysUntil(today: LocalDate): Long = ChronoUnit.DAYS.between(today, dueDate)
}

object Dues {
    /**
     * Everything due in [month]. A debt counts as paid when a payment was recorded against it that
     * month, and rent counts as collected when that month's rental income covers the lease rent.
     */
    fun forMonth(
        month: YearMonth,
        bills: List<Bill>,
        debts: List<Debt>,
        leases: List<Lease>,
        properties: List<Property>,
        monthTxns: List<Txn>,
    ): List<DueItem> {
        val ym = FinanceMath.ym(month)
        val propertyNames = properties.associate { it.id to it.name }
        val items = mutableListOf<DueItem>()

        bills.filter { it.enabled }.forEach { b ->
            items += DueItem(
                key = "bill_${b.id}_$ym",
                source = DueSource.BILL,
                sourceId = b.id,
                title = b.title,
                subtitle = b.kind.label,
                amount = b.amount,
                dueDate = FinanceMath.dueDate(month, b.dueDay),
                done = b.lastPaidYm >= ym,
            )
        }

        debts.filter { !it.closed && it.monthlyPayment > 0 }.forEach { d ->
            val paid = monthTxns.any { it.debtId == d.id }
            items += DueItem(
                key = "debt_${d.id}_$ym",
                source = DueSource.DEBT,
                sourceId = d.id,
                title = d.name,
                subtitle = "${d.kind.label} payment",
                amount = d.monthlyPayment,
                dueDate = FinanceMath.dueDate(month, d.dueDay),
                done = paid,
            )
        }

        leases.filter { it.active }.forEach { l ->
            val collected = monthTxns
                .filter { it.type == TxType.INCOME && it.propertyId == l.propertyId }
                .sumOf { it.amount }
            items += DueItem(
                key = "rent_${l.id}_$ym",
                source = DueSource.RENT,
                sourceId = l.id,
                title = "Rent from ${l.tenantName}",
                subtitle = propertyNames[l.propertyId] ?: "Rental",
                amount = l.monthlyRent,
                dueDate = FinanceMath.dueDate(month, l.rentDueDay),
                done = collected + 0.005 >= l.monthlyRent,
            )
        }

        return items.sortedWith(compareBy<DueItem> { it.done }.thenBy { it.dueDate })
    }
}
