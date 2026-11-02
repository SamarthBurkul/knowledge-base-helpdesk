# Waits for a container started by the Jenkinsfile. Succeeds only when the
# container is running, Docker reports it healthy and its HTTP health endpoint
# answers UP. Fails early, with the container's logs, if the container stops.
param(
    [Parameter(Mandatory = $true)][string]$ContainerName,
    [Parameter(Mandatory = $true)][string]$HealthUrl,
    [int]$TimeoutSeconds = 120
)

# Windows PowerShell 5.1 can turn docker's stderr into a terminating error under
# 'Stop', so failures are detected and thrown explicitly below.
$ErrorActionPreference = 'Continue'

function Show-ContainerLogs {
    Write-Output "--- last 50 log lines of $ContainerName ---"
    cmd /c "docker logs --tail 50 $ContainerName 2>&1"
    Write-Output '--- end of container log ---'
}

$format = '{{.State.Status}}|{{if .State.Health}}{{.State.Health.Status}}{{else}}none{{end}}|{{.State.ExitCode}}'
$clock = [Diagnostics.Stopwatch]::StartNew()
$lastSeen = ''

Write-Output "Waiting up to $TimeoutSeconds s for $ContainerName ($HealthUrl)"
while ($clock.Elapsed.TotalSeconds -lt $TimeoutSeconds) {
    $inspect = docker inspect --format $format $ContainerName 2>$null
    if (-not $inspect) {
        throw "Container $ContainerName does not exist. Check the 'docker run' output above."
    }

    $state, $dockerHealth, $exitCode = "$inspect".Split('|')
    if ($state -in 'exited', 'dead') {
        Show-ContainerLogs
        throw "Container $ContainerName stopped during start-up (state $state, exit code $exitCode)."
    }

    $httpHealth = 'unreachable'
    try {
        $httpHealth = (Invoke-RestMethod -Uri $HealthUrl -TimeoutSec 3 -ErrorAction Stop).status
    } catch { }

    # Log only changes, so a slow start does not flood the console.
    $seen = "state=$state, Docker health=$dockerHealth, HTTP health=$httpHealth"
    if ($seen -ne $lastSeen) {
        Write-Output "[$([int]$clock.Elapsed.TotalSeconds) s] $ContainerName $seen"
        $lastSeen = $seen
    }

    if ($httpHealth -eq 'UP' -and $dockerHealth -eq 'healthy') {
        Write-Output "$ContainerName is healthy (HTTP UP, Docker healthy)."
        return
    }
    Start-Sleep -Seconds 2
}

Show-ContainerLogs
throw "Container $ContainerName did not become healthy within $TimeoutSeconds s (last seen: $lastSeen)."
