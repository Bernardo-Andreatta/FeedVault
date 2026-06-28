package com.bernardo.feedvault.ui.components

import com.bernardo.feedvault.util.normalizeForSearch
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp


@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun TagEditorDialog(
    currentTags: List<String>,
    allAvailableTags: List<String>,
    onConfirm: (List<String>) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedTags by remember { mutableStateOf(currentTags) }
    var query by remember { mutableStateOf("") }

    val suggestions = remember(query, allAvailableTags, selectedTags) {
        val norm = query.normalizeForSearch()
        if (query.isBlank()) allAvailableTags.filter { it !in selectedTags }
        else allAvailableTags.filter { it.normalizeForSearch().contains(norm) && it !in selectedTags }
    }
    val trimmed = query.trim()
    val trimmedNorm = trimmed.normalizeForSearch()
    val existingMatch = allAvailableTags.firstOrNull { it.normalizeForSearch() == trimmedNorm }
    val canCreate = trimmed.isNotBlank() && existingMatch == null &&
        !selectedTags.any { it.normalizeForSearch() == trimmedNorm }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        windowInsets = WindowInsets(0)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(top = 4.dp, bottom = 16.dp)
                .navigationBarsPadding()
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Editar Tags", style = MaterialTheme.typography.titleMedium)
                Row {
                    TextButton(onClick = onDismiss) { Text("Cancelar") }
                    Button(onClick = {
                        val finalTags = when {
                            existingMatch != null && existingMatch !in selectedTags -> selectedTags + existingMatch
                            canCreate -> selectedTags + trimmed
                            else -> selectedTags
                        }
                        onConfirm(finalTags); onDismiss()
                    }) { Text("Salvar") }
                }
            }

            if (selectedTags.isNotEmpty()) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    selectedTags.forEach { tag ->
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .padding(start = 10.dp, end = 6.dp, top = 4.dp, bottom = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                tag,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Remover",
                                modifier = Modifier
                                    .size(14.dp)
                                    .clickable { selectedTags = selectedTags - tag },
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("Buscar ou criar tag") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    when {
                        existingMatch != null && existingMatch !in selectedTags -> {
                            selectedTags = selectedTags + existingMatch; query = ""
                        }
                        canCreate -> { selectedTags = selectedTags + trimmed; query = "" }
                    }
                })
            )

            val showExisting = existingMatch != null && existingMatch !in selectedTags
            if (canCreate || showExisting || suggestions.isNotEmpty()) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 240.dp)
                ) {
                    if (showExisting && existingMatch != null) {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedTags = selectedTags + existingMatch; query = "" }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                Text("Usar tag existente: \"$existingMatch\"", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                            }
                            Divider()
                        }
                    }
                    if (canCreate) {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedTags = selectedTags + trimmed; query = "" }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                Text("Criar tag: \"$trimmed\"", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                            }
                            Divider()
                        }
                    }
                    items(suggestions) { tag ->
                        Text(
                            text = tag,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedTags = selectedTags + tag; query = "" }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun PeopleEditorDialog(
    currentPeople: List<String>,
    allAvailablePeople: List<String>,
    onConfirm: (List<String>) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedPeople by remember { mutableStateOf(currentPeople) }
    var query by remember { mutableStateOf("") }

    val suggestions = remember(query, allAvailablePeople, selectedPeople) {
        val norm = query.normalizeForSearch()
        if (query.isBlank()) allAvailablePeople.filter { it !in selectedPeople }
        else allAvailablePeople.filter { it.normalizeForSearch().contains(norm) && it !in selectedPeople }
    }
    val trimmed = query.trim()
    val trimmedNorm = trimmed.normalizeForSearch()
    val existingMatch = allAvailablePeople.firstOrNull { it.normalizeForSearch() == trimmedNorm }
    val canCreate = trimmed.isNotBlank() && existingMatch == null &&
        !selectedPeople.any { it.normalizeForSearch() == trimmedNorm }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        windowInsets = WindowInsets(0)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(top = 4.dp, bottom = 16.dp)
                .navigationBarsPadding()
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Pessoas / Categorias",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                Row {
                    TextButton(onClick = onDismiss) { Text("Cancelar") }
                    Button(onClick = {
                        val finalPeople = when {
                            existingMatch != null && existingMatch !in selectedPeople -> selectedPeople + existingMatch
                            canCreate -> selectedPeople + trimmed
                            else -> selectedPeople
                        }
                        onConfirm(finalPeople); onDismiss()
                    }) { Text("Salvar") }
                }
            }

            if (selectedPeople.isNotEmpty()) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    selectedPeople.forEach { person ->
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .padding(start = 10.dp, end = 6.dp, top = 4.dp, bottom = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                person,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Remover",
                                modifier = Modifier
                                    .size(14.dp)
                                    .clickable { selectedPeople = selectedPeople - person },
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("Buscar ou adicionar pessoa") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    when {
                        existingMatch != null && existingMatch !in selectedPeople -> {
                            selectedPeople = selectedPeople + existingMatch; query = ""
                        }
                        canCreate -> { selectedPeople = selectedPeople + trimmed; query = "" }
                    }
                })
            )

            val showExisting = existingMatch != null && existingMatch !in selectedPeople
            if (canCreate || showExisting || suggestions.isNotEmpty()) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 240.dp)
                ) {
                    if (showExisting && existingMatch != null) {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedPeople = selectedPeople + existingMatch; query = "" }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                Text("Usar pessoa existente: \"$existingMatch\"", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                            }
                            Divider()
                        }
                    }
                    if (canCreate) {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedPeople = selectedPeople + trimmed; query = "" }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                Text("Adicionar: \"$trimmed\"", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                            }
                            Divider()
                        }
                    }
                    items(suggestions) { person ->
                        Text(
                            text = person,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedPeople = selectedPeople + person; query = "" }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ManagePeopleDialog(
    allPeople: List<String>,
    onDeletePerson: (String) -> Unit,
    onRenamePerson: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var renamingPerson by remember { mutableStateOf<String?>(null) }
    var renameValue by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pessoas / Categorias") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (allPeople.isEmpty()) {
                    Text("Nenhuma pessoa encontrada.", style = MaterialTheme.typography.bodyMedium)
                } else {
                    LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 320.dp)) {
                        items(allPeople) { person ->
                            if (renamingPerson == person) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    TextField(
                                        value = renameValue,
                                        onValueChange = { renameValue = it },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true
                                    )
                                    IconButton(onClick = {
                                        onRenamePerson(person, renameValue)
                                        renamingPerson = null
                                    }) {
                                        Icon(Icons.Default.Done, contentDescription = "Confirmar", tint = MaterialTheme.colorScheme.primary)
                                    }
                                    IconButton(onClick = { renamingPerson = null }) {
                                        Icon(Icons.Default.Close, contentDescription = "Cancelar")
                                    }
                                }
                            } else {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = person,
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(onClick = {
                                        renamingPerson = person
                                        renameValue = person
                                    }) {
                                        Icon(Icons.Default.Edit, contentDescription = "Renomear")
                                    }
                                    IconButton(onClick = { onDeletePerson(person) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Deletar", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) { Text("Fechar") }
        }
    )
}

@Composable
fun TagFilterChip(
    tag: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    count: Int? = null
) {
    val bgColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
    val textColor = if (selected) Color.White else MaterialTheme.colorScheme.onSurface
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(text = tag, color = textColor, style = MaterialTheme.typography.labelMedium)
        if (count != null) {
            Text(
                text = count.toString(),
                color = textColor.copy(alpha = 0.65f),
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

@Composable
fun ManageTagsDialog(
    allTags: List<String>,
    onDeleteTag: (String) -> Unit,
    onRenameTag: (String, String) -> Unit,
    onDeleteAllTags: () -> Unit,
    onDismiss: () -> Unit
) {
    var renamingTag by remember { mutableStateOf<String?>(null) }
    var renameValue by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Gerenciar Tags") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (allTags.isEmpty()) {
                    Text("Nenhuma tag encontrada.", style = MaterialTheme.typography.bodyMedium)
                } else {
                    LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 320.dp)) {
                        items(allTags) { tag ->
                            if (renamingTag == tag) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    TextField(
                                        value = renameValue,
                                        onValueChange = { renameValue = it },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true
                                    )
                                    IconButton(onClick = {
                                        onRenameTag(tag, renameValue)
                                        renamingTag = null
                                    }) {
                                        Icon(Icons.Default.Done, contentDescription = "Confirmar", tint = MaterialTheme.colorScheme.primary)
                                    }
                                    IconButton(onClick = { renamingTag = null }) {
                                        Icon(Icons.Default.Close, contentDescription = "Cancelar")
                                    }
                                }
                            } else {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = tag,
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(onClick = {
                                        renamingTag = tag
                                        renameValue = tag
                                    }) {
                                        Icon(Icons.Default.Edit, contentDescription = "Renomear")
                                    }
                                    IconButton(onClick = { onDeleteTag(tag) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Deletar", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }

                    OutlinedButton(
                        onClick = onDeleteAllTags,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Deletar Todas as Tags")
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) { Text("Fechar") }
        }
    )
}
