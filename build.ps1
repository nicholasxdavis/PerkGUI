# Build script for PerkShop plugin
Write-Host "Building PerkShop plugin..." -ForegroundColor Green

# Navigate to plugin directory
Set-Location $PSScriptRoot

# Check if Maven is available
$mavenCmd = Get-Command mvn -ErrorAction SilentlyContinue

if (-not $mavenCmd) {
    Write-Host "Maven not found. Downloading Maven..." -ForegroundColor Yellow
    
    $mavenVersion = "3.9.6"
    $mavenUrl = "https://archive.apache.org/dist/maven/maven-3/$mavenVersion/binaries/apache-maven-$mavenVersion-bin.zip"
    $mavenZip = "maven-temp.zip"
    $mavenDir = "maven-temp"
    
    try {
        Write-Host "Downloading Maven from $mavenUrl..." -ForegroundColor Yellow
        Invoke-WebRequest -Uri $mavenUrl -OutFile $mavenZip -UseBasicParsing
        
        Write-Host "Extracting Maven..." -ForegroundColor Yellow
        Expand-Archive -Path $mavenZip -DestinationPath $mavenDir -Force
        Remove-Item $mavenZip
        
        $mvnPath = Join-Path (Resolve-Path $mavenDir) "apache-maven-$mavenVersion\bin\mvn.cmd"
        
        if (Test-Path $mvnPath) {
            Write-Host "Building plugin with downloaded Maven..." -ForegroundColor Green
            & $mvnPath clean package
            
            Write-Host "Cleaning up Maven..." -ForegroundColor Yellow
            Remove-Item -Recurse -Force $mavenDir
        } else {
            Write-Host "Maven extraction failed. Trying alternative method..." -ForegroundColor Red
            throw "Maven not found"
        }
    } catch {
        Write-Host "Failed to download Maven. Please install Maven manually:" -ForegroundColor Red
        Write-Host "1. Download from https://maven.apache.org/download.cgi" -ForegroundColor Yellow
        Write-Host "2. Extract and add to PATH" -ForegroundColor Yellow
        Write-Host "3. Run: mvn clean package" -ForegroundColor Yellow
        exit 1
    }
} else {
    Write-Host "Using system Maven..." -ForegroundColor Green
    mvn clean package
}

if ($LASTEXITCODE -eq 0) {
    Write-Host "Build successful! JAR file is in the target directory." -ForegroundColor Green
    
    # Copy to plugins folder if it exists
    $pluginsPath = Join-Path $PSScriptRoot "..\plugins"
    if (Test-Path $pluginsPath) {
        $jarFile = Get-ChildItem -Path "target" -Filter "PerkShop-*.jar" | Select-Object -First 1
        if ($jarFile) {
            Copy-Item $jarFile.FullName -Destination $pluginsPath -Force
            Write-Host "Copied JAR to plugins folder: $($jarFile.Name)" -ForegroundColor Green
        }
    }
} else {
    Write-Host "Build failed! Check the errors above." -ForegroundColor Red
}

Set-Location $PSScriptRoot








