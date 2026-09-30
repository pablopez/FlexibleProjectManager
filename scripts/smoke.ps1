$ErrorActionPreference = 'Stop'
$baseUrl = if ($args.Count -gt 0) { $args[0] } else { 'http://localhost:8080' }
Invoke-WebRequest "$baseUrl/api/v1/system/liveness" -UseBasicParsing | Out-Null
Invoke-WebRequest "$baseUrl/api/v1/system/readiness" -UseBasicParsing | Out-Null
Invoke-WebRequest "$baseUrl/api/v1/setup/status" -UseBasicParsing | Out-Null
Write-Output "backend packaged smoke check passed: $baseUrl"
