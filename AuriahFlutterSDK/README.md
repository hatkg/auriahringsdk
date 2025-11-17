# AuriahFlutterSDK — Documento de especificação para implementação do plugin Flutter

Por favor, crie uma pasta no repositório atual chamada `AuriahFlutterSDK` e adicione este documento como `README.md` dentro dela.  
Este arquivo descreve em detalhe todas as tarefas, arquivos, design e critérios de aceitação necessários para implementar um SDK/Plugin Flutter que integre o anel J2301A (BLE SDK já presente no projeto Android) com um app Flutter.

> **Objetivo**: entregar um plugin Flutter (Android nativo + Dart) que exponha todas as funcionalidades do `BluetoothManager.kt` / `blesdk_2301` ao Dart/Flutter por meio de MethodChannels e EventChannels, um app de exemplo e documentação de uso.

---

## Índice

1. [Visão geral e escopo](#1-visão-geral-e-escopo)
2. [Estrutura da pasta e arquivos iniciais](#2-estrutura-da-pasta-e-arquivos-iniciais)
3. [Arquitetura e decisões de design](#3-arquitetura-e-decisões-de-design)
4. [API pública do plugin (métodos e streams)](#4-api-pública-do-plugin-métodos-e-streams)
5. [Implementação Android (Kotlin) — detalhes técnicos](#5-implementação-android-kotlin--detalhes-técnicos)
6. [Implementação Dart — modelos e manager](#6-implementação-dart--modelos-e-manager)
7. [Permissões e manifestos Android](#7-permissões-e-manifestos-android)
8. [Dependências e build (Gradle)](#8-dependências-e-build-gradle)
9. [Testes, exemplos e QA](#9-testes-exemplos-e-qa)
10. [Publicação e versão](#10-publicação-e-versão)
11. [Segurança, privacidade e conformidade](#11-segurança-privacidade-e-conformidade)
12. [Checklist de entrega e critérios de aceitação](#12-checklist-de-entrega-e-critérios-de-aceitação)
13. [Estimativa de esforço e divisão de tarefas](#13-estimativa-de-esforço-e-divisão-de-tarefas)

---

## 1) Visão geral e escopo

Construir um plugin Flutter chamado `auriah_ring_sdk` (ou `auriah_flutter_sdk`) que:

- **Reutilize o módulo nativo existente**: `:blesdk_2301` e a implementação `BluetoothManager.kt` (especificamente `RealBluetoothManager.kt`).
- **Exponha os principais recursos**:
  - Scan de dispositivos BLE
  - Conectar/desconectar dispositivos
  - Eventos em tempo real: HR (frequência cardíaca), SpO2, temperatura, ECG, HRV
  - Configurações: personal info, sync time, monitoramento automático
  - Leitura de bateria
  - Dados de atividade/sono
  - Modos esportivos
  - Medição de glicose
  - Etc.
- **Forneça um app exemplo** (`example/`) usando o plugin.
- **Tenha documentação clara** para desenvolvedores Flutter.

### Plataformas

- **Inicialmente Android** (Kotlin)
- **Preparar esqueleto iOS** (Swift/ObjC) para futuro, mas não necessário implementar agora — marcar como backlog.

---

## 2) Estrutura da pasta e arquivos iniciais

Na raiz do repositório, criar a pasta `AuriahFlutterSDK/` com a seguinte estrutura sugerida:

```
AuriahFlutterSDK/
├── README.md                      # Este documento
├── plugin/                        # Diretório do plugin (padrão flutter plugin)
│   ├── pubspec.yaml
│   ├── lib/
│   │   ├── auriah_ring_sdk.dart      # API pública
│   │   ├── models/
│   │   │   ├── device.dart
│   │   │   ├── heart_rate_data.dart
│   │   │   ├── spo2_data.dart
│   │   │   ├── temperature_data.dart
│   │   │   ├── activity_data.dart
│   │   │   ├── sleep_data.dart
│   │   │   ├── ecg_data.dart
│   │   │   ├── hrv_data.dart
│   │   │   └── personal_info.dart
│   │   └── src/
│   │       └── ring_manager.dart       # Lógica Dart "client" leve (opcional)
│   ├── android/
│   │   ├── src/main/kotlin/com/auriah/ring/
│   │   │   ├── AuriahRingPlugin.kt
│   │   │   ├── BleManager.kt
│   │   │   ├── DataParsers.kt
│   │   │   └── PermissionsHelper.kt
│   │   ├── build.gradle
│   │   ├── src/main/AndroidManifest.xml
│   │   └── proguard-rules.pro
│   ├── example/                    # App exemplo gerado pelo `flutter create --template=plugin`
│   │   ├── lib/
│   │   │   └── main.dart
│   │   ├── android/
│   │   └── pubspec.yaml
│   └── test/
│       └── # Unit tests for Dart models & manager
├── docs/
│   ├── API.md
│   ├── USAGE.md
│   ├── ARCHITECTURE.md
│   └── RELEASE.md
└── ci/
    ├── android-lint.yml
    └── flutter-tests.yml
```

### Observação

Podemos gerar o template do plugin com:
```bash
flutter create --template=plugin --platforms=android auriah_ring_sdk
```
dentro da pasta `plugin/` para já montar estrutura padrão.

---

## 3) Arquitetura e decisões de design

### 3.1 Tipo de plugin

- **Plugin federado NÃO é necessário inicialmente**; um plugin com implementação nativa Android (Kotlin) + Dart é suficiente.
- **Estrutura recomendada**: plugin Flutter "single platform" com MethodChannel para comandos síncronos/assíncronos e EventChannel(s) para streams de dados em tempo real.

### 3.2 Comunicação

#### MethodChannel
Chamadas request/response (initialize, startScan, connect, setPersonalInfo, getBatteryLevel, startECG, stopECG, etc.).

#### EventChannel(s)
Para dados contínuos e estados:
- `scanned_devices` (lista atualizada de dispositivos)
- `connection_state` (enum)
- `heart_rate`
- `spo2`
- `temperature`
- `ecg` (stream de amostras)
- `hrv`
- `sleep_data` / `activity_data` (eventos de sincronização)
- `logs/debug` (opcional)

### 3.3 Modelos

- Todos os objetos trocados serão convertidos para `Map<String, dynamic>` no Kotlin e para classes Dart no lado Flutter.
- Documentar campos obrigatórios e formatos (ex.: timestamp em ms UTC).

### 3.4 Threading & lifecycle

- No lado Kotlin, usar **coroutines/flows** para coletar os flows do `BluetoothManager` e enviar eventos via `EventSink`.
- Gerenciar lifecycle do plugin para desconectar/dispose corretamente quando a Activity for destruída.
- Cuidado com Activity/Context para solicitações de permissões (usar `ActivityAware` se necessário).

### 3.5 Erros e códigos

Definir um esquema de erros consistente (p.ex. códigos):
- `INVALID_ARG`
- `NOT_CONNECTED`
- `TIMEOUT`
- `PERMISSION_DENIED`
- `BLUETOOTH_DISABLED`
- `INTERNAL_ERROR`

Sempre retornar em MethodChannel com `success/result` ou `result.error(codigo, mensagem, detalhes)`.

---

## 4) API pública do plugin (métodos e streams)

### 4.1 Inicialização e permissões

**MethodChannel methods:**
- `initialize()` → `bool`
- `checkPermissions()` → `bool`
- `requestPermissions()` → `bool`
- `isBluetoothEnabled()` → `bool`

### 4.2 Scan e descoberta

**MethodChannel methods:**
- `startScan()` → `void` (inicia)
- `stopScan()` → `void`

**EventChannel:**
- `scannedDevices` → `Stream<List<RingDevice>>`
  - RingDevice = `{ name, macAddress, rssi, isBonded?, manufData? }`

### 4.3 Conexão

**MethodChannel methods:**
- `connectToDevice({ macAddress: String })` → `bool` / error
- `disconnect()` → `void`

**EventChannel:**
- `connectionState` → `Stream<int>` (mapear para enum Dart: DISCONNECTED=0, CONNECTING=1, CONNECTED=2, DISCONNECTING=3)

### 4.4 Configuração do dispositivo

**MethodChannel methods:**
- `setPersonalInfo(PersonalInfo)` → `bool`
  - PersonalInfo: `{ gender:int, age:int, height:int, weight:int, stride:int }`
- `syncDeviceTime()` → `bool`
- `setAutomaticHeartRateMonitoring({ enabled: bool, intervalMinutes: int })` → `bool`
- `setAutomaticSpO2Monitoring({...})` → `bool`

### 4.5 Medições em tempo real (EventChannels)

**EventChannels:**
- `heartRateData` → `Stream<HeartRateData>`
  - HeartRateData: `{ heartRate:int, timestamp:ms, isRealTime:bool }`
- `spo2Data` → `Stream<SpO2Data>`
  - SpO2Data: `{ spo2Value:int, timestamp:ms, isAutomatic:bool }`
- `temperatureData` → `Stream<TemperatureData>`
  - TemperatureData: `{ temperature:double, unit:String, isBodyTemperature:bool, timestamp:ms }`
- `ecgData` → `Stream<ECGChunk>`
  - ECGChunk: `{ samples: [int|double], timestamp:ms, seq:Int }`
- `hrvData` → `Stream<HRVData>`
- `batteryLevel` → Event or polling method `getBatteryLevel()` → `int`

### 4.6 Ações (MethodChannel)

**MethodChannel methods:**
- `startHeartRateMeasurement()` → `bool`
- `stopHeartRateMeasurement()` → `bool`
- `startSpO2Measurement()` → `bool`
- `stopSpO2Measurement()` → `bool`
- `startECGMeasurement()` → `bool`
- `stopECGMeasurement()` → `bool`
- `startHRVMeasurement()` → `bool`
- `startBloodGlucoseMeasurement()` → `bool`
- `stopBloodGlucoseMeasurement()` → `bool`
- `startExerciseMode(mode:int)` → `bool`
- `stopExerciseMode()` → `bool`
- `getActivityData()` → `Map | null`
- `getSleepData()` → `Map | null`

### 4.7 Eventos de logs e diagnósticos (opcional)

**EventChannel:**
- `debugLog` → EventChannel para facilitar troubleshooting durante desenvolvimento

### 4.8 Exemplos de nomes de canais

**MethodChannel:**
- `"auriah_ring_sdk/methods"`

**EventChannels:**
- `"auriah_ring_sdk/events/scanned_devices"`
- `"auriah_ring_sdk/events/connection_state"`
- `"auriah_ring_sdk/events/heart_rate"`
- `"auriah_ring_sdk/events/spo2"`
- `"auriah_ring_sdk/events/temperature"`
- `"auriah_ring_sdk/events/ecg"`
- `"auriah_ring_sdk/events/hrv"`
- `"auriah_ring_sdk/events/activity"`
- `"auriah_ring_sdk/events/sleep"`
- `"auriah_ring_sdk/events/logs"`

---

## 5) Implementação Android (Kotlin) — detalhes técnicos

### 5.1 Reusar BluetoothManager.kt

- **Importante**: o repositório já contém `RealBluetoothManager.kt` (classe central).
- Criar `BleManager.kt` que encapsule `RealBluetoothManager.getInstance(context)` e oferece handlers para enviar dados aos EventChannels.
- **Não modificar diretamente** o `RealBluetoothManager.kt` salvo para acomodar callbacks se necessário — preferir wrapper.

### 5.2 AuriahRingPlugin.kt

- Implementar `FlutterPlugin` & `MethodCallHandler`.
- Registrar MethodChannel e EventChannels no `onAttachedToEngine`.
- Se o plugin precisar de permissões, implementar `ActivityAware` para requisitar permissões via Activity.

### 5.3 Streams

Para cada EventChannel, criar `StreamHandler` que:
- Na `onListen`: inicia a coleta do Flow/LiveData do BluetoothManager e mapeia para `eventSink.success(mapOf(...))`.
- Na `onCancel`: parar coleta.
- Usar `CoroutineScope` + `SupervisorJob` no BleManager para coletar flows.

### 5.4 Permissões e Activity

- Para `requestPermissions()` usar Activity para mostrar dialogos do Android.
- Implementar um `PermissionsHelper.kt` que solicite:
  - **Android 12+**: `BLUETOOTH_SCAN`, `BLUETOOTH_CONNECT`, `ACCESS_FINE_LOCATION` (conforme a documentação do repo).
  - **Android <12**: `BLUETOOTH`, `BLUETOOTH_ADMIN`, `ACCESS_COARSE_LOCATION`
- Documentar como a Activity deve encaminhar resultados (flutter embedding v2 — usar ActivityAware hooks).

### 5.5 Dependências e hooking do módulo `:blesdk_2301`

- Configurar `android/build.gradle` do plugin para depender do projeto `:blesdk_2301`.
- Exemplo:
  ```gradle
  implementation project(':blesdk_2301')
  ```
- Garantir que `settings.gradle` do repositório inclua `:blesdk_2301` e que o plugin android use o mesmo gradle settings.

### 5.6 ProGuard/R8

- Expor regras em `proguard-rules.pro` copiando regras necessárias do `blesdk_2301`.
- Garantir que ao empacotar, classes utilizadas por reflection não sejam ofuscadas.

### 5.7 Testes nativos

Criar testes instrumentados para validar:
- `startScan`/`stopScan` → `scannedDevices` stream
- `connect`/`disconnect` → estados corretos
- Emissões de `heartRate`/`SpO2`/`temperature`

### 5.8 Logging e troubleshooting

Permitir ativar logs detalhados via método `enableVerboseLogging()` que apenas encaminha para `BluetoothManager.setLogLevel(...)`.

---

## 6) Implementação Dart — modelos e manager

### 6.1 Modelos Dart (em `lib/models`)

Exemplo `heart_rate_data.dart`:
```dart
class HeartRateData {
  final int heartRate;
  final DateTime timestamp;
  final bool isRealTime;
  
  HeartRateData({
    required this.heartRate,
    required this.timestamp,
    required this.isRealTime,
  });
  
  factory HeartRateData.fromJson(Map<String, dynamic> json) {
    return HeartRateData(
      heartRate: json['heartRate'],
      timestamp: DateTime.fromMillisecondsSinceEpoch(json['timestamp']),
      isRealTime: json['isRealTime'] ?? false,
    );
  }
  
  Map<String, dynamic> toJson() {
    return {
      'heartRate': heartRate,
      'timestamp': timestamp.millisecondsSinceEpoch,
      'isRealTime': isRealTime,
    };
  }
}
```

Criar modelos para:
- `RingDevice`
- `HeartRateData`
- `SpO2Data`
- `TemperatureData`
- `ECGChunk`
- `HRVData`
- `ActivityData`
- `SleepData`
- `PersonalInfo`

### 6.2 ring_manager.dart (client-side helper)

- Abstrai MethodChannel/EventChannels em uma API idiomática Dart.
- Exponha Streams fortemente tipadas: `Stream<HeartRateData> get heartRateStream;`
- Fornecer métodos de conveniência e reconexão automática (retries).
- Configurar timeouts e tratamento de erros.

Exemplo de uso no app:
```dart
AuriahRingSDK.instance.startScan();
AuriahRingSDK.instance.scannedDevices.listen(...);
```

### 6.3 Example app

- Gerar `example/` com tela de scan, conexão e dashboard básico semelhante ao documento anterior (fornecido anteriormente na conversa).
- Incluir instruções de debug (como ativar logs, puxar logs e exportar).

### 6.4 Tests Dart

- **Unit tests** para os modelos e para o parsing JSON.
- **Mockito** para mock MethodChannel e EventChannel e testar ring_manager behavior.

---

## 7) Permissões e Manifestos Android

### 7.1 AndroidManifest.xml

`plugin/android/src/main/AndroidManifest.xml` — adicionar:

```xml
<uses-permission android:name="android.permission.BLUETOOTH" />
<uses-permission android:name="android.permission.BLUETOOTH_ADMIN" />
<uses-permission android:name="android.permission.BLUETOOTH_SCAN" 
                 android:usesPermissionFlags="neverForLocation" />
<uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
```

Fornecer condicional para targets SDK >= 31 para uso correto.

### 7.2 Solicitação em runtime

Implementar fluxo de `requestPermissions()` no plugin que mapeie para `Activity.requestPermissions` e aguarde callback.

### 7.3 Background/Foreground

- Se houver necessidade de manter scan/connect em background, documentar que será necessário um Foreground Service (API level e permissões extra).
- **Inicialmente NÃO implementar** background scanning contínuo — colocar como requisito futuro.

---

## 8) Dependências e build (Gradle)

### 8.1 plugin/android/build.gradle

- Definir `minSdkVersion` conforme SDK nativo (ex.: 21/23/24).
- Add dependency:
  ```gradle
  implementation project(':blesdk_2301')
  ```
- Incluir `kotlin-stdlib` e coroutines se necessário.

### 8.2 settings.gradle (raiz do repo)

- Garantir que `:blesdk_2301` está incluído.
- Se o plugin for referenciado localmente no app exemplo, ajustar `includeBuild` ou usar path.

### 8.3 Versão e naming

`pubspec.yaml`:
- `name: auriah_ring_sdk`
- `version: 0.1.0`
- `description`, `author`, `homepage`, `repository`

Documentar semântica de versionamento (SemVer).

---

## 9) Testes, exemplos e QA

### 9.1 Unit tests

- **Dart**: models parsing, ring_manager error handling.
- **Kotlin**: wrappers (usar Robolectric ou instrumented tests).

### 9.2 Integration tests

Manual QA checklist: conectar com um anel J2301A real e validar:
- Scan detecta dispositivos J2301
- Conexão e envio de dados em tempo real (HR, SpO2, Temp)
- ECG stream (amostras contínuas)
- Configurações aplicadas no dispositivo (personal info, time sync)
- Monitoramento automático ativado e desativado

### 9.3 Test matrix

- **Android API levels**: 23, 28, 31, 33
- Testar em dispositivos com Bluetooth Low Energy válidos (não em emulador)
- Testar com permissões negadas e restauradas

### 9.4 Test data

Criar fixtures JSON para dados de health (heart rate, spo2, ecg) para testes.

---

## 10) Publicação e versão

### 10.1 Publicação no pub.dev

**Passos:**
1. Completar README com instruções de uso
2. Incluir example app
3. Garantir license, authors e changelog
4. Rodar `dart pub publish --dry-run` e corrigir warnings

Se for privado, documentar como referenciar via path/git no pubspec do app principal.

### 10.2 Release notes

Manter `CHANGELOG.md` e versão semântica.

---

## 11) Segurança, privacidade e conformidade

### 11.1 Dados de saúde são sensíveis

- **Documentar claramente**: o plugin NÃO envia dados a servidores automaticamente.
- Fornecer instruções para criptografia e armazenamento seguro no app (por exemplo, uso de Keystore/EncryptedSharedPreferences).
- Incluir aviso de privacidade e pedir consentimento do usuário antes de ativar medições contínuas.

### 11.2 Permissões e políticas do Google Play

- Documentar justificativas para `BLUETOOTH`, `BLUETOOTH_SCAN`, `BLUETOOTH_CONNECT` e `LOCATION`.
- Preparar texto para declarar uso de dados sensíveis no Play Console (Health data).

---

## 12) Checklist de entrega e critérios de aceitação

### 12.1 Entregáveis mínimos (MVP)

- [ ] Plugin Flutter com MethodChannel e EventChannels funcionando para:
  - `initialize`, `startScan`/`stopScan`, `scannedDevices` stream
  - `connectToDevice`/`disconnect`, `connectionState` stream
  - `heartRateData`, `spo2Data`, `temperatureData` streams
  - `setPersonalInfo`, `syncDeviceTime`
  - start/stop heart rate, spo2, ecg
  - `getBatteryLevel`
- [ ] Example app funcional para Android com scan, conectar, dashboard básico
- [ ] Documentação de uso (`USAGE.md`) e `API.md`
- [ ] Unit tests Dart para modelos
- [ ] CI rodando lint e tests básicos

### 12.2 Critérios de aceitação

- Conectar com anel J2301A real e receber dados em tempo real (HR, SpO2) no example app.
- Code review aprovado (Kotlin e Dart) e PRs com testes mínimos.
- Instruções para build e inclusão do `:blesdk_2301` funcionando.

---

## 13) Estimativa de esforço e divisão de tarefas

| Tarefa | Estimativa | Responsável |
|--------|-----------|-------------|
| Criar scaffold do plugin (`flutter create --template=plugin`) | 0.5d | Dev A |
| Implementar wrapper Kotlin (`BleManager.kt`) + EventChannels | 2.0d | Dev B |
| Implementar plugin class `AuriahRingPlugin.kt` com MethodChannel | 1.5d | Dev B |
| Implementar PermissionHelper & ActivityAware flow | 1.0d | Dev B |
| Implementar modelos Dart e ring_manager | 1.5d | Dev C |
| Example app (UI scan/connect/dashboard simples) | 2.0d | Dev C |
| Tests unit & integration (Dart & Android instrumented) | 2.0d | Dev D |
| Docs (USAGE.md, API.md, RELEASE.md) | 1.0d | Tech Writer |
| CI (GitHub Actions) | 0.5d | DevOps |
| Buffer / QA | 1.5d | Team |

**Total estimado**: ~13.5 dias úteis (1-3 devs trabalhando em paralelo reduz tempo).

---

## Tarefas iniciais prontas para execução

Instruções curtas para a equipe:

1. **No repositório, criar a pasta:**
   ```bash
   mkdir AuriahFlutterSDK
   cd AuriahFlutterSDK
   ```

2. **Gerar scaffold do plugin:**
   ```bash
   flutter create --template=plugin --platforms=android auriah_ring_sdk
   # (mover/ajustar para ficar dentro de AuriahFlutterSDK/plugin)
   ```

3. **Adicionar referência ao módulo `:blesdk_2301`:**
   - Editar `plugin/android/build.gradle` e incluir `implementation project(':blesdk_2301')`
   - Confirmar `settings.gradle` inclui `:blesdk_2301`

4. **Commit inicial com scaffold e este README.md.**

5. **Abrir PR** com descrição "feat: add AuriahFlutterSDK plugin scaffold and specification" e atribuir revisores.

---

## Anexos úteis / referências

- **Conteúdo relevante existente**: `RealBluetoothManager.kt` no repositório (usar como referência de APIs internas)
- **UUIDs BLE e comandos** já documentados no arquivo `DOCUMENTACAO_COMPLETA_ANEL_J2301A.md` (este repo)
- **Permissões Android 12+** e guidelines

---

## Próximos passos (curto prazo)

1. ✅ Crie a pasta `AuriahFlutterSDK` e adicione este README.md
2. Gere o scaffold do plugin com `flutter create --template=plugin`
3. Faça um PR inicial com scaffold + README e marque os desenvolvedores responsáveis
4. Em paralelo, quem for implementar Kotlin deve estudar `RealBluetoothManager.kt` e mapear quais flows/callbacks serão expostos primeiro

---

## Geração automática de código (próximos passos)

Se desejar, podemos gerar automaticamente:

- Scaffold de `pubspec.yaml` e um `AuriahRingPlugin.kt` de exemplo já preenchido (fornecendo o conteúdo pronto para colar).
- Modelos Dart básicos (`HeartRateData`, `SpO2Data`, `RingDevice`, `PersonalInfo`).
- Checklist de PRs e issues (task breakdown) em formato pronto para abrir no GitHub.

**Qual desses você quer que eu já gere agora?**
- Arquivos de exemplo
- Modelos Dart
- `AuriahRingPlugin.kt` + `BleManager.kt` stub
