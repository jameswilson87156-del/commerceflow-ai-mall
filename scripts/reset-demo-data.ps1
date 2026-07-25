$ErrorActionPreference = 'Stop'
Set-Location (Split-Path -Parent $PSScriptRoot)
docker compose exec mysql mysql -ucommerceflow -pcommerceflow_demo commerceflow -e "DELETE FROM cart_item; DELETE FROM order_item; DELETE FROM orders; DELETE FROM inventory_change_log; DELETE FROM ai_trace;"
Write-Host 'Demo transactional data reset. Product catalog remains available.'
