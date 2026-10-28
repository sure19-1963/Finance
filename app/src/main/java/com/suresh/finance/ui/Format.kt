package com.suresh.finance.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.House
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.Work
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import com.suresh.finance.FinanceApp
import com.suresh.finance.data.FinanceRepository
import java.text.DecimalFormat
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import kotlin.math.abs

val LocalCurrency = compositionLocalOf { "AED" }

@Composable
fun repo(): FinanceRepository = (LocalContext.current.applicationContext as FinanceApp).repo

private val moneyFormat = DecimalFormat("#,##0.00")
private val shortFormat = DecimalFormat("#,##0")

/** "AED 1,234.50". Negative values get a leading minus. */
fun formatMoney(amount: Double, currency: String): String {
    val sign = if (amount < 0) "-" else ""
    return "$sign$currency ${moneyFormat.format(abs(amount))}"
}

/** Compact form for chart labels: 950, 12.5K, 1.2M. */
fun formatCompact(amount: Double): String {
    val a = abs(amount)
    val sign = if (amount < 0) "-" else ""
    return when {
        a >= 1_000_000 -> sign + DecimalFormat("#,##0.#").format(a / 1_000_000) + "M"
        a >= 10_000 -> sign + DecimalFormat("#,##0.#").format(a / 1_000) + "K"
        else -> sign + shortFormat.format(a)
    }
}

@Composable
fun money(amount: Double): String = formatMoney(amount, LocalCurrency.current)

private val dayFormat = DateTimeFormatter.ofPattern("d MMM yyyy")
private val dayShortFormat = DateTimeFormatter.ofPattern("EEE, d MMM")
private val monthFormat = DateTimeFormatter.ofPattern("MMMM yyyy")
private val monthShort = DateTimeFormatter.ofPattern("MMM")

fun LocalDate.pretty(): String = format(dayFormat)
fun LocalDate.prettyShort(): String = format(dayShortFormat)
fun YearMonth.pretty(): String = format(monthFormat)
fun YearMonth.short(): String = format(monthShort)
fun epochDay(day: Long): LocalDate = LocalDate.ofEpochDay(day)

/** Parses user-typed amounts, ignoring thousands separators. */
fun parseAmount(text: String): Double? = text.replace(",", "").trim().toDoubleOrNull()

fun Double.asInput(): String = if (this == 0.0) "" else DecimalFormat("0.##").format(this)

/** Icon keys stored on categories. */
val CATEGORY_ICONS: Map<String, ImageVector> = linkedMapOf(
    "salary" to Icons.Filled.Work,
    "rent_in" to Icons.Filled.House,
    "business" to Icons.Filled.Store,
    "invest" to Icons.Filled.Savings,
    "other_in" to Icons.Filled.AttachMoney,
    "card" to Icons.Filled.CreditCard,
    "bank" to Icons.Filled.AccountBalance,
    "payments" to Icons.Filled.Payments,
    "home" to Icons.Filled.Home,
    "school" to Icons.Filled.School,
    "hotel" to Icons.Filled.Hotel,
    "cart" to Icons.Filled.ShoppingCart,
    "house" to Icons.Filled.House,
    "bolt" to Icons.Filled.Bolt,
    "car" to Icons.Filled.DirectionsCar,
    "health" to Icons.Filled.LocalHospital,
    "build" to Icons.Filled.Build,
    "food" to Icons.Filled.Restaurant,
    "misc" to Icons.Filled.MoreHoriz,
    "category" to Icons.Filled.Category,
)

fun categoryIcon(key: String): ImageVector = CATEGORY_ICONS[key] ?: Icons.Filled.Category

/** Colours offered when creating a category. */
val CATEGORY_COLORS = listOf(
    0xFFE53935, 0xFFD81B60, 0xFF8E24AA, 0xFF5E35B1, 0xFF3949AB, 0xFF1E88E5, 0xFF039BE5, 0xFF00897B,
    0xFF43A047, 0xFF7CB342, 0xFFFFB300, 0xFFF4511E, 0xFF6D4C41, 0xFF546E7A, 0xFF757575,
)
