package com.suresh.finance.data

import androidx.room.withTransaction
import java.time.LocalDate
import java.time.YearMonth

/** Database access plus the few actions that touch more than one table. */
class FinanceRepository(val db: AppDatabase, val settings: SettingsStore) {
    val categories = db.categoryDao()
    val txns = db.txnDao()
    val budgets = db.budgetDao()
    val properties = db.propertyDao()
    val leases = db.leaseDao()
    val debts = db.debtDao()
    val bills = db.billDao()
    val assets = db.assetDao()

    fun monthTxns(month: YearMonth) =
        txns.inRange(month.atDay(1).toEpochDay(), month.atEndOfMonth().toEpochDay())

    private suspend fun categoryIdFor(key: String): Long =
        (categories.bySystemKey(key) ?: categories.bySystemKey("misc"))?.id ?: 0L

    /** Records a payment as an expense and lowers the debt's balance. */
    suspend fun recordDebtPayment(debt: Debt, amount: Double, date: LocalDate, note: String = "") {
        val key = when (debt.kind) {
            DebtKind.CREDIT_CARD -> "credit_card"
            DebtKind.LOAN -> "loan"
            DebtKind.MORTGAGE -> "mortgage"
            DebtKind.HOME_PLAN -> "home_plan"
        }
        val categoryId = categoryIdFor(key)
        db.withTransaction {
            txns.upsert(
                Txn(
                    type = TxType.EXPENSE,
                    amount = amount,
                    categoryId = categoryId,
                    date = date.toEpochDay(),
                    note = note.ifBlank { "Payment: ${debt.name}" },
                    debtId = debt.id,
                ),
            )
            val newBalance = FinanceMath.applyPayment(debt.balance, debt.interestRate, amount, debt.kind)
            debts.upsert(debt.copy(balance = newBalance))
        }
    }

    /** Records rent received for a lease as rental income linked to its property. */
    suspend fun recordRent(lease: Lease, amount: Double, date: LocalDate, note: String = "") {
        txns.upsert(
            Txn(
                type = TxType.INCOME,
                amount = amount,
                categoryId = categoryIdFor("rental_income"),
                date = date.toEpochDay(),
                note = note.ifBlank { "Rent: ${lease.tenantName}" },
                propertyId = lease.propertyId,
            ),
        )
    }

    /** Marks a bill paid for the month of [date] and records it as an expense. */
    suspend fun markBillPaid(bill: Bill, date: LocalDate) {
        val categoryId = bill.categoryId ?: categoryIdFor("misc")
        db.withTransaction {
            txns.upsert(
                Txn(
                    type = TxType.EXPENSE,
                    amount = bill.amount,
                    categoryId = categoryId,
                    date = date.toEpochDay(),
                    note = bill.title,
                ),
            )
            bills.upsert(bill.copy(lastPaidYm = FinanceMath.ym(YearMonth.from(date))))
        }
    }

    suspend fun deleteProperty(property: Property) {
        db.withTransaction {
            leases.deleteForProperty(property.id)
            properties.delete(property)
        }
    }
}
