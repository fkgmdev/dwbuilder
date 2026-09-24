package com.dwbuilder.app.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dwbuilder.app.data.GameData
import com.dwbuilder.app.domain.MantraRules
import com.dwbuilder.app.domain.model.Mantra
import com.dwbuilder.app.ui.BuilderViewModel
import com.dwbuilder.app.ui.components.ListRowCard
import com.dwbuilder.app.ui.components.num
import kotlinx.coroutines.delay

@Composable
fun MantrasScreen(vm: BuilderViewModel) {
    val data = vm.data ?: return
    val build = vm.build
    var query by rememberSaveable { mutableStateOf("") }
    var debouncedQuery by rememberSaveable { mutableStateOf("") }
    var categoryFilter by rememberSaveable { mutableStateOf("All") }

    LaunchedEffect(query) {
        if (query != debouncedQuery) {
            delay(120)
            debouncedQuery = query
        }
    }

    val oath = data.oath(build.oath)
    val slots = remember(build.talents, oath) { MantraRules.availableSlots(build.talents, oath) }
    val assigned = remember(build.mantras, slots, data) {
        MantraRules.assign(build.mantras, data::mantra, slots)
    }

    val categories = listOf("All") + MantraRules.SLOT_CATEGORIES
    val all = remember(data) { data.mantras.values.sortedBy { it.name } }
    val filtered = remember(debouncedQuery, categoryFilter, all) {
        all.filter { m ->
            (categoryFilter == "All" || m.category == categoryFilter) &&
                (debouncedQuery.isBlank() ||
                    m.name.contains(debouncedQuery, ignoreCase = true) ||
                    m.description.contains(debouncedQuery, ignoreCase = true))
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item { SlotSummary(slots, assigned) }
        item {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search mantras…") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true,
            )
        }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(categories) { c ->
                    FilterChip(selected = categoryFilter == c, onClick = { categoryFilter = c }, label = { Text(c) })
                }
            }
        }
        items(filtered, key = { it.name }, contentType = { "mantra" }) { mantra ->
            MantraRow(
                mantra = mantra,
                taken = mantra.name in build.mantras,
                canAdd = canAdd(mantra, assigned, slots),
                onClick = { vm.toggleMantra(mantra.name) },
            )
        }
    }
}

private fun canAdd(m: Mantra, assigned: MantraRules.AssignedSlots, slots: Map<String, Int>): Boolean {
    val count = assigned.counts[m.category] ?: 0
    if (count < (slots[m.category] ?: 0)) return true
    if (m.category == "Wisp" && (assigned.counts["Support"] ?: 0) < (slots["Support"] ?: 0)) return true
    return (assigned.counts["Wildcard"] ?: 0) < (slots["Wildcard"] ?: 0)
}

@Composable
private fun SlotSummary(slots: Map<String, Int>, assigned: MantraRules.AssignedSlots) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Slots", style = MaterialTheme.typography.titleMedium)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MantraRules.SLOT_CATEGORIES.forEach { category ->
                    val used = assigned.counts[category] ?: 0
                    val max = slots[category] ?: 0
                    SuggestionChip(onClick = {}, label = { Text("$category $used/$max") })
                }
            }
            if (assigned.overflow > 0) {
                Text(
                    "$assigned.overflow mantras over the cap — remove some to free slots",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun MantraRow(mantra: Mantra, taken: Boolean, canAdd: Boolean, onClick: () -> Unit) {
    val level1 = mantra.damage.firstOrNull()?.levels?.firstOrNull()
    val scaling = mantra.scaling.entries.joinToString(" ") { (attunement, factor) -> "$attunement ×${num(factor)}" }
    ListRowCard(
        selected = taken,
        enabled = taken || canAdd,
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        content = {
            Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(mantra.name, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
                    if (taken) {
                        Icon(Icons.Filled.Check, contentDescription = "Taken", tint = MaterialTheme.colorScheme.primary)
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier.padding(start = 6.dp),
                        ) {
                            Text(mantra.category, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                        }
                    }
                }
                Text(
                    mantra.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                val attrText = mantra.attributes.joinToString(" / ")
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (level1?.damage != null) {
                        Text("L1 ${num(level1.damage)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    }
                    if (attrText.isNotBlank()) {
                        Text("· $attrText", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                if (scaling.isNotBlank()) {
                    Text("Scaling: $scaling", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (mantra.vaulted) {
                    Text("Vaulted", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                }
            }
        },
    )
}