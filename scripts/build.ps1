param([string[]]$Tasks=@('assemblePreviewDebug','assembleDevDebug','testPreviewDebugUnitTest','testDevDebugUnitTest','lintPreviewDebug','lintDevDebug'),[string]$ProxyHost='',[int]$ProxyPort=0)
$ErrorActionPreference='Stop'
$repo=Split-Path $PSScriptRoot -Parent
$env:ANDROID_USER_HOME=Join-Path $repo '.local\android'
New-Item -ItemType Directory -Path $env:ANDROID_USER_HOME -Force | Out-Null
$gradleArgs=@('--no-daemon','-p',$repo,'--console=plain')
if($ProxyHost -and $ProxyPort -gt 0){$gradleArgs+=@("-Dhttps.proxyHost=$ProxyHost","-Dhttps.proxyPort=$ProxyPort","-Dhttp.proxyHost=$ProxyHost","-Dhttp.proxyPort=$ProxyPort")}
& (Join-Path $repo 'gradlew.bat') @gradleArgs @Tasks
if($LASTEXITCODE -ne 0){exit $LASTEXITCODE}
