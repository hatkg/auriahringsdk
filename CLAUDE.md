# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

JCRingApp is a modern Android health monitoring application built for the J2301A smart ring device. It provides comprehensive health tracking with real-time Bluetooth Low Energy (BLE) connectivity, monitoring heart rate, SpO2, temperature, activity, and sleep data through a Jetpack Compose Material 3 interface.

## Project Structure

```
JCRingApp/
├── app/                           # Main Android application module
│   ├── src/main/java/com/jcring/app/
│   │   ├── data/                  # Data layer (BLE, database, repository)
│   │   ├── presentation/          # UI layer (Compose screens, ViewModels)
│   │   └── JCRingApplication.kt   # Application class with DI
├── blesdk_2301/                   # BLE SDK module (Java-based)
└── build configuration files
```

## Development Commands

### Build Commands
```bash
# Debug build (fastest)
./gradlew assembleDebug

# Release build 
./gradlew assembleRelease

# Clean build
./gradlew clean

# Install debug APK on device
./gradlew installDebug

# Run unit tests
./gradlew test

# Run Android instrumented tests
./gradlew connectedAndroidTest

# Generate BLE SDK JAR
./gradlew makejar
```

### Quick Build Scripts
```bash
# Linux/WSL automated APK build
./build-apk.sh

# Windows automated APK build
build-apk.bat
```

### Lint and Validation
```bash
# Android lint check
./gradlew lint

# Kotlin code style check
./gradlew ktlintCheck

# Fix Kotlin code style
./gradlew ktlintFormat
```

## Architecture Overview

### Core Architecture Pattern
- **MVVM**: ViewModels manage UI state with StateFlow/LiveData
- **Repository Pattern**: `HealthDataRepository` centralizes data access
- **Clean Architecture**: Clear separation between data, domain, and presentation layers
- **Reactive Programming**: Kotlin Coroutines and Flow for async operations

### Key Components
- **BluetoothManager**: Handles BLE device discovery, connection, and data streaming
- **HealthDatabase**: Room database for local health data persistence  
- **Compose Screens**: Modern declarative UI with Material 3 theming
- **ViewModels**: State management and business logic coordination
- **BLE SDK Module**: Separate Java module for device communication protocols

### Technology Stack
- **UI**: Jetpack Compose + Material 3 Design System
- **Language**: Kotlin (primary) with Java (BLE SDK interop)
- **Database**: Room Database (SQLite) with simplified configuration
- **Charts**: MPAndroidChart for health data visualization
- **Architecture**: MVVM with Repository pattern
- **Build**: Gradle with Kotlin DSL, Android Gradle Plugin 8.1.4

## Build Configuration

### Target Specifications
- **Compile SDK**: 34 (Android 14)
- **Target SDK**: 34  
- **Min SDK**: 21 (Android 5.0)
- **Java Version**: 17
- **Kotlin**: 1.9.10
- **Gradle**: 8.5

### Important Build Notes
- Room annotation processing simplified to avoid build complexity
- Hilt dependency injection removed for build stability
- ProGuard rules configured to preserve BLE SDK classes
- JVM memory allocation: 2GB for builds (configured in gradle.properties)

## Health Monitoring Features

### Core Metrics
- **Heart Rate**: Real-time monitoring with historical data
- **SpO2**: Blood oxygen saturation measurement
- **Temperature**: Body temperature tracking
- **Activity**: Steps, calories, distance, active minutes
- **HRV**: Heart Rate Variability analysis
- **Sleep**: Sleep pattern tracking (expandable implementation)

### Device Management
- BLE device scanning and pairing
- Real-time data streaming from J2301A ring
- Battery level monitoring
- Automatic reconnection handling
- Device configuration synchronization

## Development Guidelines

### Permissions
Required runtime permissions for full functionality:
- `BLUETOOTH_CONNECT`, `BLUETOOTH_SCAN` (Android 12+)
- `ACCESS_FINE_LOCATION` (BLE device discovery)
- `FOREGROUND_SERVICE` (continuous monitoring)

### Database Operations
- Use Room DAOs through repository pattern
- Leverage Kotlin Coroutines for database operations
- Health data entities auto-generate timestamps
- Foreign key relationships properly configured

### BLE Communication
- All device communication goes through `BluetoothManager`
- Use callback patterns defined in `blesdk_2301` module
- Handle connection state changes appropriately
- Implement proper error handling for BLE operations

### UI Development
- Follow Material 3 design principles
- Use existing theme and color system in `presentation/theme/`
- Compose screens should observe ViewModel state via StateFlow
- Reuse components from `presentation/ui/components/`

## Testing

### Test Structure
- **Unit Tests**: `src/test/` using JUnit 4.13.2
- **UI Tests**: Compose testing framework for screen interactions
- **Instrumented Tests**: `src/androidTest/` using Espresso and AndroidX Test
- **BLE Tests**: Basic SDK functionality tests in `blesdk_2301` module

### Test Execution
```bash
# All unit tests
./gradlew test

# Specific test class
./gradlew test --tests "com.jcring.app.HealthDataRepositoryTest"

# UI tests (requires connected device)
./gradlew connectedAndroidTest
```

## Documentation References

- **README.md**: Main project documentation (Portuguese)
- **BUILD_GUIDE.md**: Comprehensive APK build instructions
- **QUICK_BUILD.md**: 5-minute build guide
- **ENHANCED_FEATURES.md**: Advanced feature documentation

Note: Primary documentation is in Portuguese. English technical documentation available in individual guide files.

## Recent BLE Integration Updates

### BLE Connection Fix (J2301A Ring Support)
The BluetoothManager has been updated with full J2301A ring integration:

**Key Changes:**
- Uncommented all SDK calls that were previously disabled
- Added proper BLE service UUIDs: `FFF0` (service), `FFF6` (send), `FFF7` (receive)
- Implemented `com.jstyle.test2025.ble.BleManager` and `BleService` from original demo
- Fixed data type conversions for MyPersonalInfo and MyDeviceTime
- Added proper data listener setup for real-time health data

**BLE Service Configuration:**
```kotlin
// Service UUIDs for J2301A ring
SERVICE_DATA = "0000fff0-0000-1000-8000-00805f9b34fb"
DATA_Characteristic = "0000fff6-0000-1000-8000-00805f9b34fb"  
NOTIY_Characteristic = "0000fff7-0000-1000-8000-00805f9b34fb"
```

**Device Scanning:**
- Filters for device names: "J2301", "Ring", "J2301A"
- Supports both paired and new device discovery
- Automatic BLE service filtering

**Health Data Support:**
- Real-time heart rate monitoring
- SpO2 (blood oxygen) measurement  
- Temperature tracking
- Activity data (steps, calories, distance)
- Battery level monitoring

**Usage Example:**
```kotlin
// Initialize BLE
bluetoothManager.initialize()

// Start scanning
bluetoothManager.startScan { device ->
    // Ring found, connect
    bluetoothManager.connectToDevice(device) { success, error ->
        if (success) {
            // Set personal info
            bluetoothManager.setPersonalInfo(personalInfo) { sent -> }
            // Start measurements
            bluetoothManager.startHeartRateMeasurement { started -> }
        }
    }
}

// Monitor data
bluetoothManager.heartRateData.collect { data ->
    // Real-time heart rate data
}
```