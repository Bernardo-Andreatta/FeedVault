# 🏗️ Build & Deployment Guide

## 📋 Pré-requisitos

- **Android Studio** (versão 2022.3+)
- **JDK 17+** (vem com Android Studio)
- **Android SDK** 34+ instalado
- **Android Emulator** OU **Device real** (Samsung recomendado)
- **USB Debugging ativado** (se usar device real)

## 🔧 Setup Inicial

### 1. Baixar Android Studio

```bash
# Mac
brew install android-studio

# Linux
# Download em: https://developer.android.com/studio

# Windows
# Download em: https://developer.android.com/studio
```

### 2. Instalar SDK (se não tiver)

```bash
# No Android Studio:
# Tools → SDK Manager → SDK Platforms
# ✓ Marque: Android 14 (API 34)
# ✓ Marque: SDK Tools (Gradle, CMake, etc)
# Clique "Apply"
```

### 3. Abrir Projeto

```bash
# Terminal
cd ~/Documentos/SecureGallery
open -a "Android Studio" .  # Mac
# ou abra manualmente no Android Studio

# File → Open → SecureGallery (select folder)
```

### 4. Sync Gradle

Quando abrir projeto:
- Android Studio pedirá sync automático
- Clique "Sync Now"
- Vai baixar todas as dependências (~2-3 min)
- Espere "Gradle sync finished" no console

## ▶️ Build para Debug (Desenvolvimento)

### Via Android Studio (Fácil)

1. Conecte seu telefone Samsung (USB)
2. Ative USB Debugging: Settings > Developer Options > USB Debugging
3. Confira conexão: `adb devices`
4. Android Studio: **Run → Run 'app'**
5. Selecione seu device
6. Aperte Play (▶) ou **Shift + F10** (Win/Linux) / **Control + R** (Mac)

### Via Terminal (Avançado)

```bash
cd SecureGallery

# Build debug APK
./gradlew assembleDebug

# Instalar no device
adb install app/build/outputs/apk/debug/app-debug.apk

# Rodar
adb shell am start -n com.example.securegallery/.MainActivity

# Ver logs
adb logcat -s SecureGallery
```

## 📦 Build para Release (Distribuição)

> ⚠️ Você só precisa fazer isso quando quiser distribuir o app!

### 1. Criar Keystore (uma vez)

```bash
# Gerar chave de assinatura (nunca delete isso!)
keytool -genkey -v -keystore ~/secure_gallery.jks \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000 \
  -alias secure_gallery_key

# Respostas ao prompt:
# Enter keystore password: [digite uma senha segura]
# Re-enter new password: [repita]
# What is your first and last name?: Seu Nome
# ... (resto default, aperte Enter)
# Key password: [mesma senha]

# Guardará em ~/secure_gallery.jks
```

### 2. Configurar Signing no Gradle

Abra `app/build.gradle.kts` e adicione antes de `buildTypes`:

```gradle
signingConfigs {
    release {
        storeFile = file(System.getenv("KEYSTORE_PATH") ?: "~/secure_gallery.jks")
        storePassword = System.getenv("KEYSTORE_PASSWORD")
        keyAlias = "secure_gallery_key"
        keyPassword = System.getenv("KEY_PASSWORD")
    }
}
```

Ou crie `local.properties`:

```properties
KEYSTORE_PATH=/Users/seu_user/secure_gallery.jks
KEYSTORE_PASSWORD=sua_senha_aqui
KEY_PASSWORD=sua_senha_aqui
```

### 3. Build Release APK

```bash
cd SecureGallery

# Build APK assinado
./gradlew assembleRelease

# Resultado em:
# app/build/outputs/apk/release/app-release.apk
```

### 4. Build AAB (Google Play)

Se quiser distribuir na Google Play:

```bash
./gradlew bundleRelease

# Resultado em:
# app/build/outputs/bundle/release/app-release.aab
```

## 📱 Instalar APK Release

```bash
# No seu telefone
adb install app/build/outputs/apk/release/app-release.apk

# Ou via GUI - Android Studio
# Run → Run 'app' → Selecione apk release
```

## 📊 Verificar Build

```bash
# Ver informações do APK
aapt dump badging app/build/outputs/apk/release/app-release.apk

# Ver tamanho
ls -lh app/build/outputs/apk/release/app-release.apk

# Descompactar e explorar
unzip -l app/build/outputs/apk/release/app-release.apk | head -20
```

## 🐛 Troubleshooting Build

### Erro: "Gradle sync failed"

```bash
# Limpar cache Gradle
./gradlew clean

# Atualizar dependências
./gradlew --refresh-dependencies

# Sincronizar novamente
./gradlew build
```

### Erro: "Compilation failed"

```bash
# Verificar versões Kotlin
./gradlew tasks --all | grep kotlin

# Se problema com versão, edite:
# app/build.gradle.kts
# kotlinOptions { jvmTarget = "17" }
```

### Erro: "Device not found"

```bash
# Verificar conexão
adb devices

# Se não aparecer, reinicie:
adb kill-server
adb start-server
adb devices

# Force reconnect
adb disconnect
adb connect <IP_DO_DEVICE>

# Se ainda não funcionar:
# Desconecte USB, reconnecte, autorize em Device
```

### Erro: "Permission denied" (Pasta Segura)

```bash
# Isso é normal na primeira vez
# Você precisa ir em:
# Configurações > Pasta Segura > Permissões > Seu App
# E autorizar acesso a Mídia

# Ou simplesmente deixar SAF picker fazer seu trabalho
# (system pede permissão quando clica em "Selecionar Pasta")
```

## 🚀 Ambiente de Desenvolvimento Recomendado

### Hardware
- **Android Device:** Samsung Galaxy S20+
- **Mínimo:** Android 7.0 (API 24)
- **Recomendado:** Android 12+ (API 31+)

### Software
```bash
# Versões recomendadas
Android Studio: 2023.1+
Gradle: 8.1+
Kotlin: 1.9+
Android SDK: 34 (API Level 34)
JDK: 17+
```

## 📝 Checklist de Build

- [ ] Android Studio instalado
- [ ] SDK API 34 instalado
- [ ] `./gradlew clean` executado
- [ ] Gradle sync finished (sem erros)
- [ ] App compila com `./gradlew build`
- [ ] APK gera em `app/build/outputs/apk/debug/`
- [ ] App instala com `adb install`
- [ ] App roda no device/emulator
- [ ] SAF picker abre quando clica "Pasta"
- [ ] Consegue scanear vídeos

## 🔍 Inspeccionar APK Pronto

```bash
# Listar conteúdo
unzip -l app-release.apk | grep -E "\.so|\.dex|res/"

# Verificar certificado
jarsigner -verify -verbose -certs app-release.apk

# Tamanho de cada componente
ls -lh app/build/outputs/apk/*/
```

## 💾 Distribuir o App

### Opção 1: Transferir APK via USB

```bash
# Copiar APK pro computador
adb pull /data/app/com.example.securegallery*/app-release.apk ~/Desktop/

# Ou copiar arquivo local
cp app/build/outputs/apk/release/app-release.apk ~/Desktop/SecureGallery.apk

# Enviar por Email, Drive, etc
```

### Opção 2: Compartilhar com Amigos

```bash
# 1. Gere APK release assinado
./gradlew assembleRelease

# 2. Copie pra pasta compartilhada
cp app/build/outputs/apk/release/app-release.apk ~/Dropbox/Apps/

# 3. Compartilhe link
# Amigos baixam e instalam:
# adb install SecureGallery.apk
```

### Opção 3: Google Play (Opcional)

1. Crie conta de desenvolvedor ($25 one-time)
2. Build AAB: `./gradlew bundleRelease`
3. Upload em Google Play Console
4. Aguarde review (~4 horas)
5. Publique!

## 📈 Otimizações Pré-Release

### 1. ProGuard/R8 (Obfuscation)

Já está configurado em `proguard-rules.pro`. Para ativar:

```gradle
buildTypes {
    release {
        isMinifyEnabled = true
        proguardFiles(
            getDefaultProguardFile("proguard-android-optimize.txt"),
            "proguard-rules.pro"
        )
    }
}
```

**Resultado:** APK menor (~30% redução)

### 2. Shrink Resources

```gradle
buildTypes {
    release {
        isMinifyEnabled = true
        shrinkResources = true  // ← Remover recursos não usados
    }
}
```

**Resultado:** APK ainda menor

### 3. Ativar Split APKs (por arquitetura)

Se quiser APK otimizado por device:

```gradle
android {
    bundle {
        density.enableSplit = true
        abi.enableSplit = true
    }
}
```

## 📊 Tamanho Final Esperado

```
Debug APK:     ~50-80 MB
Release APK:   ~25-40 MB (com ProGuard)
AAB (Play):    ~20-30 MB
Instalado:     ~80-100 MB (com dados)
```

## 🎉 Você está pronto!

Agora você tem:
✅ Projeto completo
✅ Build system configurado
✅ Sabe como compilar
✅ Sabe como instalar
✅ Sabe como distribuir

**Próximo passo:** `./gradlew build` e teste! 🚀

---

**Dúvidas? Revise QUICKSTART.md ou ARCHITECTURE.md!**
