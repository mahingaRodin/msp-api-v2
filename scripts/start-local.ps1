# Starts LocalStack, then runs the Spring Boot app with the local profile.
# Prerequisites: PostgreSQL on localhost:5432 and Redis on localhost:6379 (see .env).
$ErrorActionPreference = "Stop"
$ProjectRoot = Split-Path -Parent $PSScriptRoot
Set-Location $ProjectRoot

& (Join-Path $PSScriptRoot "ensure-localstack.ps1")
if ($null -ne $LASTEXITCODE -and $LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

Write-Host "==> Initializing AWS resources in LocalStack..." -ForegroundColor Cyan
& (Join-Path $PSScriptRoot "init-localstack.ps1")

# Load .env if present (optional overrides)
$envFile = Join-Path $ProjectRoot ".env"
if (Test-Path $envFile) {
    Get-Content $envFile | ForEach-Object {
        if ($_ -match '^\s*([^#][^=]+)=(.*)$') {
            $name = $matches[1].Trim()
            $value = $matches[2].Trim()
            Set-Item -Path "Env:$name" -Value $value
        }
    }
}

& (Join-Path $PSScriptRoot "resolve-java.ps1")
if ($null -ne $LASTEXITCODE -and $LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

Write-Host "==> Starting Spring Boot (profile: local) on http://localhost:5000" -ForegroundColor Cyan
Write-Host "    Swagger UI: http://localhost:5000/swagger-ui.html" -ForegroundColor Cyan
& .\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local"
