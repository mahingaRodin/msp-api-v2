# Run from repo root or this folder.
#   .\load-test\run-all.ps1
#   .\load-test\run-all.ps1 -CustomerEmail a@b.com -CustomerPassword x -AdminEmail o@b.com -AdminPassword y
#   .\load-test\run-all.ps1 -PlaceOrders   # also writes real checkout orders (test account only)

param(
  [string] $BaseUrl = "http://localhost:5000",
  [string] $CustomerEmail = $env:CUSTOMER_EMAIL,
  [string] $CustomerPassword = $env:CUSTOMER_PASSWORD,
  [string] $AdminEmail = $env:ADMIN_EMAIL,
  [string] $AdminPassword = $env:ADMIN_PASSWORD,
  [switch] $PlaceOrders
)

$ErrorActionPreference = "Stop"
$here = Split-Path -Parent $MyInvocation.MyCommand.Path
$k6dir = Join-Path $here "k6"

function Need-K6 {
  $k6 = Get-Command k6 -ErrorAction SilentlyContinue
  if (-not $k6) {
    Write-Host "k6 is not installed. On Windows: winget install Grafana.k6"
    Write-Host "Then reopen the terminal and run this script again."
    exit 1
  }
}

function Invoke-K6([string] $file, [string[]] $extra) {
  Write-Host "`n>>> k6 $file" -ForegroundColor Cyan
  Push-Location $k6dir
  try {
    & k6 run @extra $file
    if ($LASTEXITCODE -ne 0) { throw "k6 $file failed with exit $LASTEXITCODE" }
  } finally {
    Pop-Location
  }
}

Need-K6

Write-Host "Checking API $BaseUrl/actuator/health ..."
try {
  $h = Invoke-RestMethod -Uri "$BaseUrl/actuator/health" -TimeoutSec 5
  Write-Host "API health: $($h.status)"
} catch {
  Write-Host "API is not reachable at $BaseUrl. Start msp-api (port 5000) first."
  exit 1
}

Invoke-K6 "catalog.js" @("-e", "BASE_URL=$BaseUrl")

if ($CustomerEmail -and $CustomerPassword) {
  Invoke-K6 "customer.js" @(
    "-e", "BASE_URL=$BaseUrl",
    "-e", "EMAIL=$CustomerEmail",
    "-e", "PASSWORD=$CustomerPassword"
  )
  $checkoutArgs = @(
    "-e", "BASE_URL=$BaseUrl",
    "-e", "EMAIL=$CustomerEmail",
    "-e", "PASSWORD=$CustomerPassword"
  )
  if ($PlaceOrders) { $checkoutArgs += @("-e", "PLACE_ORDER=1") }
  Invoke-K6 "checkout.js" $checkoutArgs
} else {
  Write-Host "`nSkip customer/checkout — pass -CustomerEmail and -CustomerPassword" -ForegroundColor Yellow
}

if ($AdminEmail -and $AdminPassword) {
  Invoke-K6 "store-admin.js" @(
    "-e", "BASE_URL=$BaseUrl",
    "-e", "EMAIL=$AdminEmail",
    "-e", "PASSWORD=$AdminPassword"
  )
} else {
  Write-Host "`nSkip store-admin — pass -AdminEmail and -AdminPassword" -ForegroundColor Yellow
}

Write-Host "`nDone. For browser scores (FE must be running):" -ForegroundColor Green
Write-Host "  node load-test/lighthouse.mjs"
Write-Host "  `$env:FE_URL='http://localhost:4173'; node load-test/lighthouse.mjs"
