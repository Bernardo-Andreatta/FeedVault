package com.example.securegallery.ui.components

import android.app.Activity
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.MediaItem as Media3Item
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.securegallery.vault.VaultSession
import com.example.securegallery.ui.theme.FavoriteRose
import com.example.securegallery.data.MediaItem
import com.example.securegallery.data.VideoClip
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val clipFrameCache = HashMap<String, List<Bitmap>>()

@Composable
fun ClipsFeed(
    clips: List<VideoClip>,
    mediaById: Map<Long, MediaItem>,
    allAvailableTags: List<String> = emptyList(),
    currentlyPlayingUri: String? = null,
    onPlayRequested: (String) -> Unit = {},
    onClipPlayStarted: (Int) -> Unit = {},
    onDeleteClip: (VideoClip) -> Unit,
    onFullscreenClip: (VideoClip, Long) -> Unit = { _, _ -> },
    onUpdateClipTags: (VideoClip, List<String>) -> Unit = { _, _ -> },
    onToggleFavoriteClip: (VideoClip) -> Unit = {},
    onFilterByTag: (String) -> Unit = {},
    onFilterByPerson: (String) -> Unit = {},
    onGoToOriginal: (Long) -> Unit = {},
    seekTokens: Map<String, Pair<Int, Long>> = emptyMap(),
    onSeekTokenConsumed: (String) -> Unit = {},
    listState: LazyListState = rememberLazyListState(),
    modifier: Modifier = Modifier
) {
    if (clips.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Nenhum clipe criado", style = MaterialTheme.typography.headlineSmall)
                Text(
                    "Abra um vídeo em tela cheia e use ✂ para criar clipes",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        return
    }

    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(vertical = 2.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
        modifier = modifier
    ) {
        items(clips, key = { it.id }) { clip ->
            val mediaItem = mediaById[clip.mediaItemId]
            val clipKey = "clip_${clip.id}"
            val seekPair = seekTokens[clipKey]
            val clipIdx = clips.indexOfFirst { it.id == clip.id }
            ClipCard(
                clip = clip,
                aspectRatio = mediaItem?.aspectRatio ?: (16f / 9f),
                people = mediaItem?.people ?: emptyList(),
                allAvailableTags = allAvailableTags,
                currentlyPlayingUri = currentlyPlayingUri,
                onPlayRequested = onPlayRequested,
                onPlayStarted = { if (clipIdx >= 0) onClipPlayStarted(clipIdx) },
                onDelete = { onDeleteClip(clip) },
                onFullscreen = { pos -> onFullscreenClip(clip, pos) },
                onUpdateTags = { tags -> onUpdateClipTags(clip, tags) },
                onToggleFavorite = { onToggleFavoriteClip(clip) },
                onFilterByTag = onFilterByTag,
                onFilterByPerson = onFilterByPerson,
                onGoToOriginal = { onGoToOriginal(clip.mediaItemId) },
                seekToken = seekPair?.first ?: 0,
                seekPositionMs = seekPair?.second ?: 0L,
                onSeekTokenConsumed = { onSeekTokenConsumed(clipKey) }
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ClipCard(
    clip: VideoClip,
    aspectRatio: Float,
    people: List<String> = emptyList(),
    allAvailableTags: List<String> = emptyList(),
    currentlyPlayingUri: String? = null,
    onPlayRequested: (String) -> Unit = {},
    onPlayStarted: () -> Unit = {},
    onDelete: () -> Unit,
    onFullscreen: (Long) -> Unit = {},
    onUpdateTags: (List<String>) -> Unit = {},
    onToggleFavorite: () -> Unit = {},
    onFilterByTag: (String) -> Unit = {},
    onFilterByPerson: (String) -> Unit = {},
    onGoToOriginal: () -> Unit = {},
    seekToken: Int = 0,
    seekPositionMs: Long = 0L,
    onSeekTokenConsumed: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val clipDuration = clip.endMs - clip.startMs
    var showTagEditor by remember(clip.id) { mutableStateOf(false) }
    var showDeleteConfirm by remember(clip.id) { mutableStateOf(false) }
    var tagEditMode by remember(clip.id) { mutableStateOf(false) }
    val tags = clip.tags.filter { it.isNotBlank() }
    var clipSeekRequest by remember(clip.id) { mutableStateOf(0 to 0L) }

    LaunchedEffect(seekToken) {
        if (seekToken > 0) {
            clipSeekRequest = (clipSeekRequest.first + 1) to seekPositionMs
            onSeekTokenConsumed()
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(0.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column {
            VideoPlayer(
                uri = clip.uri,
                aspectRatio = if (aspectRatio > 0f) aspectRatio else 16f / 9f,
                clipStartMs = clip.startMs,
                clipEndMs = clip.endMs,
                thumbnailFrameMs = clip.startMs,
                videoResizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT,
                currentlyPlayingUri = currentlyPlayingUri,
                onPlayRequested = { uri -> onPlayRequested(uri); onPlayStarted() },
                onFullscreenRequested = { pos, _, _ -> onFullscreen(pos) },
                externalPlayRequestKey = clipSeekRequest.first,
                externalPlayRequestMs = clipSeekRequest.second
            )

            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (clip.label.isNotBlank()) {
                    Text(clip.label, style = MaterialTheme.typography.bodyMedium)
                }
                Text(
                    "${formatMs(clip.startMs)} → ${formatMs(clip.endMs)}  (${formatMs(clipDuration)})",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    people.filter { it.isNotBlank() }.forEach { person ->
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
                                .clickable { showTagEditor = true }
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text("Adicionar tag", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onTertiaryContainer)
                        }
                    } else {
                        tags.forEach { tag ->
                            if (tagEditMode) {
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
                                            .clickable { onUpdateTags(clip.tags - tag) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = "Remover $tag", tint = Color.White, modifier = Modifier.size(10.dp))
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
                                    Text(tag, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                }
                            }
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .clickable { showTagEditor = true }
                                .padding(horizontal = 10.dp, vertical = 3.dp)
                        ) {
                            Text("+", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onGoToOriginal, modifier = Modifier.size(32.dp)) {
                        Icon(
                            Icons.Default.Photo,
                            contentDescription = "Ver original na galeria",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    if (tags.isNotEmpty()) {
                        IconButton(onClick = { tagEditMode = !tagEditMode }, modifier = Modifier.size(32.dp)) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "Editar tags",
                                tint = if (tagEditMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    IconButton(onClick = onToggleFavorite, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = if (clip.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favorito",
                            tint = if (clip.isFavorite) FavoriteRose else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(onClick = { showDeleteConfirm = true }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Excluir clipe", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }

    if (showDeleteConfirm) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Excluir clipe?") },
            text = { Text("Esta ação não pode ser desfeita.") },
            confirmButton = {
                androidx.compose.material3.TextButton(
                    onClick = { showDeleteConfirm = false; onDelete() },
                    colors = androidx.compose.material3.ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Excluir") }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancelar") }
            }
        )
    }

    if (showTagEditor) {
        TagEditorDialog(
            currentTags = clip.tags,
            allAvailableTags = allAvailableTags,
            onConfirm = { newTags -> onUpdateTags(newTags) },
            onDismiss = { showTagEditor = false }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ClipFullscreenOverlay(
    clips: List<VideoClip>,
    startIndex: Int,
    startPosition: Long = 0L,
    mediaById: Map<Long, MediaItem>,
    onDismiss: () -> Unit,
    onToggleClipFavorite: (Long) -> Unit = {},
    onFinalPosition: (Long, Long) -> Unit = { _, _ -> },
    onReturnedToEmbed: (Long) -> Unit = {}
) {
    val context = LocalContext.current
    val activity = context as? Activity
    var isZoomed by remember { mutableStateOf(false) }
    val safeStart = startIndex.coerceIn(0, (clips.size - 1).coerceAtLeast(0))
    val pagerState = rememberPagerState(initialPage = safeStart) { clips.size }
    var currentPagePositionMs by remember { mutableStateOf(startPosition) }

    val handleDismiss = {
        val currentClip = clips.getOrNull(pagerState.currentPage)
        if (currentClip != null) {
            onFinalPosition(currentClip.id, currentPagePositionMs)
            if (pagerState.currentPage != safeStart) onReturnedToEmbed(currentClip.id)
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
            activity?.window?.let { win ->
                WindowCompat.setDecorFitsSystemWindows(win, true)
                WindowCompat.getInsetsController(win, win.decorView).show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        VerticalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            userScrollEnabled = !isZoomed
        ) { pageIndex ->
            val clip = clips[pageIndex]
            val aspectRatio = mediaById[clip.mediaItemId]?.aspectRatio ?: (16f / 9f)
            ClipFullscreenPage(
                clip = clip,
                aspectRatio = aspectRatio,
                isActive = pageIndex == pagerState.currentPage,
                startPosition = if (pageIndex == safeStart) startPosition else 0L,
                onZoomChanged = { isZoomed = it },
                onToggleFavorite = { onToggleClipFavorite(clip.id) },
                onPositionChanged = { pos -> if (pageIndex == pagerState.currentPage) currentPagePositionMs = pos }
            )
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

        if (clips.size > 1) {
            Text(
                text = "${pagerState.currentPage + 1}/${clips.size}",
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

@Composable
private fun ClipFullscreenPage(
    clip: VideoClip,
    aspectRatio: Float,
    isActive: Boolean,
    startPosition: Long = 0L,
    onZoomChanged: (Boolean) -> Unit = {},
    onToggleFavorite: () -> Unit = {},
    onPositionChanged: (Long) -> Unit = {}
) {
    val context = LocalContext.current
    var isPlaying by remember { mutableStateOf(false) }
    var showControls by remember { mutableStateOf(false) }
    var isMuted by remember { mutableStateOf(true) }
    var currentPosition by remember { mutableStateOf(0L) }
    var isUserScrubbing by remember { mutableStateOf(false) }
    var scrubPosition by remember { mutableStateOf(0L) }
    var showScrubPreview by remember { mutableStateOf(false) }
    val clipDuration = clip.endMs - clip.startMs
    val cacheKey = "${clip.id}"
    var scrubFrames by remember(clip.id) { mutableStateOf(clipFrameCache[cacheKey] ?: emptyList()) }
    val scrubImageBitmaps = remember(scrubFrames) { scrubFrames.map { it.asImageBitmap() } }
    var isExtractingFrames by remember(clip.id) { mutableStateOf(!clipFrameCache.containsKey(cacheKey)) }
    var zoomScale by remember { mutableStateOf(1f) }
    var zoomOffset by remember { mutableStateOf(Offset.Zero) }
    var containerWidth by remember { mutableStateOf(0) }
    var containerHeight by remember { mutableStateOf(0) }
    var seekHint by remember { mutableStateOf<Triple<String, Boolean, Long>?>(null) }

    val exoPlayer = remember(clip.id) {
        val clipConfig = Media3Item.ClippingConfiguration.Builder()
            .setStartPositionMs(clip.startMs)
            .setEndPositionMs(clip.endMs)
            .build()
        val mediaItem = Media3Item.Builder()
            .setUri(VaultSession.resolve(clip.uri))
            .setClippingConfiguration(clipConfig)
            .build()
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(mediaItem)
            repeatMode = Player.REPEAT_MODE_ONE
            volume = 0f
            prepare()
            if (startPosition > 0L) seekTo(startPosition)
            playWhenReady = true
        }
    }

    LaunchedEffect(isActive) {
        if (!isActive) exoPlayer.pause() else exoPlayer.play()
    }

    DisposableEffect(clip.id) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
                if (!playing && exoPlayer.playbackState != Player.STATE_BUFFERING) showControls = true
            }
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_ENDED) { isPlaying = false; showControls = true }
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
        onDispose { exoPlayer.removeListener(listener); exoPlayer.release() }
    }

    LaunchedEffect(isPlaying, showControls) {
        if (isPlaying && showControls) { delay(3000); showControls = false }
    }

    LaunchedEffect(exoPlayer) {
        while (true) {
            if (!isUserScrubbing) {
                currentPosition = exoPlayer.currentPosition
                onPositionChanged(currentPosition)
            }
            delay(500)
        }
    }

    LaunchedEffect(clip.id) {
        if (clipFrameCache.containsKey(cacheKey)) {
            scrubFrames = clipFrameCache[cacheKey]!!
            isExtractingFrames = false
            return@LaunchedEffect
        }
        if (clipDuration <= 0) { isExtractingFrames = false; return@LaunchedEffect }
        val count = ((clipDuration / 1000L) / 15L).toInt().coerceAtLeast(8).coerceAtMost(40)
        val frames = arrayOfNulls<Bitmap>(count)
        val parallelism = minOf(4, count)
        withContext(Dispatchers.IO) {
            coroutineScope {
                repeat(parallelism) { group ->
                    launch {
                        val retriever = MediaMetadataRetriever()
                        try {
                            retriever.setDataSource(context, VaultSession.resolve(clip.uri))
                            var i = group
                            while (i < count) {
                                val timeUs = (clip.startMs + clipDuration * i / count) * 1000L
                                retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)?.let { raw ->
                                    val scale = 240f / maxOf(raw.width, raw.height)
                                    frames[i] = Bitmap.createScaledBitmap(raw, (raw.width * scale).toInt().coerceAtLeast(1), (raw.height * scale).toInt().coerceAtLeast(1), false)
                                    raw.recycle()
                                    withContext(Dispatchers.Main) { scrubFrames = frames.filterNotNull() }
                                }
                                i += parallelism
                            }
                        } catch (_: Exception) {
                        } finally {
                            runCatching { retriever.release() }
                        }
                    }
                }
            }
        }
        val finalFrames = frames.filterNotNull()
        clipFrameCache[cacheKey] = finalFrames
        scrubFrames = finalFrames
        isExtractingFrames = false
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
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
                    onTap = { showControls = !showControls },
                    onDoubleTap = { offset ->
                        if (zoomScale > 1f) {
                            zoomScale = 1f; zoomOffset = Offset.Zero; onZoomChanged(false)
                        } else {
                            val isLeft = offset.x < size.width / 2f
                            val delta = if (isLeft) -10_000L else 10_000L
                            val dur = exoPlayer.duration.coerceAtLeast(0L)
                            val newPos = (exoPlayer.currentPosition + delta).coerceIn(0L, dur)
                            exoPlayer.seekTo(newPos)
                            currentPosition = newPos
                            seekHint = Triple(if (isLeft) "-10s" else "+10s", isLeft, System.currentTimeMillis())
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

        seekHint?.let { (text, isLeft, id) ->
            LaunchedEffect(id) {
                delay(600)
                seekHint = null
            }
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(0.5f)
                    .align(if (isLeft) Alignment.CenterStart else Alignment.CenterEnd),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = text,
                    color = Color.White,
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.35f), CircleShape)
                        .padding(20.dp)
                )
            }
        }

        if (showScrubPreview && scrubImageBitmaps.isNotEmpty() && clipDuration > 0) {
            val frameIdx = (scrubPosition.toFloat() / clipDuration.toFloat() * scrubImageBitmaps.size)
                .toInt().coerceIn(0, scrubImageBitmaps.lastIndex)
            Image(
                bitmap = scrubImageBitmaps[frameIdx],
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
        }

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

        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 12.dp, end = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.5f))
                            .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) {
                                onToggleFavorite()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (clip.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favorito",
                            tint = if (clip.isFavorite) FavoriteRose else Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.5f))
                            .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) {
                                isMuted = !isMuted; exoPlayer.volume = if (isMuted) 0f else 1f
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                            contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Row(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalArrangement = Arrangement.spacedBy(28.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            val dur = exoPlayer.duration.coerceAtLeast(0L)
                            val newPos = (exoPlayer.currentPosition - 10_000L).coerceIn(0L, dur)
                            exoPlayer.seekTo(newPos); currentPosition = newPos; showControls = true
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
                        modifier = Modifier.size(72.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.5f))
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = null, tint = Color.White, modifier = Modifier.size(48.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            val dur = exoPlayer.duration.coerceAtLeast(0L)
                            val newPos = (exoPlayer.currentPosition + 10_000L).coerceIn(0L, dur)
                            exoPlayer.seekTo(newPos); currentPosition = newPos; showControls = true
                        },
                        modifier = Modifier.size(56.dp)
                    ) {
                        Icon(Icons.Default.Forward10, contentDescription = "+10s", tint = Color.White, modifier = Modifier.size(44.dp))
                    }
                }

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f))))
                        .padding(bottom = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = formatMs(if (isUserScrubbing) scrubPosition else currentPosition),
                            color = Color.White, style = MaterialTheme.typography.labelSmall
                        )
                        Slider(
                            value = if (clipDuration > 0)
                                (if (isUserScrubbing) scrubPosition else currentPosition).toFloat() / clipDuration.toFloat()
                            else 0f,
                            onValueChange = { v ->
                                isUserScrubbing = true
                                showScrubPreview = true
                                scrubPosition = (v * clipDuration).toLong()
                                showControls = true
                            },
                            onValueChangeFinished = {
                                exoPlayer.seekTo(scrubPosition)
                                isUserScrubbing = false
                            },
                            modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                            colors = SliderDefaults.colors(
                                thumbColor = Color.White,
                                activeTrackColor = Color.White,
                                inactiveTrackColor = Color.White.copy(alpha = 0.35f)
                            )
                        )
                        Text(
                            text = formatMs(clipDuration),
                            color = Color.White, style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        }
    }
}

private fun formatMs(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val m = totalSeconds / 60
    val s = totalSeconds % 60
    return "%d:%02d".format(m, s)
}
