Set-StrictMode -Version Latest

$script:ShowcaseRoot = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path
$script:ShowcaseStateDirectory = Join-Path $script:ShowcaseRoot '.showcase'
$script:ShowcaseStateFile = Join-Path $script:ShowcaseStateDirectory 'processes.json'
$script:ShowcaseComposeProject = 'commerceflow-showcase'

function Import-ShowcaseEnvironment {
    $file = Join-Path $script:ShowcaseRoot '.env'
    if (-not (Test-Path -LiteralPath $file)) { $file = Join-Path $script:ShowcaseRoot '.env.example' }
    Get-Content -LiteralPath $file | Where-Object { $_ -match '^[A-Za-z_][A-Za-z0-9_]*=' } | ForEach-Object {
        $name, $value = $_ -split '=', 2
        if ([string]::IsNullOrWhiteSpace([Environment]::GetEnvironmentVariable($name))) { Set-Item -Path "Env:$name" -Value $value }
    }
}

function Get-ShowcaseValue([string]$Name, [string]$Fallback) {
    $value = [Environment]::GetEnvironmentVariable($Name)
    if ([string]::IsNullOrWhiteSpace($value)) { return $Fallback }
    return $value
}

function Get-ShowcaseRuntimePort([string]$ServiceName, [string]$EnvironmentName, [string]$Fallback) {
    $state = Get-ShowcaseState
    if ($null -ne $state) {
        $entry = @($state.processes | Where-Object { $_.name -eq $ServiceName } | Select-Object -First 1)
        if ($entry.Count -eq 1 -and -not [string]::IsNullOrWhiteSpace([string]$entry[0].port)) { return [string]$entry[0].port }
    }
    return Get-ShowcaseValue -Name $EnvironmentName -Fallback $Fallback
}

function Initialize-ShowcaseStateDirectory { if (-not (Test-Path $script:ShowcaseStateDirectory)) { New-Item -ItemType Directory -Path $script:ShowcaseStateDirectory | Out-Null } }
function Get-ShowcaseState { if (Test-Path $script:ShowcaseStateFile) { return Get-Content $script:ShowcaseStateFile -Raw | ConvertFrom-Json } return $null }
function Save-ShowcaseState($State) { Initialize-ShowcaseStateDirectory; $State | ConvertTo-Json -Depth 6 | Set-Content -LiteralPath $script:ShowcaseStateFile -Encoding UTF8 }
function Test-ShowcasePort([int]$Port) { return @(Get-NetTCPConnection -State Listen -LocalPort $Port -ErrorAction SilentlyContinue) }
function Get-ShowcaseProcess([int]$ProcessId) { try { Get-CimInstance Win32_Process -Filter "ProcessId=$ProcessId" -ErrorAction Stop } catch { $null } }
function Test-ShowcaseIdentity($Entry) {
    $process = Get-ShowcaseProcess ([int]$Entry.pid)
    if ($null -eq $process) { return $false }
    $listener = @(Test-ShowcasePort ([int]$Entry.port) | Where-Object { $_.OwningProcess -eq [int]$Entry.pid })
    return $listener.Count -gt 0 -and $process.CommandLine -like "*$($Entry.commandHint)*"
}
function Update-ShowcaseListenerEntry($Entry) {
    $listener = @(Test-ShowcasePort ([int]$Entry.port)) | Select-Object -First 1
    if ($null -eq $listener) { throw "服务未监听预期端口: $($Entry.port)" }
    $process = Get-ShowcaseProcess ([int]$listener.OwningProcess)
    if ($null -eq $process -or $process.CommandLine -notlike "*$($Entry.commandHint)*") { throw "端口 $($Entry.port) 的监听进程不是预期 Showcase 服务。" }
    $Entry.pid = $listener.OwningProcess
    return $Entry
}
function Wait-ShowcaseHttp([string]$Url, [int]$TimeoutSeconds) {
    $until = (Get-Date).AddSeconds($TimeoutSeconds)
    do { try { if ((Invoke-WebRequest -UseBasicParsing -Uri $Url -TimeoutSec 3).StatusCode -eq 200) { return } } catch {}; Start-Sleep -Seconds 1 } while ((Get-Date) -lt $until)
    throw "等待服务超时: $Url"
}
function Get-ShowcaseHttpStatus([string]$Url) {
    try { return (Invoke-WebRequest -UseBasicParsing -Uri $Url -TimeoutSec 3).StatusCode } catch { return $null }
}
function Assert-ShowcaseCommand([string]$Name) { if (-not (Get-Command $Name -ErrorAction SilentlyContinue)) { throw "缺少必要工具: $Name" } }
function Get-ShowcaseLog([string]$Name) { Initialize-ShowcaseStateDirectory; return (Join-Path $script:ShowcaseStateDirectory "$Name.log") }
function Start-ShowcaseProcess([string]$Name, [string]$FilePath, [string]$Arguments, [string]$WorkingDirectory, [string]$CommandHint, [int]$Port) {
    $existing = @(Test-ShowcasePort $Port)
    if ($existing.Count -gt 0) { throw "端口 $Port 已被 PID $($existing[0].OwningProcess) 占用，无法确认其属于 CommerceFlow，已安全停止启动。" }
    $log = Get-ShowcaseLog $Name
    $process = Start-Process -FilePath $FilePath -ArgumentList $Arguments -WorkingDirectory $WorkingDirectory -RedirectStandardOutput $log -RedirectStandardError "${log}.err" -PassThru
    return [pscustomobject]@{ name=$Name; pid=$process.Id; port=$Port; commandHint=$CommandHint; workingDirectory=$WorkingDirectory; startedAt=(Get-Date).ToString('o'); log=$log }
}







