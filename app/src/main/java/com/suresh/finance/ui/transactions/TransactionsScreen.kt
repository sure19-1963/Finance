package com.suresh.finance.ui.transactions

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.suresh.finance.data.Category
import com.suresh.finance.data.Property
import com.suresh.finance.data.TxType
import com.suresh.finance.data.Txn
import com.suresh.finance.notify.ReminderWorker
import com.suresh.finance.ui.LocalCurrency
import com.suresh.finance.ui.Routes
import com.suresh.finance.ui.asInput
import com.suresh.finance.ui.components.AmountField
import com.suresh.finance.ui.components.AppScaffold
import com.suresh.finance.ui.components.CategoryBadge
import com.suresh.finance.ui.components.ConfirmDialog
import com.suresh.finance.ui.components.DateField
import com.suresh.finance.ui.components.DropdownField
import com.suresh.finance.ui.components.EmptyState
import com.suresh.finance.ui.components.MonthSelector
import com.suresh.finance.ui.components.StatTile
import com.suresh.finance.ui.components.TextInput
import com.suresh.finance.ui.epochDay
import com.suresh.finance.ui.money
import com.suresh.finance.ui.parseAmount
import com.suresh.finance.ui.prettyShort
import com.suresh.finance.ui.repo
import com.suresh.finance.ui.theme.MoneyColors
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

private enum class Filter(val label: String) { ALL("All"), INCOME("Income"), EXPENSE("Expenses") }

@Composable
fun TransactionsScreen(nav: NavHostController) {
    val repo = repo()
    var month by rememberSaveable { mutableStateOf(YearMonth.now()) }
    var filter by rememberSaveable { mutableStateOf(Filter.ALL) }
    val txnsFlow = remember(month) { repo.monthTxns(month) }
    val txns by txnsFlow.collectAsStateWithLifecycle(emptyList())
    val categories by remember { repo.categories.all() }.collectAsStateWithLifecycle(emptyList())
    val byId = remember(categories) { categories.associateBy { it.id } }

    val income = txns.filter { it.type == TxType.INCOME }.sumOf { it.amount }
    val expense = txns.filter { it.type == TxType.EXPENSE }.sumOf { it.amount }
    val shown = when (filter) {
        Filter.ALL -> txns
        Filter.INCOME -> txns.filter { it.type == TxType.INCOME }
        Filter.EXPENSE -> txns.filter { it.type == TxType.EXPENSE }
    }

    AppScaffold(
        title = "Income & expenses",
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    val type = if (filter == Filter.INCOME) TxType.INCOME else TxType.EXPENSE
                    nav.navigate(Routes.txn(type = type))
                },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Add") },
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            MonthSelector(month, { month = it }, Modifier.padding(horizontal = 8.dp))
            Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatTile("Income", money(income), MoneyColors.income(), Modifier.weight(1f))
                StatTile("Expenses", money(expense), MoneyColors.expense(), Modifier.weight(1f))
                StatTile(
                    "Net",
                    money(income - expense),
                    if (income >= expense) MoneyColors.income() else MoneyColors.expense(),
                    Modifier.weight(1f),
                )
            }
            Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Filter.entries.forEach { f ->
                    FilterChip(selected = filter == f, onClick = { filter = f }, label = { Text(f.label) })
                }
            }
            if (shown.isEmpty()) {
                EmptyState("Nothing recorded for this month yet. Tap Add to record your salary or an expense.")
            } else {
                val grouped = shown.groupBy { it.date }
                LazyColumn(contentPadding = PaddingValues(bottom = 96.dp)) {
                    grouped.forEach { (day, list) ->
                        item(key = "h$day") {
                            Text(
                                epochDay(day).prettyShort(),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 4.dp),
                            )
                        }
                        items(list, key = { it.id }) { t ->
                            TxnRow(t, byId[t.categoryId]) { nav.navigate(Routes.txn(id = t.id)) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TxnRow(t: Txn, category: Category?, showDate: Boolean = false, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CategoryBadge(category)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(category?.name ?: "Uncategorised", style = MaterialTheme.typography.bodyLarge)
            val sub = listOfNotNull(
                if (showDate) epochDay(t.date).prettyShort() else null,
                t.note.ifBlank { null },
            ).joinToString("  ·  ")
            if (sub.isNotEmpty()) {
                Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            }
        }
        val income = t.type == TxType.INCOME
        Text(
            (if (income) "+" else "-") + money(t.amount),
            color = if (income) MoneyColors.income() else MoneyColors.expense(),
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
        )
    }
}

@Composable
fun TransactionEditScreen(nav: NavHostController, id: Long, initialType: TxType, propertyId: Long) {
    val repo = repo()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val categories by remember { repo.categories.all() }.collectAsStateWithLifecycle(emptyList())
    val properties by remember { repo.properties.all() }.collectAsStateWithLifecycle(emptyList())

    var existing by remember { mutableStateOf<Txn?>(null) }
    var type by rememberSaveable { mutableStateOf(initialType) }
    var amount by rememberSaveable { mutableStateOf("") }
    var categoryId by rememberSaveable { mutableStateOf(0L) }
    var date by rememberSaveable { mutableStateOf(LocalDate.now()) }
    var note by rememberSaveable { mutableStateOf("") }
    var linkedProperty by rememberSaveable { mutableStateOf(propertyId) }
    var loaded by rememberSaveable { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var showErrors by remember { mutableStateOf(false) }

    LaunchedEffect(id) {
        if (id != 0L && !loaded) {
            repo.txns.get(id)?.let { t ->
                existing = t
                type = t.type
                amount = t.amount.asInput()
                categoryId = t.categoryId
                date = epochDay(t.date)
                note = t.note
                linkedProperty = t.propertyId ?: 0L
            }
            loaded = true
        } else if (id != 0L) {
            existing = repo.txns.get(id)
        }
    }

    val options = categories.filter { it.type == type && (!it.archived || it.id == categoryId) }
    // Pick a sensible default category: rental income when coming from a property, else the first one.
    LaunchedEffect(options, type) {
        if (options.isNotEmpty() && options.none { it.id == categoryId }) {
            categoryId = when {
                type == TxType.INCOME && linkedProperty != 0L ->
                    options.firstOrNull { it.systemKey == "rental_income" }?.id ?: options.first().id
                type == TxType.EXPENSE && linkedProperty != 0L ->
                    options.firstOrNull { it.systemKey == "property_maintenance" }?.id ?: options.first().id
                else -> options.first().id
            }
        }
    }

    val parsed = parseAmount(amount)
    AppScaffold(
        title = if (id == 0L) "Add entry" else "Edit entry",
        onBack = { nav.popBackStack() },
        actions = {
            if (id != 0L) {
                IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Filled.Delete, contentDescription = "Delete") }
            }
        },
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = type == TxType.EXPENSE, onClick = { type = TxType.EXPENSE }, label = { Text("Expense") })
                FilterChip(selected = type == TxType.INCOME, onClick = { type = TxType.INCOME }, label = { Text("Income") })
            }
            AmountField(
                amount, { amount = it }, "Amount",
                prefix = LocalCurrency.current,
                isError = showErrors && (parsed == null || parsed <= 0),
            )
            DropdownField(
                label = "Category",
                options = options,
                selected = options.firstOrNull { it.id == categoryId },
                optionLabel = { it.name },
                onSelect = { categoryId = it.id },
            )
            DateField("Date", date, { date = it })
            TextInput(note, { note = it }, "Note (optional)")
            if (properties.isNotEmpty()) {
                val none = Property(id = 0, name = "None")
                DropdownField(
                    label = "Rental property (optional)",
                    options = listOf(none) + properties,
                    selected = properties.firstOrNull { it.id == linkedProperty } ?: none,
                    optionLabel = { it.name },
                    onSelect = { linkedProperty = it.id },
                )
            }
            Button(
                onClick = {
                    if (parsed == null || parsed <= 0 || categoryId == 0L) {
                        showErrors = true
                        return@Button
                    }
                    scope.launch {
                        val base = existing ?: Txn(type = type, amount = 0.0, categoryId = 0, date = 0)
                        repo.txns.upsert(
                            base.copy(
                                type = type,
                                amount = parsed,
                                categoryId = categoryId,
                                date = date.toEpochDay(),
                                note = note.trim(),
                                propertyId = linkedProperty.takeIf { it != 0L },
                            ),
                        )
                        if (type == TxType.EXPENSE) ReminderWorker.checkNow(context)
                        nav.popBackStack()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Save") }
            if (existing?.debtId != null) {
                Text(
                    "This is a debt payment. Editing it here doesn't change the debt balance, so update the balance on the debt too if needed.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            HorizontalDivider(Modifier.padding(top = 8.dp), color = MaterialTheme.colorScheme.surface)
        }
    }

    if (confirmDelete) {
        ConfirmDialog(
            title = "Delete entry?",
            text = "This can't be undone.",
            onConfirm = {
                scope.launch {
                    existing?.let { repo.txns.delete(it) }
                    nav.popBackStack()
                }
            },
            onDismiss = { confirmDelete = false },
        )
    }
}
