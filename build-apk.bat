@echo off
echo ===============================================
echo    JCRing App - Build Script
echo ===============================================
echo.

echo [1/4] Verificando ambiente...
if not exist gradlew.bat (
    echo ERRO: gradlew.bat nao encontrado!
    echo Execute este script na pasta JCRingApp
    pause
    exit /b 1
)

echo [2/4] Limpando builds anteriores...
call gradlew.bat clean

echo [3/4] Compilando aplicacao (pode demorar alguns minutos)...
call gradlew.bat assembleDebug

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo ERRO na compilacao!
    echo Verifique se o Android SDK esta instalado
    echo.
    pause
    exit /b 1
)

echo [4/4] APK gerado com sucesso!
echo.
echo Localizacao: app\build\outputs\apk\debug\app-debug.apk
echo.

if exist "app\build\outputs\apk\debug\app-debug.apk" (
    echo APK encontrado! Tamanho:
    dir "app\build\outputs\apk\debug\app-debug.apk" | find "app-debug.apk"
    echo.
    echo Pressione qualquer tecla para abrir a pasta...
    pause >nul
    explorer app\build\outputs\apk\debug\
) else (
    echo AVISO: APK nao encontrado na localizacao esperada
    echo Verifique a pasta build/outputs/apk/
)

echo.
echo Build concluido!
pause