[CmdletBinding()]
param([switch]$Live)

$ErrorActionPreference = 'Stop'
. (Join-Path (Split-Path $PSScriptRoot -Parent) 'showcase-common.ps1')

$script:assertions = 0
function Assert-ShowcaseTest {
    param([bool]$Condition, [string]$Message)
    $script:assertions++
    if (-not $Condition) { throw "ASSERTION FAILED: $Message" }
}

function Assert-ShowcaseThrows {
    param([scriptblock]$Action, [string]$ExpectedMessage)
    $script:assertions++
    try {
        & $Action
    } catch {
        if ($_.Exception.Message -like "*$ExpectedMessage*") { return }
        throw "ASSERTION FAILED: expected error containing '$ExpectedMessage', got '$($_.Exception.Message)'"
    }
    throw "ASSERTION FAILED: expected error containing '$ExpectedMessage', but no error was thrown"
}

$jsonLines = @(
    '{"Service":"mysql","State":"running","Health":"healthy"}'
    "`r`n{""Service"":""redis"",""State"":""running"",""Health"":""healthy""}`r`n"
)
$decoded = @(ConvertFrom-ShowcaseComposeJsonLines -Lines $jsonLines)
Assert-ShowcaseTest ($decoded.Count -eq 2) 'Compose JSON Lines parser should decode separate and embedded lines.'
Assert-ShowcaseTest ($decoded[0].Service -eq 'mysql' -and $decoded[1].Service -eq 'redis') 'Compose records should preserve service names.'

$healthy = Get-ShowcaseDependencyHealth -Services $decoded
Assert-ShowcaseTest ($healthy.Ready -and -not $healthy.Failed) 'healthy dependencies should be ready.'

$starting = Get-ShowcaseDependencyHealth -Services @(
    [pscustomobject]@{ Service='mysql'; State='running'; Health='healthy' }
    [pscustomobject]@{ Service='redis'; State='running'; Health='starting' }
)
Assert-ShowcaseTest (-not $starting.Ready -and -not $starting.Failed) 'starting health should wait without failing.'

$exited = Get-ShowcaseDependencyHealth -Services @(
    [pscustomobject]@{ Service='mysql'; State='running'; Health='healthy' }
    [pscustomobject]@{ Service='redis'; State='exited'; Health='' }
)
Assert-ShowcaseTest (-not $exited.Ready -and $exited.Failed) 'an exited dependency should fail fast.'

$unhealthy = Get-ShowcaseDependencyHealth -Services @(
    [pscustomobject]@{ Service='mysql'; State='running'; Health='unhealthy' }
    [pscustomobject]@{ Service='redis'; State='running'; Health='healthy' }
)
Assert-ShowcaseTest ($unhealthy.Failed) 'an unhealthy dependency should fail fast.'

$missing = Get-ShowcaseDependencyHealth -Services @(
    [pscustomobject]@{ Service='mysql'; State='running'; Health='healthy' }
)
Assert-ShowcaseTest (-not $missing.Ready -and -not $missing.Failed) 'a missing dependency should remain pending during startup.'

$duplicate = Get-ShowcaseDependencyHealth -Services @(
    [pscustomobject]@{ Service='mysql'; State='running'; Health='healthy' }
    [pscustomobject]@{ Service='mysql'; State='running'; Health='healthy' }
    [pscustomobject]@{ Service='redis'; State='running'; Health='healthy' }
)
Assert-ShowcaseTest ($duplicate.Failed) 'duplicate service records should fail closed.'

Assert-ShowcaseThrows { ConvertFrom-ShowcaseComposeJsonLines -Lines @('{invalid-json') } 'Invalid JSON'
Assert-ShowcaseThrows { ConvertFrom-ShowcaseComposeJsonLines -Lines @('{"State":"running"}') } 'no Service field'

$before = @(
    [pscustomobject]@{ Service='mysql'; State='running'; ID='mysql-existing' }
    [pscustomobject]@{ Service='redis'; State='exited'; ID='redis-existing' }
)
$after = @(
    [pscustomobject]@{ Service='mysql'; State='running'; ID='mysql-existing' }
    [pscustomobject]@{ Service='redis'; State='running'; ID='redis-existing' }
)
$newServices = @(Get-ShowcaseNewlyStartedComposeServices -Before $before -After $after)
Assert-ShowcaseTest ($newServices.Count -eq 1 -and $newServices[0] -eq 'redis') 'startup cleanup should only select the dependency newly changed to running.'
$same = @(Get-ShowcaseNewlyStartedComposeServices -Before $after -After $after)
Assert-ShowcaseTest ($same.Count -eq 0) 'startup cleanup should preserve dependencies that were already running.'

$stateProject = Resolve-ShowcaseComposeProjectName -EnvironmentProject '' -StateProject 'commerceflow-showcase-closeout-20261020'
Assert-ShowcaseTest ($stateProject -eq 'commerceflow-showcase-closeout-20261020') 'the saved runtime project should be used when no explicit override is set.'
$environmentProject = Resolve-ShowcaseComposeProjectName -EnvironmentProject 'commerceflow-explicit' -StateProject 'commerceflow-showcase-closeout-20261020'
Assert-ShowcaseTest ($environmentProject -eq 'commerceflow-explicit') 'an explicit project override should take precedence over saved runtime state.'
Assert-ShowcaseThrows { Resolve-ShowcaseComposeProjectName -EnvironmentProject 'invalid/project' -StateProject '' } 'unsupported characters'

if ($Live) {
    $liveServices = @(Get-ShowcaseComposeServices -All)
    $liveHealth = Get-ShowcaseDependencyHealth -Services $liveServices
    Assert-ShowcaseTest ($liveHealth.Ready) "live Compose dependencies should be healthy; got $($liveHealth.Summary)."
}

Write-Host "SHOWCASE_COMPOSE_HEALTH_TESTS_OK assertions=$script:assertions live=$([bool]$Live)"
