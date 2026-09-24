package com.dwbuilder.app.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.dwbuilder.app.data.GameData
import com.dwbuilder.app.domain.Points
import com.dwbuilder.app.domain.ShrineRules
import com.dwbuilder.app.domain.TalentRules
import com.dwbuilder.app.domain.Transfer
import com.dwbuilder.app.domain.model.Build
import com.dwbuilder.app.domain.model.Attributes
import com.dwbuilder.app.ui.BuilderViewModel
import com.dwbuilder.app.ui.components.DetailRow
import com.dwbuilder.app.ui.components.SectionHeader

@Composable
fun SummaryScreen(vm: BuilderViewModel) {
    val data = vm.data ?: return
    val build = vm.build
    val context = LocalContext.current

    var showImport by rememberSaveable { mutableStateOf(false) }
    var importText by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item { IdentityCard(build) }
        item { AttributeCard(build) }
        item { PoolsCard(build, data) }
        item {
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionHeader("Share & import")
                    val text = remember(build) { Transfer.export(build) }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilledTonalButton(
                            onClick = {
                                copyToClipboard(context, text)
                                Toast.makeText(context, "Build text copied", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f),
                        ) { Text("Copy text") }
                        FilledTonalButton(
                            onClick = { shareBuild(context, text) },
                            modifier = Modifier.weight(1f),
                        ) { Text("Share") }
                    }
                    OutlinedButton(
                        onClick = {
                            importText = ""
                            showImport = true
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Import in-game build…") }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { vm.resetBuild() },
                            modifier = Modifier.weight(1f),
                        ) { Text("New build") }
                        OutlinedButton(
                            onClick = { vm.applyShrineOrder() },
                            modifier = Modifier.weight(1f),
                            enabled = !ShrineRules.isEmpty(build.attributes),
                        ) { Text("Shrine of Order") }
                    }
                    Text(
                        "The in-game transfer format (name, LVL header, stats, talents, mantras) is shared with deepwoken.co — paste this text into the site's Import dialog, or paste in-game text here.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }

    if (showImport) {
        AlertDialog(
            onDismissRequest = { showImport = false },
            title = { Text("Import in-game build") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Paste build text copied from the game (or from deepwoken.co's copy). Name, level, race, origin, oath, attributes, talents and mantras are applied.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    OutlinedTextField(
                        value = importText,
                        onValueChange = { importText = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("LVL 20 …") },
                        minLines = 6,
                        maxLines = 10,
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showImport = false
                        vm.importTransfer(importText)
                    },
                ) { Text("Apply") }
            },
            dismissButton = {
                TextButton(onClick = { showImport = false }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun IdentityCard(build: Build) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            SectionHeader("Identity")
            Text(build.name.ifBlank { "Unnamed build" }, style = MaterialTheme.typography.titleMedium)
            DetailRow("Level", "${build.level}")
            DetailRow("Race", build.race)
            DetailRow("Origin", build.origin)
            DetailRow("Oath", build.oath)
            DetailRow("Bell", build.bell)
            if (build.murmur.isNotBlank()) DetailRow("Murmur", build.murmur)
            if (build.multifaceted) DetailRow("Multifaceted", "On")
        }
    }
}

@Composable
private fun AttributeCard(build: Build) {
    val a = build.attributes
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            SectionHeader("Attributes")
            DetailRow("Power", "${Points.power(build)}")
            DetailRow("Base", "${sumOf(a.base)} pts")
            Attributes.BASE_STATS.forEach { name ->
                DetailRow(name, "${a.base[name] ?: 0}")
            }
            DetailRow("Weapon skills", "${sumOf(a.weapon)} pts")
            Attributes.WEAPON_STATS.forEach { name ->
                DetailRow(name, "${a.weapon[name] ?: 0}")
            }
            DetailRow("Attunement", "${sumOf(a.attunement)} pts")
            Attributes.ATTUNEMENT_STATS.forEach { name ->
                DetailRow(name, "${a.attunement[name] ?: 0}")
            }
            DetailRow("Points left", "${Points.pointsLeft(build)}")
        }
    }
}

@Composable
private fun PoolsCard(build: Build, data: GameData) {
    val granted = remember(build, data) {
        build.equipment.values.mapNotNull(data::equipmentItem).flatMap { it.innateTalents }.toSet() +
            (build.outfit.takeIf { it.isNotEmpty() }?.let { data.outfit(it) }?.grantedTalents.orEmpty()) +
            (build.weapon.takeIf { it.isNotEmpty() }?.let { data.weapon(it) }?.grantedTalents.orEmpty())
    }
    val nonExempt = TalentRules.nonExemptMantraCount(build, data::mantra)
    val caps = TalentRules.caps(nonExempt)
    val faction = TalentRules.factionCount(build, data::talent, granted)
    val mantraCategories = remember(build, data) {
        build.mantras.mapNotNull { data.mantra(it)?.category }.groupingBy { it }.eachCount()
    }
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            SectionHeader("Build pools")
            DetailRow("Talents", "${build.talents.size} / ${caps.maxTotal}")
            DetailRow("Roll 2 pool", "${caps.roll2}")
            DetailRow("Faction", "$faction / ${TalentRules.MAX_FACTION}")
            if (granted.isNotEmpty()) {
                DetailRow("Gear grants", "${granted.size} talent${if (granted.size == 1) "" else "s"}")
            }
            DetailRow("Mantras", "${build.mantras.size}")
            mantraCategories.toSortedMap().forEach { (cat, count) ->
                DetailRow("  $cat", "$count")
            }
            DetailRow("Equipment", "${build.equipment.values.count { it.isNotEmpty() }} / 7 slots")
        }
    }
}

private fun sumOf(map: Map<String, Int>): Int = map.values.sum()

private fun copyToClipboard(context: Context, text: String) {
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    cm.setPrimaryClip(ClipData.newPlainText("dwbuilder build", text))
}

private fun shareBuild(context: Context, text: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
        putExtra(Intent.EXTRA_SUBJECT, "Deepwoken build")
    }
    context.startActivity(Intent.createChooser(intent, "Share build"))
}