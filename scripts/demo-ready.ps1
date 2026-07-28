$ErrorActionPreference = 'Stop'
Set-Location (Split-Path -Parent $PSScriptRoot)
./scripts/start-mysql.ps1
./scripts/start-redis.ps1
./mvnw.cmd -f apps/mall-api/pom.xml test
Write-Host 'CommerceFlow AI Mall demo is ready: start mall-api, ai-service, admin-web, and mobile-app.'
