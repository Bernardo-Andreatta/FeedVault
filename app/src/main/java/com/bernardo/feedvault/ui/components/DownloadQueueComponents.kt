package com.bernardo.feedvault.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.bernardo.feedvault.data.DownloadItem
import com.bernardo.feedvault.data.DownloadStatus
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadQueueFab(
    items: List<DownloadItem>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (items.isEmpty()) return
    val activeCount = items.count { it.status == DownloadStatus.QUEUED || it.status == DownloadStatus.DOWNLOADING }
    BadgedBox(
        modifier = modifier,
        badge = { if (activeCount > 0) Badge { Text("$activeCount") } }
    ) {
        SmallFloatingActionButton(
            onClick = onClick,
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ) {
            if (items.any { it.status == DownloadStatus.DOWNLOADING }) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            } else {
                Icon(
                    Icons.Default.Download,
                    contentDescription = "Downloads",
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DismissableDownloadFab(
    items: List<DownloadItem>,
    onClick: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (items.isEmpty()) return

    val activeCount = items.count { it.status == DownloadStatus.QUEUED || it.status == DownloadStatus.DOWNLOADING }
    val scope = rememberCoroutineScope()
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }

    val animX = remember { Animatable(0f) }
    val animY = remember { Animatable(0f) }

    Box(
        modifier = modifier
            .offset { IntOffset(animX.value.roundToInt(), animY.value.roundToInt()) }
            .graphicsLayer {
                alpha = (1f - abs(animX.value) / (screenWidthPx * 0.4f)).coerceIn(0.3f, 1f)
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDrag = { change, dragAmount ->
                        change.consume()
                        scope.launch {
                            animX.snapTo(animX.value + dragAmount.x)
                            animY.snapTo(animY.value + dragAmount.y)
                        }
                    },
                    onDragEnd = {
                        if (abs(animX.value) > screenWidthPx * 0.25f) {
                            val targetX = if (animX.value > 0) screenWidthPx * 1.5f else -screenWidthPx * 1.5f
                            scope.launch {
                                animX.animateTo(targetX, tween(180))
                                onDismiss()
                            }
                        } else {
                            scope.launch {
                                launch { animX.animateTo(0f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium)) }
                                launch { animY.animateTo(0f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium)) }
                            }
                        }
                    },
                    onDragCancel = {
                        scope.launch {
                            launch { animX.animateTo(0f, spring(Spring.DampingRatioMediumBouncy)) }
                            launch { animY.animateTo(0f, spring(Spring.DampingRatioMediumBouncy)) }
                        }
                    }
                )
            }
    ) {
        BadgedBox(
            badge = { if (activeCount > 0) Badge { Text("$activeCount") } }
        ) {
            SmallFloatingActionButton(
                onClick = onClick,
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            ) {
                if (items.any { it.status == DownloadStatus.DOWNLOADING }) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                } else {
                    Icon(
                        Icons.Default.Download,
                        contentDescription = "Downloads",
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadQueueSheet(
    items: List<DownloadItem>,
    onDismiss: () -> Unit,
    onRetry: (String) -> Unit,
    onDismissItem: (String) -> Unit,
    onDismissCompleted: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Downloads", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                if (items.any { it.status == DownloadStatus.DONE }) {
                    TextButton(onClick = onDismissCompleted) {
                        Text("Limpar concluídos", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
            Divider()
            if (items.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Nenhum download", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp)
                ) {
                    items(items, key = { it.id }) { item ->
                        DownloadItemCard(
                            item = item,
                            onRetry = { onRetry(item.id) },
                            onDismiss = { onDismissItem(item.id) }
                        )
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun DownloadItemCard(
    item: DownloadItem,
    onRetry: () -> Unit,
    onDismiss: () -> Unit
) {
    val containerColor = when (item.status) {
        DownloadStatus.DONE -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
        DownloadStatus.FAILED -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f)
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(modifier = Modifier.size(32.dp), contentAlignment = Alignment.Center) {
                    when (item.status) {
                        DownloadStatus.QUEUED -> Icon(
                            Icons.Default.Schedule,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        DownloadStatus.DOWNLOADING -> CircularProgressIndicator(
                            progress = item.progress.coerceIn(0f, 1f),
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.5.dp,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                        DownloadStatus.DONE -> Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        DownloadStatus.FAILED -> Icon(
                            Icons.Default.Error,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        item.name,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            item.source,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (item.status == DownloadStatus.DOWNLOADING && item.progress > 0f) {
                            Text(
                                "${(item.progress * 100).toInt()}%",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        if (item.status == DownloadStatus.FAILED && item.error != null) {
                            Text(
                                item.error,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                        }
                    }
                }
                when (item.status) {
                    DownloadStatus.FAILED -> {
                        IconButton(onClick = onRetry, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Refresh, contentDescription = "Tentar novamente", modifier = Modifier.size(18.dp))
                        }
                        IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Remover", modifier = Modifier.size(16.dp))
                        }
                    }
                    DownloadStatus.DONE -> IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Remover", modifier = Modifier.size(16.dp))
                    }
                    else -> {}
                }
            }
            if (item.status == DownloadStatus.DOWNLOADING) {
                Spacer(Modifier.height(8.dp))
                if (item.progress > 0f) {
                    LinearProgressIndicator(
                        progress = item.progress.coerceIn(0f, 1f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .clip(RoundedCornerShape(1.5.dp))
                    )
                } else {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .clip(RoundedCornerShape(1.5.dp))
                    )
                }
            }
        }
    }
}
