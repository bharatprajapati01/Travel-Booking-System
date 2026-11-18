$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot
javac -cp "lib/*" -d bin src/travel/core/*.java
java -cp "bin;lib/*" travel.core.Main
