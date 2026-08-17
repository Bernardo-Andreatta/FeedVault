package com.bernardo.feedvault.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.bernardo.feedvault.ui.theme.ThemeController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    state: AppUiState,
    viewModel: GalleryViewModel,
    onClose: () -> Unit,
    onManageTags: () -> Unit,
    onManagePeople: () -> Unit,
    onExportTags: () -> Unit
) {
    val context = LocalContext.current
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Settings") },
                    navigationIcon = {
                        IconButton(onClick = onClose) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                // ── Appearance ──────────────────────────────────────────────
                SectionTitle("Appearance")
                Text("Background", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                SwatchRow(
                    swatches = ThemeController.backgroundPresets,
                    selected = ThemeController.background,
                    onPick = { ThemeController.setBackground(context, it) }
                )
                Spacer(Modifier.height(16.dp))
                Text("Accent", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                SwatchRow(
                    swatches = ThemeController.accentPresets,
                    selected = ThemeController.accent,
                    onPick = { ThemeController.setAccent(context, it) }
                )

                SectionDivider()

                // ── Organization ────────────────────────────────────────────
                SectionTitle("Organization")
                SettingButton(Icons.Default.Label, "Manage tags", onManageTags)
                Spacer(Modifier.height(8.dp))
                SettingButton(Icons.Default.Person, "People / Categories", onManagePeople)
                Spacer(Modifier.height(8.dp))
                SettingButton(Icons.Default.Upload, "Export tags", onExportTags)

                // ── Vault ───────────────────────────────────────────────────
                if (state.vaultInitialized) {
                    SectionDivider()
                    SectionTitle("Vault")
                    ChangePasswordBlock(viewModel)
                }

                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(bottom = 10.dp)
    )
}

@Composable
private fun SectionDivider() {
    androidx.compose.material3.Divider(
        modifier = Modifier.padding(vertical = 20.dp),
        color = MaterialTheme.colorScheme.outlineVariant
    )
}

@Composable
private fun SwatchRow(
    swatches: List<Pair<Color, String>>,
    selected: Color,
    onPick: (Color) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        swatches.forEach { (color, _) ->
            val isSelected = color.value == selected.value
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(color)
                    .border(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                        shape = CircleShape
                    )
                    .clickable { onPick(color) },
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = if (color.luminance() > 0.5f) Color.Black else Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.size(10.dp))
        Text(label, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun ChangePasswordBlock(viewModel: GalleryViewModel) {
    var current by remember { mutableStateOf("") }
    var new by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var done by remember { mutableStateOf(false) }

    Text(
        "Change vault password",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = 10.dp)
    )
    OutlinedTextField(
        value = current, onValueChange = { current = it; done = false },
        label = { Text("Current password") },
        visualTransformation = PasswordVisualTransformation(),
        singleLine = true, modifier = Modifier.fillMaxWidth()
    )
    Spacer(Modifier.height(8.dp))
    OutlinedTextField(
        value = new, onValueChange = { new = it; done = false },
        label = { Text("New password") },
        visualTransformation = PasswordVisualTransformation(),
        singleLine = true, modifier = Modifier.fillMaxWidth()
    )
    Spacer(Modifier.height(8.dp))
    OutlinedTextField(
        value = confirm, onValueChange = { confirm = it; done = false },
        label = { Text("Confirm new password") },
        visualTransformation = PasswordVisualTransformation(),
        singleLine = true, modifier = Modifier.fillMaxWidth()
    )
    Spacer(Modifier.height(12.dp))
    Button(
        onClick = {
            if (new != confirm) {
                viewModel.setError("Passwords don't match")
            } else {
                viewModel.changeVaultPassword(current, new) { ok ->
                    if (ok) {
                        current = ""; new = ""; confirm = ""; done = true
                        viewModel.setError("Password changed")
                    }
                }
            }
        },
        enabled = current.isNotBlank() && new.isNotBlank() && confirm.isNotBlank(),
        modifier = Modifier.fillMaxWidth()
    ) { Text(if (done) "Password changed" else "Change password") }
}
