$ErrorActionPreference = 'Stop'
$pidPath = Join-Path $PSScriptRoot '.runtime\server.pid'
if (-not (Test-Path -LiteralPath $pidPath)) { Write-Host 'No managed PulseWatch process found.'; return }
$serverId = [int](Get-Content -LiteralPath $pidPath)
$server = Get-CimInstance Win32_Process -Filter "ProcessId = $serverId" -ErrorAction SilentlyContinue
$expectedJar = Join-Path $PSScriptRoot 'backend\target\pulsewatch-api-1.0.0.jar'
if ($server -and $server.CommandLine.Contains($expectedJar)) {
    Stop-Process -Id $serverId
    Write-Host 'PulseWatch stopped. Saved monitoring data is retained.'
} elseif ($server) { throw 'The recorded process belongs to another application; it was not stopped.' }
Remove-Item -LiteralPath $pidPath
