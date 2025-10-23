# 📱 Guia para Gerar APK - JCRing App

## 🛠️ **Métodos para Gerar o APK**

### **Método 1: Android Studio (Mais Fácil) ⭐**

1. **Abrir o projeto:**
   - Abra o Android Studio
   - File → Open
   - Navegue até `/mnt/c/repositorio/ring/JCRingApp`
   - Clique em "OK"

2. **Sincronizar o projeto:**
   - Aguarde o Gradle sync automático
   - Se aparecer erro, clique em "Sync Now"

3. **Gerar APK Debug:**
   - Menu: `Build` → `Build Bundle(s) / APK(s)` → `Build APK(s)`
   - Aguarde a compilação
   - Clique em "locate" quando aparecer a notificação

4. **Localização do APK:**
   ```
   JCRingApp/app/build/outputs/apk/debug/app-debug.apk
   ```

### **Método 2: Linha de Comando (Terminal)**

#### **Pré-requisitos:**
- Java JDK 8 ou superior
- Android SDK configurado
- Variável ANDROID_HOME definida

#### **Comandos:**

1. **Navegar para o projeto:**
   ```bash
   cd /mnt/c/repositorio/ring/JCRingApp
   ```

2. **Gerar APK Debug:**
   ```bash
   ./gradlew assembleDebug
   ```

3. **Gerar APK Release (sem assinatura):**
   ```bash
   ./gradlew assembleRelease
   ```

4. **Limpar e construir:**
   ```bash
   ./gradlew clean assembleDebug
   ```

#### **Verificar build:**
```bash
./gradlew build --info
```

### **Método 3: Windows (PowerShell/CMD)**

```cmd
cd C:\repositorio\ring\JCRingApp
gradlew.bat assembleDebug
```

## 📂 **Localização dos APKs Gerados**

### **APK Debug:**
```
JCRingApp/app/build/outputs/apk/debug/app-debug.apk
```

### **APK Release:**
```
JCRingApp/app/build/outputs/apk/release/app-release-unsigned.apk
```

## 🔧 **Solução de Problemas Comuns**

### **Erro: "gradlew command not found"**
```bash
# Tornar o gradlew executável
chmod +x gradlew

# Ou usar o gradle diretamente
gradle assembleDebug
```

### **Erro: "ANDROID_HOME not set"**
```bash
# Linux/Mac
export ANDROID_HOME=$HOME/Android/Sdk
export PATH=$PATH:$ANDROID_HOME/tools:$ANDROID_HOME/platform-tools

# Windows
set ANDROID_HOME=C:\Users\%USERNAME%\AppData\Local\Android\Sdk
set PATH=%PATH%;%ANDROID_HOME%\tools;%ANDROID_HOME%\platform-tools
```

### **Erro: "SDK not found"**
1. Abra o Android Studio
2. File → Settings → Appearance & Behavior → System Settings → Android SDK
3. Anote o caminho do SDK
4. Configure a variável ANDROID_HOME

### **Erro de memória Java:**
```bash
export GRADLE_OPTS="-Xmx4g -XX:MaxPermSize=512m"
./gradlew assembleDebug
```

### **Erro de dependências:**
```bash
./gradlew clean
./gradlew build --refresh-dependencies
```

## 🎯 **APK Assinado para Release**

### **1. Gerar Keystore:**
```bash
keytool -genkey -v -keystore jcring-release-key.keystore -alias jcring -keyalg RSA -keysize 2048 -validity 10000
```

### **2. Configurar no app/build.gradle:**
```gradle
android {
    signingConfigs {
        release {
            storeFile file('jcring-release-key.keystore')
            storePassword 'sua_senha'
            keyAlias 'jcring'
            keyPassword 'sua_senha'
        }
    }
    buildTypes {
        release {
            signingConfig signingConfigs.release
            minifyEnabled false
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
        }
    }
}
```

### **3. Gerar APK Assinado:**
```bash
./gradlew assembleRelease
```

## 📊 **Verificar Informações do APK**

### **Tamanho do APK:**
```bash
./gradlew assembleDebug
ls -lh app/build/outputs/apk/debug/app-debug.apk
```

### **Dependências:**
```bash
./gradlew app:dependencies
```

### **Tasks disponíveis:**
```bash
./gradlew tasks
```

## 🚀 **Instalação no Dispositivo**

### **Via ADB:**
```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

### **Via Android Studio:**
1. Conecte o dispositivo via USB
2. Habilite "Depuração USB" no dispositivo
3. Clique no botão "Run" no Android Studio

### **Via Arquivo APK:**
1. Copie o APK para o dispositivo
2. Habilite "Fontes desconhecidas" nas configurações
3. Toque no arquivo APK para instalar

## ⚡ **Comandos Rápidos**

```bash
# Compilação rápida
./gradlew assembleDebug --parallel --offline

# Verificar projeto
./gradlew check

# Limpar tudo
./gradlew clean

# Ver logs detalhados
./gradlew assembleDebug --debug

# Apenas compilar (sem gerar APK)
./gradlew compileDebugJavaWithJavac
```

## 📱 **Especificações do APK Gerado**

- **Nome:** JCRing App
- **Package:** com.jcring.app
- **Versão:** 1.0 (versionCode 1)
- **SDK Mínimo:** API 21 (Android 5.0)
- **SDK Alvo:** API 34 (Android 14)
- **Arquiteturas:** armeabi-v7a, arm64-v8a

## 🔍 **Verificar Conteúdo do APK**

```bash
# Extrair APK para análise
unzip -l app/build/outputs/apk/debug/app-debug.apk

# Analisar com aapt
aapt dump badging app/build/outputs/apk/debug/app-debug.apk
```

---

**💡 Dica:** Para desenvolvimento, use sempre o APK Debug. Para distribuição, use o APK Release assinado.