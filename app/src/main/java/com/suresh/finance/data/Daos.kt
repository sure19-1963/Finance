package com.suresh.finance.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY type, name")
    fun all(): Flow<List<Category>>

    @Query("SELECT * FROM categories")
    suspend fun allOnce(): List<Category>

    @Query("SELECT * FROM categories WHERE systemKey = :key LIMIT 1")
    suspend fun bySystemKey(key: String): Category?

    @Upsert
    suspend fun upsert(category: Category): Long
}

@Dao
interface TxnDao {
    @Query("SELECT * FROM transactions WHERE date BETWEEN :from AND :to ORDER BY date DESC, id DESC")
    fun inRange(from: Long, to: Long): Flow<List<Txn>>

    @Query("SELECT * FROM transactions WHERE date BETWEEN :from AND :to")
    suspend fun inRangeOnce(from: Long, to: Long): List<Txn>

    @Query("SELECT * FROM transactions WHERE propertyId = :propertyId ORDER BY date DESC, id DESC")
    fun forProperty(propertyId: Long): Flow<List<Txn>>

    @Query("SELECT * FROM transactions WHERE debtId = :debtId ORDER BY date DESC, id DESC")
    fun forDebt(debtId: Long): Flow<List<Txn>>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun get(id: Long): Txn?

    @Query("SELECT COUNT(*) FROM transactions WHERE categoryId = :categoryId")
    suspend fun countForCategory(categoryId: Long): Int

    @Upsert
    suspend fun upsert(txn: Txn): Long

    @Delete
    suspend fun delete(txn: Txn)
}

@Dao
interface BudgetDao {
    @Query("SELECT * FROM budgets")
    fun all(): Flow<List<Budget>>

    @Query("SELECT * FROM budgets")
    suspend fun allOnce(): List<Budget>

    @Upsert
    suspend fun upsert(budget: Budget)

    @Delete
    suspend fun delete(budget: Budget)
}

@Dao
interface PropertyDao {
    @Query("SELECT * FROM properties ORDER BY name")
    fun all(): Flow<List<Property>>

    @Query("SELECT * FROM properties WHERE id = :id")
    fun observe(id: Long): Flow<Property?>

    @Query("SELECT * FROM properties WHERE id = :id")
    suspend fun get(id: Long): Property?

    @Upsert
    suspend fun upsert(property: Property): Long

    @Delete
    suspend fun delete(property: Property)
}

@Dao
interface LeaseDao {
    @Query("SELECT * FROM leases ORDER BY active DESC, startDate DESC")
    fun all(): Flow<List<Lease>>

    @Query("SELECT * FROM leases WHERE active = 1")
    suspend fun activeOnce(): List<Lease>

    @Query("SELECT * FROM leases WHERE propertyId = :propertyId ORDER BY active DESC, startDate DESC")
    fun forProperty(propertyId: Long): Flow<List<Lease>>

    @Query("SELECT * FROM leases WHERE id = :id")
    suspend fun get(id: Long): Lease?

    @Query("DELETE FROM leases WHERE propertyId = :propertyId")
    suspend fun deleteForProperty(propertyId: Long)

    @Upsert
    suspend fun upsert(lease: Lease): Long

    @Delete
    suspend fun delete(lease: Lease)
}

@Dao
interface DebtDao {
    @Query("SELECT * FROM debts ORDER BY closed, kind, name")
    fun all(): Flow<List<Debt>>

    @Query("SELECT * FROM debts WHERE closed = 0")
    suspend fun openOnce(): List<Debt>

    @Query("SELECT * FROM debts WHERE id = :id")
    fun observe(id: Long): Flow<Debt?>

    @Query("SELECT * FROM debts WHERE id = :id")
    suspend fun get(id: Long): Debt?

    @Upsert
    suspend fun upsert(debt: Debt): Long

    @Delete
    suspend fun delete(debt: Debt)
}

@Dao
interface BillDao {
    @Query("SELECT * FROM bills ORDER BY dueDay, title")
    fun all(): Flow<List<Bill>>

    @Query("SELECT * FROM bills WHERE enabled = 1")
    suspend fun enabledOnce(): List<Bill>

    @Upsert
    suspend fun upsert(bill: Bill): Long

    @Delete
    suspend fun delete(bill: Bill)
}

@Dao
interface AssetDao {
    @Query("SELECT * FROM assets ORDER BY value DESC")
    fun all(): Flow<List<Asset>>

    @Upsert
    suspend fun upsert(asset: Asset): Long

    @Delete
    suspend fun delete(asset: Asset)
}
