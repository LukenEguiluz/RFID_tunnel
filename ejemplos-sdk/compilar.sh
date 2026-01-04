#!/bin/bash

echo "========================================="
echo "Compilando ejemplos del SDK Octane"
echo "========================================="
echo ""

SDK_PATH="../Octane_SDK_Java_3_0_0/lib/OctaneSDKJava-3.0.0.0-jar-with-dependencies.jar"

if [ ! -f "$SDK_PATH" ]; then
    echo "ERROR: No se encuentra el SDK en: $SDK_PATH"
    echo "Por favor, verifica que el SDK esté en la carpeta correcta."
    exit 1
fi

echo "Compilando archivos Java..."
javac -cp "$SDK_PATH" *.java

if [ $? -eq 0 ]; then
    echo ""
    echo "========================================="
    echo "Compilación exitosa!"
    echo "========================================="
    echo ""
    echo "Para ejecutar los ejemplos:"
    echo "  java -cp \".:$SDK_PATH\" TestConexion 192.168.1.100"
    echo "  java -cp \".:$SDK_PATH\" TestLecturaSimple 192.168.1.100"
    echo "  java -cp \".:$SDK_PATH\" TestTodasAntenas 192.168.1.100"
    echo "  java -cp \".:$SDK_PATH\" TestCompleto 192.168.1.100 30"
    echo ""
else
    echo ""
    echo "ERROR: La compilación falló"
    echo ""
    exit 1
fi





