# 📚 SAF TreeUri - Documentação Técnica

## O que é SAF TreeUri?

**SAF** = Storage Access Framework (Framework de Acesso a Armazenamento)
**TreeUri** = Uma URI que representa uma árvore de pastas (não apenas um arquivo)

É o mecanismo que permite acesso persistente a pastas inteiras sem pedir permissão toda vez.

## Por que usamos no seu app?

Seu objetivo:
- ✅ App quer acessar a Pasta Segura continuamente
- ✅ Sem pedir permissão toda vez que abre
- ✅ Sem duplicar os arquivos
- ✅ Acessar os arquivos originais via URI

SAF TreeUri resolve todos esses pontos.

## Como Funciona (3 passos)

### 1️⃣ User seleciona a pasta

```kotlin
// No MainActivity
folderPickerLauncher = registerForActivityResult(
    ActivityResultContracts.OpenDocumentTree()  // ← Abre seletor de pastas
) { uri ->
    uri?.let { folderUri ->
        // folderUri = "content://com.android.externalstorage.documents/tree/primary%3ADCIM"
    }
}

// Ativar
folderPickerLauncher.launch(null)
```

O que acontece:
- Abre um seletor nativo do Android
- User navega e seleciona uma pasta
- Retorna uma `TreeUri`

### 2️⃣ App persiste a permissão

```kotlin
contentResolver.takePersistableUriPermission(
    folderUri,
    Intent.FLAG_GRANT_READ_URI_PERMISSION  // Permissão READ
)
```

O que faz:
- Pede ao sistema para "lembrar" dessa permissão
- Persiste por ~24 horas (dependendo do Android)
- Próximas vezes que abrir, não precisa pedir
- A permissão fica em `contentResolver.persistedUriPermissions`

### 3️⃣ App acessa os arquivos na pasta

```kotlin
// Abrir a "árvore" da pasta
val treeUri = folderUri
val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(
    treeUri,
    DocumentsContract.getTreeDocumentId(treeUri)
)

// Query para listar tudo na pasta
contentResolver.query(
    childrenUri,
    arrayOf(
        DocumentsContract.Document.COLUMN_DOCUMENT_ID,
        DocumentsContract.Document.COLUMN_DISPLAY_NAME,
        DocumentsContract.Document.COLUMN_MIME_TYPE
    ),
    null, null, null
)?.use { cursor ->
    while (cursor.moveToNext()) {
        val id = cursor.getString(0)
        val name = cursor.getString(1)
        val mime = cursor.getString(2)
        
        // Construir URI do arquivo individual
        val fileUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, id)
        // fileUri = "content://com.android.externalstorage.documents/document/primary%3ADCIM%2FCamera%2Fvideo.mp4"
    }
}
```

O que faz:
- Usa a TreeUri para listar todos os documentos
- Para cada arquivo, constrói uma URI individual
- Essas URIs podem ser acessadas qualquer hora (com a permissão persistida)

## URI vs TreeUri

```
TreeUri (Pasta):
content://com.android.externalstorage.documents/tree/primary%3ADCIM

DocumentUri (Arquivo específico):
content://com.android.externalstorage.documents/document/primary%3ADCIM%2FCamera%2Fvideo.mp4
```

### TreeUri é como um "atalho mestre"
- Você a usa para listar conteúdo
- Cada item listado tem sua própria DocumentUri

### DocumentUri é específico de um arquivo
- Você usa para ler/escrever o arquivo
- É construído usando a TreeUri como base

## Segurança

```
┌─────────────────────────────────────┐
│   User seleciona pasta manualmente  │
│   (via seletor do sistema)          │
└──────────────┬──────────────────────┘
               │
               ▼
┌─────────────────────────────────────┐
│   Sistema autoriza app para essa    │
│   pasta (permissão persistida)      │
└──────────────┬──────────────────────┘
               │
               ▼
┌─────────────────────────────────────┐
│   App acessa APENAS aquela pasta    │
│   (não tem acesso ao resto)         │
└─────────────────────────────────────┘
```

User está no controle:
- Ele escolhe qual pasta compartilhar
- App não consegue acessar outras pastas
- Seguro mesmo para Pasta Segura

## Persistência (Tempo de vida da permissão)

```kotlin
// Salvar manualmente em SharedPreferences
val prefs = context.getSharedPreferences("saf", Context.MODE_PRIVATE)
prefs.edit().putString("folder_uri", folderUri.toString()).apply()

// Próxima vez que app abre
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    val savedUri = prefs.getString("folder_uri", null)?.toUri()
    
    // Verificar se permissão ainda existe
    if (savedUri != null && hasUriPermission(savedUri)) {
        repository.scanMediaFromFolder(savedUri)
    } else {
        // Pedir novamente
        folderPickerLauncher.launch(null)
    }
}
```

## Acessar um arquivo específico

```kotlin
// Você tem a DocumentUri
val fileUri = Uri.parse("content://com.android.externalstorage.documents/document/primary%3ADCIM%2FCamera%2Fvideo.mp4")

// Ler arquivo
val inputStream = contentResolver.openInputStream(fileUri)
val bitmap = BitmapFactory.decodeStream(inputStream)

// Ou use Coil (no seu app)
AsyncImage(
    model = fileUri,  // ← Coil entende DocumentUri direto
    contentDescription = "Vídeo"
)
```

## Limitações

### ⚠️ SAF TreeUri NÃO é infinito
- Permissão persiste ~24 horas
- Depois disso, precisa pedir novamente
- SOLUÇÃO: Mostrar um snackbar pedindo re-autenticação

### ⚠️ NÃO lista subpastas
- `buildChildDocumentsUriUsingTree` lista APENAS a pasta raiz
- Se quiser recursivar, precisa iterar manualmente
- SOLUÇÃO: No seu app, fazer query pra cada subpasta

### ⚠️ Performance com muitos arquivos
- Query em pasta com 10k+ arquivos pode ser lenta
- SOLUÇÃO: Paginar (1000 por página), mostrar loading

## Exemplo Completo no seu App

```kotlin
// 1. User seleciona
folderPickerLauncher = registerForActivityResult(
    ActivityResultContracts.OpenDocumentTree()
) { uri ->
    uri?.let { viewModel.selectFolder(it) }
}

// 2. ViewModel persiste e scanneia
fun selectFolder(folderUri: Uri) {
    viewModelScope.launch {
        // Persistir
        prefs.edit().putString("folder_uri", folderUri.toString()).apply()
        
        // Dar permissão persistente
        contentResolver.takePersistableUriPermission(
            folderUri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION
        )
        
        // Scanear
        repository.scanMediaFromFolder(folderUri)
    }
}

// 3. Repository lista e armazena
suspend fun scanMediaFromFolder(folderUri: Uri) {
    val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(
        folderUri,
        DocumentsContract.getTreeDocumentId(folderUri)
    )
    
    contentResolver.query(childrenUri, ...)?.use { cursor ->
        while (cursor.moveToNext()) {
            val id = cursor.getString(0)
            val fileUri = DocumentsContract.buildDocumentUriUsingTree(folderUri, id)
            
            // Armazenar no banco
            mediaItemDao.insert(MediaItem(
                uri = fileUri.toString(),
                ...
            ))
        }
    }
}

// 4. UI acessa os arquivos
AsyncImage(
    model = mediaItem.uri,  // ← URI armazenada no banco
    contentDescription = mediaItem.fileName
)
```

## Comparação: SAF vs Outras Abordagens

| Abordagem | Vantagem | Desvantagem |
|-----------|----------|------------|
| **SAF TreeUri** (seu app) | Acesso persistente, user controla, seguro | Permissão expira em 24h |
| SAF File-by-file | Simples | Pede permissão toda vez |
| READ_EXTERNAL_STORAGE | Rápido | Acesso a TUDO, não específico |
| Copiar files | Controle total | Duplica espaço, lento |
| DocumentFile wrapper | Simpler API | Ainda SAF por baixo |

## Links Úteis

- [SAF Documentation](https://developer.android.com/guide/topics/providers/document-provider)
- [DocumentsContract API](https://developer.android.com/reference/android/provider/DocumentsContract)
- [ActivityResultContracts](https://developer.android.com/reference/androidx/activity/result/contract/ActivityResultContracts.OpenDocumentTree)

---

**No seu app, SAF TreeUri é perfeito porque:**

1. ✅ Você quer acesso contínuo à pasta
2. ✅ User controla qual pasta compartilhar
3. ✅ Funciona na Pasta Segura
4. ✅ Não duplica arquivos (apenas armazena URIs)
5. ✅ Seguro e alinhado com privacy guidelines do Android

**Implementamos exatamente isso em `MediaRepository.kt` para você!**
