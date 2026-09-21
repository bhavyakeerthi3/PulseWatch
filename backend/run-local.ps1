$ErrorActionPreference = 'Stop'

# Windows Java NIO uses a temporary Unix-domain socket whose path must stay short.
$shortTemp = 'C:\tmp'
if (-not (Test-Path $shortTemp)) {
    New-Item -ItemType Directory -Path $shortTemp | Out-Null
}
$env:TEMP = $shortTemp
$env:TMP = $shortTemp
$env:JAVA_TOOL_OPTIONS = "-Djava.io.tmpdir=$shortTemp"

$env:SPRING_PROFILES_ACTIVE = 'local'
Push-Location $PSScriptRoot
try { mvn spring-boot:run } finally { Pop-Location }
