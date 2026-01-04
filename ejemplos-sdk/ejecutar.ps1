# Script de PowerShell para ejecutar ejemplos del SDK Octane
param(
    [Parameter(Mandatory=$true)]
    [string]$IP,
    
    [Parameter(Mandatory=$false)]
    [ValidateSet("TestConexion", "TestLecturaSimple", "TestTodasAntenas", "TestCompleto")]
    [string]$Test = "TestConexion",
    
    [Parameter(Mandatory=$false)]
    [int]$Segundos = 0
)

$SDK_PATH = "..\Octane_SDK_Java_3_0_0\lib\OctaneSDKJava-3.0.0.0-jar-with-dependencies.jar"

if (-not (Test-Path $SDK_PATH)) {
    Write-Host "ERROR: No se encuentra el SDK en: $SDK_PATH" -ForegroundColor Red
    Write-Host "Por favor, verifica que el SDK esté en la carpeta correcta." -ForegroundColor Red
    exit 1
}

# Verificar si los archivos .class existen, si no, compilar
$classFile = "$Test.class"
if (-not (Test-Path $classFile)) {
    Write-Host "Compilando $Test.java..." -ForegroundColor Yellow
    javac -cp $SDK_PATH "$Test.java"
    
    if ($LASTEXITCODE -ne 0) {
        Write-Host "ERROR: La compilación falló" -ForegroundColor Red
        exit 1
    }
}

# Construir comando de ejecución
$classpath = ".;$SDK_PATH"

Write-Host "=========================================" -ForegroundColor Cyan
Write-Host "Ejecutando: $Test" -ForegroundColor Cyan
Write-Host "IP del lector: $IP" -ForegroundColor Cyan
Write-Host "=========================================" -ForegroundColor Cyan
Write-Host ""

if ($Test -eq "TestCompleto" -and $Segundos -gt 0) {
    java -cp $classpath $Test $IP $Segundos
} else {
    java -cp $classpath $Test $IP
}





