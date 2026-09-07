$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'showcase-common.ps1')
Import-ShowcaseEnvironment
$api=Get-ShowcaseRuntimePort 'java' 'MALL_API_PORT' '8080'; $python=Get-ShowcaseRuntimePort 'python' 'AI_SERVICE_PORT' '8000'; $admin=Get-ShowcaseRuntimePort 'admin' 'ADMIN_WEB_PORT' '5174'; $mobile=Get-ShowcaseRuntimePort 'mobile' 'MOBILE_H5_PORT' '5173'
$state=Get-ShowcaseState
if($null -eq $state){Write-Host 'NO_SHOWCASE_STATE'} else {
    foreach($entry in $state.processes){
        $identity=Test-ShowcaseIdentity $entry; $listener=@(Test-ShowcasePort ([int]$entry.port))
        $label=if($identity){'RUNNING'}elseif($listener.Count){'STALE_OR_FOREIGN'}else{'STOPPED'}
        $workingDirectory = if ($null -ne $entry.PSObject.Properties['workingDirectory']) { $entry.workingDirectory } else { 'legacy-state-not-recorded' }
        Write-Host "$($entry.name): $label PID=$($entry.pid) PORT=$($entry.port) WORKDIR=$workingDirectory LOG=$($entry.log)"
    }
}
$healthStatus=Get-ShowcaseHttpStatus "http://127.0.0.1:$api/actuator/health"
$readinessStatus=Get-ShowcaseHttpStatus "http://127.0.0.1:$api/actuator/health/readiness"
$pythonStatus=Get-ShowcaseHttpStatus "http://127.0.0.1:$python/health"
$adminStatus=Get-ShowcaseHttpStatus "http://127.0.0.1:$admin"
$mobileStatus=Get-ShowcaseHttpStatus "http://127.0.0.1:$mobile"
Write-Host "Health: api=$healthStatus readiness=$readinessStatus python=$pythonStatus admin=$adminStatus mobile=$mobileStatus"
Write-Host "URLs: admin=http://127.0.0.1:$admin mobile=http://127.0.0.1:$mobile api=http://127.0.0.1:$api"
if($pythonStatus -eq 200){$provider=Invoke-RestMethod "http://127.0.0.1:$python/health"; Write-Host "Provider: $($provider.provider) / $($provider.providerMode)"}
if($readinessStatus -eq 200){$overview=Invoke-RestMethod "http://127.0.0.1:$api/api/v1/operator/operations/overview"; Write-Host "Rate limit: $($overview.runtimeBoundary.rateLimitAlgorithm), $($overview.runtimeBoundary.rateLimitLimit)/$($overview.runtimeBoundary.rateLimitWindowSeconds)s, $($overview.runtimeBoundary.rateLimitFailurePolicy)"; Write-Host "Runtime scope: $($overview.runtimeBoundary.dataScope), auth=$($overview.runtimeBoundary.authenticationMode)"}
Push-Location $script:ShowcaseRoot
$composeLines = @(docker compose -p $script:ShowcaseComposeProject ps --format json)
$composeServices = @($composeLines | Where-Object { -not [string]::IsNullOrWhiteSpace($_) } | ForEach-Object { $_ | ConvertFrom-Json })
$mysqlService = @($composeServices | Where-Object { $_.Service -eq 'mysql' } | Select-Object -First 1)
$redisService = @($composeServices | Where-Object { $_.Service -eq 'redis' } | Select-Object -First 1)
$mysqlState = if ($mysqlService.Count) { "$($mysqlService[0].State)/$($mysqlService[0].Health)" } else { 'not-running' }
$redisState = if ($redisService.Count) { "$($redisService[0].State)/$($redisService[0].Health)" } else { 'not-running' }
Write-Host "Compose: mysql=$mysqlState redis=$redisState"
$mysqlPassword=Get-ShowcaseValue -Name 'MYSQL_PASSWORD' -Fallback 'commerceflow'; $mysqlUser=Get-ShowcaseValue -Name 'MYSQL_USER' -Fallback 'commerceflow'; $mysqlDatabase=Get-ShowcaseValue -Name 'MYSQL_DATABASE' -Fallback 'commerceflow'
try { if ($mysqlService.Count -and $mysqlService[0].State -eq 'running') {$flywayVersion=docker compose -p $script:ShowcaseComposeProject exec -T -e "MYSQL_PWD=$mysqlPassword" mysql mysql -u $mysqlUser -N -s -e "SELECT MAX(version) FROM flyway_schema_history WHERE success=1" $mysqlDatabase 2>$null; if($LASTEXITCODE -eq 0){Write-Host "Flyway: V$flywayVersion"}} else {Write-Host 'Flyway: unavailable (MySQL not running)'} } finally {Pop-Location}
Write-Host "Git: $(git -C $script:ShowcaseRoot branch --show-current) $(git -C $script:ShowcaseRoot rev-parse --short HEAD)"; if(git -C $script:ShowcaseRoot status --porcelain){Write-Host 'Git worktree: DIRTY'}else{Write-Host 'Git worktree: CLEAN'}



