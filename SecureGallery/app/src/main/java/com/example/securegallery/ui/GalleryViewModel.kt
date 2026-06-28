package com.example.securegallery.ui

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.securegallery.data.AppDatabase
import com.example.securegallery.util.normalizeForSearch
import com.example.securegallery.data.MediaItem
import com.example.securegallery.data.MediaRepository
import com.example.securegallery.data.VideoClip
import com.example.securegallery.vault.VaultManager
import com.example.securegallery.vault.VaultSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.crypto.Cipher

enum class AppSection { GALLERY, CLIPS, DESKTOP }

enum class MediaSortOrder(val label: String) {
    DATE_MODIFIED_DESC("Modificado (recente)"),
    DATE_MODIFIED_ASC("Modificado (antigo)"),
    DATE_ADDED_DESC("Adicionado (recente)"),
    DATE_ADDED_ASC("Adicionado (antigo)"),
    NAME_ASC("Nome A→Z"),
    NAME_DESC("Nome Z→A"),
    FAVORITES_FIRST("Favoritos primeiro")
}

enum class ClipSortOrder(val label: String) {
    DATE_CREATED_DESC("Criado (recente)"),
    DATE_CREATED_ASC("Criado (antigo)"),
    DURATION_DESC("Duração (maior)"),
    DURATION_ASC("Duração (menor)"),
    LABEL_ASC("Label A→Z")
}

data class AppUiState(
    val allMedia: List<MediaItem> = emptyList(),
    val filteredMedia: List<MediaItem> = emptyList(),
    val allTags: List<String> = emptyList(),
    val allPeople: List<String> = emptyList(),
    val selectedPeople: List<String> = emptyList(),
    val selectedTags: List<String> = emptyList(),
    val filteredAvailableTags: List<Pair<String, Int>> = emptyList(),
    val filterUntaggedPeople: Boolean = false,
    val filterFavorites: Boolean = false,
    val isShuffled: Boolean = true,
    val mediaTypeFilter: String? = null, // null=all, "image", "gif", "video"
    val currentlyPlayingUri: String? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val folderSelected: Boolean = false,
    val hasSavedFolder: Boolean = false,
    val scanProgress: Int = 0,
    val currentSection: AppSection = AppSection.GALLERY,
    val isGridView: Boolean = false,
    val allClips: List<VideoClip> = emptyList(),
    val filteredClips: List<VideoClip> = emptyList(),
    val clipsByMediaId: Map<Long, List<VideoClip>> = emptyMap(),
    val allClipTags: List<String> = emptyList(),
    val selectedClipTags: List<String> = emptyList(),
    val filteredAvailableClipTags: List<Pair<String, Int>> = emptyList(),
    val filterFavoriteClips: Boolean = false,
    val searchQuery: String = "",
    val drawerPeopleSearch: String = "",
    val seekTokens: Map<String, Pair<Int, Long>> = emptyMap(),
    val seekOnlyTokens: Map<String, Pair<Int, Long>> = emptyMap(),
    val feedScrollToTopVersion: Int = 0,
    val clipsScrollToTopVersion: Int = 0,
    val selectedIds: Set<Long> = emptySet(),
    val isSelectionMode: Boolean = false,
    val mediaSortOrder: MediaSortOrder = MediaSortOrder.DATE_MODIFIED_DESC,
    val clipSortOrder: ClipSortOrder = ClipSortOrder.DATE_CREATED_DESC,
    // Vault ("Cofre") — encrypted mirror of the gallery
    val vaultMode: Boolean = false,
    val vaultInitialized: Boolean = false,
    val vaultUnlocked: Boolean = false,
    val vaultBusy: Boolean = false,
    val vaultBusyMessage: String? = null,
    val vaultBiometricEnabled: Boolean = false
)

class GalleryViewModel(private val context: Context) : ViewModel() {
    private lateinit var repository: MediaRepository
    private val prefs = context.getSharedPreferences("gallery_prefs", Context.MODE_PRIVATE)
    // Stable shuffle order — set once on toggle, applied consistently on every updateFilteredMedia()
    private var shuffleOrder: List<Long> = emptyList()

    private val _uiState = MutableStateFlow(AppUiState())
    val uiState: StateFlow<AppUiState> = _uiState.asStateFlow()

    init {
        val savedUri = prefs.getString("folder_uri", null)
        if (savedUri != null) {
            _uiState.value = _uiState.value.copy(hasSavedFolder = true)
        }
        setupDatabase()
    }

    private fun setupDatabase() {
        val database = AppDatabase.getDatabase(context)
        val dao = database.mediaItemDao()
        repository = MediaRepository(context, dao, database.videoClipDao())

        _uiState.value = _uiState.value.copy(
            vaultInitialized = VaultManager.isInitialized(context),
            vaultBiometricEnabled = VaultManager.isBiometricEnabled(context)
        )

        viewModelScope.launch {
            repository.getAllMediaItems().collect { media ->
                VaultSession.register(media)
                _uiState.value = _uiState.value.copy(allMedia = media)
                if (_uiState.value.isShuffled && shuffleOrder.isEmpty() && media.isNotEmpty()) {
                    shuffleOrder = media.map { it.id }.shuffled()
                }
                updateFilteredMedia()
            }
        }

        viewModelScope.launch {
            repository.precomputeMissingAspectRatios()
        }

        viewModelScope.launch {
            repository.getAllClips().collect { clips ->
                val clipTags = clips.flatMap { it.tags }.filter { it.isNotBlank() }.distinct()
                _uiState.value = _uiState.value.copy(
                    allClips = clips,
                    clipsByMediaId = clips.groupBy { it.mediaItemId },
                    allClipTags = clipTags
                )
                updateFilteredMedia()
            }
        }
    }

    fun selectFolder(folderUri: Uri) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                repository.scanMediaFromFolder(folderUri)
                prefs.edit().putString("folder_uri", folderUri.toString()).apply()
                _uiState.value = _uiState.value.copy(folderSelected = true, hasSavedFolder = true)
            } catch (e: SecurityException) {
                _uiState.value = _uiState.value.copy(errorMessage = "Sem permissão para acessar esta pasta.")
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.value = _uiState.value.copy(errorMessage = "Erro ao acessar a pasta.")
            } finally {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    fun setCurrentlyPlaying(uri: String?) {
        _uiState.value = _uiState.value.copy(currentlyPlayingUri = uri)
    }

    fun addMediaFiles(uris: List<Uri>) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                repository.addMediaFiles(uris)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    fun setError(message: String) {
        _uiState.value = _uiState.value.copy(errorMessage = message)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    fun syncMedia() {
        val savedUri = prefs.getString("folder_uri", null) ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                repository.scanMediaFromFolder(Uri.parse(savedUri))
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    fun selectPersonFromDrawer(personName: String) {
        _uiState.value = _uiState.value.copy(selectedPeople = listOf(personName), filterUntaggedPeople = false)
        updateFilteredMedia(scrollFeed = true, scrollClips = true)
    }

    fun togglePerson(personName: String) {
        val current = _uiState.value.selectedPeople.toMutableList()
        if (current.contains(personName)) current.remove(personName) else current.add(personName)
        _uiState.value = _uiState.value.copy(selectedPeople = current, filterUntaggedPeople = false)
        updateFilteredMedia(scrollFeed = true, scrollClips = true)
    }

    fun selectNoPeopleFilter() {
        _uiState.value = _uiState.value.copy(selectedPeople = emptyList(), filterUntaggedPeople = true)
        updateFilteredMedia(scrollFeed = true, scrollClips = true)
    }

    fun toggleTag(tag: String) {
        val current = _uiState.value.selectedTags.toMutableList()
        if (current.contains(tag)) current.remove(tag) else current.add(tag)
        _uiState.value = _uiState.value.copy(selectedTags = current)
        updateFilteredMedia(scrollFeed = true)
    }

    fun clearFilters() {
        _uiState.value = _uiState.value.copy(selectedPeople = emptyList(), selectedTags = emptyList(), filterUntaggedPeople = false)
        updateFilteredMedia(scrollFeed = true, scrollClips = true)
    }

    fun clearTagFilters() {
        _uiState.value = _uiState.value.copy(selectedTags = emptyList())
        updateFilteredMedia(scrollFeed = true)
    }

    fun deleteTag(tag: String) {
        viewModelScope.launch {
            _uiState.value.allMedia.filter { it.tags.contains(tag) }.forEach { item ->
                repository.updateTags(item.id, item.tags.filter { it != tag })
            }
            val newSelected = _uiState.value.selectedTags.filter { it != tag }
            _uiState.value = _uiState.value.copy(selectedTags = newSelected)
            updateFilteredMedia(scrollFeed = true)
        }
    }

    fun renameTag(oldTag: String, newTag: String) {
        if (newTag.isBlank() || newTag == oldTag) return
        viewModelScope.launch {
            _uiState.value.allMedia.filter { it.tags.contains(oldTag) }.forEach { item ->
                repository.updateTags(item.id, item.tags.map { if (it == oldTag) newTag else it })
            }
        }
    }

    fun deleteAllTags() {
        viewModelScope.launch {
            _uiState.value.allMedia.filter { it.tags.isNotEmpty() }.forEach { item ->
                repository.updateTags(item.id, emptyList())
            }
            _uiState.value = _uiState.value.copy(selectedTags = emptyList())
            updateFilteredMedia(scrollFeed = true)
        }
    }

    private fun updateFilteredMedia(scrollFeed: Boolean = false, scrollClips: Boolean = false) {
        val vaultMode = _uiState.value.vaultMode
        val vaultReady = !vaultMode || _uiState.value.vaultUnlocked
        // Each mode sees only its own media: encrypted in the Cofre, system media otherwise.
        val allMedia = if (vaultReady) _uiState.value.allMedia.filter { it.encrypted == vaultMode } else emptyList()
        val selectedPeople = _uiState.value.selectedPeople
        val selectedTags = _uiState.value.selectedTags
        val filterUntaggedPeople = _uiState.value.filterUntaggedPeople
        val filterFavorites = _uiState.value.filterFavorites
        val mediaTypeFilter = _uiState.value.mediaTypeFilter
        val searchQuery = _uiState.value.searchQuery.trim().normalizeForSearch()

        val filtered = allMedia.filter { item ->
            val personMatch = when {
                filterUntaggedPeople -> item.people.isEmpty()
                selectedPeople.isEmpty() -> true
                else -> selectedPeople.any { item.people.contains(it) }
            }
            val tagsMatch = selectedTags.isEmpty() || selectedTags.any { item.tags.contains(it) }
            val favMatch = !filterFavorites || item.isFavorite
            val isGif = item.mimeType == "image/gif" ||
                (item.mimeType.isEmpty() && item.fileName.endsWith(".gif", ignoreCase = true))
            val typeMatch = when (mediaTypeFilter) {
                "video" -> item.mediaType == "video"
                "gif"   -> item.mediaType == "image" && isGif
                "image" -> item.mediaType == "image" && !isGif
                else    -> true
            }
            val searchMatch = searchQuery.isEmpty() ||
                item.fileName.normalizeForSearch().contains(searchQuery) ||
                item.tags.any { it.normalizeForSearch().contains(searchQuery) } ||
                item.people.any { it.normalizeForSearch().contains(searchQuery) }
            personMatch && tagsMatch && favMatch && typeMatch && searchMatch
        }

        val result = if (_uiState.value.isShuffled && shuffleOrder.isNotEmpty()) {
            val rank = shuffleOrder.withIndex().associate { (i, id) -> id to i }
            filtered.sortedBy { rank[it.id] ?: Int.MAX_VALUE }
        } else {
            when (_uiState.value.mediaSortOrder) {
                MediaSortOrder.DATE_MODIFIED_DESC -> filtered.sortedByDescending { it.lastModified }
                MediaSortOrder.DATE_MODIFIED_ASC  -> filtered.sortedBy { it.lastModified }
                MediaSortOrder.DATE_ADDED_DESC    -> filtered.sortedByDescending { it.dateAdded }
                MediaSortOrder.DATE_ADDED_ASC     -> filtered.sortedBy { it.dateAdded }
                MediaSortOrder.NAME_ASC           -> filtered.sortedBy { it.fileName.lowercase() }
                MediaSortOrder.NAME_DESC          -> filtered.sortedByDescending { it.fileName.lowercase() }
                MediaSortOrder.FAVORITES_FIRST    -> filtered.sortedWith(
                    compareByDescending<MediaItem> { it.isFavorite }.thenByDescending { it.lastModified }
                )
            }
        }

        val filteredAvailableTags = allMedia.filter { item ->
            when {
                filterUntaggedPeople -> item.people.isEmpty()
                selectedPeople.isEmpty() -> true
                else -> selectedPeople.any { item.people.contains(it) }
            }
        }.flatMap { it.tags }
            .filter { it.isNotBlank() }
            .groupBy { it }
            .map { (tag, list) -> tag to list.size }
            .sortedByDescending { it.second }

        val mediaById = allMedia.associateBy { it.id }
        val selectedClipTags = _uiState.value.selectedClipTags
        val personFilteredClips = _uiState.value.allClips.filter { clip ->
            val item = mediaById[clip.mediaItemId] ?: return@filter false
            when {
                filterUntaggedPeople -> item.people.isEmpty()
                selectedPeople.isEmpty() -> true
                else -> selectedPeople.any { item.people.contains(it) }
            }
        }
        val filterFavoriteClips = _uiState.value.filterFavoriteClips
        val filteredClipsRaw = personFilteredClips.filter { clip ->
            val tagMatch = selectedClipTags.isEmpty() || selectedClipTags.any { clip.tags.contains(it) }
            val favClipMatch = !filterFavoriteClips || clip.isFavorite
            val clipSearchMatch = searchQuery.isEmpty() ||
                clip.label.normalizeForSearch().contains(searchQuery) ||
                clip.tags.any { it.normalizeForSearch().contains(searchQuery) } ||
                mediaById[clip.mediaItemId]?.let { item ->
                    item.fileName.normalizeForSearch().contains(searchQuery) ||
                    item.people.any { it.normalizeForSearch().contains(searchQuery) }
                } == true
            tagMatch && favClipMatch && clipSearchMatch
        }
        val filteredClips = when (_uiState.value.clipSortOrder) {
            ClipSortOrder.DATE_CREATED_DESC -> filteredClipsRaw.sortedByDescending { it.dateCreated }
            ClipSortOrder.DATE_CREATED_ASC  -> filteredClipsRaw.sortedBy { it.dateCreated }
            ClipSortOrder.DURATION_DESC     -> filteredClipsRaw.sortedByDescending { it.endMs - it.startMs }
            ClipSortOrder.DURATION_ASC      -> filteredClipsRaw.sortedBy { it.endMs - it.startMs }
            ClipSortOrder.LABEL_ASC         -> filteredClipsRaw.sortedBy { it.label.lowercase() }
        }
        val filteredAvailableClipTags = personFilteredClips
            .flatMap { it.tags }.filter { it.isNotBlank() }
            .groupBy { it }.map { (tag, list) -> tag to list.size }
            .sortedByDescending { it.second }

        // Tags + people are scoped to the active mode so the Cofre never leaks names into the normal gallery.
        val modeTags = allMedia.flatMap { it.tags }.filter { it.isNotBlank() }.distinct()
        val flatPeople = allMedia.flatMap { it.people }.filter { it.isNotBlank() }.distinct()
        val savedOrder = prefs.getString("people_order", null)
            ?.split(",")?.filter { it.isNotBlank() } ?: emptyList()
        val modePeople = (savedOrder.filter { it in flatPeople }.distinct() + flatPeople.filter { it !in savedOrder })

        _uiState.value = _uiState.value.copy(
            allTags = modeTags,
            allPeople = modePeople,
            filteredMedia = result,
            filteredAvailableTags = filteredAvailableTags,
            filteredClips = filteredClips,
            filteredAvailableClipTags = filteredAvailableClipTags,
            feedScrollToTopVersion = if (scrollFeed) _uiState.value.feedScrollToTopVersion + 1 else _uiState.value.feedScrollToTopVersion,
            clipsScrollToTopVersion = if (scrollClips) _uiState.value.clipsScrollToTopVersion + 1 else _uiState.value.clipsScrollToTopVersion
        )
    }

    fun scanFromMediaStore() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                repository.scanFromMediaStore()
                _uiState.value = _uiState.value.copy(folderSelected = true)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = "Erro ao escanear dispositivo.")
            } finally {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    fun updateMediaTags(itemId: Long, tags: List<String>) {
        viewModelScope.launch { repository.updateTags(itemId, tags) }
    }

    fun updateMediaPeople(itemId: Long, people: List<String>) {
        viewModelScope.launch { repository.updatePeople(itemId, people) }
    }

    fun toggleFavorite(itemId: Long) {
        viewModelScope.launch { repository.toggleFavorite(itemId) }
    }

    fun setMediaThumbnailFrame(itemId: Long, frameMs: Long) {
        viewModelScope.launch { repository.updateThumbnailFrameMs(itemId, frameMs) }
    }

    fun toggleFavoritesFilter() {
        _uiState.value = _uiState.value.copy(filterFavorites = !_uiState.value.filterFavorites)
        updateFilteredMedia(scrollFeed = true)
    }

    fun setMediaTypeFilter(type: String?) {
        _uiState.value = _uiState.value.copy(mediaTypeFilter = type)
        updateFilteredMedia(scrollFeed = true)
    }

    fun getSavedExportUri(): android.net.Uri? {
        val s = prefs.getString("export_uri", null) ?: return null
        return android.net.Uri.parse(s)
    }

    fun exportTags(uri: android.net.Uri) {
        viewModelScope.launch {
            try {
                val json = repository.exportTagData()
                context.contentResolver.openOutputStream(uri, "wt")?.use { it.write(json.toByteArray()) }
                runCatching {
                    context.contentResolver.takePersistableUriPermission(
                        uri, android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    )
                }
                prefs.edit().putString("export_uri", uri.toString()).apply()
                _uiState.value = _uiState.value.copy(errorMessage = "Tags exportadas com sucesso!")
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = "Erro ao exportar tags.")
            }
        }
    }

    fun renamePerson(oldName: String, newName: String) {
        if (newName.isBlank() || newName == oldName) return
        viewModelScope.launch {
            _uiState.value.allMedia.filter { it.people.contains(oldName) }.forEach { item ->
                repository.updatePeople(item.id, item.people.map { if (it == oldName) newName else it }.distinct())
            }
            // Map oldName → newName in order, then deduplicate in case newName already existed
            val orderedPeople = _uiState.value.allPeople.map { if (it == oldName) newName else it }.distinct()
            prefs.edit().putString("people_order", orderedPeople.joinToString(",")).apply()
            val newSelected = _uiState.value.selectedPeople.map { if (it == oldName) newName else it }.distinct()
            _uiState.value = _uiState.value.copy(selectedPeople = newSelected)
        }
    }

    fun deletePerson(name: String) {
        viewModelScope.launch {
            _uiState.value.allMedia.filter { it.people.contains(name) }.forEach { item ->
                repository.updatePeople(item.id, item.people.filter { it != name })
            }
            val newSelected = _uiState.value.selectedPeople.filter { it != name }
            val newOrdered = _uiState.value.allPeople.filter { it != name }
            prefs.edit().putString("people_order", newOrdered.joinToString(",")).apply()
            _uiState.value = _uiState.value.copy(selectedPeople = newSelected)
        }
    }

    fun reorderPeople(fromIndex: Int, toIndex: Int) {
        val current = _uiState.value.allPeople.toMutableList()
        if (fromIndex !in current.indices || toIndex !in current.indices || fromIndex == toIndex) return
        val item = current.removeAt(fromIndex)
        current.add(toIndex, item)
        prefs.edit().putString("people_order", current.joinToString(",")).apply()
        _uiState.value = _uiState.value.copy(allPeople = current)
    }

    fun movePerson(person: String, up: Boolean) {
        val current = _uiState.value.allPeople.toMutableList()
        val idx = current.indexOf(person)
        if (idx < 0) return
        val newIdx = if (up) (idx - 1).coerceAtLeast(0) else (idx + 1).coerceAtMost(current.lastIndex)
        if (newIdx == idx) return
        current.removeAt(idx)
        current.add(newIdx, person)
        prefs.edit().putString("people_order", current.joinToString(",")).apply()
        _uiState.value = _uiState.value.copy(allPeople = current)
    }

    fun toggleGridView() {
        _uiState.value = _uiState.value.copy(isGridView = !_uiState.value.isGridView)
    }

    fun setSection(section: AppSection) {
        _uiState.value = _uiState.value.copy(currentSection = section)
    }

    fun saveClip(clip: VideoClip) {
        viewModelScope.launch { repository.insertClip(clip) }
    }

    fun deleteClip(clip: VideoClip) {
        viewModelScope.launch { repository.deleteClip(clip) }
    }

    fun deleteMediaItem(item: MediaItem) {
        viewModelScope.launch { repository.deleteMediaFile(item) }
    }

    fun updateClipTags(clipId: Long, tags: List<String>) {
        viewModelScope.launch { repository.updateClipTags(clipId, tags) }
    }

    fun toggleClipTag(tag: String) {
        val current = _uiState.value.selectedClipTags.toMutableList()
        if (current.contains(tag)) current.remove(tag) else current.add(tag)
        _uiState.value = _uiState.value.copy(selectedClipTags = current)
        updateFilteredMedia(scrollClips = true)
    }

    fun clearClipTagFilters() {
        _uiState.value = _uiState.value.copy(selectedClipTags = emptyList())
        updateFilteredMedia(scrollClips = true)
    }

    fun toggleShuffle() {
        val enabling = !_uiState.value.isShuffled
        if (enabling) {
            shuffleOrder = _uiState.value.allMedia.map { it.id }.shuffled()
        } else {
            shuffleOrder = emptyList()
        }
        _uiState.value = _uiState.value.copy(isShuffled = enabling)
        updateFilteredMedia(scrollFeed = true)
    }

    fun toggleClipFavorite(clipId: Long) {
        viewModelScope.launch { repository.toggleClipFavorite(clipId) }
    }

    fun toggleFavoriteClipsFilter() {
        _uiState.value = _uiState.value.copy(filterFavoriteClips = !_uiState.value.filterFavoriteClips)
        updateFilteredMedia(scrollClips = true)
    }

    fun setSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        updateFilteredMedia(scrollFeed = true, scrollClips = true)
    }

    fun setDrawerPeopleSearch(query: String) {
        _uiState.value = _uiState.value.copy(drawerPeopleSearch = query)
    }

    fun reportSeekPosition(key: String, positionMs: Long) {
        val current = _uiState.value.seekTokens[key]
        val newToken = (current?.first ?: 0) + 1
        _uiState.value = _uiState.value.copy(
            seekTokens = _uiState.value.seekTokens + (key to (newToken to positionMs))
        )
    }

    fun clearSeekToken(key: String) {
        _uiState.value = _uiState.value.copy(
            seekTokens = _uiState.value.seekTokens - key
        )
    }

    fun reportSeekOnlyPosition(key: String, positionMs: Long) {
        val current = _uiState.value.seekOnlyTokens[key]
        val newToken = (current?.first ?: 0) + 1
        _uiState.value = _uiState.value.copy(
            seekOnlyTokens = _uiState.value.seekOnlyTokens + (key to (newToken to positionMs))
        )
    }

    fun clearSeekOnlyToken(key: String) {
        _uiState.value = _uiState.value.copy(
            seekOnlyTokens = _uiState.value.seekOnlyTokens - key
        )
    }

    fun toggleSelection(id: Long) {
        val current = _uiState.value.selectedIds.toMutableSet()
        if (id in current) current.remove(id) else current.add(id)
        _uiState.value = _uiState.value.copy(
            selectedIds = current,
            isSelectionMode = current.isNotEmpty()
        )
    }

    fun clearSelection() {
        _uiState.value = _uiState.value.copy(selectedIds = emptySet(), isSelectionMode = false)
    }

    fun deleteSelectedMediaItems() {
        viewModelScope.launch {
            val toDelete = _uiState.value.allMedia.filter { it.id in _uiState.value.selectedIds }
            toDelete.forEach { repository.deleteMediaFile(it) }
            _uiState.value = _uiState.value.copy(selectedIds = emptySet(), isSelectionMode = false)
        }
    }

    fun batchAddTags(tags: List<String>) {
        viewModelScope.launch {
            _uiState.value.selectedIds.forEach { id ->
                val item = _uiState.value.allMedia.firstOrNull { it.id == id } ?: return@forEach
                repository.updateTags(id, (item.tags + tags).distinct())
            }
        }
    }

    fun setMediaSortOrder(order: MediaSortOrder) {
        shuffleOrder = emptyList()
        _uiState.value = _uiState.value.copy(mediaSortOrder = order, isShuffled = false)
        updateFilteredMedia(scrollFeed = true)
    }

    fun setClipSortOrder(order: ClipSortOrder) {
        _uiState.value = _uiState.value.copy(clipSortOrder = order)
        updateFilteredMedia(scrollClips = true)
    }

    fun batchAddPeople(people: List<String>) {
        viewModelScope.launch {
            _uiState.value.selectedIds.forEach { id ->
                val item = _uiState.value.allMedia.firstOrNull { it.id == id } ?: return@forEach
                repository.updatePeople(id, (item.people + people).distinct())
            }
        }
    }

    // ── Vault ("Cofre") ─────────────────────────────────────────────────────────

    fun setVaultMode(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(
            vaultMode = enabled,
            vaultInitialized = VaultManager.isInitialized(context),
            vaultBiometricEnabled = VaultManager.isBiometricEnabled(context),
            selectedIds = emptySet(),
            isSelectionMode = false,
            currentSection = AppSection.GALLERY,
            currentlyPlayingUri = null
        )
        shuffleOrder = emptyList()
        updateFilteredMedia(scrollFeed = true, scrollClips = true)
    }

    fun vaultSetupPassword(password: String) {
        if (password.length < 4) { setError("Use pelo menos 4 caracteres"); return }
        viewModelScope.launch {
            withContext(Dispatchers.IO) { VaultManager.setupPassword(context, password.toCharArray()) }
            onVaultUnlocked()
        }
    }

    fun vaultUnlockPassword(password: String) {
        viewModelScope.launch {
            val ok = withContext(Dispatchers.IO) { VaultManager.unlockWithPassword(context, password.toCharArray()) }
            if (ok) onVaultUnlocked() else setError("Senha incorreta")
        }
    }

    private fun onVaultUnlocked() {
        // Decrypt everything first (gate stays up showing progress) so the feed never
        // triggers a blocking decrypt on the main thread once it becomes visible.
        _uiState.value = _uiState.value.copy(
            vaultInitialized = true,
            vaultBiometricEnabled = VaultManager.isBiometricEnabled(context),
            vaultBusy = true,
            vaultBusyMessage = "Descriptografando..."
        )
        viewModelScope.launch {
            withContext(Dispatchers.IO) { VaultSession.decryptAll() }
            _uiState.value = _uiState.value.copy(vaultUnlocked = true, vaultBusy = false, vaultBusyMessage = null)
            updateFilteredMedia(scrollFeed = true, scrollClips = true)
        }
    }

    // Auto-lock when the app is backgrounded, except across an intentional picker launch
    // (the system file picker backgrounds us too).
    private var suppressAutoLock = false

    fun markVaultPickerLaunch() { suppressAutoLock = true }

    fun onAppBackgrounded() {
        if (!suppressAutoLock && _uiState.value.vaultUnlocked) vaultLock()
    }

    fun onAppForegrounded() { suppressAutoLock = false }

    fun vaultLock() {
        VaultManager.lock(context)
        VaultSession.clear()
        _uiState.value = _uiState.value.copy(vaultUnlocked = false, currentlyPlayingUri = null)
        updateFilteredMedia()
    }

    fun importToVault(uris: List<Uri>) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(vaultBusy = true, vaultBusyMessage = "Protegendo mídia...")
            var failed = 0
            uris.forEachIndexed { i, uri ->
                _uiState.value = _uiState.value.copy(vaultBusyMessage = "Protegendo ${i + 1}/${uris.size}...")
                if (!repository.importToVault(uri)) failed++
            }
            _uiState.value = _uiState.value.copy(vaultBusy = false, vaultBusyMessage = null)
            if (failed > 0) setError("$failed item(s) não puderam ser protegidos")
        }
    }

    fun restoreFromVault(item: MediaItem) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(vaultBusy = true, vaultBusyMessage = "Restaurando...")
            val ok = repository.restoreFromVault(item)
            _uiState.value = _uiState.value.copy(vaultBusy = false, vaultBusyMessage = null)
            if (!ok) setError("Falha ao restaurar")
        }
    }

    fun restoreSelectedFromVault() {
        viewModelScope.launch {
            val items = _uiState.value.allMedia.filter { it.id in _uiState.value.selectedIds && it.encrypted }
            _uiState.value = _uiState.value.copy(vaultBusy = true, vaultBusyMessage = "Restaurando...")
            items.forEach { repository.restoreFromVault(it) }
            _uiState.value = _uiState.value.copy(
                vaultBusy = false, vaultBusyMessage = null,
                selectedIds = emptySet(), isSelectionMode = false
            )
        }
    }

    fun vaultBiometricEncryptCipher(): Cipher? =
        runCatching { VaultManager.biometricEncryptCipher() }.getOrNull()

    fun vaultBiometricDecryptCipher(): Cipher? = VaultManager.biometricDecryptCipher(context)

    fun vaultCompleteEnableBiometric(cipher: Cipher) {
        runCatching { VaultManager.enableBiometric(context, cipher) }
            .onFailure { setError("Não foi possível ativar biometria") }
        _uiState.value = _uiState.value.copy(vaultBiometricEnabled = VaultManager.isBiometricEnabled(context))
    }

    fun vaultCompleteBiometricUnlock(cipher: Cipher) {
        if (VaultManager.unlockWithBiometric(context, cipher)) onVaultUnlocked()
        else setError("Falha na biometria")
    }

}
