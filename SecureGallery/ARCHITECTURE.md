# 🎬 Secure Gallery - Overview Completo

## 📊 Arquitetura

```
┌────────────────────────────────────────────────────────┐
│                   ANDROID OS                            │
│  ┌──────────────────────────────────────────────────┐  │
│  │          STORAGE ACCESS FRAMEWORK (SAF)          │  │
│  │  ┌──────────────────────────────────────────┐   │  │
│  │  │      Pasta Segura Samsung                │   │  │
│  │  │  ├── video1.mp4                          │   │  │
│  │  │  ├── video2.mp4                          │   │  │
│  │  │  ├── foto1.jpg                           │   │  │
│  │  │  └── foto2.jpg                           │   │  │
│  │  └──────────────────────────────────────────┘   │  │
│  └──────────────────────────────────────────────────┘  │
│                        △                                 │
│                        │ (TreeUri)                      │
│  ┌────────────────────┴──────────────────────────────┐ │
│  │              SECURE GALLERY APP                   │ │
│  │                                                   │ │
│  │  ┌──────────────────────────────────────────┐   │ │
│  │  │        MainActivity (SAF Picker)         │   │ │
│  │  │  - Seleciona pasta com ACTION_OPEN_TREE │   │ │
│  │  │  - Persiste permissão com               │   │ │
│  │  │    takePersistableUriPermission          │   │ │
│  │  └──────────────────┬───────────────────────┘   │ │
│  │                     │                             │ │
│  │  ┌──────────────────▼───────────────────────┐   │ │
│  │  │     MediaRepository (SAF Access)        │   │ │
│  │  │  - scanMediaFromFolder(uri)             │   │ │
│  │  │  - Query DocumentsContract              │   │ │
│  │  │  - Construir DocumentUri pra cada file │   │ │
│  │  │  - Armazenar URI no banco               │   │ │
│  │  └──────────────────┬───────────────────────┘   │ │
│  │                     │                             │ │
│  │  ┌──────────────────▼───────────────────────┐   │ │
│  │  │     AppDatabase (Room ORM)               │   │ │
│  │  │  ┌─────────────────────────────────┐   │   │ │
│  │  │  │  MediaItem                      │   │   │ │
│  │  │  │  - id (PK)                      │   │   │ │
│  │  │  │  - uri (DocumentUri)            │   │   │ │
│  │  │  │  - fileName                     │   │   │ │
│  │  │  │  - tags (serializado)           │   │   │ │
│  │  │  │  - people (serializado)         │   │   │ │
│  │  │  │  - dateAdded                    │   │   │ │
│  │  │  │  - notes                        │   │   │ │
│  │  │  └─────────────────────────────────┘   │   │ │
│  │  │  ┌─────────────────────────────────┐   │   │ │
│  │  │  │  secure_gallery_database.db     │   │   │ │
│  │  │  │  (SQLite com encriptação)       │   │   │ │
│  │  │  └─────────────────────────────────┘   │   │ │
│  │  └──────────────────────────────────────────┘   │ │
│  │                                                   │ │
│  │  ┌──────────────────────────────────────────┐   │ │
│  │  │   GalleryViewModel (State Management)   │   │ │
│  │  │  - allMedia: List<MediaItem>            │   │ │
│  │  │  - filteredMedia: List<MediaItem>       │   │ │
│  │  │  - selectedPerson: String?              │   │ │
│  │  │  - selectedTags: List<String>           │   │ │
│  │  │  - allPeople: List<String>              │   │ │
│  │  │  - allTags: List<String>                │   │ │
│  │  │  - Methods: selectPerson(), toggleTag() │   │ │
│  │  │            updateMediaTags(), etc       │   │ │
│  │  └──────────────────────────────────────────┘   │ │
│  │                                                   │ │
│  │  ┌──────────────────────────────────────────┐   │ │
│  │  │   Jetpack Compose UI Layer               │   │ │
│  │  │  ┌──────────────────────────────────┐   │   │ │
│  │  │  │ GalleryScreen (Root)             │   │   │ │
│  │  │  │ ├── TopAppBar (Título + Botões)  │   │   │ │
│  │  │  │ ├── FilterBar (Pessoas + Tags)   │   │   │ │
│  │  │  │ │   ├── PeopleChip (selecionar)  │   │   │ │
│  │  │  │ │   └── TagFilterChip (selecion) │   │   │ │
│  │  │  │ ├── MediaGrid (2 colunas)        │   │   │ │
│  │  │  │ │   ├── MediaCard                │   │   │ │
│  │  │  │ │   │   ├── AsyncImage (thumb)   │   │   │ │
│  │  │  │ │   │   ├── Badge (vídeo?)       │   │   │ │
│  │  │  │ │   │   └── Info (tags + people) │   │   │ │
│  │  │  │ │   └── [...]                    │   │   │ │
│  │  │  │ ├── TagEditorDialog              │   │   │ │
│  │  │  │ │   ├── Lista tags selecionadas  │   │   │ │
│  │  │  │ │   ├── Adicionar nova tag       │   │   │ │
│  │  │  │ │   └── Sugestões de tags       │   │   │ │
│  │  │  │ └── PeopleEditorDialog           │   │   │ │
│  │  │  │     ├── Lista pessoas            │   │   │ │
│  │  │  │     ├── Adicionar nova pessoa    │   │   │ │
│  │  │  │     └── Sugestões de pessoas     │   │   │ │
│  │  │  └──────────────────────────────────┘   │   │ │
│  │  └──────────────────────────────────────────┘   │ │
│  │                                                   │ │
│  └───────────────────────────────────────────────────┘ │
└────────────────────────────────────────────────────────┘
```

## 🔄 Flow de Dados

### 1. Primeira Abertura

```
User abre app
    ↓
MainActivity onCreate
    ↓
setContent { GalleryScreen() }
    ↓
GalleryViewModel init
    ↓
setupDatabase()
    ├── AppDatabase.getDatabase()
    ├── mediaItemDao()
    └── MediaRepository init
    ↓
loadAllData()
    ├── repository.getAllMediaItems() → Flow
    ├── repository.getAllTags() → Flow
    └── repository.getAllPeople() → Flow
    ↓
GalleryScreen renderiza com uiState.empty
    ↓
User clica "Selecionar Pasta"
```

### 2. Selecionar Pasta

```
User clica "Pasta"
    ↓
folderPickerLauncher.launch(null)
    ↓
[Abre Android seletor de pastas]
    ↓
User navega até Pasta Segura
    ↓
User seleciona uma pasta
    ↓
Callback: onActivityResult { uri }
    ↓
contentResolver.takePersistableUriPermission(uri)
    ↓
viewModel.selectFolder(uri)
    ↓
repository.scanMediaFromFolder(uri)
    ├── DocumentsContract.buildChildDocumentsUriUsingTree()
    ├── contentResolver.query(childrenUri)
    ├── Para cada arquivo:
    │   └── mediaItemDao.insertAll(MediaItem)
    └── mediaItemDao retorna List<MediaItem>
    ↓
Flow reemite novos dados
    ↓
GalleryScreen renderiza grid com vídeos
```

### 3. Adicionar Tags a um Vídeo

```
User clica num card
    ↓
onItemClick { mediaItem }
    ↓
editingItem = mediaItem
showTagEditor = true
    ↓
TagEditorDialog abre
    ├── Mostra tags atuais
    ├── User digita nova tag
    └── User clica "Salvar"
    ↓
onConfirm { tags }
    ↓
viewModel.updateMediaTags(itemId, tags)
    ↓
repository.updateTags(itemId, tags)
    ├── mediaItemDao.getMediaItemById(itemId)
    ├── item.copy(tags = tags)
    └── mediaItemDao.updateMediaItem(item)
    ↓
Room Flow reemite dados atualizados
    ↓
GalleryScreen re-renderiza
    ├── Atualiza allTags
    └── Atualiza card com novas tags
```

### 4. Filtrar por Pessoa

```
User clica em "João" na FilterBar
    ↓
onPersonSelected("João")
    ↓
viewModel.selectPerson("João")
    ├── _uiState.update { copy(selectedPerson = "João") }
    └── updateFilteredMedia()
    ↓
updateFilteredMedia()
    ├── allMedia = _uiState.value.allMedia
    ├── Filtra { item.people.contains("João") }
    └── _uiState.update { copy(filteredMedia = filtered) }
    ↓
GalleryScreen coleta novo uiState
    ↓
MediaGrid re-renderiza com filtrado
```

### 5. Filtrar por Pessoa + Tag

```
User já tem "João" selecionado
User clica em tag "praia"
    ↓
viewModel.toggleTag("praia")
    ├── selectedTags += "praia"
    └── updateFilteredMedia()
    ↓
updateFilteredMedia()
    ├── allMedia = _uiState.value.allMedia
    ├── Filtra:
    │   ├── item.people.contains("João") AND
    │   └── item.tags.contains("praia")
    └── _uiState.update { copy(filteredMedia = filtered) }
    ↓
GalleryScreen renderiza resultado
```

## 📂 Estrutura de Pastas

```
SecureGallery/
├── .gitignore
├── build.gradle.kts                           # Root Gradle
├── settings.gradle.kts                        # Módulos
├── README.md                                  # Documentação
├── QUICKSTART.md                              # Start rápido
├── SAF_TREEURI_GUIDE.md                       # Tech detail
│
└── app/
    ├── build.gradle.kts                       # App dependencies
    ├── proguard-rules.pro                     # Obfuscation rules
    │
    ├── src/main/
    │   ├── AndroidManifest.xml                # Permissions + queries
    │   │
    │   ├── java/com/example/securegallery/
    │   │   ├── MainActivity.kt                 # SAF + setContent
    │   │   │
    │   │   ├── data/
    │   │   │   ├── MediaItem.kt               # @Entity
    │   │   │   ├── MediaItemDao.kt            # @Dao
    │   │   │   ├── AppDatabase.kt             # @Database (Room)
    │   │   │   └── MediaRepository.kt         # SAF logic
    │   │   │
    │   │   └── ui/
    │   │       ├── GalleryViewModel.kt        # State (MVVM)
    │   │       ├── theme/
    │   │       │   └── Theme.kt               # Material 3
    │   │       └── components/
    │   │           ├── Dialogs.kt             # TagEditor, PeopleEditor
    │   │           └── MediaComponents.kt     # Grid, Cards, Filters
    │   │
    │   └── res/
    │       ├── values/
    │       │   ├── strings.xml
    │       │   └── themes.xml
    │       └── [assets, colors, etc]
    │
    └── [build artifacts, generated]
```

## 🧠 Conceitos-Chave

### SAF TreeUri (Storage Access Framework)
- **O problema:** App precisa acessar pasta sem pedir permissão toda vez
- **A solução:** User seleciona pasta uma vez → permissão persiste 24h
- **Como:** `DocumentsContract.buildChildDocumentsUriUsingTree()`

### Room Database
- **O problema:** Armazenar metadados (tags, people, etc)
- **A solução:** SQLite com ORM automático
- **Como:** `@Entity`, `@Dao`, `@Database`

### StateFlow + ViewModel (Redux Pattern)
- **O problema:** UI precisa refletir estado complexo
- **A solução:** Single source of truth em ViewModel
- **Como:** `MutableStateFlow<UiState>` + `.asStateFlow()` read-only

### Jetpack Compose
- **O problema:** XML layouts são verbosos e frágeis
- **A solução:** Declarative UI em Kotlin
- **Como:** `@Composable fun GalleryScreen() { ... }`

### Coroutines
- **O problema:** SAF queries e DB access bloqueiam thread
- **A solução:** async/await com `suspend` e `launch`
- **Como:** `viewModelScope.launch { ... }`

## 🔐 Segurança & Privacy

```
┌──────────────────────────────────────────┐
│   User Control                           │
│  User escolhe qual pasta compartilhar   │
│  User pode revogar qualquer hora        │
└──────────────────────────────────────────┘

┌──────────────────────────────────────────┐
│   Scope Isolation                        │
│  App acessa APENAS pasta selecionada    │
│  Não consegue ler outras pastas         │
│  Não consegue acessar system files      │
└──────────────────────────────────────────┘

┌──────────────────────────────────────────┐
│   Local Storage                          │
│  Dados NUNCA saem do dispositivo        │
│  NUNCA enviado pra servidor             │
│  Funciona completamente offline         │
└──────────────────────────────────────────┘

┌──────────────────────────────────────────┐
│   Database Encryption                    │
│  Room pode usar EncryptedSharedPreferences
│  Backup integrado com Android            │
│  [Opcionalmente SQLCipher para DB]      │
└──────────────────────────────────────────┘
```

## 🚀 Performance

### Rendering
- **Virtual Scrolling:** Compose renderiza APENAS cards visíveis
- **Lazy Evaluation:** Queries carregam dados sob demanda
- **Coroutines:** UI nunca bloqueia

### Database
- **Indexação:** Automática em `@PrimaryKey`, `@ColumnInfo(index=true)`
- **Paginação:** Implementável com `LIMIT` + `OFFSET`
- **Caching:** Flow coletado mantém dados em memória

### Media Loading
- **Coil:** Lib moderna com cache automático de bitmaps
- **Background:** Thumbnails carregam em thread separada
- **Fallbacks:** Mostra placeholder até imagem carregar

## 🎯 Extensões Futuras (Fáceis)

### Adicionar Campo Novo
```kotlin
// 1. MediaItem.kt
@Entity
data class MediaItem(
    ...
    val rating: Int = 0  // ← Novo campo
)

// 2. Repository.kt
suspend fun updateRating(itemId: Long, rating: Int) {
    val item = mediaItemDao.getMediaItemById(itemId) ?: return
    mediaItemDao.updateMediaItem(item.copy(rating = rating))
}

// 3. ViewModel.kt
fun updateMediaRating(itemId: Long, rating: Int) {
    viewModelScope.launch {
        repository.updateRating(itemId, rating)
    }
}

// 4. UI - Adicionar UI pra rating
// (e.g., RatingBar no MediaCard)
```

### Adicionar Novo Filtro
```kotlin
// 1. ViewModel
var selectedMediaType: String? = null

// 2. updateFilteredMedia()
val filtered = allMedia.filter { item ->
    val typeMatch = selectedMediaType?.let { item.mediaType == it } ?: true
    // ... + outros filtros
    typeMatch && personMatch && tagsMatch
}

// 3. UI - Adicionar buttons
Button(onClick = { selectMediaType("video") }) { Text("Vídeos") }
Button(onClick = { selectMediaType("image") }) { Text("Fotos") }
```

### Exportar para CSV
```kotlin
fun exportToCsv(): String {
    val header = "ID,Arquivo,Pessoas,Tags,Data\n"
    val rows = uiState.value.allMedia.map { item ->
        "${item.id},${item.fileName},${item.people.joinToString("|")},${item.tags.joinToString("|")},${item.dateAdded}"
    }.joinToString("\n")
    return header + rows
}
```

## 📖 Próximos Passos Para Você

1. **Setup:** Abra Android Studio, sincronize Gradle
2. **Build:** Aperte Play para compilar
3. **Test:** Teste SAF picker com pasta real
4. **Customize:** Mude cores em Theme.kt
5. **Extend:** Adicione campos/filtros conforme necessário
6. **Deploy:** Build APK assinado quando pronto

---

**Está pronto para começar! 🚀**
