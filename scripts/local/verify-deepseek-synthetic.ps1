[CmdletBinding()]
param(
    [string]$ProviderEnvironmentFile = '',
    [ValidateRange(1024, 65535)]
    [int]$Port = 8083,
    [ValidateRange(30, 240)]
    [int]$StartupTimeoutSeconds = 150
)

$ErrorActionPreference = 'Stop'
$root = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path
$baseUrl = "http://127.0.0.1:$Port"
$log = Join-Path ([System.IO.Path]::GetTempPath()) "commerceflow-deepseek-smoke-$Port.log"
$errorLog = "$log.err"
if ([string]::IsNullOrWhiteSpace($ProviderEnvironmentFile)) {
    $ProviderEnvironmentFile = Join-Path $root 'deploy\staging\.env'
}

function Read-EnvFile([string]$path) {
    if (-not (Test-Path -LiteralPath $path)) { throw "Provider environment file not found: $path" }
    $values = @{}
    foreach ($line in Get-Content -LiteralPath $path) {
        if ($line -match '^\s*([A-Za-z_][A-Za-z0-9_]*)=(.*)$') {
            $value = $matches[2].Trim()
            if ($value.Length -ge 2 -and (($value.StartsWith('"') -and $value.EndsWith('"')) -or ($value.StartsWith("'") -and $value.EndsWith("'")))) {
                $value = $value.Substring(1, $value.Length - 2)
            }
            $values[$matches[1]] = $value
        }
    }
    return $values
}

function Require-Configured([hashtable]$values, [string]$name) {
    $value = [string]$values[$name]
    if ([string]::IsNullOrWhiteSpace($value) -or $value -match 'CHANGE_ME|placeholder|example') {
        throw "$name is missing or still contains a template value."
    }
    return $value
}

function Require-Value([string]$value, [string]$label) {
    if ([string]::IsNullOrWhiteSpace($value) -or $value -match 'CHANGE_ME|placeholder|example') {
        throw "$label is missing or still contains a template value."
    }
    return $value
}

function First-Configured([hashtable]$values, [string[]]$names, [string]$fallback = '') {
    foreach ($name in $names) {
        $value = [string]$values[$name]
        if (-not [string]::IsNullOrWhiteSpace($value) -and $value -notmatch 'CHANGE_ME|placeholder|example') { return $value }
    }
    return $fallback
}

$values = Read-EnvFile $ProviderEnvironmentFile
$sourceProvider = First-Configured $values @('COMMERCEFLOW_AI_PROVIDER') 'DEEPSEEK'
$sourceBase = First-Configured $values @('COMMERCEFLOW_AI_BASE_URL')
$sourceModel = First-Configured $values @('COMMERCEFLOW_AI_MODEL')
$sourceKey = First-Configured $values @('COMMERCEFLOW_AI_API_KEY')
$sourcePath = First-Configured $values @('COMMERCEFLOW_AI_PATH')
$provider = 'DEEPSEEK'
$base = Require-Value $sourceBase 'Provider base URL'
$model = Require-Value $sourceModel 'Provider model'
$apiKey = Require-Value $sourceKey 'Provider API key'
if ($sourceProvider.ToLowerInvariant() -notin @('deepseek', 'openai-compatible', 'openai_compatible', 'openai')) { throw 'The provider environment must point to a supported OpenAI-compatible provider.' }
if ([string]::IsNullOrWhiteSpace($sourcePath)) { $sourcePath = '/v1/chat/completions' }
if ($sourcePath -notmatch '^/') { $sourcePath = "/$sourcePath" }

if (@(Get-NetTCPConnection -State Listen -LocalPort $Port -ErrorAction SilentlyContinue).Count -gt 0) {
    throw "Refusing to use occupied smoke port: $Port"
}

$names = @(
    'MALL_API_PORT', 'DB_URL', 'DB_USERNAME', 'DB_PASSWORD', 'MYSQL_PORT', 'MYSQL_DATABASE',
    'REDIS_HOST', 'REDIS_PORT', 'REDIS_PASSWORD', 'COMMERCEFLOW_AI_PROVIDER',
    'COMMERCEFLOW_AI_BASE_URL', 'COMMERCEFLOW_AI_PATH', 'COMMERCEFLOW_AI_MODEL',
    'COMMERCEFLOW_AI_API_KEY', 'COMMERCEFLOW_AI_PROVIDER_NAME', 'COMMERCEFLOW_AI_FALLBACK_ENABLED',
    'COMMERCEFLOW_AI_CONNECT_TIMEOUT_MS', 'COMMERCEFLOW_AI_READ_TIMEOUT_MS', 'COMMERCEFLOW_AI_MAX_RETRIES',
    'AI_RATE_LIMIT_ENABLED', 'SHOWCASE_MODE', 'SHOWCASE_DATA_SCOPE', 'SHOWCASE_AUTHENTICATION_MODE',
    'SHOWCASE_DEMO_USER_ID', 'COMMERCEFLOW_LEGACY_API_ENABLED'
)
$saved = @{}
foreach ($name in $names) { $saved[$name] = [Environment]::GetEnvironmentVariable($name) }
$maven = $null

function Restore-Environment {
    foreach ($name in $saved.Keys) {
        if ($null -eq $saved[$name]) {
            Remove-Item -Path "Env:$name" -ErrorAction SilentlyContinue
        } else {
            Set-Item -Path "Env:$name" -Value $saved[$name]
        }
    }
}

function Set-SmokeEnv([string]$name, [string]$value) { Set-Item -Path "Env:$name" -Value $value }

try {
    Set-SmokeEnv 'MALL_API_PORT' ([string]$Port)
    Set-SmokeEnv 'DB_URL' 'jdbc:mysql://127.0.0.1:3307/commerceflow?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai'
    Set-SmokeEnv 'DB_USERNAME' 'commerceflow'
    Set-SmokeEnv 'DB_PASSWORD' 'commerceflow_demo'
    Set-SmokeEnv 'MYSQL_PORT' '3307'
    Set-SmokeEnv 'MYSQL_DATABASE' 'commerceflow'
    Set-SmokeEnv 'REDIS_HOST' '127.0.0.1'
    Set-SmokeEnv 'REDIS_PORT' '6380'
    Set-SmokeEnv 'REDIS_PASSWORD' ''
    Set-SmokeEnv 'COMMERCEFLOW_AI_PROVIDER' $provider
    Set-SmokeEnv 'COMMERCEFLOW_AI_BASE_URL' $base
    Set-SmokeEnv 'COMMERCEFLOW_AI_PATH' $sourcePath
    Set-SmokeEnv 'COMMERCEFLOW_AI_MODEL' $model
    Set-SmokeEnv 'COMMERCEFLOW_AI_API_KEY' $apiKey
    Set-SmokeEnv 'COMMERCEFLOW_AI_PROVIDER_NAME' 'deepseek'
    Set-SmokeEnv 'COMMERCEFLOW_AI_FALLBACK_ENABLED' 'false'
    Set-SmokeEnv 'COMMERCEFLOW_AI_CONNECT_TIMEOUT_MS' '6000'
    Set-SmokeEnv 'COMMERCEFLOW_AI_READ_TIMEOUT_MS' '30000'
    Set-SmokeEnv 'COMMERCEFLOW_AI_MAX_RETRIES' '1'
    Set-SmokeEnv 'AI_RATE_LIMIT_ENABLED' 'false'
    Set-SmokeEnv 'SHOWCASE_MODE' 'DEMO'
    Set-SmokeEnv 'SHOWCASE_DATA_SCOPE' 'LOCAL_SHOWCASE'
    Set-SmokeEnv 'SHOWCASE_AUTHENTICATION_MODE' 'DEMO_USER'
    Set-SmokeEnv 'SHOWCASE_DEMO_USER_ID' '1'
    Set-SmokeEnv 'COMMERCEFLOW_LEGACY_API_ENABLED' 'true'

    $maven = Start-Process -FilePath (Join-Path $root 'mvnw.cmd') `
        -ArgumentList @('-f', 'apps/mall-api/pom.xml', 'spring-boot:run') `
        -WorkingDirectory $root `
        -RedirectStandardOutput $log `
        -RedirectStandardError $errorLog `
        -WindowStyle Hidden `
        -PassThru

    $ready = $null
    $deadline = (Get-Date).AddSeconds($StartupTimeoutSeconds)
    do {
        Start-Sleep -Seconds 2
        if ($maven.HasExited) { throw "Commerce DeepSeek smoke process exited before readiness. Logs: $log" }
        try { $ready = Invoke-RestMethod -Uri "$baseUrl/actuator/health/readiness" -TimeoutSec 3 } catch {}
    } while ($null -eq $ready -and (Get-Date) -lt $deadline)
    if ($null -eq $ready -or $ready.status -ne 'UP') { throw 'Commerce DeepSeek smoke readiness did not become UP.' }

    $requestBody = @{
        productId = 101
        skuId = 10004
        question = 'What is the SKU code?'
        clientRequestId = "deepseek-synthetic-$([Guid]::NewGuid().ToString('N'))"
    } | ConvertTo-Json -Compress
    $response = Invoke-RestMethod -Method Post -Uri "$baseUrl/api/v1/me/ai/customer-service/ask" `
        -ContentType 'application/json' -Body $requestBody

    if ($response.provider.name -ne 'deepseek' -or $response.provider.mode -ne 'REAL_OPENAI_COMPATIBLE') { throw 'Commerce response did not prove that DeepSeek was the actual Provider.' }
    if ($response.fallbackUsed -ne $false -or $response.answerStatus -ne 'ANSWERED' -or [string]::IsNullOrWhiteSpace([string]$response.answer)) { throw 'Commerce DeepSeek response was not a successful no-fallback answer.' }
    if (@($response.evidence).Count -lt 1 -or @($response.trace).Count -lt 1) { throw 'Commerce DeepSeek response did not preserve Evidence and Trace.' }

    Write-Host "COMMERCE_DEEPSEEK_SYNTHETIC_OK provider=deepseek model=$model product=$($response.businessFacts.productId) sku=$($response.businessFacts.skuId) status=$($response.answerStatus) evidence=$(@($response.evidence).Count) trace=$(@($response.trace).Count) latencyMs=$($response.latencyMs)"
} finally {
    if ($maven -and -not $maven.HasExited) { Stop-Process -Id $maven.Id -Force -ErrorAction SilentlyContinue }
    Start-Sleep -Seconds 2
    $listeners = @(Get-NetTCPConnection -State Listen -LocalPort $Port -ErrorAction SilentlyContinue | Select-Object -ExpandProperty OwningProcess -Unique)
    foreach ($listenerPid in $listeners) {
        $process = Get-CimInstance Win32_Process -Filter "ProcessId=$listenerPid" -ErrorAction SilentlyContinue
        if ($null -ne $process -and $process.CommandLine.Contains('MallApiApplication')) {
            Stop-Process -Id $listenerPid -Force -ErrorAction SilentlyContinue
        }
    }
    Restore-Environment
}
