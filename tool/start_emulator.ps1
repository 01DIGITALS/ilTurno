param([ValidateSet(36, 37)][int]$Api = 36)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$emulatorRoot = Join-Path $projectRoot $(if ($Api -eq 36) { '.gradle/emulator' } else { '.gradle/emulator-api37' })
$avdName = "IlTurno_API$Api"
$port = if ($Api -eq 36) { 5554 } else { 5556 }
$sourceAvd = if ($Api -eq 36) { 'Pixel_10' } else { 'Pixel_10a' }
$avdDirectory = Join-Path $emulatorRoot "$avdName.avd"
New-Item -ItemType Directory -Force $avdDirectory | Out-Null
$sourceConfig = "$env:USERPROFILE/.android/avd/$sourceAvd.avd/config.ini"
$config = Get-Content -LiteralPath $sourceConfig -Raw
$config = $config -replace '(?m)^AvdId\s*=.*', "AvdId=$avdName"
$config = $config -replace '(?m)^avd.ini.displayname\s*=.*', "avd.ini.displayname=ilTurno API $Api"
$config = $config -replace '(?m)^disk.dataPartition.size\s*=.*', 'disk.dataPartition.size=2G'
$config = $config -replace '(?m)^hw.sdCard\s*=.*', 'hw.sdCard=no'
$config = $config -replace '(?m)^hw.camera.(back|front)\s*=.*', 'hw.camera.$1=none'
Set-Content -LiteralPath (Join-Path $avdDirectory 'config.ini') -Value $config -Encoding utf8
Set-Content -LiteralPath (Join-Path $emulatorRoot "$avdName.ini") -Value "avd.ini.encoding=UTF-8`npath=$avdDirectory`ntarget=android-$Api.1" -Encoding utf8
$env:ANDROID_AVD_HOME = $emulatorRoot
$env:ANDROID_USER_HOME = Join-Path $emulatorRoot 'user'
$env:ANDROID_EMULATOR_HOME = $env:ANDROID_USER_HOME
New-Item -ItemType Directory -Force $env:ANDROID_USER_HOME | Out-Null
$adbBinary = "$env:LOCALAPPDATA/Android/Sdk/platform-tools/adb.exe"
$publicKey = "$env:USERPROFILE/.android/adbkey.pub"
if (Test-Path -LiteralPath $publicKey) {
    Copy-Item -LiteralPath $publicKey -Destination (Join-Path $env:ANDROID_USER_HOME 'adbkey.pub') -Force
}
& $adbBinary start-server
if ($LASTEXITCODE -ne 0) { throw 'Impossibile avviare ADB.' }
$process = Start-Process -FilePath "$env:LOCALAPPDATA/Android/Sdk/emulator/emulator.exe" -WindowStyle Hidden `
    -ArgumentList @('-avd', $avdName, '-no-window', '-no-audio', '-no-snapshot', '-no-boot-anim', '-gpu', 'swiftshader', '-port', "$port", '-no-metrics', '-adb-path', $adbBinary) `
    -RedirectStandardOutput (Join-Path $emulatorRoot 'stdout.log') `
    -RedirectStandardError (Join-Path $emulatorRoot 'stderr.log') -PassThru
Write-Output "Emulatore API $Api avviato: PID $($process.Id), porta $port"
