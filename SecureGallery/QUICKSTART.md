# ⚡ Quick Start - Secure Gallery

## 1️⃣ Setup (5 min)

```bash
# Abra em Android Studio
cd ~/Documentos/SecureGallery
open -a "Android Studio" . # Mac
# ou abra manualmente o Android Studio e abra a pasta

# Espere Gradle sincronizar (vai baixar dependências)
# Isso pode levar 2-3 min na primeira vez
```

## 2️⃣ Configurar Device

```bash
# Conecte seu telefone Samsung
# Ative Mode de desenvolvedor: Configurações > Sobre > Numero de versão (clique 7x)
# Ative USB Debugging: Configurações > Opções de desenvolvedor > USB Debugging

# Confirme conexão:
adb devices
# Deve listar seu dispositivo
```

## 3️⃣ Build & Run

```bash
# No Android Studio:
# Run > Run 'app'
# Ou pressione Shift + F10 (Windows/Linux) ou Control + R (Mac)

# Ou via terminal:
./gradlew installDebug
adb shell am start -n com.example.securegallery/.MainActivity
```

## 4️⃣ Usar o App

1. **Primeira vez:**
   - Clique "Pasta" no topo
   - Navegue até sua Pasta Segura do Samsung
   - Selecione a pasta com fotos/vídeos
   - App vai scanear automaticamente

2. **Adicionar tags:**
   - Clique num vídeo no grid
   - Aparece um modal para editar tags
   - Digite tags separadas por Enter
   - Clique "Salvar"

3. **Marcar pessoas:**
   - Clique num vídeo
   - Vá para "Editar Pessoas"
   - Digite o nome da pessoa
   - Próximos vídeos você vai sugerir os nomes

4. **Filtrar:**
   - **Pessoa:** Clique em qualquer nome na barra azul
   - **Tags:** Clique em qualquer tag na barra roxa
   - **Limpar:** Clique "Limpar Filtros"

## 🗂️ Estrutura do Código

### Camada de Dados (`data/`)
```
MediaItem.kt         → Modelo (id, uri, tags, people)
MediaItemDao.kt      → Queries SQL
AppDatabase.kt       → Banco Room
MediaRepository.kt   → SAF + lógica
```

### Camada de UI (`ui/`)
```
GalleryViewModel.kt       → Estado (Redux-like)
components/
  ├── Dialogs.kt          → Tag/People editors
  └── MediaComponents.kt  → Grid, cards, filters
theme/Theme.kt            → Design tokens
```

### Activity
```
MainActivity.kt  → SAF picker + setContent()
GalleryScreen() → Root Composable
```

## 🔌 SAF TreeUri (O que faz a mágica)

```kotlin
// 1. User seleciona pasta
folderPickerLauncher.launch(null)

// 2. App persiste a permissão
contentResolver.takePersistableUriPermission(
    folderUri,
    Intent.FLAG_GRANT_READ_URI_PERMISSION
)

// 3. App tem acesso contínuo (sem pedir toda vez)
repository.scanMediaFromFolder(folderUri)

// 4. Banco local armazena:
// uri_hash, fileName, tags, people, etc
```

## 🐛 Debug

```bash
# Ver logs em tempo real
adb logcat | grep "SecureGallery"

# Acessar banco de dados
adb shell
run-as com.example.securegallery
cat databases/secure_gallery_database

# Limpar dados do app
adb shell pm clear com.example.securegallery

# Reinstalar
adb uninstall com.example.securegallery
./gradlew installDebug
```

## ⚙️ Personalizações Fáceis

### Mudar cor primária
Abra `Theme.kt`:
```kotlin
primary = Color(0xFF6200EE)  // Mude para sua cor
```

### Adicionar mais campos
1. Abra `MediaItem.kt`
2. Adicione o campo (ex: `var rating: Int = 0`)
3. Atualize `MediaRepository.kt` com o novo campo

### Mudar textos
Abra `strings.xml` e edite valores

## 📱 Samsung Secure Folder Específico

- App instalado dentro da Pasta Segura tem acesso à galeria dela via SAF
- Permissão persist por ~24 horas
- Você pode re-selecionar a pasta qualquer hora
- Dados nunca sai do Secure Folder

## 🎓 Para Aprender Mais

Arquivos com comentários:
- `MainActivity.kt` - SAF + Compose
- `MediaRepository.kt` - DocumentsContract API
- `GalleryViewModel.kt` - State management

Documentação oficial:
- [Storage Access Framework](https://developer.android.com/guide/topics/providers/document-provider)
- [Jetpack Compose](https://developer.android.com/jetpack/compose)
- [Room Database](https://developer.android.com/training/data-storage/room)

## ✅ Checklist de Configuração

- [ ] Android Studio aberto
- [ ] Gradle sincronizado (sem erros)
- [ ] Device conectado (adb devices mostra)
- [ ] USB Debugging ativado
- [ ] App roda sem erros
- [ ] Consegue selecionar pasta
- [ ] Vídeos aparecem no grid
- [ ] Consegue adicionar tags
- [ ] Filtros funcionam

## 🚀 Próximos Passos Sugeridos

1. **Entenda o flow:**
   - MainActivity.kt → GalleryScreen() → MediaGrid()

2. **Experimente em código:**
   - Mude cores em Theme.kt
   - Adicione um novo campo em MediaItem.kt

3. **Teste bem:**
   - Adicione 10+ vídeos
   - Crie 3-4 tags diferentes
   - Marque 2-3 pessoas
   - Teste combinar filtros

4. **Deploy:**
   - Build signed APK: Build → Generate Signed Bundle
   - Distribua para quem quiser usar

---

**Dúvidas? Revise o README.md completo!**
