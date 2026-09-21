param([switch]$SkipBuild)
$ErrorActionPreference = 'Stop'
$projectRoot = $PSScriptRoot
$runtimePath = Join-Path $projectRoot '.runtime'
$backendPath = Join-Path $projectRoot 'backend'
$jarPath = Join-Path $backendPath 'target\pulsewatch-api-1.0.0.jar'
New-Item -ItemType Directory -Force -Path $runtimePath | Out-Null
foreach ($tool in @('java', 'mvn', 'npm')) {
    if (-not (Get-Command $tool -ErrorAction SilentlyContinue)) { throw "Install $tool and add it to PATH before starting PulseWatch." }
}
$listener = Get-NetTCPConnection -LocalPort 8080 -State Listen -ErrorAction SilentlyContinue
if ($listener) {
    try {
        $page = Invoke-WebRequest 'http://127.0.0.1:8080/' -UseBasicParsing -TimeoutSec 3
        if ($page.Content -match '<title>PulseWatch') {
            Write-Host 'PulseWatch is already running: http://localhost:8080'
            Write-Host 'Use Stop-PulseWatch.ps1 before rebuilding changed code.'
            return
        }
    } catch { }
    throw 'Port 8080 is occupied. Stop the existing server before starting PulseWatch.'
}
Push-Location $projectRoot
try {
    if (-not $SkipBuild) {
        if (-not (Test-Path 'frontend\node_modules')) {
            npm ci --prefix frontend
            if ($LASTEXITCODE -ne 0) { throw 'Dependency installation failed.' }
        }
        npm run build --prefix frontend
        if ($LASTEXITCODE -ne 0) { throw 'Dashboard build failed.' }
        mvn -q -f backend/pom.xml clean package -DskipTests
        if ($LASTEXITCODE -ne 0) { throw 'API build failed.' }
    }
    if (-not (Test-Path -LiteralPath $jarPath)) { throw 'Application package missing. Run without -SkipBuild.' }
    # A short path avoids the Windows JDK Unix-domain socket path limitation.
    $shortTemp = Join-Path $env:SystemDrive 'tmp'
    New-Item -ItemType Directory -Force -Path $shortTemp | Out-Null
    $env:TEMP = $shortTemp
    $env:TMP = $shortTemp
    $server = Start-Process -FilePath (Get-Command java).Source -ArgumentList @("-Djava.io.tmpdir=$shortTemp", '-jar', "`"$jarPath`"", '--spring.profiles.active=local', '--server.port=8080') -WorkingDirectory $backendPath -WindowStyle Hidden -PassThru -RedirectStandardOutput (Join-Path $runtimePath 'server.log') -RedirectStandardError (Join-Path $runtimePath 'server-error.log')
    $server.Id | Set-Content (Join-Path $runtimePath 'server.pid')
    for ($attempt = 0; $attempt -lt 60; $attempt++) {
        if ($server.HasExited) { throw "Server stopped. See $runtimePath\server.log and server-error.log" }
        try {
            $health = Invoke-RestMethod 'http://127.0.0.1:8080/actuator/health' -TimeoutSec 2
            if ($health.status -eq 'UP') {
                Write-Host 'PulseWatch is ready: http://localhost:8080'
                Write-Host 'No login. Data persists in backend/data. Stop with Stop-PulseWatch.ps1.'
                return
            }
        } catch { }
        Start-Sleep -Seconds 1
    }
    throw "Startup did not become healthy. See $runtimePath\server.log"
} finally { Pop-Location }
