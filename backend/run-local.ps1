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
if (-not $env:ADMIN_USERNAME) {
    $env:ADMIN_USERNAME = 'admin'
}
if (-not $env:ADMIN_PASSWORD) {
    $securePassword = Read-Host 'Set the local PulseWatch admin password' -AsSecureString
    $env:ADMIN_PASSWORD = [System.Net.NetworkCredential]::new('', $securePassword).Password
}

mvn spring-boot:run
