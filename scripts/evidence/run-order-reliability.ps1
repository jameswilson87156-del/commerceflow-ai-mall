[CmdletBinding()]
param([ValidateRange(1,10)][int]$RunCount = 3)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
[Console]::OutputEncoding = New-Object System.Text.UTF8Encoding($false)

$root = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path
$evidenceRoot = Join-Path $root 'docs\evidence\order-reliability-v1'
$logRoot = Join-Path $evidenceRoot 'logs'
$targetResults = Join-Path $root 'apps\mall-api\target\order-reliability-results.json'
New-Item -ItemType Directory -Force -Path $logRoot | Out-Null


function Wait-MySqlHealthy([string]$ContainerName) {
    $deadline = (Get-Date).AddSeconds(90)
    do {
        & docker exec -e MYSQL_PWD=order_reliability_demo $ContainerName mysqladmin ping -h 127.0.0.1 -u order_reliability --silent *> $null
        if ($LASTEXITCODE -eq 0) { return }
        Start-Sleep -Seconds 1
    } while ((Get-Date) -lt $deadline)
    throw 'The isolated Phase O1 MySQL container did not become ready within 90 seconds.'
}

function Write-RunLog([string]$Path, [int]$RunNumber, [datetime]$Started, [datetime]$Finished, [int]$ExitCode, [string]$StdOut, [string]$StdErr) {
    @(
        "Run ID: run-$('{0:d2}' -f $RunNumber)",
        'Command: .\mvnw.cmd -f apps\mall-api\pom.xml -Dtest=OrderReliabilityMySqlTests test',
        'Working directory: .',
        ('Started at: ' + $Started.ToUniversalTime().ToString('o')),
        '--- stdout ---'
    ) | Set-Content -LiteralPath $Path -Encoding UTF8
    $redact = {
        param([string]$line)
        $line = $line -replace 'jdbc:mysql://[^ )]+', '[isolated-mysql-connection-redacted]'
        $line = $line -replace '(?i)started by\s+[^\s]+', 'started by [local-user]'
        $line = $line -replace '(?i)[A-Z]:\\[^\s\]]+', '[worktree]'
        return $line
    }
    if (Test-Path -LiteralPath $StdOut) { Get-Content -LiteralPath $StdOut | ForEach-Object { & $redact $_ } | Add-Content -LiteralPath $Path -Encoding UTF8 }
    '--- stderr ---' | Add-Content -LiteralPath $Path -Encoding UTF8
    if (Test-Path -LiteralPath $StdErr) { Get-Content -LiteralPath $StdErr | ForEach-Object { & $redact $_ } | Add-Content -LiteralPath $Path -Encoding UTF8 }
    @(
        ('Exit Code: ' + $ExitCode),
        ('Finished at: ' + $Finished.ToUniversalTime().ToString('o'))
    ) | Add-Content -LiteralPath $Path -Encoding UTF8
}

function Write-EvidenceDocuments([object[]]$Runs, [string]$Status) {
    $last = $Runs[$Runs.Count - 1]
    $lastJson = Get-Content -Raw -LiteralPath $last.ResultFile | ConvertFrom-Json
    $results = [ordered]@{
        database = 'MySQL 8.4'
        status = $Status
        runs = @($Runs | ForEach-Object { [ordered]@{ runId=$_.RunId; startedAt=$_.StartedAt; finishedAt=$_.FinishedAt; exitCode=$_.ExitCode; scenarios=$_.Scenarios } })
        scenarios = $lastJson.scenarios
    }
    $results | ConvertTo-Json -Depth 12 | Set-Content -LiteralPath (Join-Path $evidenceRoot 'results.json') -Encoding UTF8

    @(
        '# Order Reliability Evidence V1',
        '',
        '## Purpose',
        'Verify local MySQL order, inventory, idempotency, aggregation, and transaction rollback correctness. This is not a throughput or production-capacity claim.',
        '',
        '## Environment and reproducibility',
        '- Database: MySQL 8.4 in a dedicated temporary Docker container per run.',
        '- Runner: `powershell -NoProfile -File .\scripts\evidence\run-order-reliability.ps1`.',
        '- The runner creates a fresh database for every run, waits for health, lets Flyway apply the existing migrations, runs only `OrderReliabilityMySqlTests`, and removes its own container in `finally`.',
        '- The test uses isolated fixture users, products, SKUs, inventory, and request keys. It does not use Showcase orders or modify official seed migrations.',
        '',
        '## Scenarios',
        '- A — 50 distinct-key concurrent requests against stock 10.',
        '- B — 20 same-key concurrent requests plus one sequential replay.',
        '- C — same key with a different request body.',
        '- D — multi-SKU request where a later SKU is out of stock.',
        '- E — duplicate SKU lines aggregated before stock deduction.',
        '',
        '## Result',
        "- Stability status: $Status.",
        "- Completed isolated runs: $($Runs.Count).",
        '- Detailed machine-readable counts: `results.json`.',
        '- Full command output: `logs/01-baseline-java-tests.log` and `logs/02-real-mysql-order-reliability.log`.',
        '',
        '## Boundaries',
        '- Redis is not part of order or inventory correctness.',
        '- No real AI provider, API key, payment, shipping, refund, or production-load claim is involved.'
    ) | Set-Content -LiteralPath (Join-Path $evidenceRoot 'README.md') -Encoding UTF8

    @(
        '# Database invariants',
        '',
        '- `inventory.available_stock` never becomes negative.',
        '- A successful order writes one order, its aggregated order items, and matching `inventory_movement` rows in the same transaction.',
        '- `inventory_movement` uses the existing unique `(order_no, sku_id, movement_type)` constraint.',
        '- Any insufficient SKU rolls back prior deductions and all order-side writes.',
        '- Scenario-level actual counts are in `results.json`.'
    ) | Set-Content -LiteralPath (Join-Path $evidenceRoot 'database-invariants.md') -Encoding UTF8

    @(
        '# Request outcomes',
        '',
        '- Distinct idempotency keys: exactly the available stock can create orders; exhausted requests return `INVENTORY_INSUFFICIENT`.',
        '- Same key and same body: one physical order; concurrent callers may receive `IDEMPOTENCY_IN_PROGRESS`; a later replay returns the original order.',
        '- Same key and different body: `IDEMPOTENCY_KEY_REUSED` (HTTP 409 at the API boundary) with no extra write.',
        '- Outcome counts and final persistence counts are recorded in `results.json`.'
    ) | Set-Content -LiteralPath (Join-Path $evidenceRoot 'request-outcomes.md') -Encoding UTF8

    @(
        '# Known limitations',
        '',
        '- This is a local MySQL correctness suite, not a QPS, latency, failover, or production-capacity benchmark.',
        '- It verifies one application process and one isolated MySQL container; distributed multi-instance idempotency behavior is outside this evidence.',
        '- The in-progress idempotency response is an allowed transient outcome during the first request.',
        '- No payment, shipping, refund, address, coupon, or Redis inventory logic is tested.'
    ) | Set-Content -LiteralPath (Join-Path $evidenceRoot 'known-limitations.md') -Encoding UTF8
}

if (-not (Get-Command docker -ErrorAction SilentlyContinue)) { throw 'Docker is required for the Phase O1 isolated MySQL evidence runner.' }
& docker version --format '{{.Server.Version}}' *> $null
if ($LASTEXITCODE -ne 0) { throw 'Docker daemon is not available for the Phase O1 isolated MySQL evidence runner.' }

$combinedLog = Join-Path $logRoot '02-real-mysql-order-reliability.log'
Remove-Item -LiteralPath $combinedLog -Force -ErrorAction SilentlyContinue
$previous = @{}
$variableNames = @('ORDER_RELIABILITY_MYSQL_ENABLED','ORDER_RELIABILITY_DB_URL','ORDER_RELIABILITY_DB_USERNAME','ORDER_RELIABILITY_DB_PASSWORD','ORDER_RELIABILITY_RESULTS_FILE')
foreach($name in $variableNames){ $previous[$name] = [Environment]::GetEnvironmentVariable($name, 'Process') }
$runs = @()
$status = 'PASS'
try {
    for($number = 1; $number -le $RunCount; $number++) {
        $runId = 'run-' + ('{0:d2}' -f $number)
        $container = 'commerceflow-order-reliability-' + [Guid]::NewGuid().ToString('N').Substring(0,12)
        $stdout = Join-Path $env:TEMP ('commerceflow-o1-' + $runId + '-out.log')
        $stderr = Join-Path $env:TEMP ('commerceflow-o1-' + $runId + '-err.log')
        $runLog = Join-Path $logRoot ('02-real-mysql-order-reliability-' + $runId + '.log')
        $resultCopy = Join-Path $logRoot ($runId + '-results.json')
        $started = Get-Date
        $exitCode = -1
        try {
            & docker run --rm -d --name $container -e MYSQL_DATABASE=order_reliability -e MYSQL_USER=order_reliability -e MYSQL_PASSWORD=order_reliability_demo -e MYSQL_ROOT_PASSWORD=order_reliability_root_demo -p '127.0.0.1::3306' mysql:8.4 *> $null
            if ($LASTEXITCODE -ne 0) { throw 'Unable to start the isolated Phase O1 MySQL container.' }
            $mapping = (& docker port $container 3306/tcp 2>$null | Select-Object -First 1).Trim()
            if ($mapping -notmatch ':(\d+)$') { throw 'Unable to determine the dedicated temporary MySQL port.' }
            $port = $Matches[1]
            Wait-MySqlHealthy $container
            Remove-Item -LiteralPath $targetResults -Force -ErrorAction SilentlyContinue
            $env:ORDER_RELIABILITY_MYSQL_ENABLED = 'true'
            $env:ORDER_RELIABILITY_DB_URL = "jdbc:mysql://127.0.0.1:$port/order_reliability?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
            $env:ORDER_RELIABILITY_DB_USERNAME = 'order_reliability'
            $env:ORDER_RELIABILITY_DB_PASSWORD = 'order_reliability_demo'
            $env:ORDER_RELIABILITY_RESULTS_FILE = 'target/order-reliability-results.json'
            $process = Start-Process -FilePath $env:ComSpec -ArgumentList @('/d','/c','mvnw.cmd -f apps/mall-api/pom.xml -Dtest=OrderReliabilityMySqlTests test') -WorkingDirectory $root -NoNewWindow -Wait -PassThru -RedirectStandardOutput $stdout -RedirectStandardError $stderr
            $exitCode = $process.ExitCode
            $finished = Get-Date
            Write-RunLog $runLog $number $started $finished $exitCode $stdout $stderr
            Get-Content -LiteralPath $runLog | Add-Content -LiteralPath $combinedLog -Encoding UTF8
            if ($exitCode -ne 0) { throw "Phase O1 MySQL test run $runId failed." }
            if (-not (Test-Path -LiteralPath $targetResults)) { throw "Phase O1 MySQL test run $runId did not produce machine-readable scenario evidence." }
            Copy-Item -LiteralPath $targetResults -Destination $resultCopy -Force
            $scenarioResult = (Get-Content -Raw -LiteralPath $resultCopy | ConvertFrom-Json).scenarios
            $runs += [pscustomobject]@{ RunId=$runId; StartedAt=$started.ToUniversalTime().ToString('o'); FinishedAt=$finished.ToUniversalTime().ToString('o'); ExitCode=$exitCode; ResultFile=$resultCopy; Scenarios=$scenarioResult }
        } finally {
            if (Test-Path -LiteralPath $stdout) { Remove-Item -LiteralPath $stdout -Force -ErrorAction SilentlyContinue }
            if (Test-Path -LiteralPath $stderr) { Remove-Item -LiteralPath $stderr -Force -ErrorAction SilentlyContinue }
            $previousPreference = $ErrorActionPreference
            try { $ErrorActionPreference = 'Continue'; & docker rm -f $container *> $null } finally { $ErrorActionPreference = $previousPreference }
        }
    }
    Write-EvidenceDocuments $runs 'PASS'
    Write-Output 'ORDER_RELIABILITY_PASS'
} catch {
    $status = if($runs.Count -gt 0){'ORDER_RELIABILITY_FLAKY'}elseif($_.Exception.Message -match 'MySQL container|Docker|temporary MySQL port'){'ORDER_RELIABILITY_ENVIRONMENT_BLOCKED'}else{'ORDER_RELIABILITY_INVARIANT_FAILED'}
    @(
        ('Status: ' + $status),
        ('Failure category: ' + $_.Exception.Message),
        'See the most recent run log for stdout, stderr, exit code, and timestamps.'
    ) | Set-Content -LiteralPath (Join-Path $evidenceRoot 'known-limitations.md') -Encoding UTF8
    throw
} finally {
    foreach($name in $variableNames){
        if ($null -eq $previous[$name]) { Remove-Item ("Env:" + $name) -ErrorAction SilentlyContinue }
        else { Set-Item ("Env:" + $name) -Value $previous[$name] }
    }
}
