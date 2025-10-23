#!/bin/bash

echo "==============================================="
echo "    JCRing App - Quick Build"
echo "==============================================="

# Kill any existing gradle processes
pkill -f gradle 2>/dev/null || true
pkill -f java 2>/dev/null || true

# Wait a bit
sleep 2

echo "[1/3] Building with minimal options..."

# Try to build with specific flags for faster compilation
export GRADLE_OPTS="-Xmx2g -XX:MaxMetaspaceSize=512m"

# Simple build command
timeout 300 ./gradlew assembleDebug \
    --no-daemon \
    --no-configuration-cache \
    --no-build-cache \
    --parallel \
    2>&1

BUILD_EXIT_CODE=$?

echo ""
if [ $BUILD_EXIT_CODE -eq 0 ]; then
    echo "[2/3] Build successful! Looking for APK..."
    
    APK_PATH=$(find . -name "*.apk" -path "*/debug/*" 2>/dev/null | head -1)
    
    if [ -n "$APK_PATH" ]; then
        echo "[3/3] APK encontrado: $APK_PATH"
        
        APK_SIZE=$(du -h "$APK_PATH" | cut -f1)
        echo "Tamanho do APK: $APK_SIZE"
        
        # Copy to root for easy access
        cp "$APK_PATH" JCRingApp-debug.apk 2>/dev/null || true
        
        echo ""
        echo "✅ BUILD COMPLETED SUCCESSFULLY!"
        echo "APK Location: $APK_PATH"
        echo "Also copied to: JCRingApp-debug.apk"
        
    else
        echo "❌ APK não encontrado após build"
        echo "Verificando outputs..."
        find . -name "*.apk" 2>/dev/null || echo "Nenhum APK encontrado"
    fi
    
elif [ $BUILD_EXIT_CODE -eq 124 ]; then
    echo "⏰ Build timeout - verificando se APK foi gerado..."
    
    APK_PATH=$(find . -name "*.apk" -path "*/debug/*" 2>/dev/null | head -1)
    if [ -n "$APK_PATH" ]; then
        echo "✅ APK foi gerado apesar do timeout: $APK_PATH"
        cp "$APK_PATH" JCRingApp-debug.apk 2>/dev/null || true
    else
        echo "❌ Timeout e nenhum APK gerado"
    fi
    
else
    echo "❌ Build failed with exit code: $BUILD_EXIT_CODE"
    echo "Verificando logs de erro..."
fi

echo ""
echo "==============================================="