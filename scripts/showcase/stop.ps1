[CmdletBinding()]
param([switch]$RemoveData)
$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'showcase-common.ps1')
$state=Get-ShowcaseState; if($state){foreach($entry in $state.processes){if(Test-ShowcaseIdentity $entry){Stop-Process -Id $entry.pid -ErrorAction Stop; Write-Host "已停止 $($entry.name) PID=$($entry.pid)"}else{Write-Warning "跳过非本项目或过期 PID: $($entry.name) PID=$($entry.pid)"}}}
if($RemoveData){$answer=Read-Host '确认删除 CommerceFlow Showcase Docker 数据卷？输入 REMOVE_DATA 继续';if($answer -ne 'REMOVE_DATA'){throw '已取消删除数据卷。'}}
Push-Location $script:ShowcaseRoot; if($RemoveData){docker compose -p $script:ShowcaseComposeProject down -v}else{docker compose -p $script:ShowcaseComposeProject stop mysql redis}; Pop-Location
& (Join-Path $PSScriptRoot 'status.ps1')







