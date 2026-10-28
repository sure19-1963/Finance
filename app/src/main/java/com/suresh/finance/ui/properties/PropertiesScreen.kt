package com.suresh.finance.ui.properties

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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.suresh.finance.data.Lease
import com.suresh.finance.data.Property
import com.suresh.finance.data.TxType
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
import com.suresh.finance.ui.epochDay
import com.suresh.finance.ui.money
import com.suresh.finance.ui.parseAmount
import com.suresh.finance.ui.pretty
import com.suresh.finance.ui.repo
import com.suresh.finance.ui.theme.MoneyColors
import com.suresh.finance.ui.transactions.TxnRow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

val PROPERTY_KINDS = listOf("Apartment", "Villa", "Townhouse", "Studio", "Room", "Shop", "Office", "Warehouse", "Land", "Other")

@Composable
fun PropertiesScreen(nav: NavHostController) {
    val repo = repo()
    val properties by remember { repo.properties.all() }.collectAsStateWithLifecycle(emptyList())
    val leases by remember { repo.leases.all() }.collectAsStateWithLifecycle(emptyList())
    val month = YearMonth.now()
    val txnsFlow = remember { repo.monthTxns(month) }
    val monthTxns by txnsFlow.collectAsStateWithLifecycle(emptyList())

    val active = leases.filter { it.active }
    val occupiedIds = active.map { it.propertyId }.toSet()
    val rentDue = active.sumOf { it.monthlyRent }
    val collected = monthTxns.filter { it.type == TxType.INCOME && it.propertyId != null }.sumOf { it.amount }
    val potential = properties.filter { it.id !in occupiedIds }.sumOf { it.expectedRent }

    AppScaffold(
        title = "Rental properties",
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { nav.navigate(Routes.propertyEdit()) },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Add property") },
            )
        },
    ) { padding ->
        if (properties.isEmpty()) {
            EmptyState(
                "Add the houses or flats you rent out. You can record tenants and agreements, collect rent, and track maintenance costs.",
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
                    StatTile("Occupied", "${occupiedIds.size} of ${properties.size}", MaterialTheme.colorScheme.primary, Modifier.weight(1f))
                    StatTile("Rent this month", money(collected), MoneyColors.income(), Modifier.weight(1f))
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatTile("Rent due per month", money(rentDue), MaterialTheme.colorScheme.onSurface, Modifier.weight(1f))
                    StatTile("Vacant: lost per month", money(potential), MoneyColors.warning(), Modifier.weight(1f))
                }
            }
            items(properties, key = { it.id }) { p ->
                val lease = active.firstOrNull { it.propertyId == p.id }
                SectionCard(modifier = Modifier.clickable { nav.navigate(Routes.property(p.id)) }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(p.name, style = MaterialTheme.typography.titleMedium)
                            Text(
                                listOf(p.kind, p.address).filter { it.isNotBlank() }.joinToString("  ·  "),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        AssistChip(
                            onClick = { nav.navigate(Routes.property(p.id)) },
                            label = { Text(if (lease != null) "Occupied" else "Vacant") },
                        )
                    }
                    if (lease != null) {
                        LabelValue("Tenant", lease.tenantName)
                        LabelValue("Rent", "${money(lease.monthlyRent)} / month")
                        lease.endDate?.let { end -> LeaseEndWarning(epochDay(end)) }
                    } else {
                        LabelValue("Expected rent", "${money(p.expectedRent)} / month")
                    }
                }
            }
        }
    }
}

@Composable
private fun LeaseEndWarning(end: LocalDate) {
    val days = ChronoUnit.DAYS.between(LocalDate.now(), end)
    when {
        days < 0 -> Text("Agreement ended ${end.pretty()}", color = MoneyColors.expense(), style = MaterialTheme.typography.bodySmall)
        days <= 60 -> Text("Agreement ends in $days days (${end.pretty()})", color = MoneyColors.warning(), style = MaterialTheme.typography.bodySmall)
        else -> LabelValue("Agreement ends", end.pretty())
    }
}

@Composable
fun PropertyDetailScreen(nav: NavHostController, id: Long) {
    val repo = repo()
    val scope = rememberCoroutineScope()
    val property by remember(id) { repo.properties.observe(id) }.collectAsStateWithLifecycle(null)
    val leases by remember(id) { repo.leases.forProperty(id) }.collectAsStateWithLifecycle(emptyList())
    val txns by remember(id) { repo.txns.forProperty(id) }.collectAsStateWithLifecycle(emptyList())
    val categories by remember { repo.categories.all() }.collectAsStateWithLifecycle(emptyList())
    val byId = categories.associateBy { it.id }
    var confirmDelete by remember { mutableStateOf(false) }
    var rentFor by remember { mutableStateOf<Lease?>(null) }

    val p = property
    AppScaffold(
        title = p?.name ?: "Property",
        onBack = { nav.popBackStack() },
        actions = {
            IconButton(onClick = { nav.navigate(Routes.propertyEdit(id)) }) { Icon(Icons.Filled.Edit, contentDescription = "Edit") }
            IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Filled.Delete, contentDescription = "Delete") }
        },
    ) { padding ->
        if (p == null) return@AppScaffold
        val year = LocalDate.now().year
        val yearStart = LocalDate.of(year, 1, 1).toEpochDay()
        val yearTxns = txns.filter { it.date >= yearStart }
        val yIncome = yearTxns.filter { it.type == TxType.INCOME }.sumOf { it.amount }
        val yExpense = yearTxns.filter { it.type == TxType.EXPENSE }.sumOf { it.amount }
        val active = leases.firstOrNull { it.active }

        LazyColumn(
            Modifier.padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                SectionCard("Overview") {
                    if (p.address.isNotBlank()) LabelValue("Address", p.address)
                    LabelValue("Type", p.kind)
                    LabelValue("Status", if (active != null) "Occupied" else "Vacant")
                    LabelValue("Expected rent", "${money(p.expectedRent)} / month")
                    if (p.marketValue > 0) LabelValue("Market value", money(p.marketValue))
                    if (p.notes.isNotBlank()) Text(p.notes, style = MaterialTheme.typography.bodySmall)
                }
            }
            item {
                SectionCard("$year so far") {
                    LabelValue("Rent received", money(yIncome), valueColor = MoneyColors.income())
                    LabelValue("Maintenance and costs", money(yExpense), valueColor = MoneyColors.expense())
                    LabelValue("Net from this property", money(yIncome - yExpense), valueStyle = MaterialTheme.typography.titleSmall)
                }
            }
            item {
                SectionCard(
                    "Current agreement",
                    action = {
                        TextButton(onClick = { nav.navigate(Routes.lease(id, active?.id ?: 0L)) }) {
                            Text(if (active == null) "Add tenant" else "Edit")
                        }
                    },
                ) {
                    if (active == null) {
                        Text("Vacant. Add a tenant when it's rented out.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        LeaseDetails(active)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { rentFor = active }) { Text("Rent received") }
                            OutlinedButton(onClick = { nav.navigate(Routes.txn(type = TxType.EXPENSE, propertyId = id)) }) {
                                Text("Add expense")
                            }
                        }
                    }
                }
            }
            if (active == null) {
                item {
                    OutlinedButton(
                        onClick = { nav.navigate(Routes.txn(type = TxType.EXPENSE, propertyId = id)) },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Add maintenance expense") }
                }
            }
            val past = leases.filter { !it.active }
            if (past.isNotEmpty()) {
                item { Text("Past agreements", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 8.dp)) }
                items(past, key = { "l${it.id}" }) { l ->
                    SectionCard(modifier = Modifier.clickable { nav.navigate(Routes.lease(id, l.id)) }) {
                        Text(l.tenantName, style = MaterialTheme.typography.titleSmall)
                        Text(
                            epochDay(l.startDate).pretty() + " to " + (l.endDate?.let { epochDay(it).pretty() } ?: "open"),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
            if (txns.isNotEmpty()) {
                item { Text("Rent and expenses", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 8.dp)) }
                items(txns.take(50), key = { "t${it.id}" }) { t ->
                    TxnRow(t, byId[t.categoryId], showDate = true) { nav.navigate(Routes.txn(id = t.id)) }
                }
            }
        }
    }

    rentFor?.let { lease ->
        RentDialog(
            lease = lease,
            onSave = { amount, date -> scope.launch { repo.recordRent(lease, amount, date) } },
            onDismiss = { rentFor = null },
        )
    }

    if (confirmDelete && p != null) {
        ConfirmDialog(
            title = "Delete ${p.name}?",
            text = "Its agreements will be deleted. Rent and expense entries stay in your records.",
            onConfirm = {
                scope.launch {
                    repo.deleteProperty(p)
                    nav.popBackStack()
                }
            },
            onDismiss = { confirmDelete = false },
        )
    }
}

@Composable
private fun LeaseDetails(l: Lease) {
    LabelValue("Tenant", l.tenantName)
    if (l.tenantPhone.isNotBlank()) LabelValue("Phone", l.tenantPhone)
    LabelValue("Rent", "${money(l.monthlyRent)} / month (${money(l.monthlyRent * 12)} / year)")
    LabelValue("Rent due on", "Day ${l.rentDueDay} of each month")
    LabelValue("Start", epochDay(l.startDate).pretty())
    l.endDate?.let { LeaseEndWarning(epochDay(it)) }
    if (l.deposit > 0) LabelValue("Security deposit", money(l.deposit))
    if (l.chequeCount > 0) LabelValue("Payment", "${l.chequeCount} cheque${if (l.chequeCount == 1) "" else "s"}")
    if (l.notes.isNotBlank()) Text(l.notes, style = MaterialTheme.typography.bodySmall)
}

@Composable
fun RentDialog(lease: Lease, onSave: (Double, LocalDate) -> Unit, onDismiss: () -> Unit) {
    var amount by remember { mutableStateOf(lease.monthlyRent.asInput()) }
    var date by remember { mutableStateOf(LocalDate.now()) }
    val parsed = parseAmount(amount)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rent from ${lease.tenantName}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                AmountField(amount, { amount = it }, "Amount received", prefix = LocalCurrency.current)
                DateField("Date received", date, { date = it })
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
fun PropertyEditScreen(nav: NavHostController, id: Long) {
    val repo = repo()
    val scope = rememberCoroutineScope()
    var existing by remember { mutableStateOf<Property?>(null) }
    var name by rememberSaveable { mutableStateOf("") }
    var address by rememberSaveable { mutableStateOf("") }
    var kind by rememberSaveable { mutableStateOf(PROPERTY_KINDS.first()) }
    var rent by rememberSaveable { mutableStateOf("") }
    var value by rememberSaveable { mutableStateOf("") }
    var notes by rememberSaveable { mutableStateOf("") }
    var loaded by rememberSaveable { mutableStateOf(false) }
    var showErrors by remember { mutableStateOf(false) }

    LaunchedEffect(id) {
        if (id == 0L) return@LaunchedEffect
        val p = repo.properties.get(id) ?: return@LaunchedEffect
        existing = p
        if (!loaded) {
            name = p.name; address = p.address; kind = p.kind
            rent = p.expectedRent.asInput(); value = p.marketValue.asInput(); notes = p.notes
            loaded = true
        }
    }

    AppScaffold(title = if (id == 0L) "Add property" else "Edit property", onBack = { nav.popBackStack() }) { padding ->
        Column(
            Modifier
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TextInput(name, { name = it }, "Name (for example: Marina flat 1203)", isError = showErrors && name.isBlank())
            TextInput(address, { address = it }, "Address")
            DropdownField("Type", PROPERTY_KINDS, kind, { it }, { kind = it })
            AmountField(rent, { rent = it }, "Expected monthly rent", prefix = LocalCurrency.current)
            AmountField(value, { value = it }, "Market value (optional)", prefix = LocalCurrency.current)
            TextInput(notes, { notes = it }, "Notes", singleLine = false)
            Button(
                onClick = {
                    if (name.isBlank()) {
                        showErrors = true
                        return@Button
                    }
                    scope.launch {
                        val base = existing ?: Property(name = "")
                        val newId = repo.properties.upsert(
                            base.copy(
                                name = name.trim(),
                                address = address.trim(),
                                kind = kind,
                                expectedRent = parseAmount(rent) ?: 0.0,
                                marketValue = parseAmount(value) ?: 0.0,
                                notes = notes.trim(),
                            ),
                        )
                        nav.popBackStack()
                        if (id == 0L && newId > 0) nav.navigate(Routes.property(newId))
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Save") }
        }
    }
}

@Composable
fun LeaseEditScreen(nav: NavHostController, propertyId: Long, id: Long) {
    val repo = repo()
    val scope = rememberCoroutineScope()
    var existing by remember { mutableStateOf<Lease?>(null) }
    var tenant by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }
    var start by rememberSaveable { mutableStateOf(LocalDate.now()) }
    var end by rememberSaveable { mutableStateOf<LocalDate?>(LocalDate.now().plusYears(1).minusDays(1)) }
    var rent by rememberSaveable { mutableStateOf("") }
    var deposit by rememberSaveable { mutableStateOf("") }
    var dueDay by rememberSaveable { mutableStateOf("1") }
    var cheques by rememberSaveable { mutableStateOf("") }
    var notes by rememberSaveable { mutableStateOf("") }
    var active by rememberSaveable { mutableStateOf(true) }
    var loaded by rememberSaveable { mutableStateOf(false) }
    var showErrors by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    LaunchedEffect(id) {
        if (id == 0L) {
            if (!loaded) {
                repo.properties.get(propertyId)?.let { if (it.expectedRent > 0) rent = it.expectedRent.asInput() }
                loaded = true
            }
            return@LaunchedEffect
        }
        val l = repo.leases.get(id) ?: return@LaunchedEffect
        existing = l
        if (!loaded) {
            tenant = l.tenantName; phone = l.tenantPhone
            start = epochDay(l.startDate); end = l.endDate?.let { epochDay(it) }
            rent = l.monthlyRent.asInput(); deposit = l.deposit.asInput()
            dueDay = l.rentDueDay.toString(); cheques = if (l.chequeCount > 0) l.chequeCount.toString() else ""
            notes = l.notes; active = l.active
            loaded = true
        }
    }

    val rentValue = parseAmount(rent)
    AppScaffold(
        title = if (id == 0L) "Add tenant" else "Rental agreement",
        onBack = { nav.popBackStack() },
        actions = {
            if (id != 0L) IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Filled.Delete, contentDescription = "Delete") }
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
            TextInput(tenant, { tenant = it }, "Tenant name", isError = showErrors && tenant.isBlank())
            TextInput(phone, { phone = it }, "Phone", keyboardType = androidx.compose.ui.text.input.KeyboardType.Phone)
            DateField("Agreement start", start, { start = it })
            DateField("Agreement end", end, { end = it })
            AmountField(
                rent, { rent = it }, "Monthly rent",
                prefix = LocalCurrency.current,
                isError = showErrors && (rentValue == null || rentValue <= 0),
            )
            if (rentValue != null && rentValue > 0) {
                Text(
                    "That's ${money(rentValue * 12)} a year. For a yearly rent, enter it divided by 12.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            AmountField(deposit, { deposit = it }, "Security deposit", prefix = LocalCurrency.current)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                NumberField(dueDay, { dueDay = it }, "Rent due day (1-31)", Modifier.weight(1f))
                NumberField(cheques, { cheques = it }, "Number of cheques", Modifier.weight(1f))
            }
            TextInput(notes, { notes = it }, "Notes", singleLine = false)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Current agreement", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "Turn off when the tenant moves out. The property then shows as vacant.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.width(8.dp))
                Switch(checked = active, onCheckedChange = { active = it })
            }
            Button(
                onClick = {
                    if (tenant.isBlank() || rentValue == null || rentValue <= 0) {
                        showErrors = true
                        return@Button
                    }
                    scope.launch {
                        val base = existing ?: Lease(propertyId = propertyId, tenantName = "", startDate = 0, monthlyRent = 0.0)
                        val lease = base.copy(
                            tenantName = tenant.trim(),
                            tenantPhone = phone.trim(),
                            startDate = start.toEpochDay(),
                            endDate = end?.toEpochDay(),
                            monthlyRent = rentValue,
                            deposit = parseAmount(deposit) ?: 0.0,
                            rentDueDay = (dueDay.toIntOrNull() ?: 1).coerceIn(1, 31),
                            chequeCount = cheques.toIntOrNull() ?: 0,
                            notes = notes.trim(),
                            active = active,
                        )
                        // Only one current agreement per property.
                        if (active) {
                            repo.leases.activeOnce()
                                .filter { it.propertyId == propertyId && it.id != lease.id }
                                .forEach { repo.leases.upsert(it.copy(active = false)) }
                        }
                        repo.leases.upsert(lease)
                        nav.popBackStack()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Save") }
        }
    }

    if (confirmDelete) {
        ConfirmDialog(
            title = "Delete this agreement?",
            text = "Rent entries already recorded stay in your records.",
            onConfirm = {
                scope.launch {
                    existing?.let { repo.leases.delete(it) }
                    nav.popBackStack()
                }
            },
            onDismiss = { confirmDelete = false },
        )
    }
}
