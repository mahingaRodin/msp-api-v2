# Enables repo git hooks that strip Cursor co-author lines from commits.
$ErrorActionPreference = "Stop"
$ProjectRoot = Split-Path -Parent $PSScriptRoot
Set-Location $ProjectRoot

git config core.hooksPath .githooks

$hookPath = ".githooks/prepare-commit-msg"
if (-not (Test-Path $hookPath)) {
    Write-Error "Missing $hookPath"
    exit 1
}

Write-Host "Git hooks enabled ($hookPath)" -ForegroundColor Green
Write-Host "Also disable in Cursor: Settings -> Agents -> Attribution -> turn OFF Commit + PR Attribution"
