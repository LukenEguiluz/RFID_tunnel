@echo off
echo =========================================
echo Compilando ejemplos del SDK Octane
echo =========================================
echo.

set SDK_PATH=..\Octane_SDK_Java_3_0_0\lib\OctaneSDKJava-3.0.0.0-jar-with-dependencies.jar

if not exist "%SDK_PATH%" (
    echo ERROR: No se encuentra el SDK en: %SDK_PATH%
    echo Por favor, verifica que el SDK esté en la carpeta correcta.
    pause
    exit /b 1
)

echo Compilando archivos Java...
javac -encoding UTF-8 -cp "%SDK_PATH%" *.java

if %ERRORLEVEL% EQU 0 (
    echo.
    echo =========================================
    echo Compilacion exitosa!
    echo =========================================
    echo.
    echo Para ejecutar los ejemplos:
    echo   java -cp ".;%SDK_PATH%" TestConexion 192.168.1.100
    echo   java -cp ".;%SDK_PATH%" TestLecturaSimple 192.168.1.100
    echo   java -cp ".;%SDK_PATH%" TestTodasAntenas 192.168.1.100
    echo   java -cp ".;%SDK_PATH%" TestCompleto 192.168.1.100 30
    echo.
) else (
    echo.
    echo ERROR: La compilacion fallo
    echo.
)

pause


