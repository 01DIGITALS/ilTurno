param(
    [ValidateSet('emulator-5554', 'emulator-5556')][string]$Serial = 'emulator-5554',
    [string]$LogName = 'device-tests',
    [string]$TestClass = ''
)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$adbBinary = "$env:LOCALAPPDATA/Android/Sdk/platform-tools/adb.exe"
Push-Location $projectRoot
try {
    New-Item -ItemType Directory -Force build/qa | Out-Null
    foreach ($apk in @('app/build/outputs/apk/debug/app-debug.apk', 'app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk')) {
        & $adbBinary -s $Serial install -r $apk
        if ($LASTEXITCODE -ne 0) { throw "Installazione fallita: $apk" }
    }
    $testArgs = @('-s', $Serial, 'shell', 'am', 'instrument', '-w')
    if ($TestClass) { $testArgs += @('-e', 'class', $TestClass) }
    $testArgs += 'it.sanges.ilturno.test/androidx.test.runner.AndroidJUnitRunner'
    $testLog = Join-Path $projectRoot "build/qa/$LogName.log"
    & $adbBinary @testArgs *> $testLog
    $testExitCode = $LASTEXITCODE
    $result = Get-Content -LiteralPath $testLog -Raw
    Write-Output $result
    if ($testExitCode -ne 0 -or $result -notmatch 'OK \(\d+ tests?\)' -or $result -match 'FAILURES|INSTRUMENTATION_FAILED|Process crashed') {
        throw "Test falliti: vedere $testLog"
    }
} finally { Pop-Location }
