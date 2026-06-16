# Runs Flyway + prod-profile startup test against ephemeral PostgreSQL (Testcontainers).
$ErrorActionPreference = "Stop"
$ProjectRoot = Split-Path -Parent $PSScriptRoot
Set-Location $ProjectRoot

$javaHome = & "$PSScriptRoot\resolve-java.ps1"
if ($javaHome) {
    $env:JAVA_HOME = $javaHome
    $env:Path = "$javaHome\bin;" + $env:Path
}

Write-Host "Running Flyway migration integration test (prod profile + PostgreSQL)..." -ForegroundColor Cyan
mvn -B test -Dtest=FlywayMigrationIntegrationTest
