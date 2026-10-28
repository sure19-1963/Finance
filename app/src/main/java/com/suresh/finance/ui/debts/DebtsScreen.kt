package com.suresh.finance.ui.debts

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.suresh.finance.data.Debt
import com.suresh.finance.data.DebtKind
import com.suresh.finance.data.FinanceMath
import com.suresh.finance.ui.LocalCurrency
import com.suresh.finance.ui.Routes
import com.suresh.finance.ui.asInput
import com.suresh.finance.ui.components.AmountField
import com.suresh.finance.ui.components.AppScaffold
import com.suresh.finance.ui.components.ConfirmDialog
import com.suresh.finance.ui.components.DateField
import com.suresh.finance.ui.components.DropdownField
import com.suresh.finance.ui.components.EmptyState
import com.suresh.finance.ui.components.LabelValue
import com.suresh.finance.ui.components.NumberField
import com.suresh.finance.ui.components.SectionCard
import com.suresh.finance.ui.components.StatTile
import com.suresh.finance.ui.components.TextInput
import com.suresh.finance.ui.components.UsageBar
import com.suresh.finance.ui.epochDay
import com.suresh.finance.ui.money
import com.suresh.finance.ui.parseAmount
import com.suresh.finance.ui.pretty
import com.suresh.finance.ui.repo
import com.suresh.finance.ui.theme.MoneyColors
import com.suresh.finance.ui.transactions.TxnRow
import kotlinx.coroutines.launch
import java.time.LocalDate

@Composable
fun DebtsScreen(nav: NavHostController) {
    val repo = repo()
    val debts by remember { repo.debts.all() }.collectAsStateWithLifecycle(emptyList())
    val open = debts.filter { !it.closed }
    val cards = open.filter { it.kind == DebtKind.CREDIT_CARD }
    val cardLimit = cards.sumOf { it.creditLimit }
    val cardBalance = cards.sumOf { it.balance }

    AppScaffold(
        title = "Debts",
        onBack = { nav.popBackStack() },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { nav.navigate(Routes.debtEdit()) },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Add debt") },
            )
        },
    ) { padding ->
        if (debts.isEmpty()) {
            EmptyState(
                "Add your credit cards, loans, mortgage and home payment plans to track balances, interest and payoff progress.",
                Modifier.padding(padding),
            )
            return@AppScaffold
        }
        LazyColumn(
            Modifier.padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatTile("Total owed", money(open.sumOf { it.balance }), MoneyColors.expense(), Modifier.weight(1f))
                    StatTile("Monthly payments", money(open.sumOf { it.monthlyPayment }), MaterialTheme.colorScheme.onSurface, Modifier.weight(1f))
                }
            }
            if (cardLimit > 0) {
                item {
                    SectionCard("Credit card usage") {
                        LabelValue("Used", "${money(cardBalance)} of ${money(cardLimit)} (${(cardBalance / cardLimit * 100).toInt()}%)")
                        UsageBar(cardBalance, cardLimit)
                        Text(
                            "Keeping usage under 30% helps your credit score.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            items(debts, key = { it.id }) { d -> DebtCard(d) { nav.navigate(Routes.debt(d.id)) } }
        }
    }
}

@Composable
private fun DebtCard(d: Debt, onClick: () -> Unit) {
    SectionCard(modifier = Modifier.clickable(onClick = onClick)) {
        Row {
            Column(Modifier.weight(1f)) {
                Text(d.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    listOf(d.kind.label, d.lender).filter { it.isNotBlank() }.joinToString("  ·  ") + if (d.closed) "  ·  Closed" else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(money(d.balance), style = MaterialTheme.typography.titleMedium, color = MoneyColors.expense())
        }
        if (d.kind == DebtKind.CREDIT_CARD && d.creditLimit > 0) {
            UsageBar(d.balance, d.creditLimit)
            LabelValue("Limit", money(d.creditLimit))
        } else if (d.originalAmount > 0) {
            val progress = FinanceMath.progress(d.originalAmount, d.balance)
            androidx.compose.material3.LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                color = MoneyColors.income(),
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
            LabelValue("Paid off", "${(progress * 100).toInt()}% of ${money(d.originalAmount)}")
        }
        if (d.monthlyPayment > 0 && !d.closed) LabelValue("Payment", "${money(d.monthlyPayment)} on day ${d.dueDay}")
    }
}

@Composable
fun DebtDetailScreen(nav: NavHostController, id: Long) {
    val repo = repo()
    val scope = rememberCoroutineScope()
    val debt by remember(id) { repo.debts.observe(id) }.collectAsStateWithLifecycle(null)
    val payments by remember(id) { repo.txns.forDebt(id) }.collectAsStateWithLifecycle(emptyList())
    val categories by remember { repo.categories.all() }.collectAsStateWithLifecycle(emptyList())
    val byId = categories.associateBy { it.id }
    var paying by remember { mutableStateOf(false) }
    var updatingBalance by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    val d = debt
    AppScaffold(
        title = d?.name ?: "Debt",
        onBack = { nav.popBackStack() },
        actions = {
            IconButton(onClick = { nav.navigate(Routes.debtEdit(id)) }) { Icon(Icons.Filled.Edit, contentDescription = "Edit") }
            IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Filled.Delete, contentDescription = "Delete") }
        },
    ) { padding ->
        if (d == null) return@AppScaffold
        val months = FinanceMath.monthsToPayoff(d.balance, d.interestRate, d.monthlyPayment)
        val interestLeft = FinanceMath.remainingInterest(d.balance, d.interestRate, d.monthlyPayment)
        LazyColumn(
            Modifier.padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                SectionCard {
                    LabelValue("Balance", money(d.balance), valueStyle = MaterialTheme.typography.titleLarge, valueColor = MoneyColors.expense())
                    LabelValue("Type", d.kind.label)
                    if (d.lender.isNotBlank()) LabelValue("Lender", d.lender)
                    if (d.originalAmount > 0 && d.kind != DebtKind.CREDIT_CARD) {
                        LabelValue("Original amount", money(d.originalAmount))
                        val progress = FinanceMath.progress(d.originalAmount, d.balance)
                        LabelValue("Paid off", "${money(d.originalAmount - d.balance)} (${(progress * 100).toInt()}%)")
                    }
                    if (d.kind == DebtKind.CREDIT_CARD && d.creditLimit > 0) {
                        LabelValue("Credit limit", money(d.creditLimit))
                        LabelValue("Available", money(d.creditLimit - d.balance))
                        UsageBar(d.balance, d.creditLimit)
                    }
                    LabelValue("Interest rate", "${d.interestRate}% a year")
                    if (d.balance > 0 && d.interestRate > 0) {
                        LabelValue("Interest this month", money(FinanceMath.monthlyInterest(d.balance, d.interestRate)))
                    }
                    if (d.monthlyPayment > 0) LabelValue("Monthly payment", "${money(d.monthlyPayment)} on day ${d.dueDay}")
                    LabelValue("Started", epochDay(d.startDate).pretty())
                    if (d.notes.isNotBlank()) Text(d.notes, style = MaterialTheme.typography.bodySmall)
                }
            }
            if (d.balance > 0 && d.monthlyPayment > 0) {
                item {
                    SectionCard("Payoff estimate") {
                        if (months == null) {
                            Text(
                                "The monthly payment doesn't cover the interest, so this balance won't go down. Try paying more each month.",
                                color = MoneyColors.expense(),
                            )
                        } else {
                            val payoff = LocalDate.now().plusMonths(months.toLong())
                            LabelValue("Months left", "$months (about ${months / 12} yr ${months % 12} mo)")
                            LabelValue("Debt-free by", payoff.withDayOfMonth(1).let { "${it.month.name.lowercase().replaceFirstChar { c -> c.uppercase() }} ${it.year}" })
                            interestLeft?.let { LabelValue("Interest still to pay", money(it)) }
                        }
                    }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { paying = true }, enabled = !d.closed) { Text("Record payment") }
                    OutlinedButton(onClick = { updatingBalance = true }) { Text("Update balance") }
                }
            }
            if (d.balance <= 0 && !d.closed) {
                item {
                    TextButton(onClick = { scope.launch { repo.debts.upsert(d.copy(closed = true)) } }) {
                        Text("Paid off. Mark as closed")
                    }
                }
            }
            if (payments.isNotEmpty()) {
                item { Text("Payments", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 8.dp)) }
                items(payments, key = { it.id }) { t ->
                    TxnRow(t, byId[t.categoryId], showDate = true) { nav.navigate(Routes.txn(id = t.id)) }
                }
            }
        }
    }

    if (paying && d != null) {
        PaymentDialog(
            title = "Payment for ${d.name}",
            initial = d.monthlyPayment,
            onSave = { amount, date -> scope.launch { repo.recordDebtPayment(d, amount, date) } },
            onDismiss = { paying = false },
        )
    }
    if (updatingBalance && d != null) {
        BalanceDialog(
            current = d.balance,
            onSave = { scope.launch { repo.debts.upsert(d.copy(balance = it)) } },
            onDismiss = { updatingBalance = false },
        )
    }
    if (confirmDelete && d != null) {
        ConfirmDialog(
            title = "Delete ${d.name}?",
            text = "Payments already recorded stay in your expenses.",
            onConfirm = {
                scope.launch {
                    repo.debts.delete(d)
                    nav.popBackStack()
                }
            },
            onDismiss = { confirmDelete = false },
        )
    }
}

@Composable
fun PaymentDialog(title: String, initial: Double, onSave: (Double, LocalDate) -> Unit, onDismiss: () -> Unit) {
    var amount by remember { mutableStateOf(initial.asInput()) }
    var date by remember { mutableStateOf(LocalDate.now()) }
    val parsed = parseAmount(amount)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                AmountField(amount, { amount = it }, "Amount paid", prefix = LocalCurrency.current)
                DateField("Date paid", date, { date = it })
                Text(
                    "This is saved as an expense and lowers the balance. For loans, the month's interest is taken out first.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        },
        confirmButton = {
            TextButton(enabled = parsed != null && parsed > 0, onClick = {
                onSave(parsed ?: return@TextButton, date)
                onDismiss()
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun BalanceDialog(current: Double, onSave: (Double) -> Unit, onDismiss: () -> Unit) {
    var amount by remember { mutableStateOf(current.asInput()) }
    val parsed = parseAmount(amount.ifBlank { "0" })
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Update balance") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Enter the balance from your latest statement.", style = MaterialTheme.typography.bodyMedium)
                AmountField(amount, { amount = it }, "Current balance", prefix = LocalCurrency.current)
            }
        },
        confirmButton = {
            TextButton(enabled = parsed != null, onClick = {
                onSave(parsed ?: return@TextButton)
                onDismiss()
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
fun DebtEditScreen(nav: NavHostController, id: Long) {
    val repo = repo()
    val scope = rememberCoroutineScope()
    var existing by remember { mutableStateOf<Debt?>(null) }
    var name by rememberSaveable { mutableStateOf("") }
    var kind by rememberSaveable { mutableStateOf(DebtKind.CREDIT_CARD) }
    var lender by rememberSaveable { mutableStateOf("") }
    var original by rememberSaveable { mutableStateOf("") }
    var balance by rememberSaveable { mutableStateOf("") }
    var rate by rememberSaveable { mutableStateOf("") }
    var payment by rememberSaveable { mutableStateOf("") }
    var dueDay by rememberSaveable { mutableStateOf("1") }
    var limit by rememberSaveable { mutableStateOf("") }
    var term by rememberSaveable { mutableStateOf("") }
    var start by rememberSaveable { mutableStateOf(LocalDate.now()) }
    var notes by rememberSaveable { mutableStateOf("") }
    var closed by rememberSaveable { mutableStateOf(false) }
    var loaded by rememberSaveable { mutableStateOf(false) }
    var showErrors by remember { mutableStateOf(false) }

    LaunchedEffect(id) {
        if (id == 0L) return@LaunchedEffect
        val d = repo.debts.get(id) ?: return@LaunchedEffect
        existing = d
        if (!loaded) {
            name = d.name; kind = d.kind; lender = d.lender
            original = d.originalAmount.asInput(); balance = d.balance.asInput()
            rate = if (d.interestRate == 0.0) "" else d.interestRate.toString()
            payment = d.monthlyPayment.asInput(); dueDay = d.dueDay.toString()
            limit = d.creditLimit.asInput(); start = epochDay(d.startDate); notes = d.notes; closed = d.closed
            loaded = true
        }
    }

    val isCard = kind == DebtKind.CREDIT_CARD
    val currency = LocalCurrency.current
    AppScaffold(title = if (id == 0L) "Add debt" else "Edit debt", onBack = { nav.popBackStack() }) { padding ->
        Column(
            Modifier
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            DropdownField("Type", DebtKind.entries.toList(), kind, { it.label }, { kind = it })
            TextInput(name, { name = it }, if (isCard) "Card name (for example: ENBD Visa)" else "Name (for example: Car loan)", isError = showErrors && name.isBlank())
            TextInput(lender, { lender = it }, "Bank or lender")
            if (isCard) {
                AmountField(limit, { limit = it }, "Credit limit", prefix = currency)
            } else {
                AmountField(original, { original = it }, "Original amount borrowed", prefix = currency)
            }
            AmountField(balance, { balance = it }, "Balance owed now", prefix = currency, isError = showErrors && parseAmount(balance) == null)
            AmountField(rate, { rate = it }, "Interest rate (% per year)")
            if (!isCard) {
                NumberField(term, { term = it }, "Term in months (optional, to work out the EMI)")
                val p = parseAmount(original)
                val months = term.toIntOrNull()
                if (p != null && months != null && months > 0) {
                    val emi = FinanceMath.emi(p, parseAmount(rate) ?: 0.0, months)
                    TextButton(onClick = { payment = emi.asInput() }) { Text("Use calculated EMI: ${money(emi)}") }
                }
            }
            AmountField(payment, { payment = it }, if (isCard) "Usual monthly payment" else "Monthly payment (EMI)", prefix = currency)
            NumberField(dueDay, { dueDay = it }, "Payment due day (1-31)")
            DateField(if (isCard) "Card opened" else "Start date", start, { start = it })
            TextInput(notes, { notes = it }, "Notes", singleLine = false)
            if (existing != null) {
                DropdownField("Status", listOf(false, true), closed, { if (it) "Closed / paid off" else "Open" }, { closed = it })
            }
            Button(
                onClick = {
                    val bal = parseAmount(balance)
                    if (name.isBlank() || bal == null) {
                        showErrors = true
                        return@Button
                    }
                    scope.launch {
                        val base = existing ?: Debt(name = "", kind = kind, originalAmount = 0.0, balance = 0.0, startDate = 0)
                        repo.debts.upsert(
                            base.copy(
                                name = name.trim(),
                                kind = kind,
                                lender = lender.trim(),
                                originalAmount = if (isCard) bal else parseAmount(original) ?: bal,
                                balance = bal,
                                interestRate = parseAmount(rate) ?: 0.0,
                                monthlyPayment = parseAmount(payment) ?: 0.0,
                                dueDay = (dueDay.toIntOrNull() ?: 1).coerceIn(1, 31),
                                creditLimit = if (isCard) parseAmount(limit) ?: 0.0 else 0.0,
                                startDate = start.toEpochDay(),
                                notes = notes.trim(),
                                closed = closed,
                            ),
                        )
                        nav.popBackStack()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Save") }
        }
    }
}
