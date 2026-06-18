package com.example.securegallery.ui.components

import android.app.Activity
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.graphics.drawable.Animatable
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.text.TextStyle
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.MediaItem as Media3Item
import androidx.media3.common.Player
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import coil.decode.VideoFrameDecoder
import coil.imageLoader
import coil.request.ImageRequest
import com.example.securegallery.data.MediaItem
import com.example.securegallery.data.VideoClip
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.TextButton

private fun findItemIdAt(
    gridState: LazyGridState,
    position: Offset
): Long? {
    for (info in gridState.layoutInfo.visibleItemsInfo) {
        val left = info.offset.x.toFloat()
        val top = info.offset.y.toFloat()
        val right = left + info.size.width
        val bottom = top + info.size.height
        if (position.x in left..right && position.y in top..bottom) {
            // Use grid item key (= item.id) instead of items[info.index].
            // When items reorder on first load, layoutInfo indices lag one frame
            // behind the new list → items.getOrNull(info.index) returns the wrong item.
            // The key is always in sync with what is visually shown at that position.
            return info.key as? Long
        }
    }
    return null
}

@Composable
fun MediaFeed(
    items: List<MediaItem>,
    currentlyPlayingUri: String?,
    onPlayRequested: (String) -> Unit,
    onEditTags: (MediaItem) -> Unit,
    onEditPeople: (MediaItem) -> Unit,
    onToggleFavorite: (MediaItem) -> Unit,
    onFullscreenRequested: (MediaItem, Long, Boolean, Boolean) -> Unit,
    clipsByMediaId: Map<Long, List<VideoClip>> = emptyMap(),
    onRemoveTag: ((MediaItem, String) -> Unit)? = null,
    onFilterByTag: (String) -> Unit = {},
    onFilterByPerson: (String) -> Unit = {},
    isGridView: Boolean = false,
    seekTokens: Map<String, Pair<Int, Long>> = emptyMap(),
    onSeekTokenConsumed: (String) -> Unit = {},
    seekOnlyTokens: Map<String, Pair<Int, Long>> = emptyMap(),
    onSeekOnlyTokenConsumed: (String) -> Unit = {},
    listState: LazyListState = rememberLazyListState(),
    gridState: LazyGridState = rememberLazyGridState(),
    selectedIds: Set<Long> = emptySet(),
    isSelectionMode: Boolean = false,
    onToggleSelection: (Long) -> Unit = {},
    onDeleteMedia: (MediaItem) -> Unit = {},
    modifier: Modifier = Modifier
) {
    if (isGridView) {
        val currentSelectedIds by rememberUpdatedState(selectedIds)
        val currentIsSelectionMode by rememberUpdatedState(isSelectionMode)
        val currentOnFullscreenRequested by rememberUpdatedState(onFullscreenRequested)
        val currentOnToggleSelection by rememberUpdatedState(onToggleSelection)
        val isScrolling by remember { derivedStateOf { gridState.isScrollInProgress } }
        val hapticFeedback = LocalHapticFeedback.current

        Box(
            modifier = modifier.pointerInput(items) {
                awaitEachGesture {
                    // Detect down using Main pass (default) — grid scroll tracking starts in parallel
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val downPos = down.position
                    val downId = findItemIdAt(gridState, downPos)
                    val downItem = if (downId != null) items.firstOrNull { it.id == downId } else null

                    // Use Initial pass to classify gesture before grid consumes events
                    val gestureType = withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            val change = event.changes.firstOrNull { it.id == down.id }
                                ?: return@withTimeoutOrNull "TAP"
                            if (!change.pressed) return@withTimeoutOrNull "TAP"
                            if ((change.position - downPos).getDistance() > viewConfiguration.touchSlop)
                                return@withTimeoutOrNull "SCROLL"
                        }
                        @Suppress("UNREACHABLE_CODE") "TAP"
                    }

                    when (gestureType) {
                        "TAP" -> {
                            if (downItem != null) {
                                if (currentIsSelectionMode) currentOnToggleSelection(downItem.id)
                                else currentOnFullscreenRequested(downItem, 0L, true, true)
                            }
                        }
                        "SCROLL" -> { /* not consumed → grid scrolls normally */ }
                        null -> {
                            // Long press: select starting item
                            val startIndex = items.indexOfFirst { it.id == downId }
                            val dragAddedIds = mutableSetOf<Long>()
                            if (downItem != null && downItem.id !in currentSelectedIds) {
                                currentOnToggleSelection(downItem.id)
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                if (downId != null) dragAddedIds.add(downId)
                            }
                            var lastId: Long? = downId
                            while (true) {
                                // Consume at Initial pass → prevents grid from scrolling during drag-select
                                val event = awaitPointerEvent(PointerEventPass.Initial)
                                val change = event.changes.firstOrNull() ?: break
                                if (!change.pressed) break
                                change.consume()
                                val id = findItemIdAt(gridState, change.position)
                                if (id != null && id != lastId) {
                                    val currentIndex = items.indexOfFirst { it.id == id }
                                    if (startIndex >= 0 && currentIndex >= 0) {
                                        val from = minOf(startIndex, currentIndex)
                                        val to = maxOf(startIndex, currentIndex)
                                        for (i in from..to) {
                                            val rangeId = items[i].id
                                            if (rangeId !in currentSelectedIds && rangeId !in dragAddedIds) {
                                                currentOnToggleSelection(rangeId)
                                                hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                dragAddedIds.add(rangeId)
                                            }
                                        }
                                    }
                                    lastId = id
                                }
                            }
                        }
                    }
                }
            }
        ) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                state = gridState,
                contentPadding = PaddingValues(2.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                gridItems(items, key = { it.id }) { item ->
                    MediaGridItem(
                        item = item,
                        isSelected = item.id in selectedIds,
                        isSelectionMode = isSelectionMode,
                        isScrolling = isScrolling
                    )
                }
            }
        }
    } else {
        BoxWithConstraints(modifier = modifier) {
            val feedHeight = maxHeight
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(vertical = 2.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(items, key = { it.id }) { item ->
                    MediaPost(
                        item = item,
                        currentlyPlayingUri = currentlyPlayingUri,
                        onPlayRequested = onPlayRequested,
                        onEditTags = { onEditTags(item) },
                        onEditPeople = { onEditPeople(item) },
                        onToggleFavorite = { onToggleFavorite(item) },
                        onFullscreenRequested = { pos, wasPlaying, muted -> onFullscreenRequested(item, pos, wasPlaying, muted) },
                        clips = clipsByMediaId[item.id] ?: emptyList(),
                        onRemoveTag = if (onRemoveTag != null) { tag -> onRemoveTag(item, tag) } else null,
                        onFilterByTag = onFilterByTag,
                        onFilterByPerson = onFilterByPerson,
                        onDeleteMedia = { onDeleteMedia(item) },
                        availableHeight = feedHeight,
                        seekToken = seekTokens[item.uri]?.first ?: 0,
                        seekPositionMs = seekTokens[item.uri]?.second ?: 0L,
                        onSeekTokenConsumed = { onSeekTokenConsumed(item.uri) },
                        seekOnlyToken = seekOnlyTokens[item.uri]?.first ?: 0,
                        seekOnlyMs = seekOnlyTokens[item.uri]?.second ?: 0L,
                        onSeekOnlyTokenConsumed = { onSeekOnlyTokenConsumed(item.uri) }
                    )
                }
            }
        }
    }
}

@Composable
fun MediaGridItem(
    item: MediaItem,
    isSelected: Boolean = false,
    isSelectionMode: Boolean = false,
    isScrolling: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showImageFullscreen by remember { mutableStateOf(false) }
    val isGif = item.mimeType == "image/gif" ||
        (item.mimeType.isEmpty() && item.fileName.endsWith(".gif", ignoreCase = true))

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(Uri.parse(item.uri))
                .apply {
                    if (item.mediaType == "video") {
                        decoderFactory(VideoFrameDecoder.Factory())
                        if (item.thumbnailFrameMs >= 0) setParameter("coil#video_frame_micros", item.thumbnailFrameMs * 1000L)
                    }
                }
                .memoryCacheKey("${item.uri}:${item.thumbnailFrameMs}:static")
                .build(),
            // Static loader for all images — decodes only first frame, no animation.
            // Prevents animated GIFs/WebP from playing in the grid regardless of format detection.
            imageLoader = if (item.mediaType != "video") com.example.securegallery.App.staticImageLoader
                          else coil.Coil.imageLoader(context),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        if (item.mediaType == "video" && !isSelectionMode) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.55f))
                    .align(Alignment.Center),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
            }
        }
        if (isGif && !isSelectionMode) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(4.dp)
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(4.dp))
                    .background(Color.Black.copy(alpha = 0.65f))
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "GIF",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF69F0AE),
                    fontSize = androidx.compose.ui.unit.TextUnit(9f, androidx.compose.ui.unit.TextUnitType.Sp)
                )
            }
        }
        if (item.isFavorite && !isSelectionMode) {
            Icon(
                Icons.Default.Favorite,
                contentDescription = null,
                tint = Color(0xFFE91E63),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(3.dp)
                    .size(14.dp)
            )
        }
        if (isSelectionMode) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        if (isSelected) Color.Black.copy(alpha = 0.45f)
                        else Color.Transparent
                    )
            )
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(5.dp)
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primary
                        else Color.Black.copy(alpha = 0.45f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                }
            }
        }
    }
    if (showImageFullscreen) {
        ImageFullscreenDialog(uri = item.uri, onDismiss = { showImageFullscreen = false })
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MediaPost(
    item: MediaItem,
    currentlyPlayingUri: String?,
    onPlayRequested: (String) -> Unit,
    onEditTags: () -> Unit,
    onEditPeople: () -> Unit,
    onToggleFavorite: () -> Unit = {},
    onFullscreenRequested: (Long, Boolean, Boolean) -> Unit = { _, _, _ -> },
    clips: List<VideoClip> = emptyList(),
    onRemoveTag: ((String) -> Unit)? = null,
    onFilterByTag: (String) -> Unit = {},
    onFilterByPerson: (String) -> Unit = {},
    onDeleteMedia: () -> Unit = {},
    availableHeight: Dp = Dp.Unspecified,
    seekToken: Int = 0,
    seekPositionMs: Long = 0L,
    onSeekTokenConsumed: () -> Unit = {},
    seekOnlyToken: Int = 0,
    seekOnlyMs: Long = 0L,
    onSeekOnlyTokenConsumed: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showImageFullscreen by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var clipPlayRequest by remember(item.id) { mutableStateOf(0 to 0L) }
    var clipSeekOnlyRequest by remember(item.id) { mutableStateOf(0 to 0L) }
    var expandedClipId by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(seekToken) {
        if (seekToken > 0) {
            clipPlayRequest = (clipPlayRequest.first + 1) to seekPositionMs
            onSeekTokenConsumed()
        }
    }

    LaunchedEffect(seekOnlyToken) {
        if (seekOnlyToken > 0) {
            clipSeekOnlyRequest = (clipSeekOnlyRequest.first + 1) to seekOnlyMs
            onSeekOnlyTokenConsumed()
        }
    }
    val tags = item.tags.filter { it.isNotBlank() }
    val maxVisible = 5
    var tagsExpanded by remember(item.id) { mutableStateOf(false) }
    var tagEditMode by remember(item.id) { mutableStateOf(false) }
    val visibleTags = if (tagsExpanded || tags.size <= maxVisible) tags else tags.take(maxVisible)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(0.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column {
            if (item.mediaType == "video") {
                VideoPlayer(
                    uri = item.uri,
                    aspectRatio = item.aspectRatio,
                    maxMediaHeight = availableHeight,
                    currentlyPlayingUri = currentlyPlayingUri,
                    onPlayRequested = onPlayRequested,
                    onFullscreenRequested = onFullscreenRequested,
                    thumbnailFrameMs = item.thumbnailFrameMs,
                    externalPlayRequestKey = clipPlayRequest.first,
                    externalPlayRequestMs = clipPlayRequest.second,
                    externalSeekOnlyKey = clipSeekOnlyRequest.first,
                    externalSeekOnlyMs = clipSeekOnlyRequest.second
                )
            } else {
                val isItemGif = item.mimeType == "image/gif" ||
                    (item.mimeType.isEmpty() && item.fileName.endsWith(".gif", ignoreCase = true))
                val context = LocalContext.current
                if (isItemGif) {
                    // GIF plays inline like a video: starts paused (first frame), tap toggles
                    // animation. Static loader for the paused frame, animated singleton loader
                    // for playback. Fullscreen only via the dedicated button.
                    var gifPlaying by remember(item.id) { mutableStateOf(false) }
                    Box {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(Uri.parse(item.uri))
                                .memoryCacheKey(if (gifPlaying) "${item.uri}:anim" else "${item.uri}:static")
                                .build(),
                            imageLoader = if (gifPlaying) context.imageLoader
                                          else com.example.securegallery.App.staticImageLoader,
                            contentDescription = item.fileName,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = if (availableHeight != Dp.Unspecified) availableHeight else 560.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { gifPlaying = !gifPlaying }
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(6.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color.Black.copy(alpha = 0.65f))
                                .padding(horizontal = 5.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "GIF",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF69F0AE)
                            )
                        }
                        // Play hint — shown only while paused
                        if (!gifPlaying) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.45f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.PlayArrow,
                                    contentDescription = "Reproduzir GIF",
                                    tint = Color.White,
                                    modifier = Modifier.size(34.dp)
                                )
                            }
                        }
                        // Fullscreen button — bottom-end
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(6.dp)
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.55f))
                                .clickable { onFullscreenRequested(0L, true, true) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Fullscreen,
                                contentDescription = "Tela cheia",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                } else {
                    Box {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(Uri.parse(item.uri))
                                .memoryCacheKey("${item.uri}:static")
                                .build(),
                            // Static loader for all images: first frame only, no animation in list view.
                            imageLoader = com.example.securegallery.App.staticImageLoader,
                            contentDescription = item.fileName,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = if (availableHeight != Dp.Unspecified) availableHeight else 560.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { onFullscreenRequested(0L, true, true) }
                        )
                    }
                }
            }

            // Clip pills — visible only for videos with at least one clip
            if (item.mediaType == "video" && clips.isNotEmpty()) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(clips, key = { it.id }) { clip ->
                        Box {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.tertiaryContainer)
                                    .clickable { expandedClipId = clip.id }
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                val label = if (clip.label.isNotBlank()) clip.label
                                            else "${formatDuration(clip.startMs)}→${formatDuration(clip.endMs)}"
                                Text(
                                    text = "✂ $label",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }
                            DropdownMenu(
                                expanded = expandedClipId == clip.id,
                                onDismissRequest = { expandedClipId = null }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Tela cheia") },
                                    onClick = {
                                        expandedClipId = null
                                        onFullscreenRequested(clip.startMs, true, true)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Ver incorporado") },
                                    onClick = {
                                        expandedClipId = null
                                        clipPlayRequest = (clipPlayRequest.first + 1) to clip.startMs
                                        onPlayRequested(item.uri)
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Tags + people row
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                // People pills first
                item.people.filter { it.isNotBlank() }.forEach { person ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.secondaryContainer)
                            .clickable { onFilterByPerson(person) }
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = person,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }

                if (tags.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.tertiaryContainer)
                            .clickable { onEditTags() }
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            "Adicionar tag",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                } else {
                    visibleTags.forEach { tag ->
                        if (tagEditMode && onRemoveTag != null) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer)
                            ) {
                                Text(
                                    text = tag,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(start = 8.dp, top = 3.dp, bottom = 3.dp)
                                )
                                Box(
                                    modifier = Modifier
                                        .padding(horizontal = 4.dp)
                                        .size(14.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.error)
                                        .clickable { onRemoveTag(tag) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Remover $tag",
                                        tint = Color.White,
                                        modifier = Modifier.size(10.dp)
                                    )
                                }
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer)
                                    .clickable { onFilterByTag(tag) }
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = tag,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                    if (!tagsExpanded && tags.size > maxVisible) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { tagsExpanded = true }
                                .padding(horizontal = 10.dp, vertical = 3.dp)
                        ) {
                            Text("▾ ${tags.size - maxVisible}", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    if (tagsExpanded && tags.size > maxVisible) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { tagsExpanded = false }
                                .padding(horizontal = 10.dp, vertical = 3.dp)
                        ) {
                            Text("▴", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .clickable { onEditTags() }
                            .padding(horizontal = 10.dp, vertical = 3.dp)
                    ) {
                        Text("+", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
            }

            // Action row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 1.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(modifier = Modifier.weight(1f))
                if (onRemoveTag != null && tags.isNotEmpty()) {
                    IconButton(
                        onClick = { tagEditMode = !tagEditMode },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Editar tags",
                            tint = if (tagEditMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = if (item.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorito",
                        tint = if (item.isFavorite) Color(0xFFE91E63) else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(
                    onClick = onEditPeople,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Pessoas",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                IconButton(
                    onClick = { showDeleteConfirm = true },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Deletar",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.75f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }

    if (showImageFullscreen) {
        ImageFullscreenDialog(uri = item.uri, onDismiss = { showImageFullscreen = false })
    }
    if (showDeleteConfirm) {
        DeleteConfirmDialog(
            fileName = item.fileName,
            onConfirm = onDeleteMedia,
            onDismiss = { showDeleteConfirm = false }
        )
    }
}

@Composable
fun VideoPlayer(
    uri: String,
    aspectRatio: Float = 0f,
    maxMediaHeight: Dp = Dp.Unspecified,
    currentlyPlayingUri: String?,
    onPlayRequested: (String) -> Unit,
    onFullscreenRequested: (Long, Boolean, Boolean) -> Unit = { _, _, _ -> },
    clipStartMs: Long? = null,
    clipEndMs: Long? = null,
    thumbnailFrameMs: Long = -1L,
    videoResizeMode: Int = AspectRatioFrameLayout.RESIZE_MODE_FILL,
    externalPlayRequestKey: Int = 0,
    externalPlayRequestMs: Long = 0L,
    externalSeekOnlyKey: Int = 0,
    externalSeekOnlyMs: Long = 0L,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val windowView = LocalView.current
    var isPlaying by remember(uri) { mutableStateOf(false) }
    var isBuffering by remember(uri) { mutableStateOf(false) }
    var showControls by remember(uri) { mutableStateOf(true) }
    var isMuted by remember(uri) { mutableStateOf(true) }
    var hideTimer by remember(uri) { mutableStateOf(0) }
    var currentPosition by remember(uri) { mutableStateOf(0L) }
    var duration by remember(uri) { mutableStateOf(0L) }
    var isScrubbing by remember(uri) { mutableStateOf(false) }
    var scrubPos by remember(uri) { mutableStateOf(0L) }
    var embedSeekHint by remember(uri) { mutableStateOf<SeekHint?>(null) }
    var embedSeekAccumMs by remember(uri) { mutableStateOf(0L) }
    var pendingSeekMs by remember(uri) { mutableStateOf(0L) }
    var itemVisible by remember { mutableStateOf(true) }
    val exoPlayerRef = remember(uri) { mutableStateOf<ExoPlayer?>(null) }
    val exoPlayer = exoPlayerRef.value

    // Int counter key guarantees re-run on every reveal; hides 3s later if still playing
    LaunchedEffect(hideTimer) {
        if (hideTimer == 0) return@LaunchedEffect
        delay(3000)
        if (isPlaying) showControls = false
    }

    LaunchedEffect(currentlyPlayingUri) {
        if (currentlyPlayingUri != uri) {
            // stop() releases decoder buffers; pause() does not.
            // Two 4K decoders alive simultaneously (list + fullscreen) → OOM.
            exoPlayerRef.value?.stop()
        }
    }

    LaunchedEffect(itemVisible) {
        if (!itemVisible) {
            exoPlayerRef.value?.stop()
        }
    }

    // Poll duration until known (fires as soon as player exists, independent of play state)
    LaunchedEffect(exoPlayer) {
        val p = exoPlayer ?: return@LaunchedEffect
        while (duration == 0L) {
            val d = p.duration
            if (d > 0) duration = d
            delay(200)
        }
    }

    // Poll position while playing
    LaunchedEffect(exoPlayer, isPlaying) {
        val p = exoPlayer ?: return@LaunchedEffect
        if (!isPlaying) return@LaunchedEffect
        while (true) {
            if (!isScrubbing) currentPosition = p.currentPosition
            delay(500)
        }
    }

    DisposableEffect(uri) {
        onDispose {
            exoPlayerRef.value?.release()
            exoPlayerRef.value = null
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                exoPlayerRef.value?.pause()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    fun ensurePlayerAndSeek(seekMs: Long) {
        val p = exoPlayerRef.value
        if (p != null) {
            if (p.playbackState == Player.STATE_IDLE) {
                p.prepare()
            }
            if (seekMs > 0) p.seekTo(seekMs)
            p.repeatMode = Player.REPEAT_MODE_ONE
            if (!p.isPlaying) { onPlayRequested(uri); p.play() }
            showControls = true
            hideTimer++
        } else {
            onPlayRequested(uri)
            val newPlayer = ExoPlayer.Builder(context)
                .setLoadControl(
                    DefaultLoadControl.Builder()
                        .setBufferDurationsMs(
                            15_000,
                            30_000,
                            DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_MS,
                            DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS
                        )
                        .build()
                )
                .build().apply {
                val mediaItem = if (clipStartMs != null && clipEndMs != null) {
                    Media3Item.Builder()
                        .setUri(Uri.parse(uri))
                        .setClippingConfiguration(
                            Media3Item.ClippingConfiguration.Builder()
                                .setStartPositionMs(clipStartMs)
                                .setEndPositionMs(clipEndMs)
                                .build()
                        )
                        .build()
                } else {
                    Media3Item.fromUri(Uri.parse(uri))
                }
                setMediaItem(mediaItem)
                repeatMode = Player.REPEAT_MODE_ONE
                prepare()
                if (seekMs > 0) seekTo(seekMs)
                playWhenReady = true
                volume = 0f
            }
            newPlayer.addListener(object : Player.Listener {
                override fun onIsPlayingChanged(playing: Boolean) {
                    isPlaying = playing
                    if (playing) {
                        showControls = false
                    } else if (newPlayer.playbackState != Player.STATE_BUFFERING) {
                        showControls = true
                    }
                }
                override fun onPlaybackStateChanged(state: Int) {
                    isBuffering = state == Player.STATE_BUFFERING
                    if (state == Player.STATE_READY && duration == 0L) {
                        val d = newPlayer.duration; if (d > 0) duration = d
                    }
                    if (state == Player.STATE_ENDED) {
                        isPlaying = false; showControls = true; currentPosition = 0L
                    }
                }
            })
            exoPlayerRef.value = newPlayer
            pendingSeekMs = 0L
            showControls = true
            hideTimer++
        }
    }

    fun seekAndPlayExternal(seekMs: Long) {
        val p = exoPlayerRef.value
        if (p != null) {
            if (p.playbackState == Player.STATE_IDLE) {
                p.prepare()
            }
            if (seekMs > 0) p.seekTo(seekMs)
            p.repeatMode = Player.REPEAT_MODE_ONE
            if (!p.isPlaying) p.play()
            showControls = true
            hideTimer++
        } else {
            val newPlayer = ExoPlayer.Builder(context)
                .setLoadControl(
                    DefaultLoadControl.Builder()
                        .setBufferDurationsMs(
                            15_000,
                            30_000,
                            DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_MS,
                            DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS
                        )
                        .build()
                )
                .build().apply {
                val mediaItem = if (clipStartMs != null && clipEndMs != null) {
                    Media3Item.Builder()
                        .setUri(Uri.parse(uri))
                        .setClippingConfiguration(
                            Media3Item.ClippingConfiguration.Builder()
                                .setStartPositionMs(clipStartMs)
                                .setEndPositionMs(clipEndMs)
                                .build()
                        )
                        .build()
                } else {
                    Media3Item.fromUri(Uri.parse(uri))
                }
                setMediaItem(mediaItem)
                repeatMode = Player.REPEAT_MODE_ONE
                prepare()
                if (seekMs > 0) seekTo(seekMs)
                playWhenReady = true
                volume = 0f
            }
            newPlayer.addListener(object : Player.Listener {
                override fun onIsPlayingChanged(playing: Boolean) {
                    isPlaying = playing
                    if (playing) showControls = false
                    else if (newPlayer.playbackState != Player.STATE_BUFFERING) showControls = true
                }
                override fun onPlaybackStateChanged(state: Int) {
                    isBuffering = state == Player.STATE_BUFFERING
                    if (state == Player.STATE_READY && duration == 0L) {
                        val d = newPlayer.duration; if (d > 0) duration = d
                    }
                    if (state == Player.STATE_ENDED) {
                        isPlaying = false; showControls = true; currentPosition = 0L
                    }
                }
            })
            exoPlayerRef.value = newPlayer
            showControls = true
            hideTimer++
        }
    }

    LaunchedEffect(externalPlayRequestKey) {
        if (externalPlayRequestKey > 0) {
            onPlayRequested(uri)
            seekAndPlayExternal(externalPlayRequestMs)
        }
    }

    fun seekOnlyExternal(seekMs: Long) {
        val p = exoPlayerRef.value
        if (p != null) {
            p.pause()
            if (seekMs > 0) p.seekTo(seekMs)
            currentPosition = seekMs
            showControls = true
            hideTimer = 0
        } else {
            // No player yet (video never played in feed). Store position so
            // it's applied when user taps play, and update seek bar display.
            pendingSeekMs = seekMs
            currentPosition = seekMs
        }
    }

    LaunchedEffect(externalSeekOnlyKey) {
        if (externalSeekOnlyKey > 0) seekOnlyExternal(externalSeekOnlyMs)
    }

    val ratio = if (aspectRatio > 0f) aspectRatio else 16f / 9f
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.Black)
            .onGloballyPositioned { coords ->
                val bounds = coords.boundsInWindow()
                val vh = windowView.height.toFloat()
                itemVisible = bounds.top < vh && bounds.bottom > 0f
            }
    ) {
        val naturalH = maxWidth / ratio
        val finalH = if (maxMediaHeight != Dp.Unspecified) minOf(naturalH, maxMediaHeight) else naturalH
        val finalW = minOf(maxWidth, finalH * ratio)
        Box(modifier = Modifier.fillMaxWidth().height(finalH), contentAlignment = Alignment.Center) {
    Box(
        modifier = Modifier
            .width(finalW)
            .height(finalH)
            .pointerInput(uri) {
                detectTapGestures(
                    onTap = {
                        if (exoPlayerRef.value == null) {
                            ensurePlayerAndSeek(pendingSeekMs)
                        } else if (isPlaying && showControls) {
                            showControls = false
                        } else {
                            showControls = true
                            hideTimer++
                        }
                    },
                    onDoubleTap = { offset ->
                        val p = exoPlayerRef.value ?: return@detectTapGestures
                        val isLeft = offset.x < size.width / 2f
                        val delta = if (isLeft) -10_000L else 10_000L
                        val dur = p.duration
                        val newPos = if (dur > 0) (p.currentPosition + delta).coerceIn(0L, dur)
                                     else (p.currentPosition + delta).coerceAtLeast(0L)
                        p.seekTo(newPos)
                        currentPosition = newPos
                        val current = embedSeekHint
                        val accum = if (current != null && current.isLeft == isLeft) embedSeekAccumMs + 10_000L else 10_000L
                        embedSeekAccumMs = accum
                        val secs = (accum / 1000L).toInt()
                        embedSeekHint = SeekHint(if (isLeft) "-${secs}s" else "+${secs}s", isLeft, System.currentTimeMillis())
                    }
                )
            }
    ) {
        if (exoPlayer != null) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = exoPlayer
                        useController = false
                        this.resizeMode = videoResizeMode
                    }
                },
                update = { view -> view.player = exoPlayer },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            val thumbFrameMs = clipStartMs ?: thumbnailFrameMs.takeIf { it >= 0 }
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(Uri.parse(uri))
                    .decoderFactory(VideoFrameDecoder.Factory())
                    .apply { if (thumbFrameMs != null) setParameter("coil#video_frame_micros", thumbFrameMs * 1000L) }
                    .memoryCacheKey("$uri:${thumbFrameMs ?: -1}")
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Seek hint flash
        embedSeekHint?.let { hint ->
            LaunchedEffect(hint.id) {
                delay(600)
                embedSeekHint = null
                embedSeekAccumMs = 0L
            }
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(0.5f)
                    .align(if (hint.isLeft) Alignment.CenterStart else Alignment.CenterEnd),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = hint.text,
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                        .padding(12.dp)
                )
            }
        }

        // Controls overlay — suppress during seek-buffering to avoid flash
        if (showControls || (!isPlaying && !isBuffering)) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.45f))
            ) {
                if (exoPlayer == null) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.6f))
                            .align(Alignment.Center),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(38.dp))
                    }
                } else {
                    // Center: Replay10 | play/pause | Forward10
                    Row(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                val dur = exoPlayer.duration
                                val newPos = if (dur > 0) (exoPlayer.currentPosition - 10_000L).coerceIn(0L, dur)
                                            else (exoPlayer.currentPosition - 10_000L).coerceAtLeast(0L)
                                exoPlayer.seekTo(newPos); currentPosition = newPos
                                showControls = true; hideTimer++
                            },
                            modifier = Modifier.size(44.dp)
                        ) {
                            Icon(Icons.Default.Replay10, contentDescription = "-10s", tint = Color.White, modifier = Modifier.size(34.dp))
                        }

                        IconButton(
                            onClick = {
                                if (exoPlayer.isPlaying) {
                                    exoPlayer.pause()
                                } else {
                                    if (exoPlayer.playbackState == Player.STATE_IDLE) exoPlayer.prepare()
                                    if (exoPlayer.playbackState == Player.STATE_ENDED) exoPlayer.seekTo(0)
                                    onPlayRequested(uri)
                                    exoPlayer.play()
                                    hideTimer++
                                }
                                showControls = true
                            },
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.55f))
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        IconButton(
                            onClick = {
                                val dur = exoPlayer.duration
                                val newPos = if (dur > 0) (exoPlayer.currentPosition + 10_000L).coerceIn(0L, dur)
                                            else (exoPlayer.currentPosition + 10_000L).coerceAtLeast(0L)
                                exoPlayer.seekTo(newPos); currentPosition = newPos
                                showControls = true; hideTimer++
                            },
                            modifier = Modifier.size(44.dp)
                        ) {
                            Icon(Icons.Default.Forward10, contentDescription = "+10s", tint = Color.White, modifier = Modifier.size(34.dp))
                        }
                    }

                    // Bottom: progress bar (no film reel in embedded)
                    if (duration > 0) {
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Slider(
                                value = if (isScrubbing) scrubPos.toFloat() / duration.toFloat()
                                        else currentPosition.toFloat() / duration.toFloat(),
                                onValueChange = { v ->
                                    isScrubbing = true
                                    scrubPos = (v * duration).toLong()
                                    hideTimer++
                                },
                                onValueChangeFinished = {
                                    exoPlayer.seekTo(scrubPos)
                                    currentPosition = scrubPos
                                    isScrubbing = false
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = SliderDefaults.colors(
                                    thumbColor = Color.White,
                                    activeTrackColor = Color.White,
                                    inactiveTrackColor = Color.White.copy(alpha = 0.35f)
                                )
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(formatDuration(if (isScrubbing) scrubPos else currentPosition), color = Color.White, style = MaterialTheme.typography.labelSmall)
                                Text(formatDuration(duration), color = Color.White, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 4.dp, end = 4.dp)
                .size(24.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.55f))
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { onFullscreenRequested(exoPlayer?.currentPosition ?: 0L, if (exoPlayer != null) isPlaying else true, isMuted) },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Fullscreen, contentDescription = "Tela cheia", tint = Color.White, modifier = Modifier.size(16.dp))
        }

        if (exoPlayer != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(top = 4.dp, start = 4.dp)
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.55f))
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) {
                        isMuted = !isMuted
                        exoPlayerRef.value?.volume = if (isMuted) 0f else 1f
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                    contentDescription = if (isMuted) "Ativar som" else "Desativar som",
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FullscreenVideoOverlay(
    items: List<MediaItem>,
    startIndex: Int,
    startPosition: Long,
    onDismiss: () -> Unit,
    autoPlay: Boolean = true,
    startMuted: Boolean = true,
    allAvailableTags: List<String> = emptyList(),
    allAvailablePeople: List<String> = emptyList(),
    onSaveClip: (mediaItemId: Long, uri: String, startMs: Long, endMs: Long, label: String) -> Unit = { _, _, _, _, _ -> },
    onSetThumbnail: (mediaItemId: Long, frameMs: Long) -> Unit = { _, _ -> },
    onToggleFavorite: (Long) -> Unit = {},
    onUpdateTags: (Long, List<String>) -> Unit = { _, _ -> },
    onUpdatePeople: (Long, List<String>) -> Unit = { _, _ -> },
    onFinalPosition: (String, Long) -> Unit = { _, _ -> },
    onFinalPositionPaused: (String, Long) -> Unit = { _, _ -> },
    onReturnedToEmbed: (String) -> Unit = {},
    onFilterByTag: (String) -> Unit = {},
    onFilterByPerson: (String) -> Unit = {},
    onDeleteMedia: (MediaItem) -> Unit = {}
) {
    if (items.isEmpty()) { onDismiss(); return }

    val context = LocalContext.current
    val activity = context as? Activity
    val safeIndex = startIndex.coerceIn(0, items.lastIndex)
    val pagerState = rememberPagerState(initialPage = safeIndex) { items.size }
    var currentPagePositionMs by remember { mutableStateOf(startPosition) }
    var isCurrentPagePlaying by remember { mutableStateOf(autoPlay) }

    val handleDismiss = {
        val currentItem = items.getOrNull(pagerState.currentPage)
        if (currentItem != null) {
            if (isCurrentPagePlaying) {
                onFinalPosition(currentItem.uri, currentPagePositionMs)
            } else {
                onFinalPositionPaused(currentItem.uri, currentPagePositionMs)
            }
            if (pagerState.currentPage != safeIndex) onReturnedToEmbed(currentItem.uri)
        }
        onDismiss()
    }

    BackHandler { handleDismiss() }

    DisposableEffect(Unit) {
        activity?.window?.let { win ->
            WindowCompat.setDecorFitsSystemWindows(win, false)
            WindowCompat.getInsetsController(win, win.decorView).apply {
                hide(WindowInsetsCompat.Type.systemBars())
                systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        }
        onDispose {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            activity?.window?.let { win ->
                WindowCompat.setDecorFitsSystemWindows(win, true)
                WindowCompat.getInsetsController(win, win.decorView)
                    .show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    var isZoomed by remember { mutableStateOf(false) }
    var dismissOffsetY by remember { mutableStateOf(0f) }
    val dismissScope = rememberCoroutineScope()
    val dismissThresholdPx = with(LocalDensity.current) { 150.dp.toPx() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                translationY = dismissOffsetY
                alpha = (1f - dismissOffsetY / 700f).coerceIn(0f, 1f)
            }
            .background(Color.Black)
            .draggable(
                orientation = Orientation.Vertical,
                enabled = !isZoomed,
                state = rememberDraggableState { delta ->
                    if (delta > 0f || dismissOffsetY > 0f)
                        dismissOffsetY = (dismissOffsetY + delta).coerceAtLeast(0f)
                },
                onDragStopped = { velocity ->
                    if (dismissOffsetY > dismissThresholdPx || velocity > 800f) {
                        handleDismiss()
                        dismissOffsetY = 0f
                    } else {
                        dismissScope.launch {
                            animate(
                                initialValue = dismissOffsetY,
                                targetValue = 0f,
                                animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy)
                            ) { v, _ -> dismissOffsetY = v }
                        }
                    }
                }
            )
    ) {
        HorizontalPager(
            state = pagerState,
            userScrollEnabled = !isZoomed,
            // Bind each page slot to the item id, not the index. Without a stable key
            // Compose reuses page slots by position; a reorder/shuffle that happens between
            // the open request and first layout could leave a slot rendering the previous
            // item → wrong video/image in fullscreen.
            key = { page -> items[page].id },
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val item = items[page]
            if (item.mediaType == "video") {
                FullscreenVideoPage(
                    mediaItem = item,
                    isActive = pagerState.currentPage == page,
                    startPosition = if (page == safeIndex) startPosition else 0L,
                    autoPlay = if (page == safeIndex) autoPlay else true,
                    startMuted = if (page == safeIndex) startMuted else true,
                    allAvailableTags = allAvailableTags,
                    allAvailablePeople = allAvailablePeople,
                    onSaveClip = { startMs, endMs, label ->
                        onSaveClip(item.id, item.uri, startMs, endMs, label)
                    },
                    onSetThumbnail = { frameMs -> onSetThumbnail(item.id, frameMs) },
                    onToggleFavorite = { onToggleFavorite(item.id) },
                    onUpdateTags = { tags -> onUpdateTags(item.id, tags) },
                    onUpdatePeople = { people -> onUpdatePeople(item.id, people) },
                    onZoomChanged = { isZoomed = it },
                    onPositionChanged = { pos -> if (page == pagerState.currentPage) currentPagePositionMs = pos },
                    onPlayStateChanged = { playing -> if (page == pagerState.currentPage) isCurrentPagePlaying = playing },
                    onFilterByTag = { tag -> handleDismiss(); onFilterByTag(tag) },
                    onFilterByPerson = { person -> handleDismiss(); onFilterByPerson(person) },
                    onDeleteMedia = { onDeleteMedia(item); onDismiss() }
                )
            } else {
                FullscreenImagePage(
                    mediaItem = item,
                    onZoomChanged = { isZoomed = it },
                    onFilterByTag = { tag -> handleDismiss(); onFilterByTag(tag) },
                    onFilterByPerson = { person -> handleDismiss(); onFilterByPerson(person) },
                    onDeleteMedia = { onDeleteMedia(item); onDismiss() }
                )
            }
        }

        IconButton(
            onClick = { handleDismiss() },
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(8.dp)
                .size(40.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.6f))
        ) {
            Icon(Icons.Default.Close, contentDescription = "Fechar", tint = Color.White)
        }

        if (items.size > 1) {
            Text(
                text = "${pagerState.currentPage + 1}/${items.size}",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
                    .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
    }
}

private data class SeekHint(val text: String, val isLeft: Boolean, val id: Long)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FullscreenVideoPage(
    mediaItem: MediaItem,
    isActive: Boolean,
    startPosition: Long = 0L,
    autoPlay: Boolean = true,
    startMuted: Boolean = true,
    allAvailableTags: List<String> = emptyList(),
    allAvailablePeople: List<String> = emptyList(),
    onSaveClip: (startMs: Long, endMs: Long, label: String) -> Unit = { _, _, _ -> },
    onSetThumbnail: (frameMs: Long) -> Unit = {},
    onToggleFavorite: () -> Unit = {},
    onUpdateTags: (List<String>) -> Unit = {},
    onUpdatePeople: (List<String>) -> Unit = {},
    onZoomChanged: (Boolean) -> Unit = {},
    onPositionChanged: (Long) -> Unit = {},
    onPlayStateChanged: (Boolean) -> Unit = {},
    onFilterByTag: (String) -> Unit = {},
    onFilterByPerson: (String) -> Unit = {},
    onDeleteMedia: () -> Unit = {}
) {
    val context = LocalContext.current
    var isPlaying by remember { mutableStateOf(false) }
    var isBuffering by remember { mutableStateOf(false) }
    var showControls by remember { mutableStateOf(false) }
    var isMuted by remember { mutableStateOf(startMuted) }
    var currentPosition by remember { mutableStateOf(startPosition) }
    var duration by remember { mutableStateOf(0L) }
    var isUserScrubbing by remember { mutableStateOf(false) }
    var scrubPosition by remember { mutableStateOf(0L) }
    var showScrubPreview by remember { mutableStateOf(false) }
    var seekHint by remember { mutableStateOf<SeekHint?>(null) }
    var seekAccumMs by remember { mutableStateOf(0L) }
    var isFavorite by remember(mediaItem.id) { mutableStateOf(mediaItem.isFavorite) }
    var showMetaPanel by remember { mutableStateOf(false) }
    var showTagEditor by remember { mutableStateOf(false) }
    var showPeopleEditor by remember { mutableStateOf(false) }
    var localTags by remember(mediaItem.id) { mutableStateOf(mediaItem.tags) }
    var localPeople by remember(mediaItem.id) { mutableStateOf(mediaItem.people) }
    var clipMode by remember { mutableStateOf(false) }
    var clipStart by remember { mutableStateOf<Long?>(null) }
    var clipEnd by remember { mutableStateOf<Long?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var pendingThumbnailMs by remember { mutableStateOf<Long?>(null) }
    var hasFirstActivated by remember { mutableStateOf(false) }
    var zoomScale by remember { mutableStateOf(1f) }
    var zoomOffset by remember { mutableStateOf(Offset.Zero) }
    var containerWidth by remember { mutableStateOf(0) }
    var containerHeight by remember { mutableStateOf(0) }
    var scrubFrames by remember(mediaItem.uri) { mutableStateOf(reelFrameCache[mediaItem.uri] ?: emptyList()) }
    val scrubImageBitmaps = remember(scrubFrames) { scrubFrames.map { it.asImageBitmap() } }
    var isExtractingFrames by remember(mediaItem.uri) { mutableStateOf(!reelFrameCache.containsKey(mediaItem.uri)) }
    var playerReady by remember(mediaItem.uri) { mutableStateOf(false) }
    val exoPlayer = remember(mediaItem.uri) {
        // Drop cached frames for OTHER videos before allocating decoder buffers — prevents OOM.
        // Do NOT recycle() here: BitmapPainters in adjacent pager pages may still be drawing
        // those bitmaps on the main thread, and recycle() while drawing → crash.
        reelFrameCache.keys.filter { it != mediaItem.uri }.toList().forEach { key ->
            reelFrameCache.remove(key)
        }
        ExoPlayer.Builder(context)
            .setLoadControl(
                DefaultLoadControl.Builder()
                    .setBufferDurationsMs(
                        15_000,
                        30_000,
                        DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_MS,
                        DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS
                    )
                    .build()
            )
            .build().apply {
                setMediaItem(Media3Item.fromUri(Uri.parse(mediaItem.uri)))
                repeatMode = Player.REPEAT_MODE_ONE
                playWhenReady = false
                volume = if (startMuted) 0f else 1f
                // prepare() is called in LaunchedEffect(isActive) so adjacent pager pages
                // don't allocate decoder buffers simultaneously (→ OOM on 4K video).
            }
    }

    // Listen for playback state changes
    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
                onPlayStateChanged(playing)
                if (!playing && exoPlayer.playbackState != Player.STATE_BUFFERING) showControls = true
            }
            override fun onPlaybackStateChanged(state: Int) {
                isBuffering = state == Player.STATE_BUFFERING
                if (state == Player.STATE_READY) {
                    playerReady = true
                    if (duration == 0L) duration = exoPlayer.duration.takeIf { it > 0 } ?: 0L
                }
                if (state == Player.STATE_ENDED) {
                    isPlaying = false
                    showControls = true
                }
            }
            override fun onPositionDiscontinuity(
                oldPosition: Player.PositionInfo,
                newPosition: Player.PositionInfo,
                reason: Int
            ) {
                if (reason == Player.DISCONTINUITY_REASON_SEEK ||
                    reason == Player.DISCONTINUITY_REASON_SEEK_ADJUSTMENT) {
                    showScrubPreview = false
                }
            }
        }
        exoPlayer.addListener(listener)
        onDispose { exoPlayer.removeListener(listener) }
    }

    val fullscreenLifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(fullscreenLifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                exoPlayer.pause()
            }
        }
        fullscreenLifecycleOwner.lifecycle.addObserver(observer)
        onDispose { fullscreenLifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Poll position — 500ms is smooth enough for progress bar.
    // Skip while STATE_IDLE to avoid overwriting startPosition before LaunchedEffect(isActive) seeks.
    LaunchedEffect(exoPlayer) {
        while (true) {
            if (!isUserScrubbing && exoPlayer.playbackState != Player.STATE_IDLE) {
                currentPosition = exoPlayer.currentPosition
                onPositionChanged(currentPosition)
                if (duration == 0L) {
                    val d = exoPlayer.duration
                    if (d > 0) duration = d
                }
            }
            delay(500)
        }
    }

    // Extract frames for scrub preview — wait for player STATE_READY first to avoid
    // racing with decoder buffer allocation (causes OOM on 4K video).
    LaunchedEffect(mediaItem.uri, playerReady) {
        if (!playerReady) return@LaunchedEffect
        if (reelFrameCache.containsKey(mediaItem.uri)) {
            scrubFrames = reelFrameCache[mediaItem.uri]!!
            isExtractingFrames = false
            return@LaunchedEffect
        }
        data class VideoMeta(val dur: Long, val targetPx: Int)
        val meta = withContext(Dispatchers.IO) {
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(context, Uri.parse(mediaItem.uri))
                val dur = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
                val w = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
                val h = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
                val targetPx = when {
                    maxOf(w, h) > 2500 -> 320  // 4K
                    maxOf(w, h) > 1500 -> 240  // 2K
                    else -> 200
                }
                VideoMeta(dur, targetPx)
            } catch (_: Throwable) { VideoMeta(0L, 240) }
            finally { runCatching { retriever.release() } }
        }
        val dur = meta.dur
        val targetPx = meta.targetPx
        if (dur <= 0) { isExtractingFrames = false; return@LaunchedEffect }
        val count = ((dur / 1000L) / 15L).toInt().coerceAtLeast(10).coerceAtMost(60)
        val frames = arrayOfNulls<Bitmap>(count)
        // On API < 27, getFrameAtTime returns full-res bitmaps; limit parallelism to 1 for
        // high-res video to avoid OOM from multiple large frames in memory simultaneously.
        val parallelism = if (android.os.Build.VERSION.SDK_INT >= 27) minOf(4, count) else 1
        withContext(Dispatchers.IO) {
            coroutineScope {
                repeat(parallelism) { group ->
                    launch {
                        val retriever = MediaMetadataRetriever()
                        try {
                            retriever.setDataSource(context, Uri.parse(mediaItem.uri))
                            var i = group
                            while (i < count) {
                                val timeUs = dur * 1000L * i / count
                                val scaled: Bitmap? = if (android.os.Build.VERSION.SDK_INT >= 27) {
                                    retriever.getScaledFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC, targetPx, targetPx)
                                } else {
                                    retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)?.let { raw ->
                                        val scale = targetPx.toFloat() / maxOf(raw.width, raw.height)
                                        Bitmap.createScaledBitmap(raw, (raw.width * scale).toInt().coerceAtLeast(1), (raw.height * scale).toInt().coerceAtLeast(1), true)
                                            .also { raw.recycle() }
                                    }
                                }
                                if (scaled != null) {
                                    frames[i] = scaled
                                    withContext(Dispatchers.Main) { scrubFrames = frames.filterNotNull() }
                                }
                                i += parallelism
                            }
                        } catch (_: Throwable) {
                        } finally {
                            runCatching { retriever.release() }
                        }
                    }
                }
            }
        }
        val finalFrames = frames.filterNotNull()
        reelFrameCache[mediaItem.uri] = finalFrames
        scrubFrames = finalFrames
        isExtractingFrames = false
    }

    // Auto-hide controls 3s after playback — suppressed while clip mode is active
    LaunchedEffect(isPlaying, showControls, clipMode) {
        if (isPlaying && showControls && !clipMode) {
            delay(3000)
            showControls = false
        }
    }

    // Keep controls visible while clip panel is open; clear state on close
    LaunchedEffect(clipMode) {
        if (clipMode) showControls = true
        else { clipStart = null; clipEnd = null; pendingThumbnailMs = null }
    }

    LaunchedEffect(isActive) {
        if (isActive) {
            // Re-prepare if stopped (decoder was released to free heap for other pages).
            if (exoPlayer.playbackState == Player.STATE_IDLE) {
                // First activation: use startPosition param — currentPosition may have been
                // zeroed by the polling loop before this effect ran (player still STATE_IDLE).
                // Re-activations: use currentPosition saved before stop() was called.
                val seekMs = if (!hasFirstActivated) startPosition else currentPosition
                exoPlayer.prepare()
                if (seekMs > 0) exoPlayer.seekTo(seekMs)
            }
            if (!hasFirstActivated) {
                hasFirstActivated = true
                if (autoPlay) {
                    exoPlayer.play()
                    if (!clipMode) showControls = false
                } else {
                    showControls = true
                }
            } else {
                exoPlayer.play()
                if (!clipMode) showControls = false
            }
        } else {
            exoPlayer.pause()
            if (!clipMode) showControls = false
            // Release decoder buffers — 4K decoders can hold 30–60 MB each.
            // Position is saved in currentPosition state; re-seek on next activation.
            exoPlayer.stop()
            playerReady = false
        }
    }

    DisposableEffect(mediaItem.uri) {
        onDispose { exoPlayer.release() }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .onSizeChanged { containerWidth = it.width; containerHeight = it.height }
            .pointerInput("zoom") {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    var isMultiTouch = false
                    while (true) {
                        val event = awaitPointerEvent()
                        if (event.changes.none { it.pressed }) break
                        if (event.changes.count { it.pressed } > 1) isMultiTouch = true
                        if (isMultiTouch || zoomScale > 1f) {
                            event.changes.forEach { it.consume() }
                            val zoomChange = event.calculateZoom()
                            val panChange = event.calculatePan()
                            val newScale = (zoomScale * zoomChange).coerceIn(1f, 5f)
                            val maxX = (newScale - 1f) * containerWidth / 2f
                            val maxY = (newScale - 1f) * containerHeight / 2f
                            zoomOffset = if (newScale > 1f)
                                Offset((zoomOffset.x + panChange.x).coerceIn(-maxX, maxX),
                                       (zoomOffset.y + panChange.y).coerceIn(-maxY, maxY))
                            else Offset.Zero
                            zoomScale = newScale
                            onZoomChanged(newScale > 1f)
                        }
                        // single-touch + scale==1f → don't consume → VerticalPager sees it
                    }
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { if (showMetaPanel) showMetaPanel = false else if (!clipMode) showControls = !showControls },
                    onDoubleTap = { offset ->
                        if (zoomScale > 1f) {
                            zoomScale = 1f; zoomOffset = Offset.Zero; onZoomChanged(false)
                        } else {
                            val isLeft = offset.x < size.width / 2f
                            val delta = if (isLeft) -10_000L else 10_000L
                            val dur = exoPlayer.duration
                            val newPos = if (dur > 0) (exoPlayer.currentPosition + delta).coerceIn(0L, dur)
                                         else (exoPlayer.currentPosition + delta).coerceAtLeast(0L)
                            exoPlayer.seekTo(newPos)
                            val current = seekHint
                            val accum = if (current != null && current.isLeft == isLeft) seekAccumMs + 10_000L else 10_000L
                            seekAccumMs = accum
                            val secs = (accum / 1000L).toInt()
                            seekHint = SeekHint(
                                text = if (isLeft) "-${secs}s" else "+${secs}s",
                                isLeft = isLeft,
                                id = System.currentTimeMillis()
                            )
                        }
                    }
                )
            }
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                }
            },
            update = { view -> view.player = exoPlayer },
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = zoomScale; scaleY = zoomScale
                    translationX = zoomOffset.x; translationY = zoomOffset.y
                }
        )

        // Seek hint flash (+10s / -10s)
        seekHint?.let { hint ->
            LaunchedEffect(hint.id) {
                delay(700)
                seekHint = null
                seekAccumMs = 0L
            }
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(0.5f)
                    .align(if (hint.isLeft) Alignment.CenterStart else Alignment.CenterEnd),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = hint.text,
                    color = Color.White,
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.35f), CircleShape)
                        .padding(20.dp)
                )
            }
        }

        // Scrub frame preview — stays until ExoPlayer confirms seek via onPositionDiscontinuity
        if (showScrubPreview && scrubImageBitmaps.isNotEmpty() && duration > 0) {
            val frameIdx = (scrubPosition.toFloat() / duration.toFloat() * scrubImageBitmaps.size)
                .toInt().coerceIn(0, scrubImageBitmaps.lastIndex)
            Image(
                bitmap = scrubImageBitmaps[frameIdx],
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Frame extraction progress — always visible, not tied to controls
        if (isExtractingFrames) {
            LinearProgressIndicator(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(2.dp),
                color = Color.White.copy(alpha = 0.7f),
                trackColor = Color.White.copy(alpha = 0.15f)
            )
        }

        // Media pills — shown when controls are active (above controls bar)
        val videoTags = localTags.filter { it.isNotBlank() }
        val videoPeople = localPeople.filter { it.isNotBlank() }
        AnimatedVisibility(
            visible = showControls && !showMetaPanel && (videoTags.isNotEmpty() || videoPeople.isNotEmpty()),
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))))
                    .padding(start = 12.dp, end = 12.dp, top = 20.dp, bottom = 68.dp)
            ) {
                FullscreenMediaPills(
                    tags = videoTags,
                    people = videoPeople,
                    onFilterByTag = onFilterByTag,
                    onFilterByPerson = onFilterByPerson
                )
            }
        }

        // Controls overlay
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Favorite toggle — top start below close button
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(top = 56.dp, start = 8.dp)
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.5f))
                        .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) {
                            isFavorite = !isFavorite
                            onToggleFavorite()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = if (isFavorite) "Remover favorito" else "Favoritar",
                        tint = if (isFavorite) Color(0xFFE91E63) else Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Mute toggle — below the 1/N counter at top end
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 56.dp, end = 8.dp)
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.5f))
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) {
                            isMuted = !isMuted
                            exoPlayer.volume = if (isMuted) 0f else 1f
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                        contentDescription = if (isMuted) "Ativar som" else "Desativar som",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Clip/thumb panel toggle (···) — below mute button
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 104.dp, end = 8.dp)
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(
                            if (clipMode) MaterialTheme.colorScheme.tertiary.copy(alpha = 0.85f)
                            else Color.Black.copy(alpha = 0.5f)
                        )
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) { clipMode = !clipMode },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Clipe / miniatura",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Meta panel toggle — below favorite button
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(top = 104.dp, start = 8.dp)
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(
                            if (showMetaPanel) MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
                            else Color.Black.copy(alpha = 0.5f)
                        )
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) { showMetaPanel = !showMetaPanel },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Pessoas e tags",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Expanded clip / thumbnail panel
                if (clipMode) {
                    val pos = if (isUserScrubbing) scrubPosition else currentPosition
                    val canSaveClip = clipStart != null && clipEnd != null && clipEnd!! > clipStart!!
                    val canSave = canSaveClip || pendingThumbnailMs != null
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 152.dp, end = 4.dp)
                            .background(Color.Black.copy(alpha = 0.75f), RoundedCornerShape(10.dp))
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        // Clip start — scissors mirrored (pointing left = mark in-point)
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    if (clipStart != null) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                                    else Color.Transparent
                                )
                                .clickable { clipStart = pos }
                                .padding(horizontal = 6.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCut,
                                contentDescription = "Início do clipe",
                                tint = if (clipStart != null) MaterialTheme.colorScheme.primary else Color.White,
                                modifier = Modifier
                                    .size(22.dp)
                                    .graphicsLayer { scaleX = -1f }
                            )
                            Text(
                                clipStart?.let { formatDuration(it) } ?: "--:--",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (clipStart != null) MaterialTheme.colorScheme.primary else Color.White
                            )
                        }

                        // Clip end — scissors normal (pointing right = mark out-point)
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    if (clipEnd != null) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                                    else Color.Transparent
                                )
                                .clickable { clipEnd = pos }
                                .padding(horizontal = 6.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCut,
                                contentDescription = "Fim do clipe",
                                tint = if (clipEnd != null) MaterialTheme.colorScheme.primary else Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                            Text(
                                clipEnd?.let { formatDuration(it) } ?: "--:--",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (clipEnd != null) MaterialTheme.colorScheme.primary else Color.White
                            )
                        }

                        // Thumbnail — camera icon
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    if (pendingThumbnailMs != null) MaterialTheme.colorScheme.tertiary.copy(alpha = 0.3f)
                                    else Color.Transparent
                                )
                                .clickable { pendingThumbnailMs = pos }
                                .padding(horizontal = 6.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhotoCamera,
                                contentDescription = "Miniatura",
                                tint = if (pendingThumbnailMs != null) MaterialTheme.colorScheme.tertiary else Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                            Text(
                                pendingThumbnailMs?.let { formatDuration(it) } ?: "--:--",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (pendingThumbnailMs != null) MaterialTheme.colorScheme.tertiary else Color.White
                            )
                        }

                        // Save button — appears when clip or thumbnail is ready
                        if (canSave) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(MaterialTheme.colorScheme.primary)
                                    .clickable {
                                        if (canSaveClip) onSaveClip(clipStart!!, clipEnd!!, "")
                                        pendingThumbnailMs?.let { onSetThumbnail(it) }
                                        clipMode = false
                                    }
                                    .padding(horizontal = 6.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Salvar",
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                }

                // Center: -10s | play/pause | +10s
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalArrangement = Arrangement.spacedBy(28.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            val dur = exoPlayer.duration
                            val newPos = if (dur > 0) (exoPlayer.currentPosition - 10_000L).coerceIn(0L, dur)
                                         else (exoPlayer.currentPosition - 10_000L).coerceAtLeast(0L)
                            exoPlayer.seekTo(newPos)
                            showControls = true
                        },
                        modifier = Modifier.size(56.dp)
                    ) {
                        Icon(Icons.Default.Replay10, contentDescription = "-10s", tint = Color.White, modifier = Modifier.size(44.dp))
                    }

                    IconButton(
                        onClick = {
                            if (exoPlayer.isPlaying) exoPlayer.pause()
                            else { if (exoPlayer.playbackState == Player.STATE_ENDED) exoPlayer.seekTo(0); exoPlayer.play() }
                            showControls = true
                        },
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.5f))
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = Color.White,
                            modifier = Modifier.size(48.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            val dur = exoPlayer.duration
                            val newPos = if (dur > 0) (exoPlayer.currentPosition + 10_000L).coerceIn(0L, dur)
                                         else (exoPlayer.currentPosition + 10_000L).coerceAtLeast(0L)
                            exoPlayer.seekTo(newPos)
                            showControls = true
                        },
                        modifier = Modifier.size(56.dp)
                    ) {
                        Icon(Icons.Default.Forward10, contentDescription = "+10s", tint = Color.White, modifier = Modifier.size(44.dp))
                    }
                }

                // Delete button — top-start column, below meta/edit button (top=104dp+48dp=152dp)
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(top = 152.dp, start = 8.dp)
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.5f))
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) { showDeleteConfirm = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Deletar",
                        tint = Color(0xFFEF9A9A),
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Bottom: progress bar
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f))
                            )
                        )
                        .padding(bottom = 12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = formatDuration(if (isUserScrubbing) scrubPosition else currentPosition),
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall
                        )
                        Slider(
                            value = if (duration > 0)
                                (if (isUserScrubbing) scrubPosition else currentPosition).toFloat() / duration.toFloat()
                            else 0f,
                            onValueChange = { v ->
                                isUserScrubbing = true
                                showScrubPreview = true
                                scrubPosition = (v * duration).toLong()
                                showControls = true
                            },
                            onValueChangeFinished = {
                                exoPlayer.seekTo(scrubPosition)
                                isUserScrubbing = false
                                // showScrubPreview stays true — cleared by onPositionDiscontinuity
                            },
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 8.dp),
                            colors = SliderDefaults.colors(
                                thumbColor = Color.White,
                                activeTrackColor = Color.White,
                                inactiveTrackColor = Color.White.copy(alpha = 0.35f)
                            )
                        )
                        Text(
                            text = formatDuration(duration),
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        }

        // Meta panel
        if (showMetaPanel) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) {}
                    .background(Color(0xF2111111), RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .padding(horizontal = 20.dp)
                    .padding(top = 12.dp, bottom = 24.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    // Drag handle
                    Box(
                        modifier = Modifier
                            .width(36.dp)
                            .height(4.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f))
                            .align(Alignment.CenterHorizontally)
                    )

                    // Pessoas section
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { showPeopleEditor = true }
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "PESSOAS",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.45f)
                        )
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Editar pessoas",
                            tint = Color.White.copy(alpha = 0.45f),
                            modifier = Modifier.size(14.dp)
                        )
                    }

                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.07f)))

                    // Tags section
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { showTagEditor = true }
                                .padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "TAGS",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.45f)
                            )
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "Editar tags",
                                tint = Color.White.copy(alpha = 0.45f),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        val tags = localTags.filter { it.isNotBlank() }
                        if (tags.isEmpty()) {
                            Text(
                                "Nenhuma tag — toque para adicionar",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.25f),
                                modifier = Modifier.clickable { showTagEditor = true }
                            )
                        } else {
                            FullscreenTagChips(tags = tags, onFilterByTag = onFilterByTag)
                        }
                    }
                }
            }
        }
    }

    if (showMetaPanel) {
        BackHandler { showMetaPanel = false }
    }

    if (showTagEditor) {
        TagEditorDialog(
            currentTags = localTags,
            allAvailableTags = allAvailableTags,
            onConfirm = { tags -> localTags = tags; onUpdateTags(tags); showTagEditor = false },
            onDismiss = { showTagEditor = false }
        )
    }

    if (showPeopleEditor) {
        PeopleEditorDialog(
            currentPeople = localPeople,
            allAvailablePeople = allAvailablePeople,
            onConfirm = { people -> localPeople = people; onUpdatePeople(people); showPeopleEditor = false },
            onDismiss = { showPeopleEditor = false }
        )
    }
    if (showDeleteConfirm) {
        DeleteConfirmDialog(
            fileName = mediaItem.fileName,
            onConfirm = onDeleteMedia,
            onDismiss = { showDeleteConfirm = false }
        )
    }
}

// Process-lifetime frame cache — survives controls hide/show cycles
private val reelFrameCache = HashMap<String, List<Bitmap>>()

@Composable
fun VideoScrubberReel(
    uri: String,
    duration: Long,
    currentPosition: Long,
    onSeekStart: (Long) -> Unit,
    onSeek: (Long) -> Unit,
    onSeekEnd: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var scaledBitmaps by remember(uri) { mutableStateOf(reelFrameCache[uri] ?: emptyList()) }
    var reelWidthPx by remember { mutableStateOf(0) }
    var lastDragPos by remember { mutableStateOf(0L) }

    LaunchedEffect(uri) {
        if (reelFrameCache.containsKey(uri)) return@LaunchedEffect
        withContext(Dispatchers.IO) {
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(context, Uri.parse(uri))
                val dur = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                    ?.toLongOrNull() ?: 0L
                val vw = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
                val vh = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
                val targetPx = when {
                    maxOf(vw, vh) > 2500 -> 320  // 4K
                    maxOf(vw, vh) > 1500 -> 240  // 2K
                    else -> 200
                }
                if (dur > 0) {
                    val count = ((dur / 1000L) / 15L).toInt().coerceAtLeast(10).coerceAtMost(60)
                    val frames = mutableListOf<Bitmap>()
                    for (i in 0 until count) {
                        val timeUs = dur * 1000L * i / count
                        val scaled: Bitmap = if (android.os.Build.VERSION.SDK_INT >= 27) {
                            retriever.getScaledFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC, targetPx, targetPx)
                        } else {
                            val raw = retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                                ?: continue
                            val scale = targetPx.toFloat() / maxOf(raw.width, raw.height)
                            Bitmap.createScaledBitmap(raw, (raw.width * scale).toInt().coerceAtLeast(1), (raw.height * scale).toInt().coerceAtLeast(1), true)
                                .also { raw.recycle() }
                        } ?: continue
                        frames.add(scaled)
                        val snapshot = frames.toList()
                        withContext(Dispatchers.Main) { scaledBitmaps = snapshot }
                    }
                    reelFrameCache[uri] = frames
                }
            } catch (_: Throwable) {
            } finally {
                runCatching { retriever.release() }
            }
        }
    }

    // Convert once, cache — avoids repeated asImageBitmap() calls on every 500ms recompose
    val imageBitmaps = remember(scaledBitmaps) { scaledBitmaps.map { it.asImageBitmap() } }

    if (imageBitmaps.isEmpty()) return

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(100.dp)
            .onSizeChanged { reelWidthPx = it.width }
            .clip(RoundedCornerShape(4.dp))
            .pointerInput(duration) {
                detectHorizontalDragGestures(
                    onDragStart = { offset ->
                        if (reelWidthPx > 0 && duration > 0) {
                            val pos = (offset.x / reelWidthPx * duration).toLong().coerceIn(0L, duration)
                            lastDragPos = pos
                            onSeekStart(pos)
                        }
                    },
                    onHorizontalDrag = { change, _ ->
                        change.consume()
                        if (reelWidthPx > 0 && duration > 0) {
                            val pos = (change.position.x / reelWidthPx * duration).toLong().coerceIn(0L, duration)
                            lastDragPos = pos
                            onSeek(pos)
                        }
                    },
                    onDragEnd = { onSeekEnd(lastDragPos) },
                    onDragCancel = { onSeekEnd(lastDragPos) }
                )
            }
    ) {
        // Frame strip in separate composable — Compose skips it when only currentPosition changes
        ReelFrameStrip(imageBitmaps = imageBitmaps)

        // Canvas indicator — cheap draw, no extra composable node per recompose
        if (duration > 0) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val x = (currentPosition.toFloat() / duration.toFloat()) * size.width
                drawRect(
                    color = Color.White,
                    topLeft = Offset(x - 1.5f, 0f),
                    size = Size(3f, size.height)
                )
            }
        }
    }
}

@Composable
private fun ReelFrameStrip(imageBitmaps: List<ImageBitmap>) {
    Row(modifier = Modifier.fillMaxSize()) {
        imageBitmaps.forEach { bmp ->
            Image(
                bitmap = bmp,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )
        }
    }
}

@Composable
private fun DeleteConfirmDialog(fileName: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Deletar arquivo?") },
        text = { Text("\"$fileName\" será removido permanentemente do dispositivo.") },
        confirmButton = {
            TextButton(onClick = { onDismiss(); onConfirm() }) {
                Text("Deletar", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

private fun formatDuration(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val m = totalSeconds / 60
    val s = totalSeconds % 60
    return "%d:%02d".format(m, s)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FullscreenTagChips(
    tags: List<String>,
    modifier: Modifier = Modifier,
    onFilterByTag: (String) -> Unit
) {
    if (tags.isEmpty()) return
    var expanded by remember { mutableStateOf(false) }
    val maxVisible = 6
    val visible = if (expanded || tags.size <= maxVisible) tags else tags.take(maxVisible)
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        visible.forEach { tag ->
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f))
                    .clickable { onFilterByTag(tag) }
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(tag, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
        }
        if (!expanded && tags.size > maxVisible) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f))
                    .clickable { expanded = true }
                    .padding(horizontal = 10.dp, vertical = 3.dp)
            ) { Text("▾ ${tags.size - maxVisible}", style = MaterialTheme.typography.labelSmall) }
        }
        if (expanded && tags.size > maxVisible) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f))
                    .clickable { expanded = false }
                    .padding(horizontal = 10.dp, vertical = 3.dp)
            ) { Text("▴", style = MaterialTheme.typography.labelSmall) }
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun FullscreenMediaPills(
    tags: List<String>,
    people: List<String>,
    modifier: Modifier = Modifier,
    onFilterByTag: (String) -> Unit,
    onFilterByPerson: (String) -> Unit
) {
    if (tags.isEmpty() && people.isEmpty()) return
    var expanded by remember { mutableStateOf(false) }
    val maxVisible = 8
    val allPeople = people.map { Pair(it, true) }
    val allTags = tags.map { Pair(it, false) }
    val all = allPeople + allTags
    val visible = if (expanded || all.size <= maxVisible) all else all.take(maxVisible)
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        visible.forEach { (name, isPerson) ->
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(if (isPerson) 20.dp else 10.dp))
                    .background(
                        if (isPerson) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.85f)
                        else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f)
                    )
                    .clickable { if (isPerson) onFilterByPerson(name) else onFilterByTag(name) }
                    .padding(horizontal = if (isPerson) 12.dp else 8.dp, vertical = 3.dp)
            ) {
                Text(
                    name,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isPerson) MaterialTheme.colorScheme.onSecondaryContainer
                            else MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
        if (!expanded && all.size > maxVisible) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f))
                    .clickable { expanded = true }
                    .padding(horizontal = 10.dp, vertical = 3.dp)
            ) { Text("▾ ${all.size - maxVisible}", style = MaterialTheme.typography.labelSmall) }
        }
        if (expanded && all.size > maxVisible) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f))
                    .clickable { expanded = false }
                    .padding(horizontal = 10.dp, vertical = 3.dp)
            ) { Text("▴", style = MaterialTheme.typography.labelSmall) }
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun FullscreenImagePage(
    mediaItem: MediaItem,
    onZoomChanged: (Boolean) -> Unit = {},
    onFilterByTag: (String) -> Unit = {},
    onFilterByPerson: (String) -> Unit = {},
    onDeleteMedia: () -> Unit = {}
) {
    var zoomScale by remember { mutableStateOf(1f) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var zoomOffset by remember { mutableStateOf(Offset.Zero) }
    var containerWidth by remember { mutableStateOf(0) }
    var containerHeight by remember { mutableStateOf(0) }
    val tags = mediaItem.tags.filter { it.isNotBlank() }
    val people = mediaItem.people.filter { it.isNotBlank() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .onSizeChanged { containerWidth = it.width; containerHeight = it.height }
            .pointerInput("zoom") {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    var isMultiTouch = false
                    while (true) {
                        val event = awaitPointerEvent()
                        if (event.changes.none { it.pressed }) break
                        if (event.changes.count { it.pressed } > 1) isMultiTouch = true
                        if (isMultiTouch || zoomScale > 1f) {
                            event.changes.forEach { it.consume() }
                            val zoomChange = event.calculateZoom()
                            val panChange = event.calculatePan()
                            val newScale = (zoomScale * zoomChange).coerceIn(1f, 8f)
                            val maxX = (newScale - 1f) * containerWidth / 2f
                            val maxY = (newScale - 1f) * containerHeight / 2f
                            zoomOffset = if (newScale > 1f)
                                Offset(
                                    (zoomOffset.x + panChange.x).coerceIn(-maxX, maxX),
                                    (zoomOffset.y + panChange.y).coerceIn(-maxY, maxY)
                                )
                            else Offset.Zero
                            zoomScale = newScale
                            onZoomChanged(newScale > 1f)
                        }
                    }
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = { zoomScale = 1f; zoomOffset = Offset.Zero; onZoomChanged(false) }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = Uri.parse(mediaItem.uri),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = zoomScale; scaleY = zoomScale
                    translationX = zoomOffset.x; translationY = zoomOffset.y
                }
        )

        if (tags.isNotEmpty() || people.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))))
                    .padding(start = 12.dp, end = 12.dp, top = 20.dp, bottom = 28.dp)
            ) {
                FullscreenMediaPills(
                    tags = tags,
                    people = people,
                    onFilterByTag = onFilterByTag,
                    onFilterByPerson = onFilterByPerson
                )
            }
        }

        // Delete button — top-end, below the overlay counter (~12dp+text height ≈ 40dp)
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 56.dp, end = 8.dp)
                .size(40.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.55f))
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { showDeleteConfirm = true },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Deletar",
                tint = Color(0xFFEF9A9A),
                modifier = Modifier.size(20.dp)
            )
        }
    }

    if (showDeleteConfirm) {
        DeleteConfirmDialog(
            fileName = mediaItem.fileName,
            onConfirm = onDeleteMedia,
            onDismiss = { showDeleteConfirm = false }
        )
    }
}

@Composable
fun ImageFullscreenDialog(uri: String, onDismiss: () -> Unit) {
    var zoomScale by remember { mutableStateOf(1f) }
    var zoomOffset by remember { mutableStateOf(Offset.Zero) }
    var containerWidth by remember { mutableStateOf(0) }
    var containerHeight by remember { mutableStateOf(0) }
    val transformState = rememberTransformableState { zoomChange, panChange, _ ->
        val newScale = (zoomScale * zoomChange).coerceIn(1f, 8f)
        val maxX = (newScale - 1f) * containerWidth / 2f
        val maxY = (newScale - 1f) * containerHeight / 2f
        zoomOffset = if (newScale > 1f)
            Offset((zoomOffset.x + panChange.x).coerceIn(-maxX, maxX), (zoomOffset.y + panChange.y).coerceIn(-maxY, maxY))
        else Offset.Zero
        zoomScale = newScale
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnBackPress = true, dismissOnClickOutside = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .onSizeChanged { containerWidth = it.width; containerHeight = it.height }
                .transformable(state = transformState)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { if (zoomScale == 1f) onDismiss() },
                        onDoubleTap = { zoomScale = 1f; zoomOffset = Offset.Zero }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = Uri.parse(uri),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = zoomScale; scaleY = zoomScale
                        translationX = zoomOffset.x; translationY = zoomOffset.y
                    }
            )
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.6f))
            ) {
                Icon(Icons.Default.Close, contentDescription = "Fechar", tint = Color.White)
            }
        }
    }
}

@Composable
fun CompactSearchBar(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 3.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            Icons.Default.Search,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(15.dp)
        )
        Box(modifier = Modifier.weight(1f)) {
            if (value.isEmpty()) {
                Text(
                    text = placeholder,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = TextStyle(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = MaterialTheme.typography.bodySmall.fontSize
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }
        if (value.isNotEmpty()) {
            Icon(
                Icons.Default.Close,
                contentDescription = "Limpar",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .size(14.dp)
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { onValueChange("") }
            )
        }
    }
}

@Composable
fun MetaChip(text: String, color: androidx.compose.ui.graphics.Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(color)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(text = text, style = MaterialTheme.typography.labelSmall)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FilterBar(
    selectedTags: List<String>,
    availableTags: List<Pair<String, Int>>,
    onTagToggled: (String) -> Unit,
    onClearFilters: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (availableTags.isEmpty()) return

    // Estimate how many chips fit in 2 rows (~360dp wide, chips avg ~80dp each + 6dp gap → ~8 per 2 rows)
    // Use character-based estimate: max ~60 chars of tag text across 2 rows
    var charBudget = 60
    val collapsedTags = mutableListOf<Pair<String, Int>>()
    for (pair in availableTags) {
        if (charBudget <= 0) break
        collapsedTags.add(pair)
        charBudget -= pair.first.length + 2
    }
    val canExpand = collapsedTags.size < availableTags.size

    var expanded by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
        // Always-visible collapsed row
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            if (selectedTags.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.error)
                        .clickable { onClearFilters() }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text("✕", style = MaterialTheme.typography.labelSmall, color = Color.White)
                }
            }
            (if (expanded) availableTags else collapsedTags).forEach { (tag, count) ->
                TagFilterChip(
                    tag = tag,
                    count = count,
                    selected = selectedTags.contains(tag),
                    onClick = { onTagToggled(tag) }
                )
            }
            if (!expanded && canExpand) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable { expanded = true }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        "▾ ${availableTags.size - collapsedTags.size}",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
            if (expanded && canExpand) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable { expanded = false }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text("▴", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
fun EmptyStateView(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(horizontal = 32.dp)
        ) {
            Text("Nenhuma mídia encontrada", style = MaterialTheme.typography.headlineSmall, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            Text("Selecione uma pasta com fotos e vídeos", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
    }
}

@Composable
fun LoadingView(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            CircularProgressIndicator()
            Text("Carregando mídia...")
        }
    }
}
