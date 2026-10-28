package com.suresh.finance.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.suresh.finance.data.TxType
import com.suresh.finance.ui.assets.AssetsScreen
import com.suresh.finance.ui.budgets.BudgetsScreen
import com.suresh.finance.ui.dashboard.DashboardScreen
import com.suresh.finance.ui.debts.DebtDetailScreen
import com.suresh.finance.ui.debts.DebtEditScreen
import com.suresh.finance.ui.debts.DebtsScreen
import com.suresh.finance.ui.properties.LeaseEditScreen
import com.suresh.finance.ui.properties.PropertiesScreen
import com.suresh.finance.ui.properties.PropertyDetailScreen
import com.suresh.finance.ui.properties.PropertyEditScreen
import com.suresh.finance.ui.reminders.RemindersScreen
import com.suresh.finance.ui.reports.ReportsScreen
import com.suresh.finance.ui.settings.CategoriesScreen
import com.suresh.finance.ui.settings.MoreScreen
import com.suresh.finance.ui.settings.SettingsScreen
import com.suresh.finance.ui.transactions.TransactionEditScreen
import com.suresh.finance.ui.transactions.TransactionsScreen

/** Route builders, so screens don't hand-assemble route strings. */
object Routes {
    const val HOME = "home"
    const val TRANSACTIONS = "transactions"
    const val BUDGETS = "budgets"
    const val PROPERTIES = "properties"
    const val MORE = "more"
    const val DEBTS = "debts"
    const val REMINDERS = "reminders"
    const val REPORTS = "reports"
    const val ASSETS = "assets"
    const val SETTINGS = "settings"
    const val CATEGORIES = "categories"

    fun txn(id: Long = 0, type: TxType = TxType.EXPENSE, propertyId: Long = 0) =
        "txn?id=$id&type=${type.name}&propertyId=$propertyId"
    fun property(id: Long) = "property/$id"
    fun propertyEdit(id: Long = 0) = "propertyEdit?id=$id"
    fun lease(propertyId: Long, id: Long = 0) = "lease?propertyId=$propertyId&id=$id"
    fun debt(id: Long) = "debt/$id"
    fun debtEdit(id: Long = 0) = "debtEdit?id=$id"
}

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val TABS = listOf(
    Tab(Routes.HOME, "Home", Icons.Filled.Dashboard),
    Tab(Routes.TRANSACTIONS, "Money", Icons.Filled.Receipt),
    Tab(Routes.BUDGETS, "Budgets", Icons.Filled.PieChart),
    Tab(Routes.PROPERTIES, "Rentals", Icons.Filled.Apartment),
    Tab(Routes.MORE, "More", Icons.Filled.Menu),
)

@Composable
fun AppNav() {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val route = backStack?.destination?.route
    val showBar = TABS.any { it.route == route }

    Scaffold(
        bottomBar = {
            if (showBar) {
                NavigationBar {
                    TABS.forEach { tab ->
                        NavigationBarItem(
                            selected = route == tab.route,
                            onClick = { nav.switchTab(tab.route) },
                            icon = { Icon(tab.icon, contentDescription = null) },
                            label = { Text(tab.label) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = nav,
            startDestination = Routes.HOME,
            // Each screen draws its own top bar, so only take the bottom inset here.
            modifier = Modifier.padding(bottom = padding.calculateBottomPadding()),
        ) {
            composable(Routes.HOME) { DashboardScreen(nav) }
            composable(Routes.TRANSACTIONS) { TransactionsScreen(nav) }
            composable(Routes.BUDGETS) { BudgetsScreen() }
            composable(Routes.PROPERTIES) { PropertiesScreen(nav) }
            composable(Routes.MORE) { MoreScreen(nav) }

            composable(
                "txn?id={id}&type={type}&propertyId={propertyId}",
                arguments = listOf(
                    navArgument("id") { type = NavType.LongType; defaultValue = 0L },
                    navArgument("type") { type = NavType.StringType; defaultValue = TxType.EXPENSE.name },
                    navArgument("propertyId") { type = NavType.LongType; defaultValue = 0L },
                ),
            ) { entry ->
                val args = entry.arguments
                TransactionEditScreen(
                    nav = nav,
                    id = args?.getLong("id") ?: 0L,
                    initialType = TxType.valueOf(args?.getString("type") ?: TxType.EXPENSE.name),
                    propertyId = args?.getLong("propertyId") ?: 0L,
                )
            }

            composable(
                "property/{id}",
                arguments = listOf(navArgument("id") { type = NavType.LongType }),
            ) { entry -> PropertyDetailScreen(nav, entry.arguments?.getLong("id") ?: 0L) }

            composable(
                "propertyEdit?id={id}",
                arguments = listOf(navArgument("id") { type = NavType.LongType; defaultValue = 0L }),
            ) { entry -> PropertyEditScreen(nav, entry.arguments?.getLong("id") ?: 0L) }

            composable(
                "lease?propertyId={propertyId}&id={id}",
                arguments = listOf(
                    navArgument("propertyId") { type = NavType.LongType; defaultValue = 0L },
                    navArgument("id") { type = NavType.LongType; defaultValue = 0L },
                ),
            ) { entry ->
                LeaseEditScreen(
                    nav,
                    propertyId = entry.arguments?.getLong("propertyId") ?: 0L,
                    id = entry.arguments?.getLong("id") ?: 0L,
                )
            }

            composable(Routes.DEBTS) { DebtsScreen(nav) }
            composable(
                "debt/{id}",
                arguments = listOf(navArgument("id") { type = NavType.LongType }),
            ) { entry -> DebtDetailScreen(nav, entry.arguments?.getLong("id") ?: 0L) }
            composable(
                "debtEdit?id={id}",
                arguments = listOf(navArgument("id") { type = NavType.LongType; defaultValue = 0L }),
            ) { entry -> DebtEditScreen(nav, entry.arguments?.getLong("id") ?: 0L) }

            composable(Routes.REMINDERS) { RemindersScreen(nav) }
            composable(Routes.REPORTS) { ReportsScreen(nav) }
            composable(Routes.ASSETS) { AssetsScreen(nav) }
            composable(Routes.SETTINGS) { SettingsScreen(nav) }
            composable(Routes.CATEGORIES) { CategoriesScreen(nav) }
        }
    }
}

fun NavHostController.switchTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
