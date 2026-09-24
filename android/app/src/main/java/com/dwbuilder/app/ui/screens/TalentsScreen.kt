package com.dwbuilder.app.ui.screens

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import com.dwbuilder.app.domain.BuildContext
import com.dwbuilder.app.domain.Requirements
import com.dwbuilder.app.domain.TalentRules
import com.dwbuilder.app.domain.model.Talent
import com.dwbuilder.app.ui.BuilderViewModel
import com.dwbuilder.app.ui.components.ListRowCard
import com.dwbuilder.app.ui.components.RarityChip
import kotlinx.coroutines.delay

@Composable
fun TalentsScreen(vm: BuilderViewModel) {
    val data = vm.data ?: return
    val build = vm.build
    var query by rememberSaveable { mutableStateOf("") }
    var debouncedQuery by rememberSaveable { mutableStateOf("") }
    var rarityFilter by rememberSaveable { mutableStateOf("All") }

    // Debounce keystrokes so filtering 1150+ records never runs per keypress.
    LaunchedEffect(query) {
        if (query != debouncedQuery) {
            delay(120)
            debouncedQuery = query
        }
    }

    val ctx = remember(build) { BuildContext(build, data.talents, data.weapons) }
    val takenNames = build.talents

    val nonExempt = TalentRules.nonExemptMantraCount(build, data::mantra)
    val caps = TalentRules.caps(nonExempt)
    val faction = TalentRules.factionCount(build, data::talent, emptySet())

    val rarities = remember(data) {
        listOf("All") + data.talents.values.mapNotNull { it.rarity }.distinct().sorted()
    }
    val all = remember(data) { data.talents.values.sortedBy { it.name } }
    val filtered = remember(debouncedQuery, rarityFilter, all) {
        all.filter { t ->
            (rarityFilter == "All" || t.rarity == rarityFilter) &&
                (debouncedQuery.isBlank() ||
                    t.name.contains(debouncedQuery, ignoreCase = true) ||
                    t.description.contains(debouncedQuery, ignoreCase = true))
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search talents…") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true,
            )
        }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(rarities) { r ->
                    FilterChip(selected = rarityFilter == r, onClick = { rarityFilter = r }, label = { Text(r) })
                }
            }
        }
        item { TalentCountSummary(takenNames.size, caps, faction) }
        items(filtered, key = { it.name }, contentType = { "talent" }) { talent ->
            TalentRow(
                talent = talent,
                taken = talent.name in takenNames,
                eligible = TalentRules.eligible(talent, build),
                ctx = ctx,
                onClick = { vm.toggleTalent(talent.name) },
            )
        }
    }
}

@Composable
private fun TalentCountSummary(takenCount: Int, caps: TalentRules.TalentCaps, faction: Int) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                "$takenCount / ${caps.maxTotal} talents",
                style = MaterialTheme.typography.titleMedium,
                color = if (takenCount > caps.maxTotal) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
            )
            Text(
                "Roll 2 pool: ${caps.roll2} · Faction: $faction / ${TalentRules.MAX_FACTION}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun TalentRow(
    talent: Talent,
    taken: Boolean,
    eligible: Boolean,
    ctx: BuildContext,
    onClick: () -> Unit,
) {
    val check = Requirements.check(talent.requirements, ctx)
    val warning = check.warnings.firstOrNull()
    val reason = when {
        talent.rarity == "Oath" && talent.category != null -> "Requires the ${talent.category} oath"
        talent.rarity == "Origin" && talent.category != null -> "Requires the ${talent.category} origin"
        talent.rarity == "Innate" && talent.requirements?.aspect != null -> "Requires the ${talent.requirements.aspect} race"
        else -> null
    }
    ListRowCard(
        selected = taken,
        enabled = eligible,
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        content = {
            Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(talent.name, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
                    if (taken) {
                        Icon(Icons.Filled.Check, contentDescription = "Taken", tint = MaterialTheme.colorScheme.primary)
                    }
                    RarityChip(talent.rarity)
                }
                if (!talent.category.isNullOrBlank()) {
                    Text(talent.category, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                }
                Text(
                    talent.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (talent.vaulted) {
                    Text("Vaulted — no longer obtainable", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                }
                when {
                    !eligible -> Text(
                        reason ?: "Not obtainable for this build",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                    warning != null -> Text(
                        warning,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (check.hardMet) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error,
                    )
                    else -> Text("Requirements met", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.tertiary)
                }
            }
        },
    )
}