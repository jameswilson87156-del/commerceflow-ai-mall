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
    Sync-ShowcaseComposeProject
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

function Resolve-ShowcaseComposeProjectName {
    param([string]$EnvironmentProject, [string]$StateProject)
    if (-not [string]::IsNullOrWhiteSpace($EnvironmentProject)) {
        $project = $EnvironmentProject
    } elseif (-not [string]::IsNullOrWhiteSpace($StateProject)) {
        $project = $StateProject
    } else {
        $project = 'commerceflow-showcase'
    }
    if ($project -notmatch '^[A-Za-z0-9][A-Za-z0-9_-]*$') { throw 'COMMERCEFLOW_SHOWCASE_PROJECT contains unsupported characters.' }
    return $project
}

function Sync-ShowcaseComposeProject {
    $state = Get-ShowcaseState
    $stateProject = if ($null -ne $state -and $null -ne $state.PSObject.Properties['composeProject']) { [string]$state.composeProject } else { '' }
    $script:ShowcaseComposeProject = Resolve-ShowcaseComposeProjectName -EnvironmentProject ([Environment]::GetEnvironmentVariable('COMMERCEFLOW_SHOWCASE_PROJECT')) -StateProject $stateProject
}

Sync-ShowcaseComposeProject
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
    $process = Start-Process -FilePath $FilePath -ArgumentList $Arguments -WorkingDirectory $WorkingDirectory -RedirectStandardOutput $log -RedirectStandardError "${log}.err" -PassThru -WindowStyle Hidden
    return [pscustomobject]@{ name=$Name; pid=$process.Id; port=$Port; commandHint=$CommandHint; workingDirectory=$WorkingDirectory; startedAt=(Get-Date).ToString('o'); log=$log }
}
function ConvertFrom-ShowcaseComposeJsonLines {
    param([object[]]$Lines)
    $records = New-Object System.Collections.ArrayList
    $lineNumber = 0
    foreach ($inputLine in @($Lines)) {
        if ($null -eq $inputLine) { continue }
        foreach ($line in ([string]$inputLine -split '\r?\n')) {
            if ([string]::IsNullOrWhiteSpace($line)) { continue }
            $lineNumber++
            try {
                $decoded = ConvertFrom-Json -InputObject $line -ErrorAction Stop
            } catch {
                throw "Invalid JSON in docker compose ps record $lineNumber."
            }
            foreach ($record in @($decoded)) {
                if ($null -eq $record) { continue }
                if ($null -eq $record.PSObject.Properties['Service']) {
                    throw "docker compose ps JSON record $lineNumber has no Service field."
                }
                [void]$records.Add($record)
            }
        }
    }
    return $records.ToArray()
}

function Get-ShowcaseComposeServices {
    param([switch]$All)
    Push-Location $script:ShowcaseRoot
    try {
        # Avoid parsing the full Compose JSON object through Windows PowerShell's
        # native-command output decoder. The Command field can contain Unicode
        # ellipses/quotes that become mojibake under legacy console code pages.
        # These fields are ASCII-safe for the service metadata we actually use.
        $format = '{{.Service}}|{{.State}}|{{.Health}}|{{.ID}}|{{.Name}}'
        if ($All) {
            $lines = @(docker compose -p $script:ShowcaseComposeProject ps --all --format $format)
        } else {
            $lines = @(docker compose -p $script:ShowcaseComposeProject ps --format $format)
        }
        $composeExitCode = $LASTEXITCODE
        if ($composeExitCode -ne 0) {
            throw "docker compose ps failed with exit code $composeExitCode."
        }
    } finally {
        Pop-Location
    }
    $records = @()
    foreach ($line in @($lines)) {
        if ([string]::IsNullOrWhiteSpace([string]$line)) { continue }
        $parts = ([string]$line).Split([char]'|')
        if ($parts.Count -ne 5) { throw 'docker compose ps returned an invalid service status row.' }
        $records += [pscustomobject]@{
            Service = $parts[0]
            State = $parts[1]
            Health = $parts[2]
            ID = $parts[3]
            Name = $parts[4]
        }
    }
    return $records
}

function Get-ShowcaseDependencyHealth {
    param([object[]]$Services)
    $statuses = @()
    foreach ($serviceName in @('mysql', 'redis')) {
        $matches = @($Services | Where-Object { [string]$_.Service -eq $serviceName })
        $state = ''
        $health = ''
        if ($matches.Count -eq 0) {
            $status = 'missing'
        } elseif ($matches.Count -gt 1) {
            $state = 'duplicate'
            $health = 'ambiguous'
            $status = 'failed'
        } else {
            $state = [string]$matches[0].State
            $health = [string]$matches[0].Health
            $normalizedState = $state.ToLowerInvariant()
            $normalizedHealth = $health.ToLowerInvariant()
            if ($normalizedHealth -eq 'unhealthy' -or @('exited', 'dead', 'removing') -contains $normalizedState) {
                $status = 'failed'
            } elseif ($normalizedState -eq 'running' -and $normalizedHealth -eq 'healthy') {
                $status = 'healthy'
            } elseif (@('running', 'created', 'restarting') -contains $normalizedState -and @('', 'starting') -contains $normalizedHealth) {
                $status = 'starting'
            } else {
                $status = 'failed'
            }
        }
        $statuses += [pscustomobject]@{ Service = $serviceName; Status = $status; State = $state; Health = $health }
    }
    $ready = @($statuses | Where-Object { $_.Status -ne 'healthy' }).Count -eq 0
    $failed = @($statuses | Where-Object { $_.Status -eq 'failed' }).Count -gt 0
    $summary = (@($statuses | ForEach-Object { "$($_.Service)=$($_.Status)" }) -join ', ')
    return [pscustomobject]@{ Ready = $ready; Failed = $failed; Services = $statuses; Summary = $summary }
}

function Get-ShowcaseNewlyStartedComposeServices {
    param([object[]]$Before, [object[]]$After)
    $newServices = @()
    foreach ($serviceName in @('mysql', 'redis')) {
        $runningAfter = @($After | Where-Object { [string]$_.Service -eq $serviceName -and [string]$_.State -eq 'running' })
        foreach ($current in $runningAfter) {
            $wasAlreadyRunning = @($Before | Where-Object {
                [string]$_.Service -eq $serviceName -and
                [string]$_.State -eq 'running' -and
                [string]$_.ID -eq [string]$current.ID
            }).Count -gt 0
            if (-not $wasAlreadyRunning) {
                $newServices += $serviceName
                break
            }
        }
    }
    return $newServices
}

function Assert-ShowcaseComposePortAvailable {
    param([int]$Port, [string]$ServiceName, [object[]]$ComposeServices)
    $listeners = @(Test-ShowcasePort $Port)
    if ($listeners.Count -eq 0) { return }
    $ownedService = @($ComposeServices | Where-Object {
        [string]$_.Service -eq $ServiceName -and [string]$_.State -eq 'running'
    })
    if ($ownedService.Count -eq 1) { return }
    $ownerPid = [int]$listeners[0].OwningProcess
    throw "Showcase dependency port $Port is occupied by PID $ownerPid and no running $ServiceName container is recorded; refusing to stop or reuse it."
}
