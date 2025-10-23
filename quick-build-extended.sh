#!/bin/bash

echo "==============================================="
echo "    JCRing App - Extended Build (10 min)"
echo "==============================================="

# Kill any existing gradle processes
pkill -f gradle 2>/dev/null || true
pkill -f java 2>/dev/null || true

# Wait a bit
sleep 3

echo "[1/3] Building with extended timeout (600s)..."

# Try to build with extended timeout
export GRADLE_OPTS="-Xmx3g -XX:MaxMetaspaceSize=768m"

# Extended build command with 10 minute timeout
timeout 600 ./gradlew assembleDebug \
    --no-daemon \
    --no-configuration-cache \
    --no-build-cache \
    --parallel \
    --max-workers=2 \
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
        cp "$APK_PATH" JCRingApp-debug-extended.apk 2>/dev/null || true
        
        echo ""
        echo "✅ BUILD COMPLETED SUCCESSFULLY!"
        echo "APK Location: $APK_PATH"
        echo "Also copied to: JCRingApp-debug-extended.apk"
        
        # Show APK info
        echo ""
        echo "📱 APK Information:"
        echo "File: $(basename "$APK_PATH")"
        echo "Size: $APK_SIZE"
        echo "Path: $APK_PATH"
        
    else
        echo "❌ APK não encontrado após build"
        echo "Verificando outputs..."
        find . -name "*.apk" 2>/dev/null || echo "Nenhum APK encontrado"
    fi
    
elif [ $BUILD_EXIT_CODE -eq 124 ]; then
    echo "⏰ Build timeout (10 min) - verificando se APK foi gerado..."
    
    APK_PATH=$(find . -name "*.apk" -path "*/debug/*" 2>/dev/null | head -1)
    if [ -n "$APK_PATH" ]; then
        echo "✅ APK foi gerado apesar do timeout: $APK_PATH"
        cp "$APK_PATH" JCRingApp-debug-extended.apk 2>/dev/null || true
        echo "Copiado para: JCRingApp-debug-extended.apk"
    else
        echo "❌ Timeout e nenhum APK gerado"
    fi
    
else
    echo "❌ Build failed with exit code: $BUILD_EXIT_CODE"
    echo ""
    echo "🔍 Verificando problemas de compilação..."
    
    # Try to compile just Kotlin to see specific errors
    echo "Tentando compilação Kotlin isolada..."
    timeout 120 ./gradlew compileDebugKotlin --no-daemon 2>&1 | tail -20
fi

echo ""
echo "==============================================="
echo "Build process completed."
echo "Check for JCRingApp-debug-extended.apk in current directory"
echo "==============================================="