package com.suresh.finance.ui.reports

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.suresh.finance.data.Category
import com.suresh.finance.data.TxType
import com.suresh.finance.data.Txn
import com.suresh.finance.ui.LocalCurrency
import com.suresh.finance.ui.charts.BarGroup
import com.suresh.finance.ui.charts.GroupedBarChart
import com.suresh.finance.ui.charts.Legend
import com.suresh.finance.ui.charts.LegendDot
import com.suresh.finance.ui.charts.Slice
import com.suresh.finance.ui.components.AppScaffold
import com.suresh.finance.ui.components.LabelValue
import com.suresh.finance.ui.components.MonthSelector
import com.suresh.finance.ui.components.SectionCard
import com.suresh.finance.ui.components.StatTile
import com.suresh.finance.ui.components.YearSelector
import com.suresh.finance.ui.epochDay
import com.suresh.finance.ui.formatCompact
import com.suresh.finance.ui.formatMoney
import com.suresh.finance.ui.money
import com.suresh.finance.ui.pretty
import com.suresh.finance.ui.repo
import com.suresh.finance.ui.short
import com.suresh.finance.ui.theme.MoneyColors
import java.time.LocalDate
import java.time.YearMonth

private enum class Period { MONTH, YEAR }

@Composable
fun ReportsScreen(nav: NavHostController) {
    val repo = repo()
    val context = LocalContext.current
    val currency = LocalCurrency.current
    var period by rememberSaveable { mutableStateOf(Period.MONTH) }
    var month by rememberSaveable { mutableStateOf(YearMonth.now()) }
    var year by rememberSaveable { mutableIntStateOf(LocalDate.now().year) }

    val (from, to) = when (period) {
        Period.MONTH -> month.atDay(1) to month.atEndOfMonth()
        Period.YEAR -> LocalDate.of(year, 1, 1) to LocalDate.of(year, 12, 31)
    }
    val flow = remember(from, to) { repo.txns.inRange(from.toEpochDay(), to.toEpochDay()) }
    val txns by flow.collectAsStateWithLifecycle(emptyList())
    val categories by remember { repo.categories.all() }.collectAsStateWithLifecycle(emptyList())
    val properties by remember { repo.properties.all() }.collectAsStateWithLifecycle(emptyList())
    val leases by remember { repo.leases.all() }.collectAsStateWithLifecycle(emptyList())
    val byId = categories.associateBy { it.id }

    val income = txns.filter { it.type == TxType.INCOME }.sumOf { it.amount }
    val expense = txns.filter { it.type == TxType.EXPENSE }.sumOf { it.amount }
    val title = if (period == Period.MONTH) month.pretty() else year.toString()

    AppScaffold(
        title = "Reports",
        onBack = { nav.popBackStack() },
        actions = {
            IconButton(onClick = {
                val csv = buildCsv(txns, byId, properties.associate { it.id to it.name })
                val send = Intent(Intent.ACTION_SEND).apply {
                    type = "text/csv"
                    putExtra(Intent.EXTRA_SUBJECT, "Finance report $title")
                    putExtra(Intent.EXTRA_TEXT, csv)
                }
                context.startActivity(Intent.createChooser(send, "Share report"))
            }) { Icon(Icons.Filled.Share, contentDescription = "Share as CSV") }
        },
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = period == Period.MONTH, onClick = { period = Period.MONTH }, label = { Text("Monthly") })
                FilterChip(selected = period == Period.YEAR, onClick = { period = Period.YEAR }, label = { Text("Yearly") })
            }
            if (period == Period.MONTH) MonthSelector(month, { month = it }) else YearSelector(year, { year = it })

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatTile("Income", money(income), MoneyColors.income(), Modifier.weight(1f))
                StatTile("Expenses", money(expense), MoneyColors.expense(), Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val net = income - expense
                StatTile("Net savings", money(net), if (net >= 0) MoneyColors.income() else MoneyColors.expense(), Modifier.weight(1f))
                StatTile(
                    "Saved",
                    if (income > 0) "${(net / income * 100).toInt()}% of income" else "-",
                    MaterialTheme.colorScheme.onSurface,
                    Modifier.weight(1f),
                )
            }

            if (period == Period.YEAR) {
                val incomeColor = MoneyColors.income()
                val expenseColor = MoneyColors.expense()
                val months = (1..12).map { YearMonth.of(year, it) }
                val rows = months.map { m ->
                    val f = m.atDay(1).toEpochDay()
                    val t = m.atEndOfMonth().toEpochDay()
                    val inM = txns.filter { it.date in f..t }
                    Triple(m, inM.filter { it.type == TxType.INCOME }.sumOf { it.amount }, inM.filter { it.type == TxType.EXPENSE }.sumOf { it.amount })
                }
                SectionCard("Month by month") {
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        LegendDot(incomeColor, "Income")
                        LegendDot(expenseColor, "Expenses")
                    }
                    GroupedBarChart(rows.map { BarGroup(it.first.short().take(1), listOf(it.second, it.third)) }, listOf(incomeColor, expenseColor))
                    Spacer(Modifier.height(8.dp))
                    TableRow("Month", "Income", "Expenses", "Net", header = true)
                    HorizontalDivider()
                    rows.forEach { (m, i, e) ->
                        TableRow(m.short(), formatCompact(i), formatCompact(e), formatCompact(i - e), netNegative = i - e < 0)
                    }
                    HorizontalDivider()
                    TableRow("Total", formatCompact(income), formatCompact(expense), formatCompact(income - expense), header = true)
                }
            }

            BreakdownCard("Income by category", txns.filter { it.type == TxType.INCOME }, byId, income, currency)
            BreakdownCard("Expenses by category", txns.filter { it.type == TxType.EXPENSE }, byId, expense, currency)

            if (properties.isNotEmpty()) {
                val activeIds = leases.filter { it.active }.map { it.propertyId }.toSet()
                SectionCard("Rental properties") {
                    var totalIn = 0.0
                    var totalOut = 0.0
                    properties.forEach { p ->
                        val pt = txns.filter { it.propertyId == p.id }
                        val pin = pt.filter { it.type == TxType.INCOME }.sumOf { it.amount }
                        val pout = pt.filter { it.type == TxType.EXPENSE }.sumOf { it.amount }
                        totalIn += pin
                        totalOut += pout
                        Column {
                            Text(
                                "${p.name}  ·  ${if (p.id in activeIds) "Occupied" else "Vacant"}",
                                style = MaterialTheme.typography.titleSmall,
                            )
                            LabelValue("Rent received", money(pin), valueColor = MoneyColors.income())
                            LabelValue("Expenses", money(pout), valueColor = MoneyColors.expense())
                            LabelValue("Net", money(pin - pout))
                        }
                        HorizontalDivider(Modifier.padding(vertical = 4.dp))
                    }
                    LabelValue("All properties, net", money(totalIn - totalOut), valueStyle = MaterialTheme.typography.titleSmall)
                    val vacantLoss = properties.filter { it.id !in activeIds }.sumOf { it.expectedRent }
                    if (vacantLoss > 0) {
                        LabelValue("Rent missed per month from vacant units", money(vacantLoss), valueColor = MoneyColors.warning())
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun BreakdownCard(title: String, list: List<Txn>, byId: Map<Long, Category>, total: Double, currency: String) {
    SectionCard(title) {
        if (list.isEmpty()) {
            Text("Nothing recorded.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            return@SectionCard
        }
        val slices = list.groupBy { it.categoryId }.map { (cid, l) ->
            val c = byId[cid]
            Slice(c?.name ?: "Other", l.sumOf { it.amount }, c?.let { Color(it.color) } ?: Color.Gray)
        }.sortedByDescending { it.value }
        Legend(slices, total, { formatMoney(it, currency) })
    }
}

@Composable
private fun TableRow(a: String, b: String, c: String, d: String, header: Boolean = false, netNegative: Boolean = false) {
    val style = if (header) MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold) else MaterialTheme.typography.bodyMedium
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(a, style = style, modifier = Modifier.weight(1f))
        Text(b, style = style, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
        Text(c, style = style, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
        Text(
            d, style = style, modifier = Modifier.weight(1f), textAlign = TextAlign.End,
            color = if (netNegative) MoneyColors.expense() else Color.Unspecified,
        )
    }
}

private fun buildCsv(txns: List<Txn>, categories: Map<Long, Category>, properties: Map<Long, String>): String {
    fun esc(s: String) = if (s.contains(',') || s.contains('"') || s.contains('\n')) "\"" + s.replace("\"", "\"\"") + "\"" else s
    val sb = StringBuilder("Date,Type,Category,Amount,Property,Note\n")
    txns.sortedBy { it.date }.forEach { t ->
        sb.append(epochDay(t.date)).append(',')
            .append(if (t.type == TxType.INCOME) "Income" else "Expense").append(',')
            .append(esc(categories[t.categoryId]?.name ?: "")).append(',')
            .append(String.format(java.util.Locale.US, "%.2f", t.amount)).append(',')
            .append(esc(t.propertyId?.let { properties[it] } ?: "")).append(',')
            .append(esc(t.note)).append('\n')
    }
    return sb.toString()
}
