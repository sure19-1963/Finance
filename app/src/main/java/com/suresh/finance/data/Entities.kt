package com.suresh.finance.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class TxType { INCOME, EXPENSE }

enum class DebtKind(val label: String) {
    CREDIT_CARD("Credit card"),
    LOAN("Loan"),
    MORTGAGE("Mortgage"),
    HOME_PLAN("Home payment plan"),
}

enum class BillKind(val label: String) {
    BILL("Bill"),
    EMI("Loan EMI"),
    RENT("Rent I pay"),
    TUITION("Tuition / hostel"),
    OTHER("Other"),
}

enum class AssetKind(val label: String) {
    CASH("Cash"),
    BANK("Bank account"),
    INVESTMENT("Investment"),
    GOLD("Gold"),
    VEHICLE("Vehicle"),
    OTHER("Other"),
}

/** Income or expense category. [systemKey] marks built-in categories the app links to automatically. */
@Entity(tableName = "categories")
data class Category(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: TxType,
    val icon: String,
    val color: Long,
    val archived: Boolean = false,
    val systemKey: String? = null,
)

/** One income or expense entry. [date] is a LocalDate epoch day. */
@Entity(
    tableName = "transactions",
    indices = [Index("date"), Index("categoryId"), Index("propertyId"), Index("debtId")],
)
data class Txn(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: TxType,
    val amount: Double,
    val categoryId: Long,
    val date: Long,
    val note: String = "",
    val propertyId: Long? = null,
    val debtId: Long? = null,
)

/** Monthly spending limit for one expense category. */
@Entity(tableName = "budgets")
data class Budget(
    @PrimaryKey val categoryId: Long,
    val monthlyLimit: Double,
)

/** A property you own and rent out (or plan to). Vacant when it has no active lease. */
@Entity(tableName = "properties")
data class Property(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val address: String = "",
    val kind: String = "Apartment",
    val expectedRent: Double = 0.0,
    val marketValue: Double = 0.0,
    val notes: String = "",
)

/** A rental agreement with a tenant. */
@Entity(tableName = "leases", indices = [Index("propertyId")])
data class Lease(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val propertyId: Long,
    val tenantName: String,
    val tenantPhone: String = "",
    val startDate: Long,
    val endDate: Long? = null,
    val monthlyRent: Double,
    val deposit: Double = 0.0,
    val rentDueDay: Int = 1,
    val chequeCount: Int = 0,
    val notes: String = "",
    val active: Boolean = true,
)

/** Credit card, loan, mortgage or home payment plan. */
@Entity(tableName = "debts")
data class Debt(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val kind: DebtKind,
    val lender: String = "",
    val originalAmount: Double,
    val balance: Double,
    val interestRate: Double = 0.0,
    val monthlyPayment: Double = 0.0,
    val dueDay: Int = 1,
    val creditLimit: Double = 0.0,
    val startDate: Long,
    val notes: String = "",
    val closed: Boolean = false,
)

/** A recurring monthly bill. [lastPaidYm] is year*100+month of the last month it was marked paid. */
@Entity(tableName = "bills")
data class Bill(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val amount: Double,
    val dueDay: Int,
    val kind: BillKind = BillKind.BILL,
    val categoryId: Long? = null,
    val enabled: Boolean = true,
    val lastPaidYm: Int = 0,
)

/** Something you own, for the net worth chart. */
@Entity(tableName = "assets")
data class Asset(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val kind: AssetKind,
    val value: Double,
    val notes: String = "",
)
