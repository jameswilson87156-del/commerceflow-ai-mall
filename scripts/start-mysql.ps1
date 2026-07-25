param([switch]$Reset)
$ErrorActionPreference = 'Stop'
Set-Location (Split-Path -Parent $PSScriptRoot)
if ($Reset) { docker compose down -v }
docker compose up -d mysql
docker compose ps
