package com.dwbuilder.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dwbuilder.app.data.GameData
import com.dwbuilder.app.domain.Catalogs
import com.dwbuilder.app.domain.Points
import com.dwbuilder.app.domain.ShrineRules
import com.dwbuilder.app.domain.ShrineRules.WithdrawGuard
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
        item { ShrineCard(vm, build, data) }
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

/**
 * Shrine of Order / Shrine of Mastery panel — the site's `.dwb-shrine` section
 * of the Stats tab. Order runs the point redistribution (`$b`), the snapshot
 * buttons manage the pre/post attribute states (`yn`/`ss`/`ct`/`Qe`), and the
 * mastery editor steps withdrawals through the dialog guards (`ue`/`ge`).
 */
@Composable
private fun ShrineCard(vm: BuilderViewModel, build: Build, data: GameData) {
    var phase by remember { mutableStateOf("pre") }
    var modifyPre by remember { mutableStateOf(false) }

    val hasAttrs = !ShrineRules.isEmpty(build.attributes)
    val hasPre = vm.hasPreShrine
    val reshrine = hasPre && modifyPre
    val phaseMode = when (build.shrineMode) {
        "pre" -> "Pre-shrine applied"
        "post" -> "Post-shrine applied"
        else -> "Not applied"
    }

    val preTaken = ShrineRules.totalWithdrawn(build.preMastery)
    val postTaken = ShrineRules.totalWithdrawn(build.postMastery)
    val budgetTaken = preTaken + postTaken
    val budgetOver = budgetTaken > ShrineRules.MASTERY_LIMIT

    SectionHeader("Shrine")
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // ---- shrine of order ----
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Shrine of Order", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Redistribute points evenly · each stat loses at most 25",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    shape = MaterialTheme.shapes.small,
                ) {
                    Text(phaseMode, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                }
            }
            vm.shrineNotice?.let { notice ->
                Text(notice, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.tertiary)
            }
            Button(
                onClick = vm::applyShrineOrder,
                enabled = hasAttrs,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Shrine of Order")
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(
                    onClick = vm::savePreShrine,
                    enabled = hasAttrs && !hasPre,
                    modifier = Modifier.weight(1f),
                ) { Text("Save pre-shrine") }
                TextButton(
                    onClick = vm::loadPreShrine,
                    enabled = hasPre,
                    modifier = Modifier.weight(1f),
                ) { Text("Load pre-shrine") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(
                    onClick = vm::savePostShrine,
                    enabled = hasAttrs,
                    modifier = Modifier.weight(1f),
                ) { Text("Save post-shrine") }
                TextButton(
                    onClick = vm::loadPostShrine,
                    enabled = vm.hasPostShrine,
                    modifier = Modifier.weight(1f),
                ) { Text("Load post-shrine") }
            }

            HorizontalDivider()

            // ---- shrine of mastery ----
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Shrine of Mastery", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Text(
                    if (budgetOver) "$budgetTaken / ${ShrineRules.MASTERY_LIMIT} OVER" else
                        "$budgetTaken / ${ShrineRules.MASTERY_LIMIT} taken",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (budgetOver) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                )
            }
            Text(
                "Withdraw points from stats (pre $preTaken · post $postTaken)",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { phase = "pre" }, modifier = Modifier.weight(1f)) {
                    Text(if (phase == "pre") "● Pre-shrine" else "Pre-shrine")
                }
                OutlinedButton(
                    onClick = { phase = "post" },
                    enabled = hasPre,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(if (phase == "post") "● Post-shrine" else "Post-shrine")
                }
            }
            if (hasPre && phase == "pre") {
                TextButton(onClick = { modifyPre = !modifyPre }) {
                    Text(
                        if (modifyPre) "Modify (re-shrines) ✓ — Shrine of Order will re-run on apply"
                        else "Modify (re-shrines) — Shrine of Order will re-run on apply",
                    )
                }
            }
            ShrineRules.allStats(build.attributes).forEach { stat ->
                ShrineMasteryRow(vm, build, stat.block, stat.name, phase, reshrine)
            }
            Text(
                "Base stats can't drop below their racial bonus; withdrawing can't break a taken talent or the shared budget.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { vm.applyMasteries(reshrine) }, modifier = Modifier.weight(1f)) {
                    Text(if (reshrine) "Apply (re-shrines)" else "Apply mastery")
                }
                OutlinedButton(onClick = vm::resetMastery, modifier = Modifier.weight(1f)) {
                    Text("Reset")
                }
            }
        }
    }
}

@Composable
private fun ShrineMasteryRow(
    vm: BuilderViewModel,
    build: Build,
    block: String,
    stat: String,
    phase: String,
    reshrine: Boolean,
) {
    val ref = ShrineRules.StatRef(block, stat)
    val current = ShrineRules.withdrawal(build, phase, ref)
    val value = ShrineRules.displayValue(build, phase, ref)
    val plus = vm.withdrawGuard(phase, stat, current + 1, reshrine)
    val accent = if (block == "attunement") attunementColors[stat] else null
    val guarded = plus != WithdrawGuard.OK
    val reason = when (plus) {
        WithdrawGuard.PINNED -> "Pinned — would break a taken talent or race floor"
        WithdrawGuard.BUDGET_EXCEEDED -> "Over the 100-pt mastery budget"
        WithdrawGuard.POST_LOCKED -> "Run Shrine of Order first"
        WithdrawGuard.PRE_LOCKED -> "Pre-shrine exists — tap Modify (re-shrines)"
        WithdrawGuard.ABOVE_STAT -> "Can't withdraw more than the stat has"
        else -> null
    }
    Column {
        StatStepper(
            label = if (current > 0) "$stat (−$current)" else stat,
            value = value,
            accent = accent,
            plusEnabled = !guarded,
            minusEnabled = current > 0,
            onMinus = { vm.setMasteryWithdrawal(phase, stat, current - 1, reshrine) },
            onPlus = { vm.setMasteryWithdrawal(phase, stat, current + 1, reshrine) },
        )
        if (guarded && reason != null) {
            Text(
                reason,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(start = 4.dp, end = 4.dp, bottom = 4.dp),
            )
        }
    }
}