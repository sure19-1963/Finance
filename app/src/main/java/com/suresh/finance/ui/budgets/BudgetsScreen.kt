package com.suresh.finance.ui.budgets

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.suresh.finance.data.Budget
import com.suresh.finance.data.Category
import com.suresh.finance.data.TxType
import com.suresh.finance.ui.LocalCurrency
import com.suresh.finance.ui.asInput
import com.suresh.finance.ui.components.AmountField
import com.suresh.finance.ui.components.AppScaffold
import com.suresh.finance.ui.components.CategoryBadge
import com.suresh.finance.ui.components.DropdownField
import com.suresh.finance.ui.components.EmptyState
import com.suresh.finance.ui.components.LabelValue
import com.suresh.finance.ui.components.MonthSelector
import com.suresh.finance.ui.components.SectionCard
import com.suresh.finance.ui.components.UsageBar
import com.suresh.finance.ui.money
import com.suresh.finance.ui.parseAmount
import com.suresh.finance.ui.repo
import com.suresh.finance.ui.theme.MoneyColors
import kotlinx.coroutines.launch
import java.time.YearMonth

@Composable
fun BudgetsScreen() {
    val repo = repo()
    val scope = rememberCoroutineScope()
    var month by rememberSaveable { mutableStateOf(YearMonth.now()) }
    val txnsFlow = remember(month) { repo.monthTxns(month) }
    val txns by txnsFlow.collectAsStateWithLifecycle(emptyList())
    val categories by remember { repo.categories.all() }.collectAsStateWithLifecycle(emptyList())
    val budgets by remember { repo.budgets.all() }.collectAsStateWithLifecycle(emptyList())
    var editing by remember { mutableStateOf<Pair<Category?, Budget?>?>(null) }

    val expenseCats = categories.filter { it.type == TxType.EXPENSE && !it.archived }
    val byId = categories.associateBy { it.id }
    val spent = txns.filter { it.type == TxType.EXPENSE }.groupBy { it.categoryId }
        .mapValues { (_, l) -> l.sumOf { it.amount } }
    val totalLimit = budgets.sumOf { it.monthlyLimit }
    val totalSpentBudgeted = budgets.sumOf { spent[it.categoryId] ?: 0.0 }
    val unbudgeted = spent.filterKeys { k -> budgets.none { it.categoryId == k } }.values.sum()

    AppScaffold(
        title = "Budgets",
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { editing = null to null },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Set budget") },
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            MonthSelector(month, { month = it }, Modifier.padding(horizontal = 8.dp))
            if (budgets.isEmpty()) {
                EmptyState("Set a monthly limit for categories like groceries or utilities. You'll get an alert at 80% and when you go over.")
                return@Column
            }
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item {
                    SectionCard("This month") {
                        LabelValue("Total budget", money(totalLimit))
                        LabelValue(
                            "Spent in budgeted categories",
                            money(totalSpentBudgeted),
                            valueColor = if (totalSpentBudgeted > totalLimit) MoneyColors.expense() else MaterialTheme.colorScheme.onSurface,
                        )
                        UsageBar(totalSpentBudgeted, totalLimit)
                        LabelValue("Left to spend", money(totalLimit - totalSpentBudgeted))
                        if (unbudgeted > 0) LabelValue("Spent outside budgets", money(unbudgeted))
                    }
                }
                items(budgets.sortedByDescending { (spent[it.categoryId] ?: 0.0) / it.monthlyLimit.coerceAtLeast(1.0) }, key = { it.categoryId }) { b ->
                    val c = byId[b.categoryId]
                    val s = spent[b.categoryId] ?: 0.0
                    SectionCard(modifier = Modifier.clickable { editing = c to b }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CategoryBadge(c, size = 36)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(c?.name ?: "Category", style = MaterialTheme.typography.titleSmall)
                                val left = b.monthlyLimit - s
                                Text(
                                    if (left >= 0) "${money(left)} left" else "${money(-left)} over budget",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (left >= 0) MaterialTheme.colorScheme.onSurfaceVariant else MoneyColors.expense(),
                                )
                            }
                            Text("${money(s)} / ${money(b.monthlyLimit)}", style = MaterialTheme.typography.bodySmall)
                        }
                        UsageBar(s, b.monthlyLimit)
                    }
                }
            }
        }
    }

    editing?.let { (category, budget) ->
        BudgetDialog(
            categories = if (category == null) expenseCats.filter { c -> budgets.none { it.categoryId == c.id } } else listOf(category),
            initial = category,
            initialLimit = budget?.monthlyLimit ?: 0.0,
            canRemove = budget != null,
            onSave = { cid, limit -> scope.launch { repo.budgets.upsert(Budget(cid, limit)) } },
            onRemove = { budget?.let { scope.launch { repo.budgets.delete(it) } } },
            onDismiss = { editing = null },
        )
    }
}

@Composable
private fun BudgetDialog(
    categories: List<Category>,
    initial: Category?,
    initialLimit: Double,
    canRemove: Boolean,
    onSave: (Long, Double) -> Unit,
    onRemove: () -> Unit,
    onDismiss: () -> Unit,
) {
    var selected by remember { mutableStateOf(initial ?: categories.firstOrNull()) }
    var amount by remember { mutableStateOf(initialLimit.asInput()) }
    val parsed = parseAmount(amount)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Set a budget" else "Budget for ${initial.name}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (categories.isEmpty()) {
                    Text("Every expense category already has a budget.")
                } else {
                    if (initial == null) {
                        DropdownField("Category", categories, selected, { it.name }, { selected = it })
                    }
                    AmountField(amount, { amount = it }, "Monthly limit", prefix = LocalCurrency.current)
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = selected != null && parsed != null && parsed > 0,
                onClick = {
                    val c = selected ?: return@TextButton
                    onSave(c.id, parsed ?: return@TextButton)
                    onDismiss()
                },
            ) { Text("Save") }
        },
        dismissButton = {
            Row {
                if (canRemove) {
                    TextButton(onClick = {
                        onRemove()
                        onDismiss()
                    }) { Text("Remove") }
                }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        },
    )
}
