[CmdletBinding()]
param([switch]$Deep, [switch]$IncludeMobile, [switch]$IncludeOrderSmoke, [switch]$IncludeRateLimitSmoke)
$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'showcase-common.ps1')
Import-ShowcaseEnvironment
$api=Get-ShowcaseRuntimePort 'java' 'MALL_API_PORT' '8080'; $python=Get-ShowcaseRuntimePort 'python' 'AI_SERVICE_PORT' '8000'; $admin=Get-ShowcaseRuntimePort 'admin' 'ADMIN_WEB_PORT' '5174'; $mobile=Get-ShowcaseRuntimePort 'mobile' 'MOBILE_H5_PORT' '5173'
foreach($url in @("http://127.0.0.1:$api/actuator/health/readiness","http://127.0.0.1:$python/health","http://127.0.0.1:$admin")){Wait-ShowcaseHttp $url 10}
if($IncludeMobile){Wait-ShowcaseHttp "http://127.0.0.1:$mobile" 10}
$products=Invoke-RestMethod "http://127.0.0.1:$api/api/products"; if($products.Count -lt 1 -or $products[0].skus.Count -lt 1){throw '商品或 SKU API 未返回真实字段。'}
Invoke-RestMethod "http://127.0.0.1:$api/api/cart?userId=1" | Out-Null; Invoke-RestMethod "http://127.0.0.1:$api/api/orders?userId=1" | Out-Null; $overview=Invoke-RestMethod "http://127.0.0.1:$api/api/operations/overview"; if($overview.runtimeBoundary.aiMode -ne 'MOCK'){throw 'Showcase Provider 不是 MOCK。'}
$askBody=@{userId=1;productId=101;skuId=10004;question='What is the SKU code?';clientRequestId=("showcase-verify-" + [Guid]::NewGuid().ToString('N'))} | ConvertTo-Json -Compress
$askResponse=Invoke-WebRequest -UseBasicParsing -Method Post -ContentType 'application/json' -Body $askBody "http://127.0.0.1:$api/api/ai/customer-service/ask"
$answer=$askResponse.Content | ConvertFrom-Json
if($askResponse.StatusCode -ne 200 -or $answer.provider.name -ne 'commerceflow-mock' -or $answer.provider.mode -ne 'MOCK' -or $answer.evidence.Count -lt 1 -or $answer.trace.Count -lt 1){throw 'Mock AI 验证未返回真实 Provider、Evidence 或 Trace。'}
foreach($headerName in @('X-RateLimit-Limit','X-RateLimit-Remaining','X-RateLimit-Reset','X-RateLimit-Mode')){if([string]::IsNullOrWhiteSpace($askResponse.Headers[$headerName])){throw "AI 响应缺少限流响应头: $headerName"}}
foreach($path in @('product-tshirt-white.png','product-tshirt-black.png','product-tshirt-gray.png','product-tshirt-navy.png','product-tote-beige.png')){$response=Invoke-WebRequest -UseBasicParsing "http://127.0.0.1:$admin/assets/products/$path";if($response.StatusCode -ne 200 -or $response.Headers['Content-Type'] -notlike 'image/*' -or $response.RawContentLength -le 0){throw "商品图片校验失败: $path"}}
if($Deep){& "$script:ShowcaseRoot\mvnw.cmd" -f "$script:ShowcaseRoot\apps\mall-api\pom.xml" test; Push-Location "$script:ShowcaseRoot\apps\admin-web"; npm test; npm run build; Pop-Location}
Write-Host 'SHOWCASE_VERIFY_OK'







