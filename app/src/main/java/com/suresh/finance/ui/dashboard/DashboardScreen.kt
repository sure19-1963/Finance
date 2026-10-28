package com.suresh.finance.ui.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.suresh.finance.data.DebtKind
import com.suresh.finance.data.DueItem
import com.suresh.finance.data.Dues
import com.suresh.finance.data.TxType
import com.suresh.finance.ui.LocalCurrency
import com.suresh.finance.ui.Routes
import com.suresh.finance.ui.formatMoney
import com.suresh.finance.ui.charts.BarGroup
import com.suresh.finance.ui.charts.DonutChart
import com.suresh.finance.ui.charts.GroupedBarChart
import com.suresh.finance.ui.charts.HorizontalBars
import com.suresh.finance.ui.charts.Legend
import com.suresh.finance.ui.charts.LegendDot
import com.suresh.finance.ui.charts.Slice
import com.suresh.finance.ui.components.AppScaffold
import com.suresh.finance.ui.components.LabelValue
import com.suresh.finance.ui.components.MonthSelector
import com.suresh.finance.ui.components.SectionCard
import com.suresh.finance.ui.components.StatTile
import com.suresh.finance.ui.components.UsageBar
import com.suresh.finance.ui.formatCompact
import com.suresh.finance.ui.money
import com.suresh.finance.ui.prettyShort
import com.suresh.finance.ui.repo
import com.suresh.finance.ui.short
import com.suresh.finance.ui.theme.MoneyColors
import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.abs

@Composable
fun DashboardScreen(nav: NavHostController) {
    val repo = repo()
    var month by rememberSaveable { mutableStateOf(YearMonth.now()) }
    val currentMonth by rememberUpdatedState(month)

    // Six months of history ending at the selected month, for the trend chart.
    val rangeFlow = remember(month) {
        repo.txns.inRange(month.minusMonths(5).atDay(1).toEpochDay(), month.atEndOfMonth().toEpochDay())
    }
    val txns by rangeFlow.collectAsStateWithLifecycle(emptyList())
    val categories by remember { repo.categories.all() }.collectAsStateWithLifecycle(emptyList())
    val budgets by remember { repo.budgets.all() }.collectAsStateWithLifecycle(emptyList())
    val debts by remember { repo.debts.all() }.collectAsStateWithLifecycle(emptyList())
    val assets by remember { repo.assets.all() }.collectAsStateWithLifecycle(emptyList())
    val properties by remember { repo.properties.all() }.collectAsStateWithLifecycle(emptyList())
    val bills by remember { repo.bills.all() }.collectAsStateWithLifecycle(emptyList())
    val leases by remember { repo.leases.all() }.collectAsStateWithLifecycle(emptyList())

    val byId = remember(categories) { categories.associateBy { it.id } }
    val from = month.atDay(1).toEpochDay()
    val to = month.atEndOfMonth().toEpochDay()
    val monthTxns = txns.filter { it.date in from..to }
    val income = monthTxns.filter { it.type == TxType.INCOME }.sumOf { it.amount }
    val expense = monthTxns.filter { it.type == TxType.EXPENSE }.sumOf { it.amount }
    val net = income - expense

    val currency = LocalCurrency.current
    val incomeColor = MoneyColors.income()
    val expenseColor = MoneyColors.expense()

    AppScaffold(
        title = "My Finance",
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { nav.navigate(Routes.txn()) },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Add entry") },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .pointerInput(Unit) {
                    // Swipe left or right anywhere on the dashboard to change month.
                    var drag = 0f
                    detectHorizontalDragGestures(
                        onDragStart = { drag = 0f },
                        onDragEnd = {
                            if (abs(drag) > 120) month = if (drag < 0) currentMonth.plusMonths(1) else currentMonth.minusMonths(1)
                        },
                    ) { _, amount -> drag += amount }
                }
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            MonthSelector(month, { month = it })

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatTile("Income", money(income), incomeColor, Modifier.weight(1f))
                StatTile("Expenses", money(expense), expenseColor, Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatTile("Net savings", money(net), if (net >= 0) incomeColor else expenseColor, Modifier.weight(1f))
                val rate = if (income > 0) (net / income * 100).toInt() else 0
                StatTile("Savings rate", "$rate%", if (net >= 0) incomeColor else expenseColor, Modifier.weight(1f))
            }

            // Where the money went this month.
            val expenseSlices = monthTxns.filter { it.type == TxType.EXPENSE }
                .groupBy { it.categoryId }
                .map { (cid, list) ->
                    val c = byId[cid]
                    Slice(c?.name ?: "Other", list.sumOf { it.amount }, c?.let { Color(it.color) } ?: Color.Gray)
                }
                .sortedByDescending { it.value }
            SectionCard("Spending by category") {
                if (expenseSlices.isEmpty()) {
                    Text("No expenses recorded this month.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    DonutChart(expenseSlices, Modifier.fillMaxWidth()) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Spent", style = MaterialTheme.typography.labelMedium)
                            Text(formatCompact(expense), style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    val top = expenseSlices.take(6)
                    val rest = expenseSlices.drop(6).sumOf { it.value }
                    val legend = if (rest > 0) top + Slice("Everything else", rest, Color.Gray) else top
                    Legend(legend, expense, { formatMoney(it, currency) })
                }
            }

            // Six-month cash flow.
            val months = (5 downTo 0).map { month.minusMonths(it.toLong()) }
            val groups = months.map { m ->
                val f = m.atDay(1).toEpochDay()
                val t = m.atEndOfMonth().toEpochDay()
                val inM = txns.filter { it.date in f..t }
                BarGroup(
                    m.short(),
                    listOf(
                        inM.filter { it.type == TxType.INCOME }.sumOf { it.amount },
                        inM.filter { it.type == TxType.EXPENSE }.sumOf { it.amount },
                    ),
                )
            }
            SectionCard("Cash flow, last 6 months") {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    LegendDot(incomeColor, "Income")
                    LegendDot(expenseColor, "Expenses")
                }
                GroupedBarChart(groups, listOf(incomeColor, expenseColor))
            }

            // Coming up: bills, EMIs and rent.
            val dues = Dues.forMonth(month, bills, debts, leases, properties, monthTxns)
            val pending = dues.filter { !it.done }
            SectionCard(
                "Due this month",
                action = { TextButton(onClick = { nav.navigate(Routes.REMINDERS) }) { Text("See all") } },
            ) {
                if (dues.isEmpty()) {
                    Text(
                        "Add bills, debts with a monthly payment, or tenants to see due dates here.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else if (pending.isEmpty()) {
                    Text("Everything due this month is done.", color = incomeColor)
                } else {
                    pending.take(5).forEach { DueLine(it) }
                }
            }

            // Budgets closest to their limit.
            if (budgets.isNotEmpty()) {
                val spentBy = monthTxns.filter { it.type == TxType.EXPENSE }.groupBy { it.categoryId }
                    .mapValues { (_, l) -> l.sumOf { it.amount } }
                val rows = budgets.filter { it.monthlyLimit > 0 }
                    .map { b -> Triple(byId[b.categoryId]?.name ?: "Budget", spentBy[b.categoryId] ?: 0.0, b.monthlyLimit) }
                    .sortedByDescending { it.second / it.third }
                SectionCard(
                    "Budgets",
                    action = { TextButton(onClick = { nav.navigate(Routes.BUDGETS) }) { Text("Manage") } },
                ) {
                    rows.take(4).forEach { (name, spent, limit) ->
                        Column {
                            LabelValue(name, "${money(spent)} / ${money(limit)}")
                            UsageBar(spent, limit)
                        }
                    }
                }
            }

            // Net worth: what you own minus what you owe.
            val openDebts = debts.filter { !it.closed }
            val totalDebt = openDebts.sumOf { it.balance }
            val totalAssets = assets.sumOf { it.value } + properties.sumOf { it.marketValue }
            SectionCard(
                "Assets and debts",
                action = { TextButton(onClick = { nav.navigate(Routes.ASSETS) }) { Text("Assets") } },
            ) {
                LabelValue("What you own", money(totalAssets), valueColor = incomeColor)
                LabelValue("What you owe", money(totalDebt), valueColor = expenseColor)
                LabelValue(
                    "Net worth",
                    money(totalAssets - totalDebt),
                    valueStyle = MaterialTheme.typography.titleMedium,
                )
                if (openDebts.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Text("Debt by type", style = MaterialTheme.typography.labelLarge)
                    val debtSlices = DebtKind.entries.mapNotNull { kind ->
                        val sum = openDebts.filter { it.kind == kind }.sumOf { it.balance }
                        if (sum > 0) Slice(kind.label, sum, expenseColor) else null
                    }
                    HorizontalBars(debtSlices, { formatMoney(it, currency) })
                    TextButton(onClick = { nav.navigate(Routes.DEBTS) }) { Text("View debts") }
                }
            }
            Spacer(Modifier.height(88.dp))
        }
    }
}

@Composable
private fun DueLine(item: DueItem) {
    val today = LocalDate.now()
    val days = item.daysUntil(today)
    val whenText = when {
        days < 0 -> "Overdue by ${-days} day${if (days == -1L) "" else "s"}"
        days == 0L -> "Due today"
        days == 1L -> "Due tomorrow"
        else -> "Due ${item.dueDate.prettyShort()}"
    }
    val color = when {
        days < 0 -> MoneyColors.expense()
        days <= 3 -> MoneyColors.warning()
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(item.title, style = MaterialTheme.typography.bodyLarge)
            Text("${item.subtitle}  ·  $whenText", style = MaterialTheme.typography.bodySmall, color = color)
        }
        Spacer(Modifier.width(8.dp))
        Text(money(item.amount), style = MaterialTheme.typography.bodyMedium)
    }
}
