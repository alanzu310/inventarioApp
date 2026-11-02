@echo off
REM Compila y ejecuta la aplicacion sin necesidad de Maven (requiere JDK 17+).
cd /d "%~dp0"
if not exist build\classes mkdir build\classes
dir /s /b src\*.java > build\fuentes.txt
javac --release 17 -encoding UTF-8 -d build\classes @build\fuentes.txt
if errorlevel 1 (
    pause
    exit /b 1
)
start "" javaw -cp build\classes com.negocio.comida.App
