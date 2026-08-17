# Production-style load tests — run against STAGING or a dedicated load-test stack only.
#
#   .\load-test\run-prod.ps1 -BaseUrl https://staging.example.com -Profile ramp
#   .\load-test\run-prod.ps1 -BaseUrl http://localhost:5000 -AllowLocal -Profile soak
#
# Profiles: ramp (default, ~4m) | soak (~15m) | stress (~10m, find limits)

param(
  [Parameter(Mandatory = $true)]
  [string] $BaseUrl,
  [ValidateSet("ramp", "soak", "stress")]
  [string] $Profile = "ramp",
  [switch] $AllowLocal,
  [switch] $Mixed,
  [string] $CustomerEmails = $env:CUSTOMER_EMAILS,
  [string] $CustomerPasswords = $env:CUSTOMER_PASSWORDS,
  [string] $AdminEmails = $env:ADMIN_EMAILS,
  [string] $AdminPasswords = $env:ADMIN_PASSWORDS
)

$ErrorActionPreference = "Stop"
$here = Split-Path -Parent $MyInvocation.MyCommand.Path
$k6dir = Join-Path $here "k6"

if (-not (Get-Command k6 -ErrorAction SilentlyContinue)) {
  Write-Host "Install k6 first (e.g. choco install k6)." -ForegroundColor Red
  exit 1
}

$hostOnly = ([uri]$BaseUrl).Host
$isLocal = $hostOnly -in @("localhost", "127.0.0.1", "::1")
if ($isLocal -and -not $AllowLocal) {
  Write-Host "Refusing to run production profile against $BaseUrl." -ForegroundColor Yellow
  Write-Host "Use -AllowLocal for dev-machine experiments, or point -BaseUrl at staging."
  exit 1
}

Write-Host "Target: $BaseUrl  Profile: $Profile" -ForegroundColor Cyan
if (-not $isLocal) {
  Write-Host "Confirm this is a disposable staging / load-test environment (not live production)." -ForegroundColor Yellow
}

try {
  $h = Invoke-RestMethod -Uri "$BaseUrl/actuator/health" -TimeoutSec 10
  Write-Host "Health: $($h.status)"
} catch {
  Write-Host "API not reachable at $BaseUrl" -ForegroundColor Red
  exit 1
}

Push-Location $k6dir
try {
  if ($Mixed) {
    if (-not $CustomerEmails -or -not $CustomerPasswords) {
      Write-Host "Mixed mode needs CUSTOMER_EMAILS and CUSTOMER_PASSWORDS (comma-separated pools)." -ForegroundColor Red
      exit 1
    }
    $args = @(
      "run",
      "-e", "BASE_URL=$BaseUrl",
      "-e", "CUSTOMER_EMAILS=$CustomerEmails",
      "-e", "CUSTOMER_PASSWORDS=$CustomerPasswords"
    )
    if ($AdminEmails -and $AdminPasswords) {
      $args += @("-e", "ADMIN_EMAILS=$AdminEmails", "-e", "ADMIN_PASSWORDS=$AdminPasswords")
    }
    $args += "prod-mixed.js"
    & k6 @args
  } else {
    & k6 run -e "BASE_URL=$BaseUrl" -e "PROFILE=$Profile" prod-catalog.js
  }
  if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
} finally {
  Pop-Location
}

Write-Host "`nDone. Compare p95/p99 and http_req_failed to SLOs in load-test/README.md." -ForegroundColor Green
