package com.dwbuilder.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dwbuilder.app.data.GameData
import com.dwbuilder.app.domain.Catalogs
import com.dwbuilder.app.domain.Points
import com.dwbuilder.app.domain.model.Build
import com.dwbuilder.app.ui.AttrKind
import com.dwbuilder.app.ui.BuilderViewModel
import com.dwbuilder.app.ui.attunementColors
import com.dwbuilder.app.ui.components.ChoiceChipRow
import com.dwbuilder.app.ui.components.DropdownCell
import com.dwbuilder.app.ui.components.SectionHeader
import com.dwbuilder.app.ui.components.StatStepper

@Composable
fun StatsScreen(vm: BuilderViewModel) {
    val data = vm.data ?: return
    val build = vm.build

    val pointStats = build.attributes.base
    val weaponStats = build.attributes.weapon
    val attrStats = build.attributes.attunement

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { PowerHeader(build) }
        item { TraitsCard(vm, build) }
        item { IdentityCard(vm, build, data) }
        item {
            Column {
                SectionHeader("Attributes")
                Catalogs.BASE_STATS.forEach { stat ->
                    StatStepper(
                        label = stat,
                        value = pointStats[stat] ?: 0,
                        plusEnabled = vm.pointsLeft > 0,
                        minusEnabled = (pointStats[stat] ?: 0) > 0,
                        onMinus = { vm.incrementAttr(AttrKind.BASE, stat, -1) },
                        onPlus = { vm.incrementAttr(AttrKind.BASE, stat, 1) },
                    )
                }
            }
        }
        item {
            Column {
                SectionHeader("Weapon Skills")
                Catalogs.WEAPON_STATS.forEach { stat ->
                    StatStepper(
                        label = stat,
                        value = weaponStats[stat] ?: 0,
                        plusEnabled = vm.pointsLeft > 0,
                        minusEnabled = (weaponStats[stat] ?: 0) > 0,
                        onMinus = { vm.incrementAttr(AttrKind.WEAPON, stat, -1) },
                        onPlus = { vm.incrementAttr(AttrKind.WEAPON, stat, 1) },
                    )
                }
            }
        }
        item {
            Column {
                SectionHeader("Attunements")
                Catalogs.ATTUNEMENT_STATS.forEach { stat ->
                    StatStepper(
                        label = stat,
                        value = attrStats[stat] ?: 0,
                        accent = attunementColors[stat],
                        plusEnabled = vm.pointsLeft > 0,
                        minusEnabled = (attrStats[stat] ?: 0) > 0,
                        onMinus = { vm.incrementAttr(AttrKind.ATTUNEMENT, stat, -1) },
                        onPlus = { vm.incrementAttr(AttrKind.ATTUNEMENT, stat, 1) },
                    )
                }
                Text(
                    "Each extra attunement after the first costs −1 point",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp),
                )
            }
        }
        item { BoonsFlaws(vm, build, data) }
    }
}

@Composable
private fun PowerHeader(build: Build) {
    val spent = Points.spent(build)
    val power = Points.power(build)
    val left = Points.pointsLeft(build)
    val next = Points.toNextPower(build)
    val level = if (build.level in 1..20) build.level else 20
    ElevatedCard(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("LVL $level", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        "$power",
                        style = MaterialTheme.typography.displayLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        "  POWER",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 10.dp),
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "$spent",
                    style = MaterialTheme.typography.headlineMedium,
                    color = if (left < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                )
                Text("of ${Points.BUDGET} points", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    if (left <= 0) "No points left" else "$left left · next power in $next",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (left < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary,
                )
            }
        }
    }
}

@Composable
private fun TraitsCard(vm: BuilderViewModel, build: Build) {
    val used = build.traits.values.sum()
    val effects = mapOf(
        "Vitality" to "+${Catalogs.VITALITY_HP_PER_POINT} HP/pt",
        "Erudition" to "Knowledge gains",
        "Proficiency" to "+6.5% weapon scale/pt",
        "Songchant" to "+${(Catalogs.SONGCHANT_SCALING_PER_POINT * 100).toInt()}% mantra scale/pt",
    )
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Traits", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Text(
                    "$used / 12",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (used >= 12) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text("Shared pool · max 6 each", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Catalogs.TRAITS.forEach { trait ->
                StatStepper(
                    label = "$trait (${effects[trait]})",
                    value = build.traits[trait] ?: 0,
                    plusEnabled = used < 12 && (build.traits[trait] ?: 0) < 6,
                    minusEnabled = (build.traits[trait] ?: 0) > 0,
                    onMinus = { vm.setTrait(trait, (build.traits[trait] ?: 0) - 1) },
                    onPlus = { vm.setTrait(trait, (build.traits[trait] ?: 0) + 1) },
                )
            }
        }
    }
}

@Composable
private fun IdentityCard(vm: BuilderViewModel, build: Build, data: GameData) {
    val oaths = data.oaths.values
        .filter { it.name != "None" }
        .sortedWith(compareBy({ it.name != "Oathless" }, { it.name }))
        .map { it.name }
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Identity", style = MaterialTheme.typography.titleMedium)
            DropdownCell(
                label = "Race",
                selected = build.race,
                options = data.raceNames,
                labelOf = { race -> Catalogs.raceLabel(race, data.aspect(race)?.statBonuses ?: emptyMap()) },
                onSelect = vm::setRace,
            )
            DropdownCell(label = "Origin", selected = build.origin, options = Catalogs.ORIGINS, onSelect = vm::setOrigin)
            DropdownCell(label = "Oath", selected = build.oath, options = oaths, labelOf = Catalogs::oathLabel, onSelect = vm::setOath)
            DropdownCell(label = "Bell", selected = build.bell, options = Catalogs.BELLS, onSelect = vm::setBell)
            DropdownCell(
                label = "Murmur",
                selected = build.murmur.ifEmpty { "None" },
                options = listOf("None") + Catalogs.MURMURS,
                onSelect = vm::setMurmur,
            )
            HorizontalDivider()
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Multifaceted", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "Race stat bonuses are ignored (innate talent still applies)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(checked = build.multifaceted, onCheckedChange = vm::setMultifaceted)
            }
        }
    }
}

@Composable
private fun BoonsFlaws(vm: BuilderViewModel, build: Build, data: GameData) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionHeader("Boons")
        ElevatedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                build.boons.indices.forEach { i ->
                    ChoiceChipRow(
                        options = listOf("None") + data.boons.keys.sorted(),
                        selected = build.boons[i],
                        onSelect = { vm.setBoon(i, it) },
                    )
                }
            }
        }
        SectionHeader("Flaws")
        ElevatedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                build.flaws.indices.forEach { i ->
                    ChoiceChipRow(
                        options = listOf("None") + data.flaws.keys.sorted(),
                        selected = build.flaws[i],
                        onSelect = { vm.setFlaw(i, it) },
                    )
                }
            }
        }
    }
}