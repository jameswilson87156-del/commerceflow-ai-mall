[CmdletBinding()]
param([switch]$IncludeMobile, [switch]$NoBrowser, [ValidateRange(10,180)][int]$WaitTimeoutSeconds = 60, [switch]$ForceRestart)
$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'showcase-common.ps1')
Import-ShowcaseEnvironment
@('docker','java','mvn','node','npm','py') | ForEach-Object { Assert-ShowcaseCommand $_ }
if (Get-ShowcaseState) { if ($ForceRestart) { & (Join-Path $PSScriptRoot 'stop.ps1') } else { throw '检测到已有 Showcase 状态文件；请先运行 status.ps1 或使用 -ForceRestart。' } }
$mysqlPortText = Get-ShowcaseValue -Name 'MYSQL_PORT' -Fallback '3307'
$redisPortText = Get-ShowcaseValue -Name 'REDIS_PORT' -Fallback '6380'
$apiPortText = Get-ShowcaseValue -Name 'MALL_API_PORT' -Fallback '8080'
$pythonPortText = Get-ShowcaseValue -Name 'AI_SERVICE_PORT' -Fallback '8000'
$adminPortText = Get-ShowcaseValue -Name 'ADMIN_WEB_PORT' -Fallback '5174'
$mobilePortText = Get-ShowcaseValue -Name 'MOBILE_H5_PORT' -Fallback '5173'
$mysqlPort = [Convert]::ToInt32($mysqlPortText); $redisPort = [Convert]::ToInt32($redisPortText)
$apiPort = [Convert]::ToInt32($apiPortText); $pythonPort = [Convert]::ToInt32($pythonPortText)
$adminPort = [Convert]::ToInt32($adminPortText); $mobilePort = [Convert]::ToInt32($mobilePortText)
$started = @()
try {
    Push-Location $script:ShowcaseRoot
    docker compose -p $script:ShowcaseComposeProject up -d mysql redis | Out-Host
    $until=(Get-Date).AddSeconds($WaitTimeoutSeconds); do { $healthy = @((docker compose -p $script:ShowcaseComposeProject ps --format json | ConvertFrom-Json | Where-Object { $_.Health -eq 'healthy' })).Count -ge 2; if(-not $healthy){Start-Sleep 1} } while(-not $healthy -and (Get-Date) -lt $until); if(-not $healthy){throw 'MySQL 或 Redis 未达到 healthy。'}
    $started += Start-ShowcaseProcess 'java' '.\mvnw.cmd' '-f apps/mall-api/pom.xml spring-boot:run' $script:ShowcaseRoot 'com.commerceflow.mall.MallApiApplication' $apiPort
    Wait-ShowcaseHttp "http://127.0.0.1:$apiPort/actuator/health" $WaitTimeoutSeconds
    $started[-1] = Update-ShowcaseListenerEntry $started[-1]
    $started += Start-ShowcaseProcess 'python' 'py' "-3 -m uvicorn app.main:app --app-dir services/ai-service --host 127.0.0.1 --port $pythonPort" $script:ShowcaseRoot 'uvicorn app.main:app' $pythonPort
    Wait-ShowcaseHttp "http://127.0.0.1:$pythonPort/health" $WaitTimeoutSeconds
    $started[-1] = Update-ShowcaseListenerEntry $started[-1]
    $env:VITE_ADMIN_PORT=$adminPort; $env:VITE_ADMIN_HOST='127.0.0.1'; $env:VITE_ADMIN_DEV_API_TARGET="http://127.0.0.1:$apiPort"; $started += Start-ShowcaseProcess 'admin' 'npm.cmd' 'run dev -- --host 127.0.0.1' (Join-Path $script:ShowcaseRoot 'apps/admin-web') 'admin-web' $adminPort
    Wait-ShowcaseHttp "http://127.0.0.1:$adminPort" $WaitTimeoutSeconds
    $started[-1] = Update-ShowcaseListenerEntry $started[-1]
    if ($IncludeMobile) { $env:VITE_MOBILE_PORT=$mobilePort; $env:VITE_MOBILE_HOST='127.0.0.1'; $env:VITE_MOBILE_DEV_API_TARGET="http://127.0.0.1:$apiPort"; $started += Start-ShowcaseProcess 'mobile' 'npm.cmd' 'run dev -- --host 127.0.0.1' (Join-Path $script:ShowcaseRoot 'apps/mobile-app') 'mobile-app' $mobilePort; Wait-ShowcaseHttp "http://127.0.0.1:$mobilePort" $WaitTimeoutSeconds; $started[-1] = Update-ShowcaseListenerEntry $started[-1] }
    Save-ShowcaseState ([pscustomobject]@{ composeProject=$script:ShowcaseComposeProject; startedAt=(Get-Date).ToString('o'); processes=$started })
    Write-Host "Admin: http://127.0.0.1:$adminPort"; if($IncludeMobile){Write-Host "Mobile H5: http://127.0.0.1:$mobilePort"}; if(-not $NoBrowser){Start-Process "http://127.0.0.1:$adminPort"}
} catch { foreach($entry in $started){ if(Test-ShowcaseIdentity $entry){Stop-Process -Id $entry.pid -ErrorAction SilentlyContinue} }; docker compose -p $script:ShowcaseComposeProject stop mysql redis | Out-Null; throw } finally { Pop-Location }









