param(
    [string[]]$Tasks = @(':app:assembleDebug', ':app:testDebugUnitTest', ':app:assembleDebugAndroidTest'),
    [string]$LogFile = '.gradle/build.log'
)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
Push-Location $projectRoot
try {
    $env:GRADLE_RO_DEP_CACHE = "$env:USERPROFILE/.gradle/caches"
    $gradleBinary = Join-Path $projectRoot '.gradle/toolchain/gradle-9.6.0/bin/gradle.bat'
    $sdkAapt = "$env:LOCALAPPDATA/Android/Sdk/build-tools/36.0.0/aapt2.exe"
    if (!(Test-Path -LiteralPath $gradleBinary)) { throw 'La copia locale di Gradle 9.6 non è disponibile.' }
    & $gradleBinary --gradle-user-home .gradle/build-home --no-daemon --offline --console=plain `
        "-Pandroid.aapt2FromMavenOverride=$sdkAapt" '-Pkotlin.compiler.execution.strategy=in-process' @Tasks *> $LogFile
    $buildExitCode = $LASTEXITCODE
    Get-Content -LiteralPath $LogFile -Tail 75
    if ($buildExitCode -ne 0) { throw "Build fallita: vedere $LogFile" }
} finally { Pop-Location }
