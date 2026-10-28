package com.suresh.finance.ui.assets

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.suresh.finance.data.Asset
import com.suresh.finance.data.AssetKind
import com.suresh.finance.ui.LocalCurrency
import com.suresh.finance.ui.asInput
import com.suresh.finance.ui.charts.DonutChart
import com.suresh.finance.ui.charts.Legend
import com.suresh.finance.ui.charts.Slice
import com.suresh.finance.ui.components.AmountField
import com.suresh.finance.ui.components.AppScaffold
import com.suresh.finance.ui.components.DropdownField
import com.suresh.finance.ui.components.LabelValue
import com.suresh.finance.ui.components.SectionCard
import com.suresh.finance.ui.components.TextInput
import com.suresh.finance.ui.formatCompact
import com.suresh.finance.ui.formatMoney
import com.suresh.finance.ui.money
import com.suresh.finance.ui.parseAmount
import com.suresh.finance.ui.repo
import com.suresh.finance.ui.theme.MoneyColors
import kotlinx.coroutines.launch

private val KIND_COLORS = mapOf(
    AssetKind.CASH to Color(0xFF43A047),
    AssetKind.BANK to Color(0xFF1E88E5),
    AssetKind.INVESTMENT to Color(0xFF8E24AA),
    AssetKind.GOLD to Color(0xFFFFB300),
    AssetKind.VEHICLE to Color(0xFF6D4C41),
    AssetKind.OTHER to Color(0xFF757575),
)
private val PROPERTY_COLOR = Color(0xFF00897B)

@Composable
fun AssetsScreen(nav: NavHostController) {
    val repo = repo()
    val scope = rememberCoroutineScope()
    val currency = LocalCurrency.current
    val assets by remember { repo.assets.all() }.collectAsStateWithLifecycle(emptyList())
    val properties by remember { repo.properties.all() }.collectAsStateWithLifecycle(emptyList())
    val debts by remember { repo.debts.all() }.collectAsStateWithLifecycle(emptyList())
    var editing by remember { mutableStateOf<Asset?>(null) }
    var adding by remember { mutableStateOf(false) }

    val propertyValue = properties.sumOf { it.marketValue }
    val total = assets.sumOf { it.value } + propertyValue
    val owed = debts.filter { !it.closed }.sumOf { it.balance }
    val slices = AssetKind.entries.mapNotNull { k ->
        val v = assets.filter { it.kind == k }.sumOf { it.value }
        if (v > 0) Slice(k.label, v, KIND_COLORS.getValue(k)) else null
    } + listOfNotNull(if (propertyValue > 0) Slice("Property", propertyValue, PROPERTY_COLOR) else null)

    AppScaffold(
        title = "Assets & net worth",
        onBack = { nav.popBackStack() },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { adding = true },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Add asset") },
            )
        },
    ) { padding ->
        LazyColumn(
            Modifier.padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                SectionCard("Net worth") {
                    LabelValue("Assets", money(total), valueColor = MoneyColors.income())
                    LabelValue("Debts", money(owed), valueColor = MoneyColors.expense())
                    LabelValue("Net worth", money(total - owed), valueStyle = MaterialTheme.typography.titleLarge)
                }
            }
            if (slices.isNotEmpty()) {
                item {
                    SectionCard("What you own") {
                        DonutChart(slices, Modifier) {
                            Text(formatCompact(total), style = MaterialTheme.typography.titleLarge)
                        }
                        Legend(slices, total, { formatMoney(it, currency) })
                    }
                }
            }
            if (properties.any { it.marketValue > 0 }) {
                item {
                    Text(
                        "Property values come from your rental properties. Edit a property to change its value.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            items(assets, key = { it.id }) { a ->
                SectionCard(modifier = Modifier.clickable { editing = a }) {
                    Row {
                        Column(Modifier.weight(1f)) {
                            Text(a.name, style = MaterialTheme.typography.titleSmall)
                            Text(a.kind.label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(money(a.value), style = MaterialTheme.typography.titleSmall)
                    }
                }
            }
        }
    }

    if (adding || editing != null) {
        AssetDialog(
            asset = editing,
            onSave = { scope.launch { repo.assets.upsert(it) } },
            onDelete = { a -> scope.launch { repo.assets.delete(a) } },
            onDismiss = {
                adding = false
                editing = null
            },
        )
    }
}

@Composable
private fun AssetDialog(asset: Asset?, onSave: (Asset) -> Unit, onDelete: (Asset) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf(asset?.name ?: "") }
    var kind by remember { mutableStateOf(asset?.kind ?: AssetKind.BANK) }
    var value by remember { mutableStateOf(asset?.value?.asInput() ?: "") }
    val parsed = parseAmount(value)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (asset == null) "Add asset" else "Edit asset") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                TextInput(name, { name = it }, "Name (for example: Savings account)")
                DropdownField("Type", AssetKind.entries.toList(), kind, { it.label }, { kind = it })
                AmountField(value, { value = it }, "Current value", prefix = LocalCurrency.current)
            }
        },
        confirmButton = {
            TextButton(enabled = name.isNotBlank() && parsed != null, onClick = {
                val base = asset ?: Asset(name = "", kind = kind, value = 0.0)
                onSave(base.copy(name = name.trim(), kind = kind, value = parsed ?: 0.0))
                onDismiss()
            }) { Text("Save") }
        },
        dismissButton = {
            Row {
                if (asset != null) {
                    TextButton(onClick = {
                        onDelete(asset)
                        onDismiss()
                    }) { Text("Delete") }
                }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        },
    )
}
