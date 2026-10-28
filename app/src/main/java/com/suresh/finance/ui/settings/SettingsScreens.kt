package com.suresh.finance.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.suresh.finance.data.Category
import com.suresh.finance.data.SettingsStore
import com.suresh.finance.data.TxType
import com.suresh.finance.notify.ReminderWorker
import com.suresh.finance.ui.CATEGORY_COLORS
import com.suresh.finance.ui.CATEGORY_ICONS
import com.suresh.finance.ui.Routes
import com.suresh.finance.ui.components.AppScaffold
import com.suresh.finance.ui.components.CategoryBadge
import com.suresh.finance.ui.components.DropdownField
import com.suresh.finance.ui.components.SectionCard
import com.suresh.finance.ui.components.TextInput
import com.suresh.finance.ui.repo
import kotlinx.coroutines.launch

@Composable
fun MoreScreen(nav: NavHostController) {
    AppScaffold(title = "More") { padding ->
        Column(Modifier.padding(padding)) {
            MoreItem(Icons.Filled.CreditCard, "Debts", "Credit cards, loans, mortgage, home payment plans") { nav.navigate(Routes.DEBTS) }
            MoreItem(Icons.Filled.Notifications, "Reminders", "Bills, EMIs and rent due dates") { nav.navigate(Routes.REMINDERS) }
            MoreItem(Icons.Filled.Assessment, "Reports", "Monthly and yearly summaries, rental report") { nav.navigate(Routes.REPORTS) }
            MoreItem(Icons.Filled.AccountBalance, "Assets & net worth", "Savings, investments, gold, vehicles") { nav.navigate(Routes.ASSETS) }
            MoreItem(Icons.Filled.Category, "Categories", "Add or hide income and expense categories") { nav.navigate(Routes.CATEGORIES) }
            MoreItem(Icons.Filled.Settings, "Settings", "Currency and notifications") { nav.navigate(Routes.SETTINGS) }
        }
    }
}

@Composable
private fun MoreItem(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle) },
        leadingContent = { Icon(icon, contentDescription = null) },
        trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null) },
        modifier = Modifier.clickable(onClick = onClick),
    )
    HorizontalDivider()
}

@Composable
fun SettingsScreen(nav: NavHostController) {
    val repo = repo()
    val context = LocalContext.current
    val settings = repo.settings
    val currency by settings.currency.collectAsStateWithLifecycle()
    val notify by settings.notificationsEnabled.collectAsStateWithLifecycle()
    val daysAhead by settings.reminderDaysAhead.collectAsStateWithLifecycle()

    AppScaffold(title = "Settings", onBack = { nav.popBackStack() }) { padding ->
        Column(
            Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SectionCard("Currency") {
                DropdownField("Currency", SettingsStore.CURRENCIES, currency, { it }, { settings.setCurrency(it) })
                Text(
                    "Changes how amounts are shown. Amounts you've already entered aren't converted.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            SectionCard("Notifications") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Due date and budget alerts", Modifier.weight(1f))
                    Switch(checked = notify, onCheckedChange = { settings.setNotificationsEnabled(it) })
                }
                Text("Remind me before due dates", style = MaterialTheme.typography.bodyMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(1, 3, 5, 7).forEach { d ->
                        FilterChip(
                            selected = daysAhead == d,
                            onClick = { settings.setReminderDaysAhead(d) },
                            label = { Text("$d day${if (d == 1) "" else "s"}") },
                        )
                    }
                }
                OutlinedButton(onClick = { ReminderWorker.checkNow(context) }) { Text("Check for alerts now") }
            }
            SectionCard("About") {
                Text(
                    "Your data is stored only on this phone. Backup and restore are coming in a later version.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
fun CategoriesScreen(nav: NavHostController) {
    val repo = repo()
    val scope = rememberCoroutineScope()
    val categories by remember { repo.categories.all() }.collectAsStateWithLifecycle(emptyList())
    var editing by remember { mutableStateOf<Category?>(null) }
    var adding by remember { mutableStateOf(false) }

    AppScaffold(
        title = "Categories",
        onBack = { nav.popBackStack() },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { adding = true },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Add category") },
            )
        },
    ) { padding ->
        LazyColumn(Modifier.padding(padding), contentPadding = PaddingValues(bottom = 96.dp)) {
            listOf(TxType.INCOME to "Income", TxType.EXPENSE to "Expenses").forEach { (type, label) ->
                item(key = "h$type") {
                    Text(
                        label,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp),
                    )
                }
                items(categories.filter { it.type == type }, key = { it.id }) { c ->
                    ListItem(
                        headlineContent = { Text(c.name) },
                        supportingContent = if (c.archived) { { Text("Hidden") } } else null,
                        leadingContent = { CategoryBadge(c, size = 36) },
                        trailingContent = {
                            Switch(
                                checked = !c.archived,
                                onCheckedChange = { on -> scope.launch { repo.categories.upsert(c.copy(archived = !on)) } },
                            )
                        },
                        modifier = Modifier.clickable { editing = c },
                    )
                }
            }
        }
    }

    if (adding || editing != null) {
        CategoryDialog(
            category = editing,
            onSave = { scope.launch { repo.categories.upsert(it) } },
            onDismiss = {
                adding = false
                editing = null
            },
        )
    }
}

@Composable
private fun CategoryDialog(category: Category?, onSave: (Category) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf(category?.name ?: "") }
    var type by remember { mutableStateOf(category?.type ?: TxType.EXPENSE) }
    var icon by remember { mutableStateOf(category?.icon ?: "category") }
    var color by remember { mutableStateOf(category?.color ?: CATEGORY_COLORS.first()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (category == null) "Add category" else "Edit category") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                TextInput(name, { name = it }, "Name")
                if (category == null) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = type == TxType.EXPENSE, onClick = { type = TxType.EXPENSE }, label = { Text("Expense") })
                        FilterChip(selected = type == TxType.INCOME, onClick = { type = TxType.INCOME }, label = { Text("Income") })
                    }
                }
                Text("Icon", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    CATEGORY_ICONS.forEach { (key, vector) ->
                        val selected = key == icon
                        Box(
                            Modifier
                                .size(40.dp)
                                .background(
                                    if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                    CircleShape,
                                )
                                .clickable { icon = key },
                            contentAlignment = Alignment.Center,
                        ) { Icon(vector, contentDescription = key, modifier = Modifier.size(22.dp)) }
                    }
                }
                Text("Colour", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    CATEGORY_COLORS.forEach { c ->
                        Box(
                            Modifier
                                .size(32.dp)
                                .background(Color(c), CircleShape)
                                .then(
                                    if (c == color) Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape) else Modifier,
                                )
                                .clickable { color = c },
                        )
                    }
                }
                Spacer(Modifier.width(1.dp))
            }
        },
        confirmButton = {
            TextButton(enabled = name.isNotBlank(), onClick = {
                val base = category ?: Category(name = "", type = type, icon = icon, color = color)
                onSave(base.copy(name = name.trim(), type = type, icon = icon, color = color))
                onDismiss()
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
