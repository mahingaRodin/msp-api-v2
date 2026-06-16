# Ensures JAVA_HOME points to JDK 21+ (required by pom.xml).
$ErrorActionPreference = "Stop"

function Get-JavaMajorVersion {
    param([string]$JavaHome)
    $javaExe = Join-Path $JavaHome "bin\java.exe"
    if (-not (Test-Path $javaExe)) { return 0 }
    $versionLine = cmd /c "`"$javaExe`" -version 2>&1" | Select-Object -First 1
    if ($versionLine -match 'version "(\d+)') { return [int]$matches[1] }
    if ($versionLine -match 'version "1\.(\d+)') { return [int]$matches[1] }
    return 0
}

function Find-Jdk21Plus {
    $candidates = @(
        $env:JAVA_HOME,
        "C:\Program Files\Java\jdk-22",
        "C:\Program Files\Java\jdk-21",
        "C:\Program Files\Eclipse Adoptium\jdk-21*",
        "C:\Program Files\Microsoft\jdk-21*"
    ) | Where-Object { $_ }

    foreach ($candidate in $candidates) {
        foreach ($path in @(Get-Item $candidate -ErrorAction SilentlyContinue)) {
            if ((Get-JavaMajorVersion $path.FullName) -ge 21) {
                return $path.FullName
            }
        }
    }
    return $null
}

$jdk = Find-Jdk21Plus
if (-not $jdk) {
    Write-Host "JDK 21+ is required but was not found." -ForegroundColor Red
    Write-Host "Install JDK 21 or 22, then set JAVA_HOME or re-run this script." -ForegroundColor Yellow
    Write-Host "Current JAVA_HOME: $env:JAVA_HOME" -ForegroundColor Yellow
    exit 1
}

$env:JAVA_HOME = $jdk
$env:Path = "$jdk\bin;" + (($env:Path -split ';' | Where-Object { $_ -and $_ -notmatch '\\Java\\jdk-' }) -join ';')

$major = Get-JavaMajorVersion $jdk
Write-Host "Using Java $major at $jdk" -ForegroundColor Green
$global:LASTEXITCODE = 0
