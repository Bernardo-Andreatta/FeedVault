package com.bernardo.feedvault

import android.app.Activity
import android.net.Uri
import android.os.Build
import android.view.WindowManager
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.ViewStream
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.OutlinedTextField
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.zIndex
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bernardo.feedvault.data.MediaItem
import com.bernardo.feedvault.data.VideoClip
import com.bernardo.feedvault.util.normalizeForSearch
import com.bernardo.feedvault.ui.AppSection
import com.bernardo.feedvault.ui.ClipSortOrder
import com.bernardo.feedvault.ui.GalleryViewModel
import com.bernardo.feedvault.ui.SettingsScreen
import com.bernardo.feedvault.ui.MediaSortOrder

import com.bernardo.feedvault.data.DesktopRepository
import com.bernardo.feedvault.data.DownloadItem
import com.bernardo.feedvault.data.DownloadStatus
import com.bernardo.feedvault.ui.DownloadQueueViewModel
import com.bernardo.feedvault.ui.components.ClipFullscreenOverlay
import com.bernardo.feedvault.ui.components.DismissableDownloadFab
import com.bernardo.feedvault.ui.components.DownloadQueueSheet
import com.bernardo.feedvault.ui.components.ClipsFeed
import com.bernardo.feedvault.ui.DesktopViewModel
import com.bernardo.feedvault.ui.components.DesktopScreen
import com.bernardo.feedvault.vault.VaultGate
import com.bernardo.feedvault.vault.enableVaultBiometric
import com.bernardo.feedvault.ui.components.CompactSearchBar
import com.bernardo.feedvault.ui.components.EmptyStateView
import com.bernardo.feedvault.ui.components.FilterBar
import com.bernardo.feedvault.ui.components.FullscreenVideoOverlay
import com.bernardo.feedvault.ui.components.LoadingView
import com.bernardo.feedvault.ui.components.ManagePeopleDialog
import com.bernardo.feedvault.ui.components.ManageTagsDialog
import com.bernardo.feedvault.ui.components.MediaFeed
import com.bernardo.feedvault.ui.components.PeopleEditorDialog
import com.bernardo.feedvault.ui.components.TagEditorDialog

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import com.bernardo.feedvault.ui.theme.FeedVaultTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class FullscreenState(
    val items: List<MediaItem>,
    val startIndex: Int,
    val startPosition: Long,
    val startPlaying: Boolean = true,
    val startMuted: Boolean = true
)

data class ClipFullscreenState(
    val clips: List<VideoClip>,
    val startIndex: Int,
    val startPosition: Long = 0L
)

class MainActivity : FragmentActivity() {
    private lateinit var folderPickerLauncher: ActivityResultLauncher<Uri?>
    private lateinit var filePickerLauncher: ActivityResultLauncher<Array<String>>
    private lateinit var exportTagsLauncher: ActivityResultLauncher<String>

    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)

        folderPickerLauncher = registerForActivityResult(
            ActivityResultContracts.OpenDocumentTree()
        ) { uri ->
            val vm = androidx.lifecycle.ViewModelProvider(
                this,
                object : androidx.lifecycle.ViewModelProvider.Factory {
                    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                        @Suppress("UNCHECKED_CAST")
                        return GalleryViewModel(this@MainActivity) as T
                    }
                }
            )[GalleryViewModel::class.java]

            if (uri == null) return@registerForActivityResult

            try {
                contentResolver.takePersistableUriPermission(
                    uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            } catch (e: SecurityException) {
                vm.setError("Sem permissão para acessar esta pasta. Tente outra localização.")
                return@registerForActivityResult
            }

            vm.selectFolder(uri)
        }

        filePickerLauncher = registerForActivityResult(
            ActivityResultContracts.OpenMultipleDocuments()
        ) { uris ->
            if (uris.isEmpty()) return@registerForActivityResult
            val vm = androidx.lifecycle.ViewModelProvider(
                this,
                object : androidx.lifecycle.ViewModelProvider.Factory {
                    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                        @Suppress("UNCHECKED_CAST")
                        return GalleryViewModel(this@MainActivity) as T
                    }
                }
            )[GalleryViewModel::class.java]
            uris.forEach { uri ->
                runCatching {
                    contentResolver.takePersistableUriPermission(
                        uri,
                        android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    )
                }
            }
            vm.addMediaFiles(uris)
        }

        exportTagsLauncher = registerForActivityResult(
            ActivityResultContracts.CreateDocument("application/json")
        ) { uri ->
            if (uri == null) return@registerForActivityResult
            val vm = androidx.lifecycle.ViewModelProvider(
                this,
                object : androidx.lifecycle.ViewModelProvider.Factory {
                    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                        @Suppress("UNCHECKED_CAST")
                        return GalleryViewModel(this@MainActivity) as T
                    }
                }
            )[GalleryViewModel::class.java]
            vm.exportTags(uri)
        }

        setContent {
            FeedVaultTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    GalleryScreen(
                        onSelectFolder = { folderPickerLauncher.launch(null) },
                        onSelectFiles = { filePickerLauncher.launch(arrayOf("image/*", "video/*")) },
                        onExportTags = { exportTagsLauncher.launch("feedvault_tags.json") }
                    )
                }
            }
        }
    }
}

private fun computePeopleTargetIndex(
    draggedIndex: Int,
    dragOffset: Float,
    itemHeights: Map<Int, Int>,
    itemCount: Int,
    defaultHeightPx: Int
): Int {
    if (itemCount <= 1) return 0
    var acc = 0f
    val positions = FloatArray(itemCount)
    for (i in 0 until itemCount) {
        positions[i] = acc
        acc += (itemHeights[i] ?: defaultHeightPx).toFloat()
    }
    val h = (itemHeights[draggedIndex] ?: defaultHeightPx).toFloat()
    val centerY = positions[draggedIndex] + h / 2f + dragOffset
    var best = draggedIndex
    var bestDist = Float.MAX_VALUE
    for (i in 0 until itemCount) {
        val mid = positions[i] + (itemHeights[i] ?: defaultHeightPx) / 2f
        val dist = kotlin.math.abs(centerY - mid)
        if (dist < bestDist) { bestDist = dist; best = i }
    }
    return best.coerceIn(0, itemCount - 1)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryScreen(
    onSelectFolder: () -> Unit,
    onSelectFiles: () -> Unit,
    onExportTags: () -> Unit = {}
) {
    val context = LocalContext.current
    val viewModel: GalleryViewModel = viewModel(
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return GalleryViewModel(context) as T
            }
        }
    )
    // Desktop is a build-flavor feature. ENABLE_DESKTOP is a compile-time constant, so in the
    // play flavor R8 folds these branches away and strips the desktop classes from the APK.
    val desktopViewModel: DesktopViewModel? = if (BuildConfig.ENABLE_DESKTOP) viewModel(
        key = "desktop",
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return DesktopViewModel(context) as T
            }
        }
    ) else null
    val downloadQueueViewModel: DownloadQueueViewModel? = if (BuildConfig.ENABLE_DESKTOP) viewModel(
        key = "downloadQueue",
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return DownloadQueueViewModel(context) as T
            }
        }
    ) else null
    val vaultPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isEmpty()) return@rememberLauncherForActivityResult
        uris.forEach { uri ->
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            }
        }
        viewModel.importToVault(uris)
    }
    val downloadItems = if (BuildConfig.ENABLE_DESKTOP) downloadQueueViewModel!!.items.collectAsState().value else emptyList()
    var showDownloadSheet by remember { mutableStateOf(false) }
    var fabDismissed by remember { mutableStateOf(false) }
    val prevDownloadCount = remember { mutableStateOf(0) }
    LaunchedEffect(downloadItems.size) {
        if (downloadItems.size > prevDownloadCount.value) fabDismissed = false
        prevDownloadCount.value = downloadItems.size
    }
    val uiState by viewModel.uiState.collectAsState()

    var editingItem by remember { mutableStateOf<MediaItem?>(null) }
    var showTagEditor by remember { mutableStateOf(false) }
    var showPeopleEditor by remember { mutableStateOf(false) }
    var showManageTags by remember { mutableStateOf(false) }
    var showManagePeople by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var showBatchTagEditor by remember { mutableStateOf(false) }
    var showBatchPeopleEditor by remember { mutableStateOf(false) }
    var showBatchDeleteConfirm by remember { mutableStateOf(false) }
    var showBatchRestoreConfirm by remember { mutableStateOf(false) }
    var fullscreenState by remember { mutableStateOf<FullscreenState?>(null) }
    var fullscreenClipState by remember { mutableStateOf<ClipFullscreenState?>(null) }
    val feedListState = rememberLazyListState()
    val feedGridState = rememberLazyGridState()
    val clipsListState = rememberLazyListState()
    var scrollRequest by remember { mutableStateOf(0 to null as String?) }
    var sortMenuExpanded by remember { mutableStateOf(false) }
    var draggedPersonIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffset by remember { mutableStateOf(0f) }
    val peopleItemHeights = remember { HashMap<Int, Int>() }
    val defaultItemHeightPx = with(LocalDensity.current) { 56.dp.roundToPx() }

    LaunchedEffect(uiState.feedScrollToTopVersion) {
        if (uiState.feedScrollToTopVersion > 0) {
            delay(50) // let LazyColumn finish re-anchoring after item reorder before overriding
            feedListState.animateScrollToItem(0)
            feedGridState.animateScrollToItem(0)
        }
    }

    LaunchedEffect(uiState.clipsScrollToTopVersion) {
        if (uiState.clipsScrollToTopVersion > 0) {
            delay(50)
            clipsListState.scrollToItem(0)
        }
    }

    LaunchedEffect(scrollRequest) {
        val uri = scrollRequest.second ?: return@LaunchedEffect
        val item = uiState.filteredMedia.firstOrNull { it.uri == uri } ?: return@LaunchedEffect
        val index = uiState.filteredMedia.indexOf(item)
        if (index < 0) return@LaunchedEffect
        val isPortrait = (item.aspectRatio > 0f && item.aspectRatio < 1f)
        val layoutInfo = feedListState.layoutInfo
        val viewportHeight = layoutInfo.viewportEndOffset - layoutInfo.viewportStartOffset
        val visibleInfo = layoutInfo.visibleItemsInfo.firstOrNull { it.index == index }
        if (isPortrait) {
            // Portrait: align item top to viewport top
            if (visibleInfo != null) {
                if (visibleInfo.offset != 0) feedListState.animateScrollBy(visibleInfo.offset.toFloat(), animationSpec = tween(500))
            } else {
                feedListState.scrollToItem(index)
            }
        } else {
            // Landscape/square: center in viewport
            if (visibleInfo != null) {
                val delta = (visibleInfo.offset + visibleInfo.size / 2) - viewportHeight / 2
                if (delta != 0) feedListState.animateScrollBy(delta.toFloat(), animationSpec = tween(500))
            } else {
                feedListState.scrollToItem(index)
                val info = feedListState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == index }
                if (info != null && info.size < viewportHeight) {
                    val shiftDown = (viewportHeight - info.size) / 2
                    feedListState.animateScrollBy(-shiftDown.toFloat(), animationSpec = tween(500))
                }
            }
        }
    }

    var playingClipIndex by remember { mutableStateOf(-1) }
    LaunchedEffect(playingClipIndex) {
        if (playingClipIndex < 0) return@LaunchedEffect
        val layoutInfo = clipsListState.layoutInfo
        val viewportHeight = layoutInfo.viewportEndOffset - layoutInfo.viewportStartOffset
        val visibleInfo = layoutInfo.visibleItemsInfo.firstOrNull { it.index == playingClipIndex }
        if (visibleInfo != null) {
            val delta = (visibleInfo.offset + visibleInfo.size / 2) - viewportHeight / 2
            if (delta != 0) clipsListState.animateScrollBy(delta.toFloat(), animationSpec = tween(500))
        } else {
            clipsListState.scrollToItem(playingClipIndex)
            val info = clipsListState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == playingClipIndex }
            if (info != null && info.size < viewportHeight) {
                val shiftDown = (viewportHeight - info.size) / 2
                clipsListState.animateScrollBy(-shiftDown.toFloat(), animationSpec = tween(500))
            }
        }
    }

    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var pendingDesktopFile by remember { mutableStateOf<com.bernardo.feedvault.data.DesktopFile?>(null) }
    var showUseDesktopFolderDialog by remember { mutableStateOf(false) }
    var pendingDesktopFiles by remember { mutableStateOf<List<com.bernardo.feedvault.data.DesktopFile>>(emptyList()) }
    var showUseDesktopFolderBulkDialog by remember { mutableStateOf(false) }

    val desktopFolderLauncher = if (BuildConfig.ENABLE_DESKTOP) rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri == null) {
            pendingDesktopFile = null
            pendingDesktopFiles = emptyList()
            return@rememberLauncherForActivityResult
        }
        runCatching {
            context.contentResolver.takePersistableUriPermission(
                uri,
                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
        }
        desktopViewModel!!.setDownloadFolderUri(uri.toString())
        val bulk = pendingDesktopFiles
        if (bulk.isNotEmpty()) {
            pendingDesktopFiles = emptyList()
            bulk.forEach { f ->
                val url = DesktopRepository.downloadUrl(desktopViewModel.uiState.value.baseUrl, f.id)
                downloadQueueViewModel!!.enqueue(
                    DownloadItem(name = f.name, source = "Desktop", url = url, fileName = f.name, folderUri = uri)
                )
            }
        } else {
            val file = pendingDesktopFile ?: return@rememberLauncherForActivityResult
            pendingDesktopFile = null
            val url = DesktopRepository.downloadUrl(desktopViewModel.uiState.value.baseUrl, file.id)
            downloadQueueViewModel!!.enqueue(
                DownloadItem(name = file.name, source = "Desktop", url = url, fileName = file.name, folderUri = uri)
            )
        }
    } else null

    // Catch-all: lowest priority — handles everything, swallows back when already at default
    BackHandler(enabled = true) {
        when {
            uiState.isSelectionMode -> viewModel.clearSelection()
            uiState.currentSection != AppSection.GALLERY -> viewModel.setSection(AppSection.GALLERY)
            uiState.selectedPeople.isNotEmpty() || uiState.filterUntaggedPeople ||
                uiState.selectedTags.isNotEmpty() || uiState.searchQuery.isNotEmpty() -> {
                viewModel.clearFilters()
                viewModel.setSearchQuery("")
            }
            uiState.vaultMode -> {
                viewModel.vaultLock()
                viewModel.setVaultMode(false)
            }
            // else: already at Galeria Todos — swallow the back press
        }
    }
    // Higher priority: close drawer first if open
    BackHandler(enabled = drawerState.isOpen) {
        scope.launch { drawerState.close() }
    }

    // Auto-lock the vault when the app leaves the foreground (e.g. screen off, recents).
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> viewModel.onAppBackgrounded()
                Lifecycle.Event.ON_START -> viewModel.onAppForegrounded()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // While in the vault, mark the window secure so vault media is excluded from the
    // recent-apps snapshot and screenshots; cleared on exit.
    LaunchedEffect(uiState.vaultMode) {
        (context as? Activity)?.window?.let { window ->
            if (uiState.vaultMode) window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
            else window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        val msg = uiState.errorMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        viewModel.clearError()
    }

    Box(modifier = Modifier.fillMaxSize()) {
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(modifier = Modifier.width(280.dp)) {
                Text(
                    text = "FeedVault",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 20.dp)
                )
                Divider()
                Spacer(modifier = Modifier.height(4.dp))

                // Section navigation
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Photo, contentDescription = null) },
                    label = { Text("Galeria") },
                    badge = { Text("${uiState.filteredMedia.size}", style = MaterialTheme.typography.labelSmall) },
                    selected = uiState.currentSection == AppSection.GALLERY,
                    onClick = {
                        viewModel.setSection(AppSection.GALLERY)
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.VideoLibrary, contentDescription = null) },
                    label = { Text("Clipes") },
                    badge = { Text("${uiState.filteredClips.size}", style = MaterialTheme.typography.labelSmall) },
                    selected = uiState.currentSection == AppSection.CLIPS,
                    onClick = {
                        viewModel.setSection(AppSection.CLIPS)
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
                if (BuildConfig.ENABLE_DESKTOP) {
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.DesktopWindows, contentDescription = null) },
                        label = { Text("Desktop") },
                        selected = uiState.currentSection == AppSection.DESKTOP,
                        onClick = {
                            viewModel.setSection(AppSection.DESKTOP)
                            scope.launch { drawerState.close() }
                        },
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                }

                Divider()
                Spacer(modifier = Modifier.height(4.dp))

                OutlinedTextField(
                    value = uiState.drawerPeopleSearch,
                    onValueChange = { viewModel.setDrawerPeopleSearch(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    placeholder = { Text("Buscar pessoa...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (uiState.drawerPeopleSearch.isNotEmpty()) {
                            IconButton(onClick = { viewModel.setDrawerPeopleSearch("") }) {
                                Icon(Icons.Default.Close, contentDescription = "Limpar")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(24.dp)
                )

                // People navigation
                LazyColumn(modifier = Modifier.weight(1f)) {
                    item {
                        NavigationDrawerItem(
                            icon = { Icon(Icons.Default.Person, contentDescription = null) },
                            label = { Text("Todos") },
                            selected = uiState.selectedPeople.isEmpty() && !uiState.filterUntaggedPeople,
                            onClick = {
                                viewModel.clearFilters()
                                scope.launch { drawerState.close() }
                            },
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
                    }

                    item {
                        NavigationDrawerItem(
                            icon = { Icon(Icons.Default.Person, contentDescription = null) },
                            label = { Text("Sem Pessoas") },
                            selected = uiState.filterUntaggedPeople,
                            onClick = {
                                viewModel.selectNoPeopleFilter()
                                scope.launch { drawerState.close() }
                            },
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
                    }

                    val peopleToShow = if (uiState.drawerPeopleSearch.isBlank()) uiState.allPeople
                        else {
                            val q = uiState.drawerPeopleSearch.normalizeForSearch()
                            uiState.allPeople.filter { it.normalizeForSearch().contains(q) }
                        }

                    if (uiState.allPeople.isNotEmpty()) {
                        item {
                            Text(
                                text = "Pessoas / Categorias",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                            )
                        }
                        itemsIndexed(peopleToShow) { index, person ->
                            val di = draggedPersonIndex
                            val targetIdx = if (di != null)
                                computePeopleTargetIndex(di, dragOffset, peopleItemHeights, peopleToShow.size, defaultItemHeightPx)
                            else -1
                            val displacement = when {
                                di == null -> 0f
                                index == di -> dragOffset
                                di < targetIdx && index in (di + 1)..targetIdx -> -(peopleItemHeights[di]?.toFloat() ?: 0f)
                                di > targetIdx && index in targetIdx until di -> peopleItemHeights[di]?.toFloat() ?: 0f
                                else -> 0f
                            }
                            val dragScale by animateFloatAsState(
                                targetValue = if (di == index) 1.05f else 1f,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessHigh
                                ),
                                label = "dragScale"
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp)
                                    .zIndex(if (di == index) 1f else 0f)
                                    .graphicsLayer {
                                        translationY = displacement
                                        scaleX = dragScale
                                        scaleY = dragScale
                                    }
                                    .onGloballyPositioned { peopleItemHeights[index] = it.size.height },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val personCount = uiState.allMedia.count { person in it.people }
                                NavigationDrawerItem(
                                    icon = { Icon(Icons.Default.Person, contentDescription = null) },
                                    label = { Text(person) },
                                    badge = { Text("$personCount", style = MaterialTheme.typography.labelSmall) },
                                    selected = uiState.selectedPeople == listOf(person),
                                    onClick = {
                                        viewModel.selectPersonFromDrawer(person)
                                        scope.launch { drawerState.close() }
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                                if (uiState.drawerPeopleSearch.isBlank()) {
                                    Icon(
                                        Icons.Default.DragHandle,
                                        contentDescription = "Reordenar",
                                        modifier = Modifier
                                            .size(24.dp)
                                            .pointerInput(index) {
                                                detectDragGesturesAfterLongPress(
                                                    onDragStart = {
                                                        draggedPersonIndex = index
                                                        dragOffset = 0f
                                                    },
                                                    onDrag = { change, dragAmount ->
                                                        change.consume()
                                                        dragOffset += dragAmount.y
                                                    },
                                                    onDragEnd = {
                                                        val from = draggedPersonIndex ?: return@detectDragGesturesAfterLongPress
                                                        val to = computePeopleTargetIndex(from, dragOffset, peopleItemHeights, peopleToShow.size, defaultItemHeightPx)
                                                        if (from != to) viewModel.reorderPeople(from, to)
                                                        draggedPersonIndex = null
                                                        dragOffset = 0f
                                                    },
                                                    onDragCancel = {
                                                        draggedPersonIndex = null
                                                        dragOffset = 0f
                                                    }
                                                )
                                            },
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                Divider()

                // Compact actions row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = {
                        if (uiState.vaultMode) {
                            viewModel.vaultLock()
                            viewModel.setVaultMode(false)
                        } else {
                            viewModel.setVaultMode(true)
                        }
                        scope.launch { drawerState.close() }
                    }) {
                        Icon(
                            if (uiState.vaultMode) Icons.Default.LockOpen else Icons.Default.Lock,
                            contentDescription = if (uiState.vaultMode) "Sair do Cofre" else "Cofre",
                            tint = if (uiState.vaultMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    if (uiState.hasSavedFolder) {
                        IconButton(onClick = { viewModel.syncMedia(); scope.launch { drawerState.close() } }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Sincronizar")
                        }
                    }
                    IconButton(onClick = { scope.launch { drawerState.close() }; onSelectFolder() }) {
                        Icon(Icons.Default.Add, contentDescription = "Adicionar Pasta")
                    }
                    // Settings sits last on the right, separated from the rest.
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(onClick = { showSettings = true; scope.launch { drawerState.close() } }) {
                        Icon(Icons.Default.Settings, contentDescription = "Configurações")
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    ) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            floatingActionButton = {},
            topBar = {
                Column {
                    TopAppBar(
                        title = {
                            val sectionName = when (uiState.currentSection) {
                                AppSection.CLIPS -> "Clipes"
                                AppSection.DESKTOP -> "Desktop"
                                else -> "Galeria"
                            }
                            val personLabel = if (uiState.currentSection == AppSection.DESKTOP) null else when {
                                uiState.filterUntaggedPeople -> "Sem Pessoas"
                                uiState.selectedPeople.isNotEmpty() -> uiState.selectedPeople.joinToString(", ")
                                else -> null
                            }
                            val base = if (personLabel != null) "$personLabel · $sectionName" else sectionName
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                if (uiState.vaultMode) {
                                    Icon(
                                        Icons.Default.Lock,
                                        contentDescription = "Cofre",
                                        modifier = Modifier.size(18.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Text(
                                    text = base,
                                    style = MaterialTheme.typography.titleLarge,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(Icons.Default.Menu, contentDescription = "Menu")
                            }
                        },
                        actions = {
                            when (uiState.currentSection) {
                                AppSection.DESKTOP -> {}
                                else -> {
                                    if (uiState.vaultMode) {
                                        IconButton(onClick = { viewModel.markVaultPickerLaunch(); vaultPickerLauncher.launch(arrayOf("image/*", "video/*")) }) {
                                            Icon(Icons.Default.Add, contentDescription = "Adicionar ao cofre", modifier = Modifier.size(18.dp))
                                        }
                                        if (!uiState.vaultBiometricEnabled) {
                                            IconButton(onClick = { enableVaultBiometric(context, viewModel) }) {
                                                Icon(Icons.Default.Fingerprint, contentDescription = "Ativar biometria", modifier = Modifier.size(18.dp))
                                            }
                                        }
                                    }
                                    val isClips = uiState.currentSection == AppSection.CLIPS
                                    IconButton(onClick = { viewModel.toggleGridView() }) {
                                        Icon(
                                            imageVector = if (uiState.isGridView) Icons.Default.ViewStream else Icons.Default.GridView,
                                            contentDescription = "Alternar visualização",
                                            tint = if (uiState.isGridView) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    IconButton(onClick = { viewModel.toggleShuffle() }) {
                                        Icon(
                                            imageVector = Icons.Default.Shuffle,
                                            contentDescription = "Aleatório",
                                            tint = if (uiState.isShuffled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    val favActive = if (isClips) uiState.filterFavoriteClips else uiState.filterFavorites
                                    IconButton(onClick = { if (isClips) viewModel.toggleFavoriteClipsFilter() else viewModel.toggleFavoritesFilter() }) {
                                        Icon(
                                            imageVector = if (favActive) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                            contentDescription = "Favoritos",
                                            tint = if (favActive) com.bernardo.feedvault.ui.theme.FavoriteRose else MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    )
                    // ── Thin secondary bar: sort + search-with-dropdown + type filters ──
                    when (uiState.currentSection) {
                        AppSection.DESKTOP -> {}
                        else -> {
                    val isClipsBar = uiState.currentSection == AppSection.CLIPS
                    val sortIsDefault = if (isClipsBar) uiState.clipSortOrder == ClipSortOrder.DATE_CREATED_DESC
                                       else (uiState.mediaSortOrder == MediaSortOrder.DATE_MODIFIED_DESC && !uiState.isShuffled)
                    val activeTags = if (isClipsBar) uiState.selectedClipTags else uiState.selectedTags
                    val allSearchTags = if (isClipsBar) uiState.allClipTags else uiState.allTags
                    val query = uiState.searchQuery
                    val normQuery = query.normalizeForSearch()
                    val matchingTags = if (query.isBlank()) emptyList()
                        else allSearchTags.filter { it.normalizeForSearch().contains(normQuery) }.take(6)
                    val matchingPeople = if (query.isBlank()) emptyList()
                        else uiState.allPeople.filter { it.normalizeForSearch().contains(normQuery) }.take(4)
                    val showSearchDropdown = matchingTags.isNotEmpty() || matchingPeople.isNotEmpty()

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(start = 10.dp, end = 6.dp, top = 0.dp, bottom = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        BasicTextField(
                            value = query,
                            onValueChange = { viewModel.setSearchQuery(it) },
                            singleLine = true,
                            maxLines = 1,
                            textStyle = TextStyle(
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = MaterialTheme.typography.bodySmall.fontSize
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(horizontal = 9.dp, vertical = 6.dp),
                            decorationBox = { inner ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(13.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(Modifier.width(5.dp))
                                    Box(Modifier.weight(1f)) {
                                        if (query.isEmpty()) {
                                            Text(
                                                text = if (isClipsBar) "Buscar clipes, tags, pessoas..." else "Buscar, tags, pessoas...",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        inner()
                                    }
                                    if (query.isNotEmpty()) {
                                        Spacer(Modifier.width(4.dp))
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = null,
                                            modifier = Modifier
                                                .size(13.dp)
                                                .clickable { viewModel.setSearchQuery("") },
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        )
                        if (!isClipsBar) {
                            var typeMenuExpanded by remember { mutableStateOf(false) }
                            val typeOptions = listOf(null to "Todos", "video" to "Vídeos", "image" to "Fotos", "gif" to "GIFs")
                            val currentLabel = typeOptions.firstOrNull { it.first == uiState.mediaTypeFilter }?.second ?: "Todos"
                            val isFiltered = uiState.mediaTypeFilter != null
                            Box {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            if (isFiltered) MaterialTheme.colorScheme.primaryContainer
                                            else MaterialTheme.colorScheme.surfaceVariant
                                        )
                                        .clickable { typeMenuExpanded = true }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        currentLabel,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isFiltered) MaterialTheme.colorScheme.onPrimaryContainer
                                                else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                DropdownMenu(
                                    expanded = typeMenuExpanded,
                                    onDismissRequest = { typeMenuExpanded = false }
                                ) {
                                    typeOptions.forEach { (type, label) ->
                                        val active = uiState.mediaTypeFilter == type
                                        DropdownMenuItem(
                                            text = { Text(label, color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface) },
                                            leadingIcon = if (active) ({ Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp)) }) else null,
                                            onClick = { viewModel.setMediaTypeFilter(type); typeMenuExpanded = false }
                                        )
                                    }
                                }
                            }
                        }
                        Box {
                            IconButton(
                                onClick = { sortMenuExpanded = true },
                                modifier = Modifier.size(30.dp)
                            ) {
                                Icon(
                                    Icons.Default.FilterList,
                                    contentDescription = "Filtros e ordenação",
                                    modifier = Modifier.size(18.dp),
                                    tint = if (!sortIsDefault) MaterialTheme.colorScheme.primary
                                           else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            DropdownMenu(
                                expanded = sortMenuExpanded,
                                onDismissRequest = { sortMenuExpanded = false }
                            ) {
                                if (!isClipsBar) {
                                    DropdownMenuItem(
                                        text = { Text("ORDENAR", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                                        onClick = {}, enabled = false
                                    )
                                    MediaSortOrder.entries.forEach { order ->
                                        val active = uiState.mediaSortOrder == order && !uiState.isShuffled
                                        DropdownMenuItem(
                                            text = { Text(order.label, color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface) },
                                            leadingIcon = if (active) ({ Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp)) }) else null,
                                            onClick = { viewModel.setMediaSortOrder(order); sortMenuExpanded = false }
                                        )
                                    }
                                } else {
                                    DropdownMenuItem(
                                        text = { Text("ORDENAR CLIPES", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                                        onClick = {}, enabled = false
                                    )
                                    ClipSortOrder.entries.forEach { order ->
                                        val active = uiState.clipSortOrder == order
                                        DropdownMenuItem(
                                            text = { Text(order.label, color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface) },
                                            leadingIcon = if (active) ({ Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp)) }) else null,
                                            onClick = { viewModel.setClipSortOrder(order); sortMenuExpanded = false }
                                        )
                                    }
                                }
                            }
                        }
                    }
                    // ── Search suggestions (inline, not popup) ────────────────
                    if (showSearchDropdown) {
                        Divider()
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surface)
                                .heightIn(max = 220.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            if (matchingTags.isNotEmpty()) {
                                Text(
                                    "TAGS",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                                )
                                matchingTags.forEach { tag ->
                                    val isActive = activeTags.contains(tag)
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                if (isClipsBar) viewModel.toggleClipTag(tag) else viewModel.toggleTag(tag)
                                                viewModel.setSearchQuery("")
                                            }
                                            .padding(horizontal = 16.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Icon(
                                            if (isActive) Icons.Default.Check else Icons.Default.Label,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                            tint = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(tag, color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodyMedium)
                                    }
                                }
                            }
                            if (matchingPeople.isNotEmpty()) {
                                if (matchingTags.isNotEmpty()) Divider()
                                Text(
                                    "PESSOAS",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                                )
                                matchingPeople.forEach { person ->
                                    val isActive = uiState.selectedPeople.contains(person)
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                viewModel.togglePerson(person)
                                                viewModel.setSearchQuery("")
                                            }
                                            .padding(horizontal = 16.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Icon(
                                            if (isActive) Icons.Default.Check else Icons.Default.Person,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                            tint = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(person, color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodyMedium)
                                    }
                                }
                            }
                        }
                    }
                    // ── Active tag chips ──────────────────────────────────────
                    if (activeTags.isNotEmpty()) {
                        androidx.compose.foundation.lazy.LazyRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surface)
                                .padding(start = 10.dp, end = 10.dp, bottom = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            items(activeTags) { tag ->
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.primaryContainer)
                                        .padding(start = 8.dp, end = 4.dp, top = 3.dp, bottom = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Text(tag, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Remover",
                                        modifier = Modifier
                                            .size(14.dp)
                                            .clickable {
                                                if (isClipsBar) viewModel.toggleClipTag(tag) else viewModel.toggleTag(tag)
                                            },
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }
                    }
                        } // end else
                    } // end when
                }
            }
        ) { padding ->
            when {
                uiState.vaultMode && !uiState.vaultUnlocked -> {
                    VaultGate(
                        state = uiState,
                        viewModel = viewModel,
                        modifier = Modifier.fillMaxSize().padding(padding)
                    )
                }

                BuildConfig.ENABLE_DESKTOP && uiState.currentSection == AppSection.DESKTOP -> {
                    DesktopScreen(
                        viewModel = desktopViewModel!!,
                        onSaveFile = { file ->
                            val existingFolder = desktopViewModel.getDownloadFolderUri()
                            if (existingFolder == null) {
                                pendingDesktopFile = file
                                desktopFolderLauncher!!.launch(null)
                            } else {
                                pendingDesktopFile = file
                                showUseDesktopFolderDialog = true
                            }
                        },
                        onSaveAll = { files ->
                            val existingFolder = desktopViewModel.getDownloadFolderUri()
                            if (existingFolder == null) {
                                pendingDesktopFiles = files
                                desktopFolderLauncher!!.launch(null)
                            } else {
                                pendingDesktopFiles = files
                                showUseDesktopFolderBulkDialog = true
                            }
                        },
                        modifier = Modifier.fillMaxSize().padding(padding)
                    )
                }

                uiState.isLoading -> {
                    LoadingView(modifier = Modifier.padding(padding))
                }

                uiState.currentSection == AppSection.CLIPS -> {
                    val mediaById = uiState.allMedia.associateBy { it.id }
                    Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                        ClipsFeed(
                            clips = uiState.filteredClips,
                            mediaById = mediaById,
                            allAvailableTags = (uiState.allTags + uiState.allClipTags).distinct(),
                            currentlyPlayingUri = uiState.currentlyPlayingUri,
                            onPlayRequested = { uri -> viewModel.setCurrentlyPlaying(uri) },
                            onClipPlayStarted = { idx -> playingClipIndex = idx },
                            onDeleteClip = { viewModel.deleteClip(it) },
                            onFullscreenClip = { clip, position ->
                                val idx = uiState.filteredClips.indexOfFirst { it.id == clip.id }
                                if (idx >= 0) {
                                    viewModel.setCurrentlyPlaying(null)
                                    fullscreenClipState = ClipFullscreenState(uiState.filteredClips, idx, position)
                                }
                            },
                            onUpdateClipTags = { clip, tags -> viewModel.updateClipTags(clip.id, tags) },
                            onToggleFavoriteClip = { viewModel.toggleClipFavorite(it.id) },
                            onFilterByTag = { viewModel.toggleClipTag(it) },
                            onFilterByPerson = { viewModel.togglePerson(it) },
                            onGoToOriginal = { mediaItemId ->
                                val mediaItem = uiState.allMedia.firstOrNull { it.id == mediaItemId }
                                if (mediaItem != null) {
                                    viewModel.setSection(AppSection.GALLERY)
                                    scrollRequest = (scrollRequest.first + 1) to mediaItem.uri
                                }
                            },
                            seekTokens = uiState.seekTokens,
                            onSeekTokenConsumed = { key -> viewModel.clearSeekToken(key) },
                            listState = clipsListState,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                uiState.filteredMedia.isEmpty() && uiState.allMedia.isEmpty() && !uiState.folderSelected -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            EmptyStateView()
                            Button(onClick = onSelectFolder) {
                                Text("Selecionar Pasta com Mídia")
                            }
                        }
                    }
                }

                else -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                    ) {
                        if (uiState.filteredMedia.isEmpty()) {
                            EmptyStateView(modifier = Modifier.weight(1f).fillMaxWidth())
                        } else {
                            MediaFeed(
                                items = uiState.filteredMedia,
                                currentlyPlayingUri = uiState.currentlyPlayingUri,
                                onPlayRequested = { uri ->
                                    viewModel.setCurrentlyPlaying(uri)
                                    scrollRequest = (scrollRequest.first + 1) to uri
                                },
                                seekTokens = uiState.seekTokens,
                                onSeekTokenConsumed = { uri -> viewModel.clearSeekToken(uri) },
                                seekOnlyTokens = uiState.seekOnlyTokens,
                                onSeekOnlyTokenConsumed = { uri -> viewModel.clearSeekOnlyToken(uri) },
                                listState = feedListState,
                                gridState = feedGridState,
                                isGridView = uiState.isGridView,
                                clipsByMediaId = uiState.clipsByMediaId,
                                onEditTags = { item ->
                                    editingItem = item
                                    showTagEditor = true
                                },
                                onEditPeople = { item ->
                                    editingItem = item
                                    showPeopleEditor = true
                                },
                                onToggleFavorite = { item -> viewModel.toggleFavorite(item.id) },
                                onRemoveTag = { item, tag ->
                                    viewModel.updateMediaTags(item.id, item.tags - tag)
                                },
                                onFilterByTag = { viewModel.toggleTag(it) },
                                onFilterByPerson = { viewModel.togglePerson(it) },
                                onFullscreenRequested = { item, position, wasPlaying, muted ->
                                    val index = uiState.filteredMedia.indexOfFirst { it.id == item.id }
                                    if (index >= 0) {
                                        viewModel.setCurrentlyPlaying(null)
                                        fullscreenState = FullscreenState(uiState.filteredMedia, index, position, startPlaying = wasPlaying, startMuted = muted)
                                    }
                                },
                                selectedIds = uiState.selectedIds,
                                isSelectionMode = uiState.isSelectionMode,
                                onToggleSelection = { id -> viewModel.toggleSelection(id) },
                                onDeleteMedia = { item -> viewModel.deleteMediaItem(item) },
                                onRestoreMedia = { item -> viewModel.restoreFromVault(item) },
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                            )
                        }
                        if (uiState.isSelectionMode) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(onClick = { viewModel.clearSelection() }) {
                                    Icon(Icons.Default.Close, contentDescription = "Cancelar seleção")
                                }
                                Text(
                                    text = "${uiState.selectedIds.size}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.weight(1f)
                                )
                                Button(
                                    onClick = { showBatchTagEditor = true },
                                    enabled = uiState.selectedIds.isNotEmpty(),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    Icon(Icons.Default.Label, contentDescription = "Tags", modifier = Modifier.size(18.dp))
                                }
                                Button(
                                    onClick = { showBatchPeopleEditor = true },
                                    enabled = uiState.selectedIds.isNotEmpty(),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    Icon(Icons.Default.Person, contentDescription = "Pessoas", modifier = Modifier.size(18.dp))
                                }
                                if (uiState.vaultMode) {
                                    Button(
                                        onClick = { showBatchRestoreConfirm = true },
                                        enabled = uiState.selectedIds.isNotEmpty(),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                                    ) {
                                        Icon(Icons.Default.Restore, contentDescription = "Restaurar à galeria", modifier = Modifier.size(18.dp))
                                    }
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = { showBatchDeleteConfirm = true },
                                    enabled = uiState.selectedIds.isNotEmpty(),
                                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.errorContainer,
                                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                                    ),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Deletar", modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

        // Desktop download folder dialog
        if (BuildConfig.ENABLE_DESKTOP && showUseDesktopFolderDialog && pendingDesktopFile != null) {
            val folderName = desktopViewModel!!.getDownloadFolderName() ?: "pasta selecionada"
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { showUseDesktopFolderDialog = false; pendingDesktopFile = null },
                title = { Text("Salvar arquivo") },
                text = { Text("Usar \"$folderName\"?") },
                confirmButton = {
                    androidx.compose.material3.TextButton(onClick = {
                        showUseDesktopFolderDialog = false
                        val file = pendingDesktopFile ?: return@TextButton
                        pendingDesktopFile = null
                        val folder = desktopViewModel.getDownloadFolderUri() ?: return@TextButton
                        val url = DesktopRepository.downloadUrl(desktopViewModel.uiState.value.baseUrl, file.id)
                        downloadQueueViewModel!!.enqueue(
                            DownloadItem(name = file.name, source = "Desktop", url = url, fileName = file.name, folderUri = folder)
                        )
                    }) { Text("Usar esta pasta") }
                },
                dismissButton = {
                    androidx.compose.material3.TextButton(onClick = {
                        showUseDesktopFolderDialog = false
                        desktopFolderLauncher!!.launch(null)
                    }) { Text("Escolher outra pasta") }
                }
            )
        }

        // Desktop bulk download folder dialog
        if (BuildConfig.ENABLE_DESKTOP && showUseDesktopFolderBulkDialog && pendingDesktopFiles.isNotEmpty()) {
            val folderName = desktopViewModel!!.getDownloadFolderName() ?: "pasta selecionada"
            val count = pendingDesktopFiles.size
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { showUseDesktopFolderBulkDialog = false; pendingDesktopFiles = emptyList() },
                title = { Text("Baixar todos") },
                text = { Text("Baixar $count arquivo${if (count != 1) "s" else ""} para \"$folderName\"?") },
                confirmButton = {
                    androidx.compose.material3.TextButton(onClick = {
                        showUseDesktopFolderBulkDialog = false
                        val files = pendingDesktopFiles
                        pendingDesktopFiles = emptyList()
                        val folder = desktopViewModel.getDownloadFolderUri() ?: return@TextButton
                        files.forEach { f ->
                            val url = DesktopRepository.downloadUrl(desktopViewModel.uiState.value.baseUrl, f.id)
                            downloadQueueViewModel!!.enqueue(
                                DownloadItem(name = f.name, source = "Desktop", url = url, fileName = f.name, folderUri = folder)
                            )
                        }
                    }) { Text("Baixar tudo") }
                },
                dismissButton = {
                    androidx.compose.material3.TextButton(onClick = {
                        showUseDesktopFolderBulkDialog = false
                        desktopFolderLauncher!!.launch(null)
                    }) { Text("Escolher outra pasta") }
                }
            )
        }

        // Dialogs
        if (showTagEditor && editingItem != null) {
            TagEditorDialog(
                currentTags = editingItem!!.tags,
                allAvailableTags = uiState.allTags,
                onConfirm = { tags ->
                    viewModel.updateMediaTags(editingItem!!.id, tags)
                    showTagEditor = false
                },
                onDismiss = { showTagEditor = false }
            )
        }

        if (showPeopleEditor && editingItem != null) {
            PeopleEditorDialog(
                currentPeople = editingItem!!.people,
                allAvailablePeople = uiState.allPeople,
                onConfirm = { people ->
                    viewModel.updateMediaPeople(editingItem!!.id, people)
                    showPeopleEditor = false
                },
                onDismiss = { showPeopleEditor = false }
            )
        }

        if (showManageTags) {
            ManageTagsDialog(
                allTags = uiState.allTags,
                onDeleteTag = { viewModel.deleteTag(it) },
                onRenameTag = { old, new -> viewModel.renameTag(old, new) },
                onDeleteAllTags = { viewModel.deleteAllTags() },
                onDismiss = { showManageTags = false }
            )
        }

        if (showManagePeople) {
            ManagePeopleDialog(
                allPeople = uiState.allPeople,
                onDeletePerson = { viewModel.deletePerson(it) },
                onRenamePerson = { old, new -> viewModel.renamePerson(old, new) },
                onDismiss = { showManagePeople = false }
            )
        }

        if (showBatchTagEditor) {
            TagEditorDialog(
                currentTags = emptyList(),
                allAvailableTags = uiState.allTags,
                onConfirm = { tags ->
                    viewModel.batchAddTags(tags)
                    showBatchTagEditor = false
                },
                onDismiss = { showBatchTagEditor = false }
            )
        }

        if (showBatchPeopleEditor) {
            PeopleEditorDialog(
                currentPeople = emptyList(),
                allAvailablePeople = uiState.allPeople,
                onConfirm = { people ->
                    viewModel.batchAddPeople(people)
                    showBatchPeopleEditor = false
                },
                onDismiss = { showBatchPeopleEditor = false }
            )
        }

        if (showBatchDeleteConfirm) {
            val count = uiState.selectedIds.size
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { showBatchDeleteConfirm = false },
                title = { Text("Deletar $count arquivo${if (count != 1) "s" else ""}?") },
                text = { Text("Os arquivos serão removidos permanentemente do dispositivo.") },
                confirmButton = {
                    androidx.compose.material3.TextButton(onClick = {
                        showBatchDeleteConfirm = false
                        viewModel.deleteSelectedMediaItems()
                    }) { Text("Deletar", color = MaterialTheme.colorScheme.error) }
                },
                dismissButton = {
                    androidx.compose.material3.TextButton(onClick = { showBatchDeleteConfirm = false }) {
                        Text("Cancelar")
                    }
                }
            )
        }

        if (showBatchRestoreConfirm) {
            val count = uiState.selectedIds.size
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { showBatchRestoreConfirm = false },
                title = { Text("Restaurar $count arquivo${if (count != 1) "s" else ""}?") },
                text = { Text("Os arquivos serão descriptografados, devolvidos à galeria e removidos do cofre.") },
                confirmButton = {
                    androidx.compose.material3.TextButton(onClick = {
                        showBatchRestoreConfirm = false
                        viewModel.restoreSelectedFromVault()
                    }) { Text("Restaurar") }
                },
                dismissButton = {
                    androidx.compose.material3.TextButton(onClick = { showBatchRestoreConfirm = false }) {
                        Text("Cancelar")
                    }
                }
            )
        }
    }

    if (BuildConfig.ENABLE_DESKTOP && showDownloadSheet) {
        DownloadQueueSheet(
            items = downloadItems,
            onDismiss = {
                showDownloadSheet = false
                val hasActive = downloadItems.any {
                    it.status == DownloadStatus.QUEUED || it.status == DownloadStatus.DOWNLOADING
                }
                if (!hasActive) downloadQueueViewModel!!.dismissCompleted()
            },
            onRetry = { downloadQueueViewModel!!.retry(it) },
            onDismissItem = { downloadQueueViewModel!!.dismiss(it) },
            onDismissCompleted = { downloadQueueViewModel!!.dismissCompleted() }
        )
    }

    if (BuildConfig.ENABLE_DESKTOP && !fabDismissed && downloadItems.isNotEmpty() &&
        fullscreenState == null && fullscreenClipState == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 16.dp, end = 16.dp),
            contentAlignment = Alignment.BottomEnd
        ) {
            DismissableDownloadFab(
                items = downloadItems,
                onClick = { showDownloadSheet = true },
                onDismiss = { fabDismissed = true }
            )
        }
    }

    // Clip fullscreen overlay
    val fcs = fullscreenClipState
    if (fcs != null) {
        val mediaById = uiState.allMedia.associateBy { it.id }
        ClipFullscreenOverlay(
            clips = fcs.clips,
            startIndex = fcs.startIndex,
            startPosition = fcs.startPosition,
            mediaById = mediaById,
            onDismiss = { fullscreenClipState = null },
            onToggleClipFavorite = { viewModel.toggleClipFavorite(it) },
            onFinalPosition = { clipId, pos -> viewModel.reportSeekPosition("clip_$clipId", pos) },
            onReturnedToEmbed = { clipId ->
                val idx = fcs.clips.indexOfFirst { it.id == clipId }
                if (idx >= 0) playingClipIndex = idx
            }
        )
    }

    // Fullscreen overlay — outside drawer so it covers everything and can hide system bars
    val fs = fullscreenState
    if (fs != null) {
        FullscreenVideoOverlay(
            items = fs.items,
            startIndex = fs.startIndex,
            startPosition = fs.startPosition,
            onDismiss = { fullscreenState = null },
            autoPlay = fs.startPlaying,
            startMuted = fs.startMuted,
            allAvailableTags = uiState.allTags,
            allAvailablePeople = uiState.allPeople,
            onSaveClip = { mediaItemId, uri, startMs, endMs, label ->
                viewModel.saveClip(VideoClip(
                    mediaItemId = mediaItemId,
                    uri = uri,
                    startMs = startMs,
                    endMs = endMs,
                    label = label
                ))
            },
            onSetThumbnail = { itemId, frameMs -> viewModel.setMediaThumbnailFrame(itemId, frameMs) },
            onToggleFavorite = { viewModel.toggleFavorite(it) },
            onUpdateTags = { id, tags -> viewModel.updateMediaTags(id, tags) },
            onUpdatePeople = { id, people -> viewModel.updateMediaPeople(id, people) },
            onFinalPosition = { uri, pos -> viewModel.reportSeekPosition(uri, pos) },
            onFinalPositionPaused = { uri, pos -> viewModel.reportSeekOnlyPosition(uri, pos) },
            onReturnedToEmbed = { uri -> scrollRequest = (scrollRequest.first + 1) to uri },
            onFilterByTag = { tag -> fullscreenState = null; viewModel.toggleTag(tag) },
            onFilterByPerson = { person -> fullscreenState = null; viewModel.togglePerson(person) },
            onDeleteMedia = { item -> viewModel.deleteMediaItem(item); fullscreenState = null }
        )
    }

    if (showSettings) {
        SettingsScreen(
            state = uiState,
            viewModel = viewModel,
            onClose = { showSettings = false },
            onManageTags = { showManageTags = true },
            onManagePeople = { showManagePeople = true },
            onExportTags = {
                val saved = viewModel.getSavedExportUri()
                if (saved != null) viewModel.exportTags(saved) else onExportTags()
            }
        )
    }
    } // close outer Box
}
