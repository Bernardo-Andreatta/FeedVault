package com.bernardo.feedvault.ui.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.bernardo.feedvault.data.DesktopFile
import com.bernardo.feedvault.data.DesktopRepository
import com.bernardo.feedvault.ui.DesktopConnectionState
import com.bernardo.feedvault.ui.DesktopFileFilter
import com.bernardo.feedvault.ui.DesktopUiState
import com.bernardo.feedvault.ui.DesktopViewModel
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions

@Composable
fun DesktopScreen(
    viewModel: DesktopViewModel,
    onSaveFile: (DesktopFile) -> Unit,
    onSaveAll: (List<DesktopFile>) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    Box(modifier = modifier.fillMaxSize()) {
        when (uiState.connectionState) {
            DesktopConnectionState.IDLE,
            DesktopConnectionState.ERROR -> DesktopConnectScreen(
                uiState = uiState,
                onAddressChange = { viewModel.setAddress(it) },
                onConnect = { viewModel.connect() }
            )
            DesktopConnectionState.CONNECTING -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator()
                    Text("Conectando...", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            DesktopConnectionState.CONNECTED -> DesktopFileListScreen(
                uiState = uiState,
                onFilter = { viewModel.setFilter(it) },
                onRefresh = { viewModel.refresh() },
                onDisconnect = { viewModel.disconnect() },
                onSaveFile = onSaveFile,
                onSaveAll = onSaveAll,
                onToggleSelection = { viewModel.toggleSelection(it) },
                onSelectAll = { viewModel.selectAll() },
                onClearSelection = { viewModel.clearSelection() },
                onSaveSelected = { onSaveAll(uiState.selectedFiles) }
            )
        }
    }
}

// ──────────────────────────────────────────────
// Connect screen
// ──────────────────────────────────────────────

@Composable
private fun DesktopConnectScreen(
    uiState: DesktopUiState,
    onAddressChange: (String) -> Unit,
    onConnect: () -> Unit
) {
    val focusManager = LocalFocusManager.current

    val scanLauncher = rememberLauncherForActivityResult(ScanContract()) { result ->
        result.contents?.let { scanned ->
            onAddressChange(scanned)
            onConnect()
        }
    }

    Box(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier.widthIn(max = 400.dp)
        ) {
            Icon(
                Icons.Default.DesktopWindows,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Desktop Companion", style = MaterialTheme.typography.headlineSmall)
                Text(
                    "Transfira arquivos do seu computador diretamente para a galeria",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "Endereço do servidor",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = uiState.address,
                        onValueChange = onAddressChange,
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("192.168.x.x:8765") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Uri,
                            imeAction = ImeAction.Go
                        ),
                        keyboardActions = KeyboardActions(onGo = {
                            focusManager.clearFocus()
                            onConnect()
                        }),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Button(
                        onClick = { focusManager.clearFocus(); onConnect() },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = uiState.address.isNotBlank()
                    ) {
                        Text("Conectar")
                    }
                    OutlinedButton(
                        onClick = {
                            scanLauncher.launch(
                                ScanOptions().apply {
                                    setCaptureActivity(com.bernardo.feedvault.PortraitCaptureActivity::class.java)
                                    setPrompt("Aponte para o QR code exibido no servidor")
                                    setBeepEnabled(true)
                                    setOrientationLocked(false)
                                    setBarcodeImageEnabled(false)
                                }
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            Icons.Default.QrCodeScanner,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Escanear QR Code")
                    }
                }
            }

            if (uiState.error != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            uiState.error,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            Text(
                "Dê um duplo clique em run.command na pasta desktop/ para iniciar o servidor",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

// ──────────────────────────────────────────────
// Connected / file list screen
// ──────────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DesktopFileListScreen(
    uiState: DesktopUiState,
    onFilter: (DesktopFileFilter) -> Unit,
    onRefresh: () -> Unit,
    onDisconnect: () -> Unit,
    onSaveFile: (DesktopFile) -> Unit,
    onSaveAll: (List<DesktopFile>) -> Unit = {},
    onToggleSelection: (String) -> Unit = {},
    onSelectAll: () -> Unit = {},
    onClearSelection: () -> Unit = {},
    onSaveSelected: () -> Unit = {}
) {
    Column(modifier = Modifier.fillMaxSize()) {

        // Server info bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primaryContainer)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                Icons.Default.DesktopWindows,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    uiState.status?.name ?: "Desktop",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    uiState.address,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
            }
            IconButton(
                onClick = onRefresh,
                modifier = Modifier.size(32.dp)
            ) {
                if (uiState.isLoadingFiles) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                } else {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "Atualizar",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            IconButton(
                onClick = onDisconnect,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Desconectar",
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        // Stats + filter row (or selection action bar)
        if (uiState.isSelectionMode) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.secondaryContainer)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(onClick = onClearSelection, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Cancelar seleção", modifier = Modifier.size(18.dp))
                }
                Text(
                    "${uiState.selectedIds.size} selecionado${if (uiState.selectedIds.size != 1) "s" else ""}",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = onSelectAll, contentPadding = PaddingValues(horizontal = 8.dp)) {
                    Text("Todos", style = MaterialTheme.typography.labelSmall)
                }
                Button(
                    onClick = onSaveSelected,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Baixar", style = MaterialTheme.typography.labelSmall)
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val images = uiState.files.count { it.isImage }
                val videos = uiState.files.count { it.isVideo }
                Text(
                    "${uiState.files.size} arq · $images img · $videos vid",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                DesktopFileFilter.entries.forEach { filter ->
                    val active = uiState.filter == filter
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                if (active) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .clickable { onFilter(filter) }
                            .padding(horizontal = 12.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = when (filter) {
                                DesktopFileFilter.ALL -> "Todos"
                                DesktopFileFilter.IMAGES -> "Imagens"
                                DesktopFileFilter.VIDEOS -> "Vídeos"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = if (active) MaterialTheme.colorScheme.onPrimaryContainer
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Download All bar
        if (uiState.filteredFiles.isNotEmpty()) {
            val totalSize = uiState.filteredFiles.sumOf { it.size }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.Download,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "${uiState.filteredFiles.size} arquivos · ${formatFileSize(totalSize)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                FilledTonalButton(
                    onClick = { onSaveAll(uiState.filteredFiles) },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text("Baixar todos", style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        Divider()

        // File list
        if (uiState.filteredFiles.isEmpty() && !uiState.isLoadingFiles) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    if (uiState.files.isEmpty()) "Nenhum arquivo compartilhado.\nAdicione pastas no servidor."
                    else "Nenhum arquivo nesta categoria.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(uiState.filteredFiles, key = { it.id }) { file ->
                    DesktopFileCard(
                        file = file,
                        baseUrl = uiState.baseUrl,
                        isDownloading = uiState.downloadingId == file.id,
                        isSelected = file.id in uiState.selectedIds,
                        isSelectionMode = uiState.isSelectionMode,
                        onToggleSelection = { onToggleSelection(file.id) },
                        onSave = { onSaveFile(file) }
                    )
                }
            }
        }
    }
}

// ──────────────────────────────────────────────
// File card
// ──────────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DesktopFileCard(
    file: DesktopFile,
    baseUrl: String,
    isDownloading: Boolean,
    isSelected: Boolean = false,
    isSelectionMode: Boolean = false,
    onToggleSelection: () -> Unit = {},
    onSave: () -> Unit
) {
    val thumbBg = if (file.isVideo) MaterialTheme.colorScheme.secondaryContainer
                  else MaterialTheme.colorScheme.tertiaryContainer
    val thumbTint = if (file.isVideo) MaterialTheme.colorScheme.onSecondaryContainer
                    else MaterialTheme.colorScheme.onTertiaryContainer

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = { if (isSelectionMode) onToggleSelection() },
                onLongClick = { if (!isSelectionMode) onToggleSelection() }
            ),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.secondaryContainer
                             else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Thumbnail / selection indicator
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(thumbBg),
                contentAlignment = Alignment.Center
            ) {
                var thumbFailed by remember(file.id) { mutableStateOf(false) }
                if (!thumbFailed) {
                    AsyncImage(
                        model = DesktopRepository.thumbnailUrl(baseUrl, file.id),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                        onError = { thumbFailed = true }
                    )
                } else {
                    Icon(
                        imageVector = if (file.isVideo) Icons.Default.VideoFile else Icons.Default.Image,
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                        tint = thumbTint
                    )
                }
                // Video play badge
                if (file.isVideo && !thumbFailed) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.35f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(22.dp),
                            tint = androidx.compose.ui.graphics.Color.White
                        )
                    }
                }
                // Selection checkmark overlay
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(28.dp),
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                } else if (isSelectionMode) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(3.dp)
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                        contentAlignment = Alignment.Center
                    ) {}
                }
            }

            // Name + size
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    file.name,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    formatFileSize(file.size),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Download button (hidden in selection mode)
            if (!isSelectionMode) {
                if (isDownloading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                } else {
                    IconButton(onClick = onSave, modifier = Modifier.size(40.dp)) {
                        Icon(Icons.Default.Download, contentDescription = "Baixar")
                    }
                }
            }
        }
    }
}

private fun formatFileSize(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> "${"%.1f".format(bytes / 1024f)} KB"
    bytes < 1024 * 1024 * 1024 -> "${"%.1f".format(bytes / 1024f / 1024f)} MB"
    else -> "${"%.2f".format(bytes / 1024f / 1024f / 1024f)} GB"
}
