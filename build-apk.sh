#!/bin/bash

echo "==============================================="
echo "    JCRing App - Build Script"
echo "==============================================="
echo

echo "[1/4] Verificando ambiente..."
if [ ! -f "./gradlew" ]; then
    echo "ERRO: gradlew não encontrado!"
    echo "Execute este script na pasta JCRingApp"
    exit 1
fi

# Tornar gradlew executável
chmod +x ./gradlew

echo "[2/4] Limpando builds anteriores..."
./gradlew clean

echo "[3/4] Compilando aplicação (pode demorar alguns minutos)..."
./gradlew assembleDebug

if [ $? -ne 0 ]; then
    echo
    echo "ERRO na compilação!"
    echo "Verifique se o Android SDK está instalado"
    echo
    exit 1
fi

echo "[4/4] APK gerado com sucesso!"
echo

APK_PATH="app/build/outputs/apk/debug/app-debug.apk"

if [ -f "$APK_PATH" ]; then
    echo "APK encontrado! Tamanho:"
    ls -lh "$APK_PATH"
    echo
    echo "Localização: $APK_PATH"
    
    # Abrir pasta no explorador (funciona no WSL)
    if command -v explorer.exe &> /dev/null; then
        echo "Abrindo pasta no Windows Explorer..."
        explorer.exe app/build/outputs/apk/debug/
    fi
else
    echo "AVISO: APK não encontrado na localização esperada"
    echo "Verifique a pasta build/outputs/apk/"
fi

echo
echo "Build concluído!"