param(
  [string]$ApiBase = 'http://127.0.0.1:8081/api'
)

$ErrorActionPreference = 'Stop'

function Invoke-OrderRequest {
  param(
    [string]$Key,
    [string]$Body
  )

  try {
    $response = Invoke-WebRequest -UseBasicParsing -Method Post -Uri "$ApiBase/v1/me/orders" -ContentType 'application/json' -Headers @{ 'Idempotency-Key' = $Key } -Body $Body
    return [PSCustomObject]@{ Status = [int]$response.StatusCode; Body = ($response.Content | ConvertFrom-Json) }
  } catch {
    $webResponse = $_.Exception.Response
    if ($null -eq $webResponse) { throw }
    $reader = New-Object System.IO.StreamReader($webResponse.GetResponseStream())
    $content = $reader.ReadToEnd()
    return [PSCustomObject]@{ Status = [int]$webResponse.StatusCode; Body = ($content | ConvertFrom-Json) }
  }
}

function Assert-Result {
  param([bool]$Condition, [string]$Message)
  if (-not $Condition) { throw $Message }
}

$primaryKey = 'p3-demo-primary'
$primaryBody = '{"items":[{"skuId":10001,"quantity":1}]}'
$primary = Invoke-OrderRequest -Key $primaryKey -Body $primaryBody
Assert-Result ($primary.Status -eq 200) 'Primary order was not created.'

$replay = Invoke-OrderRequest -Key $primaryKey -Body $primaryBody
Assert-Result ($replay.Status -eq 200 -and $replay.Body.orderNo -eq $primary.Body.orderNo) 'Same-key replay did not return the original order.'

$conflict = Invoke-OrderRequest -Key $primaryKey -Body '{"items":[{"skuId":10001,"quantity":2}]}'
Assert-Result ($conflict.Status -eq 409 -and $conflict.Body.code -eq 'IDEMPOTENCY_KEY_REUSED') 'Same-key conflict did not return 409.'

$shortage = Invoke-OrderRequest -Key 'p3-demo-shortage' -Body '{"items":[{"skuId":10005,"quantity":1}]}'
Assert-Result ($shortage.Status -eq 400 -and $shortage.Body.code -eq 'INVENTORY_INSUFFICIENT') 'Shortage request did not fail as expected.'

$secondary = Invoke-OrderRequest -Key 'p3-demo-secondary' -Body '{"items":[{"skuId":10002,"quantity":2}]}'
Assert-Result ($secondary.Status -eq 200) 'Secondary order was not created.'

$tertiary = Invoke-OrderRequest -Key 'p3-demo-tertiary' -Body '{"items":[{"skuId":10004,"quantity":1}]}'
Assert-Result ($tertiary.Status -eq 200) 'Tertiary order was not created.'

$orders = Invoke-RestMethod -Uri "$ApiBase/v1/me/orders"
Assert-Result ($orders.Count -eq 3) 'Only the three successful requests should create order rows.'
$evidence = Invoke-RestMethod -Uri "$ApiBase/v1/me/orders/$($primary.Body.orderNo)/execution-evidence"
Assert-Result ($evidence.inventoryMovements.Count -eq 1) 'Primary order does not have exactly one inventory movement.'

[PSCustomObject]@{
  CreatedOrderNumbers = @($primary.Body.orderNo, $secondary.Body.orderNo, $tertiary.Body.orderNo)
  ReplayOrderNumber = $replay.Body.orderNo
  ConflictStatus = $conflict.Status
  ShortageStatus = $shortage.Status
  CreatedOrderCount = $orders.Count
  PrimaryMovementCount = $evidence.inventoryMovements.Count
} | ConvertTo-Json -Depth 4
