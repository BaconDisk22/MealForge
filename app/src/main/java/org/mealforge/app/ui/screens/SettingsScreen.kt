package org.mealforge.app.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import org.mealforge.app.BuildConfig
import org.mealforge.app.data.AppSettings
import org.mealforge.app.ui.AppViewModel
import org.mealforge.app.ui.ConfirmDialog
import org.mealforge.app.ui.InfoCard
import org.mealforge.app.ui.MealForgeTopBar
import org.mealforge.app.ui.SectionHeader
import org.mealforge.app.ui.SpacerH
import org.mealforge.app.ui.screenPadding
import java.time.LocalDate

@Composable
fun SettingsScreen(vm: AppViewModel, nav: NavHostController) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    var pendingRestore by remember { mutableStateOf<android.net.Uri?>(null) }
    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri -> uri?.let { vm.exportBackup(it) } }
    val restore = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> pendingRestore = uri }

    Column(Modifier.fillMaxSize()) {
        MealForgeTopBar("Settings & backup", onBack = { nav.popBackStack() })
        Column(Modifier.screenPadding.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionHeader("Currency symbol")
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("$", "€", "£", "¥", "₹", "R").forEach { c ->
                    FilterChip(selected = settings.currencySymbol == c, onClick = { vm.setSetting(AppSettings.CURRENCY, c) }, label = { Text(c) })
                }
            }
            SectionHeader("Backup & restore")
            Text(
                "Save everything (recipes, plans, lists, pantry, budget, household) to a file you control. Restore it later or on another phone.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { export.launch("mealforge-backup-${LocalDate.now()}.json") }) { Text("Back up") }
                OutlinedButton(onClick = { restore.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) }) { Text("Restore") }
            }
            Text("Recipe photos stay on your phone and aren't included in backups.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            SectionHeader("About")
            InfoCard {
                Text("MealForge ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.titleMedium)
                Text("Free and open source. No ads, accounts, subscriptions or tracking.")
                SpacerH(6)
                Text("Privacy: MealForge has no internet permission. Your data never leaves this phone unless you share or back it up yourself.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                SpacerH(6)
                Text("Nutrition and allergen information is approximate and not medical advice.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            SpacerH(24)
        }
    }

    pendingRestore?.let { uri ->
        ConfirmDialog(
            "Restore from backup?",
            "This replaces ALL current MealForge data with the backup's contents. Consider backing up first.",
            "Replace my data",
            onConfirm = { vm.importBackup(uri) },
            onDismiss = { pendingRestore = null },
        )
    }
}
