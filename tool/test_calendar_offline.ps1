param(
    [Parameter(Mandatory = $true)][string]$GradleHome,
    [string]$DependencyCache = "$env:USERPROFILE/.gradle/caches/modules-2/files-2.1"
)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$gradleLib = Join-Path $GradleHome 'lib'
$kotlinStdlib = Get-ChildItem -LiteralPath $gradleLib -Filter 'kotlin-stdlib-*.jar' | Select-Object -First 1
$junitJar = Get-ChildItem -LiteralPath (Join-Path $DependencyCache 'junit/junit/4.13.2') -Recurse -Filter 'junit-4.13.2.jar' | Select-Object -First 1
$hamcrestJar = Get-ChildItem -LiteralPath (Join-Path $DependencyCache 'org.hamcrest/hamcrest-core/1.3') -Recurse -Filter 'hamcrest-core-1.3.jar' | Select-Object -First 1
if (!$kotlinStdlib -or !$junitJar -or !$hamcrestJar) {
    throw 'Servono il compilatore Kotlin della distribuzione Gradle, JUnit 4.13.2 e Hamcrest 1.3 nella cache locale.'
}
$javaBinary = Join-Path $env:JAVA_HOME 'bin/java.exe'
$classpath = @($kotlinStdlib.FullName, $junitJar.FullName, $hamcrestJar.FullName) -join ';'
$outputDirectory = Join-Path $projectRoot 'build/m1-calendar'
New-Item -ItemType Directory -Path $outputDirectory -Force | Out-Null
$testJar = Join-Path $outputDirectory 'tests.jar'
& $javaBinary -cp "$gradleLib/*" org.jetbrains.kotlin.cli.jvm.K2JVMCompiler `
    -no-stdlib -no-reflect -jvm-target 11 -classpath $classpath -d $testJar `
    (Join-Path $projectRoot 'app/src/main/java/it/sanges/ilturno/util/DateUtils.kt') `
    (Join-Path $projectRoot 'app/src/test/java/it/sanges/ilturno/DateUtilsTest.kt')
if ($LASTEXITCODE -ne 0) { throw 'Compilazione dei test calendario fallita.' }
& $javaBinary -cp "$testJar;$classpath" org.junit.runner.JUnitCore it.sanges.ilturno.DateUtilsTest
if ($LASTEXITCODE -ne 0) { throw 'Test calendario falliti.' }
