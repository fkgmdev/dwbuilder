package com.dwbuilder.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dwbuilder.app.ui.BuilderViewModel
import com.dwbuilder.app.ui.screens.EquipmentScreen
import com.dwbuilder.app.ui.screens.MantrasScreen
import com.dwbuilder.app.ui.screens.StatsScreen
import com.dwbuilder.app.ui.screens.SummaryScreen
import com.dwbuilder.app.ui.screens.TalentsScreen
import com.dwbuilder.app.ui.screens.WeaponsScreen
import com.dwbuilder.app.ui.theme.BuilderTheme

private data class Tab(val label: String, val icon: ImageVector)

private val TABS = listOf(
    Tab("Stats", Icons.Filled.Person),
    Tab("Talents", Icons.Filled.Star),
    Tab("Mantras", Icons.Filled.List),
    Tab("Weapons", Icons.Filled.Build),
    Tab("Equipment", Icons.Filled.Settings),
    Tab("Summary", Icons.Filled.Info),
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BuilderTheme {
                val vm: BuilderViewModel = viewModel()
                AppScaffold(vm)
            }
        }
    }
}

@Composable
private fun AppScaffold(vm: BuilderViewModel) {
    var selectedTab by rememberSaveable { mutableStateOf(0) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar {
                TABS.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) },
                    )
                }
            }
        },
        contentWindowInsets = WindowInsets.safeDrawing,
    ) { innerPadding ->
        Box(Modifier.fillMaxSize().padding(innerPadding)) {
            val error = vm.error
            when {
                error != null -> Column(
                    Modifier.fillMaxSize().padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("Could not load game data", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.error)
                    Text(error, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                vm.data == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Text(
                            "Loading game data…",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 12.dp),
                        )
                    }
                }

                else -> when (selectedTab) {
                    0 -> StatsScreen(vm)
                    1 -> TalentsScreen(vm)
                    2 -> MantrasScreen(vm)
                    3 -> WeaponsScreen(vm)
                    4 -> EquipmentScreen(vm)
                    else -> SummaryScreen(vm)
                }
            }
        }
    }
}