@echo off
cd /d "%~dp0"
javac -cp "lib\*" -d bin src\travel\core\*.java
java -cp "bin;lib\*" travel.core.Main
pause
