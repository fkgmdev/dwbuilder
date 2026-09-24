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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dwbuilder.app.data.GameData
import com.dwbuilder.app.domain.BuildContext
import com.dwbuilder.app.domain.RequirementResult
import com.dwbuilder.app.domain.Requirements
import com.dwbuilder.app.domain.model.Build
import com.dwbuilder.app.domain.model.Equipment
import com.dwbuilder.app.ui.BuilderViewModel
import com.dwbuilder.app.ui.components.DetailRow
import com.dwbuilder.app.ui.components.DropdownCell
import com.dwbuilder.app.ui.components.RarityChip
import com.dwbuilder.app.ui.components.SectionHeader

private val SLOTS = listOf("Head", "Arms", "Legs", "Torso", "Face", "Earrings", "Rings")

@Composable
fun EquipmentScreen(vm: BuilderViewModel) {
    val data = vm.data ?: return
    val build = vm.build
    val bySlot = remember(data) { SLOTS.associateWith { data.equipmentBySlot(it) } }
    val outfitNames = remember(data) { data.outfits.values.map { it.name }.sorted() }
    val ctx = remember(build) { BuildContext(build, data.talents, data.weapons) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item(key = "outfit", contentType = "equip-slot") {
            OutfitCard(build, data, ctx, vm, outfitNames)
        }
        SLOTS.forEach { slot ->
            item(key = slot, contentType = "equip-slot") {
                SlotCard(slot, bySlot.getValue(slot), build, data, ctx, vm)
            }
        }
        item(key = "totals", contentType = "equip-totals") {
            EquipmentTotalsCard(build, data)
        }
    }
}

@Composable
private fun OutfitCard(
    build: Build,
    data: GameData,
    ctx: BuildContext,
    vm: BuilderViewModel,
    outfitNames: List<String>,
) {
    val chosen = build.outfit.ifEmpty { "None" }
    val outfit = build.outfit.takeIf { it.isNotEmpty() }?.let { data.outfit(it) }
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionHeader("Outfit")
            DropdownCell(
                label = "Outfit",
                selected = chosen,
                options = listOf("None") + outfitNames,
                onSelect = { vm.setOutfit(if (it == "None") "" else it) },
            )
            if (outfit != null) {
                val check = Requirements.check(outfit.requirements, ctx)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(outfit.name, style = MaterialTheme.typography.titleSmall)
                        Text("Tier ${outfit.tier}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    RarityChip(outfit.tier)
                }
                resistLine(outfit)?.let { DetailRow("Resistances", it) }
                outfit.durability?.let { DetailRow("Durability", "$it") }
                outfit.etherRegeneration?.let { DetailRow("Ether regen", "+${com.dwbuilder.app.ui.components.num(it)}") }
                equipmentRequirementLabel(outfit.requirements, check)
            }
        }
    }
}

@Composable
private fun SlotCard(
    slot: String,
    options: List<Equipment>,
    build: Build,
    data: GameData,
    ctx: BuildContext,
    vm: BuilderViewModel,
) {
    val current = build.equipment[slot].orEmpty()
    val names = remember(options, slot) { options.map { it.name } }
    val item = current.takeIf { it.isNotEmpty() }?.let { data.equipmentItem(it) }
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionHeader(slot)
            DropdownCell(
                label = slot,
                selected = current.ifEmpty { "None" },
                options = listOf("None") + names,
                onSelect = { vm.setEquipment(slot, if (it == "None") "" else it) },
            )
            if (item != null) {
                val check = Requirements.check(item.requirements, ctx)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(item.name, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
                    RarityChip(item.rarity)
                }
                item.set?.let { Text("Set: $it", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary) }
                if (item.innateStats.isNotEmpty()) {
                    Text(
                        item.innateStats.joinToString(" · ") { s ->
                            val v = if (s.percentage) "${com.dwbuilder.app.ui.components.num(s.value, 0)}%" else "+${com.dwbuilder.app.ui.components.num(s.value, 0)}"
                            "$v ${s.stat}"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (item.innatePips.isNotEmpty()) {
                    Text(
                        "Pips: ${item.innatePips.joinToString(", ") { "${it.count}× ${it.rarity}" }}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (item.innateTalents.isNotEmpty()) {
                    Text(
                        "Grants: ${item.innateTalents.joinToString(", ")}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.tertiary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                equipmentRequirementLabel(item.requirements, check)
            }
        }
    }
}

@Composable
private fun equipmentRequirementLabel(
    requirements: com.dwbuilder.app.domain.model.TalentRequirements?,
    check: RequirementResult,
) {
    if (requirements == null) return
    val warning = check.warnings.firstOrNull()
    if (check.hardMet && warning == null) {
        Text("Requirements met", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.tertiary)
    } else {
        Text(
            warning ?: "Requirements not met",
            style = MaterialTheme.typography.labelSmall,
            color = if (check.hardMet) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error,
        )
    }
}

@Composable
private fun EquipmentTotalsCard(build: Build, data: GameData) {
    val granted = remember(build, data) {
        build.equipment.values.mapNotNull(data::equipmentItem).flatMap { it.innateTalents }.toSet()
    }
    val stats = remember(build, data) {
        build.equipment.values.mapNotNull(data::equipmentItem).flatMap { it.innateStats }
    }
    val pips = remember(build, data) {
        build.equipment.values.mapNotNull(data::equipmentItem).flatMap { it.innatePips }
    }
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            SectionHeader("Equipment totals")
            DetailRow("Items equipped", "${build.equipment.values.count { it.isNotEmpty() }} / ${SLOTS.size}")
            if (stats.isNotEmpty()) {
                val byKey = stats.groupBy { it.stat to it.percentage }
                byKey.forEach { (pair, list) ->
                    val total = list.sumOf { it.value }
                    val suffix = if (pair.second) "%" else ""
                    DetailRow(pair.first, "+${com.dwbuilder.app.ui.components.num(total, 0)}$suffix")
                }
            }
            if (pips.isNotEmpty()) {
                pips.groupBy { it.rarity }.forEach { (rarity, list) ->
                    DetailRow("$rarity pip", "${list.sumOf { it.count }}×")
                }
            }
            if (granted.isNotEmpty()) {
                Text(
                    "Grants ${granted.size} talent${if (granted.size == 1) "" else "s"}: ${granted.sorted().joinToString(", ")}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.tertiary,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

private fun resistLine(o: com.dwbuilder.app.domain.model.Outfit): String? {
    val parts = mutableListOf<String>()
    o.physicalResistance?.let { parts += "Physical $it%" }
    o.slashResistance?.let { parts += "Slash $it%" }
    o.bluntResistance?.let { parts += "Blunt $it%" }
    o.elementalResistance?.let { parts += "Elemental $it%" }
    o.flameResistance?.let { parts += "Flame $it%" }
    o.iceResistance?.let { parts += "Ice $it%" }
    o.thunderResistance?.let { parts += "Thunder $it%" }
    o.windResistance?.let { parts += "Wind $it%" }
    o.shadowResistance?.let { parts += "Shadow $it%" }
    o.metalResistance?.let { parts += "Metal $it%" }
    o.bloodResistance?.let { parts += "Blood $it%" }
    return parts.joinToString(", ").ifEmpty { null }
}