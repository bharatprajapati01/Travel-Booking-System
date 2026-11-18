@echo off
cd /d "%~dp0"
javac -cp "lib\*" -d bin src\travel\core\*.java
echo Open http://localhost:8080 in your browser (Ctrl+C to stop)
java -cp "bin;lib\*" travel.core.Main web
pause
