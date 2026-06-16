# Ensures a healthy msp-localstack container is running.
$ErrorActionPreference = "Stop"
$ProjectRoot = Split-Path -Parent $PSScriptRoot
Set-Location $ProjectRoot

function Test-LocalStackHealthy {
    $health = docker inspect --format='{{if .State.Health}}{{.State.Health.Status}}{{else}}none{{end}}' msp-localstack 2>$null
    return $health -eq "healthy"
}

$existing = docker ps -aq -f name=^msp-localstack$ 2>$null
if ($existing -and -not (Test-LocalStackHealthy)) {
    Write-Host "Removing stale msp-localstack container..." -ForegroundColor Yellow
    docker rm -f msp-localstack | Out-Null
}

Write-Host "==> Starting LocalStack..." -ForegroundColor Cyan
docker compose up -d

Write-Host "==> Waiting for LocalStack..." -ForegroundColor Cyan
$maxAttempts = 60
$ready = $false
for ($i = 1; $i -le $maxAttempts; $i++) {
    if (Test-LocalStackHealthy) {
        Write-Host "LocalStack is ready." -ForegroundColor Green
        $ready = $true
        break
    }
    Start-Sleep -Seconds 2
}

if (-not $ready) {
    Write-Host "Timed out waiting for LocalStack. Check: docker logs msp-localstack" -ForegroundColor Red
    exit 1
}
