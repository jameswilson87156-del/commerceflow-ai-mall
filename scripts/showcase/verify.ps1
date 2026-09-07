[CmdletBinding()]
param([switch]$Deep, [switch]$IncludeMobile, [switch]$IncludeOrderSmoke, [switch]$IncludeRateLimitSmoke)
$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'showcase-common.ps1')
Import-ShowcaseEnvironment
$api=Get-ShowcaseRuntimePort 'java' 'MALL_API_PORT' '8080'; $python=Get-ShowcaseRuntimePort 'python' 'AI_SERVICE_PORT' '8000'; $admin=Get-ShowcaseRuntimePort 'admin' 'ADMIN_WEB_PORT' '5174'; $mobile=Get-ShowcaseRuntimePort 'mobile' 'MOBILE_H5_PORT' '5173'
foreach($url in @("http://127.0.0.1:$api/actuator/health/readiness","http://127.0.0.1:$python/health","http://127.0.0.1:$admin")){Wait-ShowcaseHttp $url 10}
if($IncludeMobile){Wait-ShowcaseHttp "http://127.0.0.1:$mobile" 10}
$products=Invoke-RestMethod "http://127.0.0.1:$api/api/products"; if($products.Count -lt 1 -or $products[0].skus.Count -lt 1){throw '商品或 SKU API 未返回真实字段。'}
Invoke-RestMethod "http://127.0.0.1:$api/api/v1/me/cart" | Out-Null; Invoke-RestMethod "http://127.0.0.1:$api/api/v1/me/orders" | Out-Null; $overview=Invoke-RestMethod "http://127.0.0.1:$api/api/v1/operator/operations/overview"; if($overview.runtimeBoundary.aiMode -ne 'MOCK'){throw 'Showcase Provider 不是 MOCK。'}
$askBody=@{productId=101;skuId=10004;question='What is the SKU code?';clientRequestId=("showcase-verify-" + [Guid]::NewGuid().ToString('N'))} | ConvertTo-Json -Compress
$askResponse=Invoke-WebRequest -UseBasicParsing -Method Post -ContentType 'application/json' -Body $askBody "http://127.0.0.1:$api/api/v1/me/ai/customer-service/ask"
$answer=$askResponse.Content | ConvertFrom-Json
if($askResponse.StatusCode -ne 200 -or $answer.provider.name -ne 'commerceflow-mock' -or $answer.provider.mode -ne 'MOCK' -or $answer.evidence.Count -lt 1 -or $answer.trace.Count -lt 1){throw 'Mock AI 验证未返回真实 Provider、Evidence 或 Trace。'}
foreach($headerName in @('X-RateLimit-Limit','X-RateLimit-Remaining','X-RateLimit-Reset','X-RateLimit-Mode')){if([string]::IsNullOrWhiteSpace($askResponse.Headers[$headerName])){throw "AI 响应缺少限流响应头: $headerName"}}
foreach($path in @('product-tshirt-white.png','product-tshirt-black.png','product-tshirt-gray.png','product-tshirt-navy.png','product-tote-beige.png')){$response=Invoke-WebRequest -UseBasicParsing "http://127.0.0.1:$admin/assets/products/$path";if($response.StatusCode -ne 200 -or $response.Headers['Content-Type'] -notlike 'image/*' -or $response.RawContentLength -le 0){throw "商品图片校验失败: $path"}}
function Invoke-ShowcaseJsonRequest {
    param([string]$Uri, [string]$Method = 'Get', [string]$Body, [hashtable]$Headers = @{})
    try {
        $response = Invoke-WebRequest -UseBasicParsing -Method $Method -Uri $Uri -ContentType 'application/json' -Headers $Headers -Body $Body
        return [pscustomobject]@{ StatusCode = [int]$response.StatusCode; Body = ($response.Content | ConvertFrom-Json); Headers = $response.Headers }
    } catch {
        $webResponse = $_.Exception.Response
        if ($null -eq $webResponse) { throw }
        $reader = New-Object System.IO.StreamReader($webResponse.GetResponseStream())
        try { $content = $reader.ReadToEnd() } finally { $reader.Dispose() }
        return [pscustomobject]@{ StatusCode = [int]$webResponse.StatusCode; Body = ($content | ConvertFrom-Json); Headers = $webResponse.Headers }
    }
}
if($IncludeOrderSmoke){
    $orderBase = "http://127.0.0.1:$api/api/v1/me/orders"
    $orderKey = "showcase-verify-" + [Guid]::NewGuid().ToString('N')
    $orderBody = '{"items":[{"skuId":10001,"quantity":1}]}'
    $created = Invoke-ShowcaseJsonRequest -Uri $orderBase -Method Post -Body $orderBody -Headers @{ 'Idempotency-Key' = $orderKey }
    if($created.StatusCode -ne 200 -or [string]::IsNullOrWhiteSpace($created.Body.orderNo)){throw '订单 smoke 未创建真实订单。'}
    $replay = Invoke-ShowcaseJsonRequest -Uri $orderBase -Method Post -Body $orderBody -Headers @{ 'Idempotency-Key' = $orderKey }
    if($replay.StatusCode -ne 200 -or $replay.Body.orderNo -ne $created.Body.orderNo){throw '订单 smoke 未返回同 Key 的原订单。'}
    $conflict = Invoke-ShowcaseJsonRequest -Uri $orderBase -Method Post -Body '{"items":[{"skuId":10001,"quantity":2}]}' -Headers @{ 'Idempotency-Key' = $orderKey }
    if($conflict.StatusCode -ne 409 -or $conflict.Body.code -ne 'IDEMPOTENCY_KEY_REUSED'){throw '订单 smoke 未拒绝同 Key 不同请求。'}
    $evidence = Invoke-RestMethod "http://127.0.0.1:$api/api/v1/me/orders/$($created.Body.orderNo)/execution-evidence"
    if($evidence.inventoryMovements.Count -ne 1){throw '订单 smoke 未返回一条库存变动证据。'}
    Write-Host "ORDER_SMOKE_OK order=$($created.Body.orderNo) replay=$($replay.Body.orderNo) conflict=$($conflict.StatusCode)"
}
if($IncludeRateLimitSmoke){
    # Remove only the local Showcase limiter namespace before asserting the
    # fixed-window boundary. This is deliberately scoped to the configured
    # prefix; it never runs FLUSHDB and never changes the server policy.
    $ratePrefix = Get-ShowcaseValue -Name 'AI_RATE_LIMIT_KEY_PREFIX' -Fallback 'commerceflow:local:ai:rate:v1'
    $rateKeys = @(docker compose -p $script:ShowcaseComposeProject exec -T redis redis-cli --raw --scan --pattern "${ratePrefix}:*" 2>$null | Where-Object { -not [string]::IsNullOrWhiteSpace($_) })
    if($rateKeys.Count -gt 0){ docker compose -p $script:ShowcaseComposeProject exec -T redis redis-cli DEL $rateKeys | Out-Null }
    $rateUri = "http://127.0.0.1:$api/api/v1/me/ai/customer-service/ask"
    $rateResponses = @()
    for($attempt = 1; $attempt -le 6; $attempt++){
        $rateBody = @{productId=101;skuId=10004;question='现在还有库存吗？';clientRequestId=("showcase-rate-$attempt-" + [Guid]::NewGuid().ToString('N'))} | ConvertTo-Json -Compress
        $rateResponses += Invoke-ShowcaseJsonRequest -Uri $rateUri -Method Post -Body $rateBody
    }
    $allowedCount = @($rateResponses | Where-Object { $_.StatusCode -eq 200 }).Count
    $blocked = @($rateResponses | Where-Object { $_.StatusCode -eq 429 })
    if($allowedCount -ne 5 -or $blocked.Count -ne 1){throw "限流 smoke 期望 5 次成功与 1 次 429，实际成功 $allowedCount、429 $($blocked.Count)。"}
    foreach($headerName in @('Retry-After','X-RateLimit-Limit','X-RateLimit-Remaining','X-RateLimit-Reset','X-RateLimit-Mode')){if([string]::IsNullOrWhiteSpace($blocked[0].Headers[$headerName])){throw "429 响应缺少限流响应头: $headerName"}}
    Write-Host "RATE_LIMIT_SMOKE_OK serverDerivedConsumerScope allowed=$allowedCount blocked=$($blocked.Count)"
}
if($Deep){& "$script:ShowcaseRoot\mvnw.cmd" -f "$script:ShowcaseRoot\apps\mall-api\pom.xml" test; Push-Location "$script:ShowcaseRoot\apps\admin-web"; npm test; npm run build; Pop-Location}
Write-Host 'SHOWCASE_VERIFY_OK'







