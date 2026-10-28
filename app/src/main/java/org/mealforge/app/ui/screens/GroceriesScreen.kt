package org.mealforge.app.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.mealforge.app.data.GroceryItemEntity
import org.mealforge.app.domain.Departments
import org.mealforge.app.domain.Money
import org.mealforge.app.domain.Quantities
import org.mealforge.app.ui.AppViewModel
import org.mealforge.app.ui.ChoiceField
import org.mealforge.app.ui.ConfirmDialog
import org.mealforge.app.ui.EmptyState
import org.mealforge.app.ui.MealForgeTopBar
import org.mealforge.app.ui.NumberField
import org.mealforge.app.ui.SectionHeader
import org.mealforge.app.ui.SpacerH
import org.mealforge.app.ui.TextPromptDialog
import org.mealforge.app.ui.screenPadding
import org.mealforge.app.domain.Dates

@Composable
fun GroceriesScreen(vm: AppViewModel) {
    val context = LocalContext.current
    val lists by vm.groceryLists.collectAsStateWithLifecycle()
    val activeId by vm.activeListId.collectAsStateWithLifecycle()
    val items by vm.groceryItems.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val active = lists.firstOrNull { it.id == activeId }
    var newItem by rememberSaveable { mutableStateOf("") }
    var menu by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<GroceryItemEntity?>(null) }
    var newList by remember { mutableStateOf(false) }
    var renameList by remember { mutableStateOf(false) }
    var deleteList by remember { mutableStateOf(false) }
    var finish by remember { mutableStateOf(false) }
    var listMenu by remember { mutableStateOf(false) }

    val estimated = items.sumOf { it.estimatedPriceCents ?: 0L }
    val checkedCount = items.count { it.checked }

    Column(Modifier.fillMaxSize().imePadding()) {
        MealForgeTopBar("Groceries", actions = {
            IconButton(onClick = { menu = true }) { Icon(Icons.Filled.MoreVert, contentDescription = "Grocery list options") }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                DropdownMenuItem(text = { Text("Build from this week's plan") }, onClick = {
                    menu = false
                    val start = Dates.weekStart(vm.today)
                    vm.generateGroceries(start, start.plusDays(6), usePantry = true)
                })
                DropdownMenuItem(text = { Text("Share list") }, onClick = { menu = false; share(context, active?.name ?: "Groceries", items) })
                DropdownMenuItem(text = { Text("Copy to clipboard") }, onClick = { menu = false; copy(context, items); vm.toast("List copied") })
                DropdownMenuItem(text = { Text("Remove checked items") }, onClick = { menu = false; vm.clearChecked() })
                DropdownMenuItem(text = { Text("New list") }, onClick = { menu = false; newList = true })
                DropdownMenuItem(text = { Text("Rename list") }, onClick = { menu = false; renameList = true })
                if (lists.size > 1) DropdownMenuItem(text = { Text("Delete list") }, onClick = { menu = false; deleteList = true })
            }
        })
        Row(Modifier.screenPadding, verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { listMenu = true }) { Text("${active?.name ?: "Groceries"} ▾", style = MaterialTheme.typography.titleMedium) }
            DropdownMenu(expanded = listMenu, onDismissRequest = { listMenu = false }) {
                lists.forEach { l -> DropdownMenuItem(text = { Text(l.name) }, onClick = { vm.selectList(l.id); listMenu = false }) }
                DropdownMenuItem(text = { Text("+ New list") }, onClick = { listMenu = false; newList = true })
            }
            Text(
                "${items.size - checkedCount} left" + if (estimated > 0) " · est. ${Money.format(estimated, settings.currencySymbol)}" else "",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f).padding(start = 8.dp),
            )
        }
        Row(Modifier.screenPadding, verticalAlignment = Alignment.CenterVertically) {
            val add = {
                val text = newItem.trim()
                if (text.isNotEmpty()) {
                    // Accept "2 lb chicken" style quick entry.
                    val parts = text.split(Regex("\\s+"), limit = 3)
                    val qty = Quantities.parse(parts[0])
                    when {
                        qty != null && parts.size == 3 -> vm.addGroceryItem(parts[2], qty, parts[1])
                        qty != null && parts.size == 2 -> vm.addGroceryItem(parts[1], qty, "")
                        else -> vm.addGroceryItem(text, null, "")
                    }
                    newItem = ""
                }
            }
            OutlinedTextField(
                value = newItem, onValueChange = { newItem = it },
                placeholder = { Text("Add item, e.g. 2 lb chicken") }, singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { add() }),
                modifier = Modifier.weight(1f),
            )
            FilledIconButton(onClick = add, modifier = Modifier.padding(start = 8.dp)) { Icon(Icons.Filled.Add, contentDescription = "Add item") }
        }
        if (items.isEmpty()) {
            EmptyState("Your list is empty", "Add items above, or build a list from this week's meal plan.") {
                OutlinedButton(onClick = {
                    val start = Dates.weekStart(vm.today)
                    vm.generateGroceries(start, start.plusDays(6), usePantry = true)
                }) { Text("Build from meal plan") }
            }
        }
        val grouped = items.filter { !it.checked }.groupBy { it.department }.toSortedMap(compareBy<String>({ Departments.all.indexOf(it).let { i -> if (i < 0) 99 else i } }, { it }))
        val done = items.filter { it.checked }
        LazyColumn(Modifier.weight(1f).screenPadding) {
            grouped.forEach { (dept, rows) ->
                item(key = "h-$dept") { SectionHeader(dept) }
                items(rows, key = { it.id }) { item -> GroceryRow(item, settings.currencySymbol, onToggle = { vm.updateGroceryItem(item.copy(checked = it)) }, onEdit = { editing = item }) }
            }
            if (done.isNotEmpty()) {
                item(key = "h-done") { SectionHeader("In the cart (${done.size})") }
                items(done, key = { it.id }) { item -> GroceryRow(item, settings.currencySymbol, onToggle = { vm.updateGroceryItem(item.copy(checked = it)) }, onEdit = { editing = item }) }
            }
            item { SpacerH(16) }
        }
        if (checkedCount > 0) {
            Button(onClick = { finish = true }, modifier = Modifier.screenPadding.fillMaxWidth().padding(bottom = 8.dp)) { Text("Finish shopping ($checkedCount)") }
        }
    }

    editing?.let { item ->
        GroceryItemDialog(item, onDismiss = { editing = null }, onSave = { vm.updateGroceryItem(it) }, onDelete = { vm.deleteGroceryItem(item) })
    }
    if (newList) TextPromptDialog("New grocery list", label = "List name", confirm = "Create", onConfirm = { vm.createList(it) }, onDismiss = { newList = false })
    if (renameList && active != null) TextPromptDialog("Rename list", initial = active.name, onConfirm = { vm.renameList(active, it) }, onDismiss = { renameList = false })
    if (deleteList && active != null) {
        ConfirmDialog("Delete ${active.name}?", "All ${items.size} items on this list will be removed.", "Delete", onConfirm = { vm.deleteList(active.id) }, onDismiss = { deleteList = false })
    }
    if (finish) {
        val checkedEstimate = items.filter { it.checked }.sumOf { it.actualPriceCents ?: it.estimatedPriceCents ?: 0L }
        var total by remember { mutableStateOf(if (checkedEstimate > 0) Money.format(checkedEstimate, "").trimStart() else "") }
        var toPantry by remember { mutableStateOf(true) }
        AlertDialog(
            onDismissRequest = { finish = false },
            title = { Text("Finish shopping") },
            text = {
                Column {
                    Text("Log what you spent and clear the $checkedCount checked items.")
                    SpacerH(8)
                    NumberField("Total spent (${settings.currencySymbol})", total, { total = it })
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = toPantry, onCheckedChange = { toPantry = it })
                        Text("Add bought items to my pantry")
                    }
                }
            },
            confirmButton = { TextButton(onClick = { vm.finishShopping(Money.parseCents(total), toPantry); finish = false }) { Text("Done") } },
            dismissButton = { TextButton(onClick = { finish = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun GroceryRow(item: GroceryItemEntity, currency: String, onToggle: (Boolean) -> Unit, onEdit: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onEdit), verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = item.checked, onCheckedChange = onToggle)
        Column(Modifier.weight(1f).padding(vertical = 8.dp)) {
            Text(
                item.name,
                textDecoration = if (item.checked) TextDecoration.LineThrough else null,
                color = if (item.checked) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
            )
            val detail = listOfNotNull(
                Quantities.describe(item.quantity, item.unit).takeIf { it.isNotBlank() },
                (item.actualPriceCents ?: item.estimatedPriceCents)?.let { Money.format(it, currency) },
                "from plan".takeIf { item.fromPlan },
            )
            if (detail.isNotEmpty()) Text(detail.joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun GroceryItemDialog(item: GroceryItemEntity, onDismiss: () -> Unit, onSave: (GroceryItemEntity) -> Unit, onDelete: () -> Unit) {
    var name by remember { mutableStateOf(item.name) }
    var qty by remember { mutableStateOf(item.quantity?.let(Quantities::format) ?: "") }
    var unit by remember { mutableStateOf(item.unit) }
    var dept by remember { mutableStateOf(item.department) }
    var est by remember { mutableStateOf(item.estimatedPriceCents?.let { Money.format(it, "") } ?: "") }
    var actual by remember { mutableStateOf(item.actualPriceCents?.let { Money.format(it, "") } ?: "") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit item") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, singleLine = true)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    NumberField("Quantity", qty, { qty = it }, Modifier.weight(1f))
                    OutlinedTextField(value = unit, onValueChange = { unit = it }, label = { Text("Unit") }, singleLine = true, modifier = Modifier.weight(1f))
                }
                ChoiceField("Department", dept, Departments.all, { dept = it })
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    NumberField("Est. price", est, { est = it }, Modifier.weight(1f))
                    NumberField("Paid", actual, { actual = it }, Modifier.weight(1f))
                }
                TextButton(onClick = { onDelete(); onDismiss() }) { Text("Delete item", color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(
                    item.copy(
                        name = name.trim().ifEmpty { item.name }, quantity = Quantities.parse(qty), unit = unit.trim(), department = dept,
                        estimatedPriceCents = Money.parseCents(est), actualPriceCents = Money.parseCents(actual),
                    ),
                )
                onDismiss()
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

private fun listAsText(items: List<GroceryItemEntity>): String =
    items.filter { !it.checked }.groupBy { it.department }.entries.joinToString("\n\n") { (dept, rows) ->
        dept + "\n" + rows.joinToString("\n") { item ->
            val q = Quantities.describe(item.quantity, item.unit)
            "• " + if (q.isBlank()) item.name else "$q ${item.name}"
        }
    }

private fun share(context: Context, title: String, items: List<GroceryItemEntity>) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, title)
        putExtra(Intent.EXTRA_TEXT, "$title\n\n" + listAsText(items))
    }
    context.startActivity(Intent.createChooser(intent, "Share grocery list"))
}

private fun copy(context: Context, items: List<GroceryItemEntity>) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("Grocery list", listAsText(items)))
}
