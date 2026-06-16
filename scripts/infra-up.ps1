# Starts LocalStack only. Postgres + Redis must already be running locally.
$ErrorActionPreference = "Stop"
$ProjectRoot = Split-Path -Parent $PSScriptRoot
Set-Location $ProjectRoot

& (Join-Path $PSScriptRoot "ensure-localstack.ps1")
if ($null -ne $LASTEXITCODE -and $LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

& (Join-Path $PSScriptRoot "init-localstack.ps1")