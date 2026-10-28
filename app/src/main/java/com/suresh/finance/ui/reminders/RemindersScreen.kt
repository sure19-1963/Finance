package com.suresh.finance.ui.reminders

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.suresh.finance.data.Bill
import com.suresh.finance.data.BillKind
import com.suresh.finance.data.Category
import com.suresh.finance.data.DueItem
import com.suresh.finance.data.DueSource
import com.suresh.finance.data.Dues
import com.suresh.finance.data.TxType
import com.suresh.finance.ui.LocalCurrency
import com.suresh.finance.ui.asInput
import com.suresh.finance.ui.components.AmountField
import com.suresh.finance.ui.components.AppScaffold
import com.suresh.finance.ui.components.DropdownField
import com.suresh.finance.ui.components.NumberField
import com.suresh.finance.ui.components.SectionCard
import com.suresh.finance.ui.components.TextInput
import com.suresh.finance.ui.debts.PaymentDialog
import com.suresh.finance.ui.money
import com.suresh.finance.ui.parseAmount
import com.suresh.finance.ui.prettyShort
import com.suresh.finance.ui.properties.RentDialog
import com.suresh.finance.ui.repo
import com.suresh.finance.ui.theme.MoneyColors
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun RemindersScreen(nav: NavHostController) {
    val repo = repo()
    val scope = rememberCoroutineScope()
    val month = YearMonth.now()
    val today = LocalDate.now()
    val txns by remember { repo.monthTxns(month) }.collectAsStateWithLifecycle(emptyList())
    val bills by remember { repo.bills.all() }.collectAsStateWithLifecycle(emptyList())
    val debts by remember { repo.debts.all() }.collectAsStateWithLifecycle(emptyList())
    val leases by remember { repo.leases.all() }.collectAsStateWithLifecycle(emptyList())
    val properties by remember { repo.properties.all() }.collectAsStateWithLifecycle(emptyList())
    val categories by remember { repo.categories.all() }.collectAsStateWithLifecycle(emptyList())
    val daysAhead by repo.settings.reminderDaysAhead.collectAsStateWithLifecycle()

    var editingBill by remember { mutableStateOf<Bill?>(null) }
    var addingBill by remember { mutableStateOf(false) }
    var payingDue by remember { mutableStateOf<DueItem?>(null) }

    val dues = Dues.forMonth(month, bills, debts, leases, properties, txns)

    AppScaffold(
        title = "Reminders",
        onBack = { nav.popBackStack() },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { addingBill = true },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Add bill") },
            )
        },
    ) { padding ->
        LazyColumn(
            Modifier.padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Text(
                    "You'll get a notification $daysAhead day${if (daysAhead == 1) "" else "s"} before each due date and on the day. " +
                        "Debts with a monthly payment and current tenants are included automatically.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            item {
                SectionCard("Due this month") {
                    if (dues.isEmpty()) {
                        Text("Nothing scheduled yet. Add your bills, like school fees, DEWA or phone.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    dues.forEach { due ->
                        DueRow(due, today) { payingDue = due }
                    }
                }
            }
            if (bills.isNotEmpty()) {
                item { Text("Monthly bills", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 8.dp)) }
                items(bills, key = { it.id }) { b ->
                    SectionCard(modifier = Modifier.clickable { editingBill = b }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(b.title, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    "${b.kind.label}  ·  day ${b.dueDay}  ·  ${money(b.amount)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Switch(checked = b.enabled, onCheckedChange = { on -> scope.launch { repo.bills.upsert(b.copy(enabled = on)) } })
                        }
                    }
                }
            }
        }
    }

    if (addingBill || editingBill != null) {
        BillDialog(
            bill = editingBill,
            categories = categories.filter { it.type == TxType.EXPENSE && !it.archived },
            onSave = { scope.launch { repo.bills.upsert(it) } },
            onDelete = { b -> scope.launch { repo.bills.delete(b) } },
            onDismiss = {
                addingBill = false
                editingBill = null
            },
        )
    }

    val dueToPay = payingDue
    if (dueToPay != null) {
        val due = dueToPay
        when (due.source) {
            DueSource.BILL -> {
                val bill = bills.firstOrNull { it.id == due.sourceId }
                if (bill != null) {
                    AlertDialog(
                        onDismissRequest = { payingDue = null },
                        title = { Text("Mark ${bill.title} paid?") },
                        text = { Text("${money(bill.amount)} will be saved as an expense for today.") },
                        confirmButton = {
                            TextButton(onClick = {
                                scope.launch { repo.markBillPaid(bill, LocalDate.now()) }
                                payingDue = null
                            }) { Text("Mark paid") }
                        },
                        dismissButton = { TextButton(onClick = { payingDue = null }) { Text("Cancel") } },
                    )
                } else {
                    payingDue = null
                }
            }
            DueSource.DEBT -> {
                val debt = debts.firstOrNull { it.id == due.sourceId }
                if (debt != null) {
                    PaymentDialog(
                        title = "Payment for ${debt.name}",
                        initial = debt.monthlyPayment,
                        onSave = { amount, date -> scope.launch { repo.recordDebtPayment(debt, amount, date) } },
                        onDismiss = { payingDue = null },
                    )
                } else {
                    payingDue = null
                }
            }
            DueSource.RENT -> {
                val lease = leases.firstOrNull { it.id == due.sourceId }
                if (lease != null) {
                    RentDialog(
                        lease = lease,
                        onSave = { amount, date -> scope.launch { repo.recordRent(lease, amount, date) } },
                        onDismiss = { payingDue = null },
                    )
                } else {
                    payingDue = null
                }
            }
        }
    }
}

@Composable
private fun DueRow(due: DueItem, today: LocalDate, onAction: () -> Unit) {
    val days = due.daysUntil(today)
    val status = when {
        due.done -> if (due.source == DueSource.RENT) "Collected" else "Paid"
        days < 0 -> "Overdue by ${-days} day${if (days == -1L) "" else "s"}"
        days == 0L -> "Due today"
        else -> "Due ${due.dueDate.prettyShort()}"
    }
    val color = when {
        due.done -> MoneyColors.income()
        days < 0 -> MoneyColors.expense()
        days <= 3 -> MoneyColors.warning()
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(due.title, style = MaterialTheme.typography.bodyLarge)
            Text("${due.subtitle}  ·  ${money(due.amount)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(status, style = MaterialTheme.typography.bodySmall, color = color)
        }
        Spacer(Modifier.width(8.dp))
        if (due.done) {
            Icon(Icons.Filled.CheckCircle, contentDescription = "Done", tint = MoneyColors.income())
        } else {
            FilledTonalButton(onClick = onAction) {
                Text(if (due.source == DueSource.RENT) "Received" else "Paid")
            }
        }
    }
}

@Composable
private fun BillDialog(
    bill: Bill?,
    categories: List<Category>,
    onSave: (Bill) -> Unit,
    onDelete: (Bill) -> Unit,
    onDismiss: () -> Unit,
) {
    var title by remember { mutableStateOf(bill?.title ?: "") }
    var amount by remember { mutableStateOf(bill?.amount?.asInput() ?: "") }
    var dueDay by remember { mutableStateOf(bill?.dueDay?.toString() ?: "1") }
    var kind by remember { mutableStateOf(bill?.kind ?: BillKind.BILL) }
    var category by remember { mutableStateOf(categories.firstOrNull { it.id == bill?.categoryId }) }
    val parsed = parseAmount(amount)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (bill == null) "Add bill" else "Edit bill") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                TextInput(title, { title = it }, "Name (for example: School fees)")
                AmountField(amount, { amount = it }, "Amount", prefix = LocalCurrency.current)
                NumberField(dueDay, { dueDay = it }, "Due day of month (1-31)")
                DropdownField("Type", BillKind.entries.toList(), kind, { it.label }, { kind = it })
                DropdownField("Expense category", categories, category, { it.name }, { category = it })
            }
        },
        confirmButton = {
            TextButton(
                enabled = title.isNotBlank() && parsed != null && parsed > 0,
                onClick = {
                    val base = bill ?: Bill(title = "", amount = 0.0, dueDay = 1)
                    onSave(
                        base.copy(
                            title = title.trim(),
                            amount = parsed ?: 0.0,
                            dueDay = (dueDay.toIntOrNull() ?: 1).coerceIn(1, 31),
                            kind = kind,
                            categoryId = category?.id,
                        ),
                    )
                    onDismiss()
                },
            ) { Text("Save") }
        },
        dismissButton = {
            Row {
                if (bill != null) {
                    TextButton(onClick = {
                        onDelete(bill)
                        onDismiss()
                    }) { Text("Delete") }
                }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        },
    )
}
