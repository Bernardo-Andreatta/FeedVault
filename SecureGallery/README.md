# 📱 Secure Gallery

Um app de catálogo de fotos e vídeos para Samsung Secure Folder, com filtros por tags e pessoas.

## 🎯 Funcionalidades

✅ **SAF TreeUri** - Acesso contínuo à pasta sem pedir permissão toda vez
✅ **Grid estilo Reddit** - Interface fluida com scroll infinito
✅ **Tags personalizadas** - Marque vídeos com tags genéricas
✅ **Filtro por pessoa** - Crie "comunidades" para cada pessoa
✅ **Filtros combinados** - Pessoa + tags juntos
✅ **Banco de dados local** - SQLite com Room ORM
✅ **Interface moderna** - Jetpack Compose + Material Design 3

## 📋 Requisitos

- Android 7.0+ (API 24+)
- Android Studio (versão recente)
- Gradle 8.1+
- Kotlin 1.9+

## 🚀 Como começar

### 1. Clonar/Copiar o projeto

```bash
cd ~/Documentos
# (ou seu caminho para Documentos)
```

### 2. Abrir no Android Studio

```bash
# Windows/Mac
open -a "Android Studio" SecureGallery

# Linux
~/Android/Studio/bin/studio.sh SecureGallery &
```

### 3. Build e Run

1. Conecte seu dispositivo Samsung (modo desenvolvedor ativado)
2. Android Studio → Run → Run 'app'
3. Selecione seu dispositivo

Ou use:
```bash
./gradlew build
./gradlew installDebug
```

## 📚 Estrutura do Projeto

```
SecureGallery/
├── app/src/main/
│   ├── java/com/example/securegallery/
│   │   ├── MainActivity.kt          # Activity principal + navegação
│   │   ├── data/
│   │   │   ├── MediaItem.kt         # Modelo de dados
│   │   │   ├── MediaItemDao.kt      # Database queries
│   │   │   ├── AppDatabase.kt       # Room database
│   │   │   └── MediaRepository.kt   # SAF + business logic
│   │   └── ui/
│   │       ├── GalleryViewModel.kt  # Estado da app
│   │       ├── theme/
│   │       │   └── Theme.kt         # Material Design
│   │       └── components/
│   │           ├── Dialogs.kt       # Tag/People editors
│   │           └── MediaComponents.kt # Grid, cards, filters
│   ├── res/
│   │   ├── values/
│   │   │   ├── strings.xml
│   │   │   └── themes.xml
│   │   └── AndroidManifest.xml
│   └── build.gradle.kts
├── build.gradle.kts
└── settings.gradle.kts
```

## 🎮 Como usar

### 1. Primeiro uso

1. Abra o app
2. Clique no botão "Pasta" no topo
3. Selecione a pasta com vídeos/fotos (pode ser na Pasta Segura)
4. O app vai scanear automaticamente

### 2. Adicionar tags

1. Clique num vídeo/foto no grid
2. Digite tags (máximo 5 recomendado)
3. Clique "Salvar"

### 3. Marcar pessoas

1. Clique num vídeo/foto
2. Na aba "Pessoas", digite o nome
3. Para próximos vídeos com a mesma pessoa, selecione da lista

### 4. Filtrar

**Por pessoa:**
- Clique no nome na barra de filtros
- Todos os vídeos daquela pessoa aparecem

**Por tag:**
- Clique em qualquer tag na barra de filtros
- Você pode combinar múltiplas tags (OR logic)

**Por pessoa + tags:**
- Selecione uma pessoa
- Depois selecione tags para filtrar dentro daquela pessoa

## 🔐 Segurança & Privacidade

✅ Usa **Storage Access Framework** (SAF) nativo do Android
✅ Banco de dados criptografado (Android encrypted SharedPrefs)
✅ Nenhum dado é enviado para servidor
✅ Funciona completamente offline
✅ Armazena apenas URIs e metadados localmente

## 🛠️ Tecnologias

- **Kotlin** - Linguagem principal
- **Jetpack Compose** - UI moderna
- **Room Database** - Local data storage
- **SAF TreeUri** - Acesso persistente a pastas
- **Coroutines** - Async/await
- **Coil** - Image loading
- **Material Design 3** - Design tokens

## 🐛 Troubleshooting

### "Permission denied" ao acessar pasta

- Certifique-se que selecionou uma pasta, não um arquivo
- No Secure Folder, a permissão é persistente (24 horas)
- Você pode re-selecionar a pasta a qualquer momento

### Thumbnails não aparecem

- Confirme que mídia foi escaneada corretamente
- Tente re-iniciar o app
- Verifique no Logcat: `adb logcat | grep SecureGallery`

### App lento com muitos vídeos (1000+)

- Compose renderiza apenas itens visíveis (virtual scrolling)
- Database queries são otimizadas com indexação
- Se ainda lento, considere adicionar paginação

## 📖 Exemplos de Código

### Selecionar folder com SAF

```kotlin
folderPickerLauncher = registerForActivityResult(
    ActivityResultContracts.OpenDocumentTree()
) { uri ->
    uri?.let { folderUri ->
        contentResolver.takePersistableUriPermission(
            folderUri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION
        )
        viewModel.selectFolder(folderUri)
    }
}
```

### Filtrar por pessoa + tags

```kotlin
val filtered = allMedia.filter { item ->
    val personMatch = if (selectedPerson != null) {
        item.people.contains(selectedPerson)
    } else true
    
    val tagsMatch = if (selectedTags.isEmpty()) {
        true
    } else {
        selectedTags.any { item.tags.contains(it) }
    }
    
    personMatch && tagsMatch
}
```

### Atualizar tags no banco

```kotlin
suspend fun updateTags(itemId: Long, tags: List<String>) {
    val item = mediaItemDao.getMediaItemById(itemId) ?: return
    mediaItemDao.updateMediaItem(item.copy(tags = tags))
}
```

## 🚧 Roadmap (ideias futuras)

- [ ] Reconhecimento facial automático de pessoas
- [ ] Edição de metadados em batch
- [ ] Exportar catálogo como CSV
- [ ] Backup/Restore de metadados
- [ ] Sincronizar com outro dispositivo via WiFi
- [ ] Dark mode aprimorado
- [ ] Busca por texto/IA

## 📝 Licença

Pessoal - Use como quiser, como quiser!

## 💬 Dúvidas?

Revise os comentários no código - está bem explicado!

---

**Desenvolvido com ❤️ em Kotlin**
