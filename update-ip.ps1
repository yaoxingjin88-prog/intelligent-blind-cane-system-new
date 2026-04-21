# ========================================================================
# Auto-detect local IP and update miniapp/src/config/env.js
# Usage (from project root):  .\update-ip.ps1
# ========================================================================

$ErrorActionPreference = 'Stop'
$OutputEncoding = [System.Text.Encoding]::UTF8
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

Write-Host ""
Write-Host "[1/3] Scanning local IPs..." -ForegroundColor Cyan

$addresses = @(Get-NetIPAddress -AddressFamily IPv4 -ErrorAction SilentlyContinue |
    Where-Object { $_.IPAddress -notlike "127.*" -and $_.IPAddress -notlike "169.254.*" } |
    Select-Object IPAddress, InterfaceAlias |
    Sort-Object InterfaceAlias)

if ($addresses.Count -eq 0) {
    Write-Host "No usable local IP found." -ForegroundColor Red
    Read-Host "Press Enter to exit"
    exit 1
}

$selectedIp = $null
if ($addresses.Count -eq 1) {
    $selectedIp = $addresses[0].IPAddress
    Write-Host ("  Auto-selected: {0}  ({1})" -f $selectedIp, $addresses[0].InterfaceAlias) -ForegroundColor Green
} else {
    Write-Host ("  Found {0} IPs:" -f $addresses.Count)
    for ($i = 0; $i -lt $addresses.Count; $i++) {
        Write-Host ("    [{0}] {1,-18} {2}" -f $i, $addresses[$i].IPAddress, $addresses[$i].InterfaceAlias) -ForegroundColor White
    }
    Write-Host ""
    while ($true) {
        $idx = Read-Host ("Enter index 0 ~ {0} (default 0)" -f ($addresses.Count - 1))
        if ([string]::IsNullOrWhiteSpace($idx)) { $idx = 0 }
        $n = 0
        if ([int]::TryParse($idx, [ref]$n) -and $n -ge 0 -and $n -lt $addresses.Count) {
            $selectedIp = $addresses[$n].IPAddress
            break
        }
        Write-Host ("  Invalid index. Please enter 0 ~ {0}." -f ($addresses.Count - 1)) -ForegroundColor Yellow
    }
    Write-Host ("  Selected: {0}" -f $selectedIp) -ForegroundColor Green
}

$port = 8081
$newHost = ("{0}:{1}" -f $selectedIp, $port)

Write-Host ""
Write-Host "[2/3] Updating miniapp/src/config/env.js..." -ForegroundColor Cyan

$envFile = Join-Path $PSScriptRoot "miniapp\src\config\env.js"
if (-not (Test-Path $envFile)) {
    Write-Host ("File not found: {0}" -f $envFile) -ForegroundColor Red
    exit 1
}

$content = Get-Content $envFile -Raw -Encoding UTF8
$newContent = $content -replace "export const BACKEND_HOST = '[^']+'", ("export const BACKEND_HOST = '{0}'" -f $newHost)
[System.IO.File]::WriteAllText($envFile, $newContent, (New-Object System.Text.UTF8Encoding $false))
Write-Host ("  BACKEND_HOST = '{0}'" -f $newHost) -ForegroundColor Green

Write-Host ""
Write-Host "[3/3] Done!" -ForegroundColor Cyan
Write-Host ""
Write-Host "Next steps:" -ForegroundColor Yellow
Write-Host "  1. HBuilderX will hot-reload. If not, Ctrl+S to save any file."
Write-Host "  2. Test wake command:"
$example = ("Invoke-WebRequest -Method POST -Uri 'http://{0}/api/device/ai-wake?deviceId=DEVICE001' -ContentType 'application/json' -Body '" + '{}' + "' -UseBasicParsing") -f $newHost
Write-Host ("     " + $example) -ForegroundColor Gray
Write-Host ""
Write-Host "Press any key to exit..." -ForegroundColor DarkGray
$null = $Host.UI.RawUI.ReadKey('NoEcho,IncludeKeyDown')
