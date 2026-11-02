#!/bin/sh
# Compila y ejecuta la aplicación sin necesidad de Maven (requiere JDK 17+).
cd "$(dirname "$0")"
mkdir -p build/classes
javac --release 17 -encoding UTF-8 -d build/classes $(find src -name '*.java') && \
java -cp build/classes com.negocio.comida.App
