[CmdletBinding()]
param([string]$BaseUrl = 'http://127.0.0.1:8088')

$ErrorActionPreference = 'Stop'
$health = Invoke-WebRequest -UseBasicParsing -Uri "$BaseUrl/health" -TimeoutSec 10
if ($health.StatusCode -ne 200) { throw 'edge health check failed' }
$products = Invoke-RestMethod -Uri "$BaseUrl/api/products" -TimeoutSec 10
if (@($products).Count -lt 1 -or @($products[0].skus).Count -lt 1) { throw 'staging catalog smoke failed' }
Write-Host "STAGING_EDGE_VERIFY_OK products=$(@($products).Count)"
