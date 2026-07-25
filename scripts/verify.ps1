$ErrorActionPreference = 'Stop'
Set-Location (Split-Path -Parent $PSScriptRoot)
./mvnw.cmd -f apps/mall-api/pom.xml test
$mallApiPort = $env:MALL_API_PORT
if ([string]::IsNullOrWhiteSpace($mallApiPort)) { $mallApiPort = '8080' }
Invoke-WebRequest "http://localhost:$mallApiPort/actuator/health" -UseBasicParsing | Select-Object StatusCode
Invoke-WebRequest http://localhost:8000/health -UseBasicParsing | Select-Object StatusCode
