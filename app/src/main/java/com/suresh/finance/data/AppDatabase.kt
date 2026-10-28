package com.suresh.finance.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Category::class, Txn::class, Budget::class, Property::class, Lease::class, Debt::class, Bill::class, Asset::class],
    version = 1,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun categoryDao(): CategoryDao
    abstract fun txnDao(): TxnDao
    abstract fun budgetDao(): BudgetDao
    abstract fun propertyDao(): PropertyDao
    abstract fun leaseDao(): LeaseDao
    abstract fun debtDao(): DebtDao
    abstract fun billDao(): BillDao
    abstract fun assetDao(): AssetDao

    companion object {
        const val NAME = "finance.db"

        @Volatile private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, NAME)
                .addCallback(SeedCallback)
                .build()
                .also { instance = it }
        }
    }
}

/** Default categories created the first time the app runs. */
data class DefaultCategory(val name: String, val type: TxType, val icon: String, val color: Long, val key: String?)

val DEFAULT_CATEGORIES = listOf(
    DefaultCategory("Salary", TxType.INCOME, "salary", 0xFF2E7D32, "salary"),
    DefaultCategory("Rental income", TxType.INCOME, "rent_in", 0xFF00897B, "rental_income"),
    DefaultCategory("Business", TxType.INCOME, "business", 0xFF5E35B1, null),
    DefaultCategory("Investments", TxType.INCOME, "invest", 0xFF1E88E5, null),
    DefaultCategory("Other income", TxType.INCOME, "other_in", 0xFF7CB342, null),

    DefaultCategory("Credit card payment", TxType.EXPENSE, "card", 0xFFE53935, "credit_card"),
    DefaultCategory("Mortgage", TxType.EXPENSE, "bank", 0xFF8E24AA, "mortgage"),
    DefaultCategory("Loan repayment", TxType.EXPENSE, "payments", 0xFFD81B60, "loan"),
    DefaultCategory("Home payment plan", TxType.EXPENSE, "home", 0xFF3949AB, "home_plan"),
    DefaultCategory("Tuition fees", TxType.EXPENSE, "school", 0xFF1E88E5, null),
    DefaultCategory("Hostel fees", TxType.EXPENSE, "hotel", 0xFF039BE5, null),
    DefaultCategory("Food & groceries", TxType.EXPENSE, "cart", 0xFF43A047, null),
    DefaultCategory("House rent", TxType.EXPENSE, "house", 0xFFF4511E, null),
    DefaultCategory("Utilities", TxType.EXPENSE, "bolt", 0xFFFFB300, null),
    DefaultCategory("Transport", TxType.EXPENSE, "car", 0xFF6D4C41, null),
    DefaultCategory("Health", TxType.EXPENSE, "health", 0xFFC2185B, null),
    DefaultCategory("Property maintenance", TxType.EXPENSE, "build", 0xFF546E7A, "property_maintenance"),
    DefaultCategory("Miscellaneous", TxType.EXPENSE, "misc", 0xFF757575, "misc"),
)

private object SeedCallback : RoomDatabase.Callback() {
    override fun onCreate(db: SupportSQLiteDatabase) {
        DEFAULT_CATEGORIES.forEach { c ->
            db.execSQL(
                "INSERT INTO categories (name, type, icon, color, archived, systemKey) VALUES (?, ?, ?, ?, 0, ?)",
                arrayOf<Any?>(c.name, c.type.name, c.icon, c.color, c.key),
            )
        }
    }
}
