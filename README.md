# JCRing App - Smart Ring Health Monitoring Application

Uma aplicação Android moderna para monitoramento de saúde usando o smart ring JCRing (modelo J2301A).

## 📱 Funcionalidades

### Conexão Bluetooth
- Escaneamento e descoberta de dispositivos JCRing
- Conexão automática e gerenciamento de estado
- Monitoramento de nível de bateria em tempo real
- Reconexão automática quando disponível

### Monitoramento de Saúde
- **Frequência Cardíaca**: Medição em tempo real e histórico
- **SpO2**: Saturação de oxigênio no sangue
- **Temperatura Corporal**: Monitoramento contínuo
- **Atividade Física**: Passos, calorias, distância
- **HRV**: Variabilidade da frequência cardíaca
- **Sono**: Análise de padrões de sono (futura implementação)
- **Exercícios**: Múltiplos modos de exercício (futura implementação)

### Interface de Usuário
- **Dashboard**: Visão geral dos dados de saúde
- **Dispositivos**: Gerenciamento de conexões Bluetooth
- **Dados de Saúde**: Visualização detalhada dos dados coletados
- **Configurações**: Configurações do dispositivo e aplicativo

## 🛠️ Tecnologias Utilizadas

### Framework Android
- **Kotlin**: Linguagem principal
- **Jetpack Compose**: Interface de usuário moderna
- **Material 3**: Design system moderno
- **ViewModel & LiveData**: Gerenciamento de estado
- **Coroutines & Flow**: Programação assíncrona

### Arquitetura
- **MVVM**: Model-View-ViewModel pattern
- **Repository Pattern**: Separação de fontes de dados
- **Clean Architecture**: Separação clara de responsabilidades

### Bibliotecas
- **BLE SDK J2301A**: SDK oficial do smart ring
- **PermissionX**: Gerenciamento simplificado de permissões
- **Navigation Component**: Navegação entre telas
- **Room Database**: Armazenamento local (futuro)

## 📋 Pré-requisitos

- Android Studio Arctic Fox ou superior
- Android SDK 21+ (Android 5.0)
- Dispositivo Android com Bluetooth Low Energy (BLE)
- Smart ring JCRing modelo J2301A

## 🚀 Instalação e Configuração

### 1. Clonar o Repositório
```bash
git clone [URL_DO_REPOSITORIO]
cd ring/JCRingApp
```

### 2. Abrir no Android Studio
1. Abra o Android Studio
2. Selecione "Open an existing project"
3. Navegue até a pasta `JCRingApp`
4. Aguarde o sync do Gradle

### 3. Configurar SDK BLE
O módulo `blesdk_2301` já está incluído no projeto com todas as dependências necessárias.

### 4. Permissões Necessárias
A aplicação requer as seguintes permissões:
- `BLUETOOTH` / `BLUETOOTH_CONNECT`
- `BLUETOOTH_ADMIN` / `BLUETOOTH_SCAN`
- `ACCESS_FINE_LOCATION`
- `ACCESS_COARSE_LOCATION`

## 🔧 Comandos de Build

### Compilar Debug
```bash
./gradlew assembleDebug
```

### Compilar Release
```bash
./gradlew assembleRelease
```

### Executar Testes
```bash
./gradlew test
```

### Limpar Build
```bash
./gradlew clean
```

## 📖 Como Usar

### 1. Primeira Execução
1. Abra a aplicação
2. Conceda as permissões de Bluetooth e localização
3. Navegue até a aba "Devices"

### 2. Conectar ao Smart Ring
1. Na aba "Devices", toque em "Scan for Devices"
2. Aguarde seu JCRing aparecer na lista
3. Toque em "Connect" no dispositivo desejado
4. Aguarde a conexão ser estabelecida

### 3. Monitorar Dados de Saúde
1. Após conectado, vá para a aba "Dashboard"
2. Use os botões "Heart Rate" e "SpO2" para medições manuais
3. Os dados aparecerão automaticamente na tela
4. Navegue para "Health Data" para ver detalhes

### 4. Configurações
1. Na aba "Settings", configure:
   - Informações pessoais
   - Unidades de medida
   - Configurações do dispositivo

## 🏗️ Estrutura do Projeto

```
app/
├── src/main/java/com/jcring/app/
│   ├── data/
│   │   ├── bluetooth/          # Gerenciamento Bluetooth
│   │   ├── models/             # Modelos de dados
│   │   └── database/           # Banco de dados (futuro)
│   ├── domain/                 # Lógica de negócio (futuro)
│   ├── presentation/
│   │   ├── ui/
│   │   │   └── screens/        # Telas da aplicação
│   │   ├── viewmodel/          # ViewModels
│   │   └── theme/              # Tema e cores
│   └── JCRingApplication.kt    # Classe Application
└── blesdk_2301/                # SDK BLE oficial
```

## 🔌 Integração com SDK BLE

### Principais Classes do SDK
- `BleManager`: Gerenciamento de conexões
- `BleSDK`: Comandos para o dispositivo
- `DataListener2301`: Callbacks de dados
- `BleConnectionListener`: Estados de conexão

### Exemplo de Uso
```kotlin
// Conectar ao dispositivo
BleManager.getInstance().connectDevice(macAddress, false, connectionListener)

// Iniciar medição de frequência cardíaca
val command = BleSDK.SetDeviceMeasurementWithType(AutoTestMode.AutoHeartRate, 60, true)
BleManager.getInstance().writeValue(command)

// Receber dados
val dataListener = object : DataListener2301 {
    override fun dataCallback(maps: Map<String, Any>?) {
        // Processar dados recebidos
    }
}
```

## 🧪 Funcionalidades Implementadas

### ✅ Concluído
- [x] Estrutura básica da aplicação
- [x] Integração com SDK BLE
- [x] Interface de usuário moderna (Compose)
- [x] Gerenciamento de estado (ViewModel)
- [x] Conexão Bluetooth
- [x] Medição de frequência cardíaca
- [x] Medição de SpO2
- [x] Monitoramento de temperatura
- [x] Dados de atividade
- [x] Dashboard interativo

### 🔄 Em Desenvolvimento
- [ ] Banco de dados local (Room)
- [ ] Gráficos e visualizações
- [ ] Histórico de dados
- [ ] Configurações avançadas
- [ ] Notificações

### 📅 Futuras Implementações
- [ ] Sincronização com nuvem
- [ ] Análise de tendências
- [ ] Relatórios de saúde
- [ ] Compartilhamento de dados
- [ ] Múltiplos dispositivos

## 🐛 Troubleshooting

### Problemas Comuns

#### Dispositivo não encontrado
- Verifique se o Bluetooth está ativado
- Confirme as permissões de localização
- Certifique-se de que o smart ring está próximo e carregado

#### Falha na conexão
- Reinicie o Bluetooth no smartphone
- Remova e reinsira a bateria do smart ring
- Verifique se não há outros apps conectados ao dispositivo

#### Dados não aparecem
- Confirme que o dispositivo está conectado
- Verifique se as medições foram iniciadas
- Aguarde alguns segundos para os dados aparecerem

## 📄 Licença

Este projeto é licenciado sob a licença MIT. Veja o arquivo `LICENSE` para detalhes.

## 🤝 Contribuição

1. Faça um fork do projeto
2. Crie uma branch para sua feature (`git checkout -b feature/AmazingFeature`)
3. Commit suas mudanças (`git commit -m 'Add some AmazingFeature'`)
4. Push para a branch (`git push origin feature/AmazingFeature`)
5. Abra um Pull Request

## 📞 Suporte

Para suporte e dúvidas:
- Crie uma issue no repositório
- Consulte a documentação do SDK JCRing
- Verifique os logs de debug na aplicação

## 📚 Recursos Adicionais

- [Documentação Android BLE](https://developer.android.com/guide/topics/connectivity/bluetooth-le)
- [Jetpack Compose Docs](https://developer.android.com/jetpack/compose)
- [Material Design 3](https://m3.material.io/)
- [Kotlin Coroutines](https://kotlinlang.org/docs/coroutines-overview.html)

---

**Desenvolvido com ❤️ para monitoramento de saúde inteligente**