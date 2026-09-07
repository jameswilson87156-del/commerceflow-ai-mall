[CmdletBinding()]
param([Alias('EnvFile')][string]$EnvironmentFile = 'deploy/staging/.env')

$ErrorActionPreference = 'Stop'
$root = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path
$file = if ([IO.Path]::IsPathRooted($EnvironmentFile)) { $EnvironmentFile } else { Join-Path $root $EnvironmentFile }
if (-not (Test-Path -LiteralPath $file -PathType Leaf)) { throw 'BLOCKED: project staging environment file is missing.' }
$values = @{}
foreach ($line in Get-Content -LiteralPath $file) {
    if ($line -match '^\s*([A-Za-z_][A-Za-z0-9_]*)=(.*)$') {
        $name = $matches[1]
        if ($values.ContainsKey($name)) { throw "Duplicate environment variable: $name" }
        $value = $matches[2].Trim()
        if ($value.Length -ge 2 -and (($value.StartsWith('"') -and $value.EndsWith('"')) -or ($value.StartsWith("'") -and $value.EndsWith("'")))) { $value = $value.Substring(1, $value.Length - 2) }
        $values[$name] = $value
    }
}
function Require-Setting([string]$name) {
    $v = [string]$values[$name]
    if ([string]::IsNullOrWhiteSpace($v)) { throw "Missing staging configuration: $name" }
    if ($v -match '(?i)CHANGE_ME|placeholder|example\.com') { throw "Template placeholder is still present: $name" }
}
function Require-Https([string]$name) {
    $uri = $null
    if (-not [Uri]::TryCreate($values[$name], [UriKind]::Absolute, [ref]$uri) -or $uri.Scheme -ne 'https' -or $uri.IsLoopback -or $uri.UserInfo -or $uri.Fragment) { throw "A non-loopback HTTPS URL without credentials is required: $name" }
}
function Require-Database([string]$name) {
    $v = [string]$values[$name]
    if ($v -notmatch '^jdbc:mysql://[A-Za-z0-9.-]+(?::[0-9]+)?/[A-Za-z0-9_]+\?' -or $v -notmatch '(?:\?|&)sslMode=VERIFY_IDENTITY(?:&|$)' -or $v -match '(?i)useSSL=false|allowPublicKeyRetrieval=true|://(?:localhost|127\.|mysql[:/])') { throw "Private MySQL URL with sslMode=VERIFY_IDENTITY is required: $name" }
}

$required = @(
    'COMMERCEFLOW_DB_URL',
    'COMMERCEFLOW_DB_USERNAME',
    'COMMERCEFLOW_DB_PASSWORD',
    'COMMERCEFLOW_DB_MIGRATION_USERNAME',
    'COMMERCEFLOW_DB_MIGRATION_PASSWORD',
    'COMMERCEFLOW_REDIS_HOST',
    'COMMERCEFLOW_REDIS_PORT',
    'COMMERCEFLOW_REDIS_PASSWORD',
    'COMMERCEFLOW_REDIS_SSL_ENABLED',
    'COMMERCEFLOW_RATE_LIMIT_HASH_SECRET',
    'STAGING_ADMIN_HOST',
    'STAGING_MOBILE_HOST',
    'ACME_EMAIL',
    'COMMERCEFLOW_CORS_ALLOWED_ORIGINS',
    'SHOWCASE_AUTHENTICATION_MODE',
    'COMMERCEFLOW_AUTH_ISSUER_URI',
    'COMMERCEFLOW_AUTH_AUDIENCE',
    'VITE_ADMIN_AUTH_CLIENT_ID',
    'VITE_MOBILE_AUTH_CLIENT_ID',
    'COMMERCEFLOW_AI_PROVIDER',
    'COMMERCEFLOW_AI_BASE_URL',
    'COMMERCEFLOW_AI_PATH',
    'COMMERCEFLOW_AI_MODEL',
    'COMMERCEFLOW_AI_API_KEY',
    'COMMERCEFLOW_AI_FALLBACK_ENABLED'
)
foreach ($name in $required) { Require-Setting $name }
Require-Https 'COMMERCEFLOW_AUTH_ISSUER_URI'
Require-Https 'COMMERCEFLOW_AI_BASE_URL'
Require-Database 'COMMERCEFLOW_DB_URL'
if ($values['ACME_EMAIL'] -notmatch '^[^@\s]+@[^@\s]+\.[^@\s]+$') { throw 'Invalid ACME_EMAIL.' }
if (@($values.Keys | Where-Object { $_ -match '^(TICKET_|VITE_TICKET_|PORTFOLIO_)' }).Count) { throw 'Cross-project environment variables are forbidden.' }
if ($values['COMMERCEFLOW_AI_FALLBACK_ENABLED'] -ne 'false') { throw 'COMMERCEFLOW_AI_FALLBACK_ENABLED must be false.' }
if ($values['COMMERCEFLOW_AI_PROVIDER'] -notin @('OPENAI','DEEPSEEK','OPENAI_COMPATIBLE')) { throw 'Unsupported COMMERCEFLOW_AI_PROVIDER.' }
if ($values['SHOWCASE_AUTHENTICATION_MODE'] -ne 'EXTERNAL') { throw 'SHOWCASE_AUTHENTICATION_MODE must be EXTERNAL.' }
if ($values['COMMERCEFLOW_RATE_LIMIT_HASH_SECRET'].Length -lt 32) { throw 'COMMERCEFLOW_RATE_LIMIT_HASH_SECRET must have at least 32 characters.' }
if ($values['COMMERCEFLOW_REDIS_SSL_ENABLED'] -ne 'true') { throw 'COMMERCEFLOW_REDIS_SSL_ENABLED must be true for this managed staging configuration.' }
if ($values['COMMERCEFLOW_REDIS_HOST'] -notmatch '^[A-Za-z0-9.-]+$' -or $values['COMMERCEFLOW_REDIS_HOST'] -match '^(localhost|redis|127\..*)$') { throw 'Invalid COMMERCEFLOW_REDIS_HOST.' }
$redisPort = 0
if (-not [int]::TryParse($values['COMMERCEFLOW_REDIS_PORT'], [ref]$redisPort) -or $redisPort -lt 1 -or $redisPort -gt 65535) { throw 'Invalid COMMERCEFLOW_REDIS_PORT.' }
foreach ($name in @('STAGING_ADMIN_HOST','STAGING_MOBILE_HOST')) {
    if ($values[$name] -notmatch '^[A-Za-z0-9.-]+\.[A-Za-z]+$') { throw "Invalid staging hostname: $name" }
}
if ($values['STAGING_ADMIN_HOST'] -eq $values['STAGING_MOBILE_HOST']) { throw 'Admin and H5 require distinct hostnames.' }
if ($values['VITE_ADMIN_AUTH_CLIENT_ID'] -eq $values['VITE_MOBILE_AUTH_CLIENT_ID']) { throw 'Admin and H5 require distinct public clients.' }
$expected = @('https://' + $values['STAGING_ADMIN_HOST']; 'https://' + $values['STAGING_MOBILE_HOST']) | Sort-Object
$origins = @($values['COMMERCEFLOW_CORS_ALLOWED_ORIGINS'].Split(',') | ForEach-Object { $_.Trim() }) | Sort-Object
if (@(Compare-Object $expected $origins).Count) { throw 'CORS must contain exactly the Admin and H5 HTTPS origins.' }
Write-Host "LOCAL_PASS: staging configuration syntax and isolation checks only; remote connectivity is STAGING_PENDING."
