
$ErrorActionPreference = "Stop"

# Move to the directory containing this script
Set-Location $PSScriptRoot

# Check the SonarQube token
if ([string]::IsNullOrWhiteSpace($env:SONAR_TOKEN)) {
    Write-Error "SONAR_TOKEN is not set. Set it before running this script."
}

# Check that this is a Maven project
if (-not (Test-Path "pom.xml")) {
    Write-Error "pom.xml not found. Run this script from your project directory."
}

# Check that SonarQube is reachable
try {
    Invoke-WebRequest "http://localhost:9000/api/system/status" `
        -UseBasicParsing -TimeoutSec 5 | Out-Null
} catch {
    Write-Error "SonarQube is unavailable. Start the container with: docker start sonarqube"
}

# Run analysis
mvn org.sonarsource.scanner.maven:sonar-maven-plugin:sonar `
    "-Dsonar.projectKey=util" `
    "-Dsonar.host.url=http://localhost:9000" `
    "-Dsonar.token=$env:SONAR_TOKEN"

if ($LASTEXITCODE -ne 0) {
    throw "SonarQube analysis failed."
}

Write-Host "Analysis completed. Open http://localhost:9000" -ForegroundColor Green
