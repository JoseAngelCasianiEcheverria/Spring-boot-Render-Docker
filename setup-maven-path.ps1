# =============================================================================
# setup-maven-path.ps1
# Ejecuta este script UNA SOLA VEZ para que "mvn" funcione globalmente
# en cualquier PowerShell sin necesidad de wrappers.
#
# Uso: .\setup-maven-path.ps1
# =============================================================================
$ErrorActionPreference = 'Stop'

Write-Host ""
Write-Host "=========================================="
Write-Host " Configurando Maven en el PATH de Windows"
Write-Host "=========================================="
Write-Host ""

# -------------------------------------------------------
# 1. Buscar Maven en ubicaciones conocidas
# -------------------------------------------------------
$candidates = @(
    # IntelliJ IDEA 2025.x bundled Maven (más probable dado el entorno detectado)
    "C:\Program Files\JetBrains\IntelliJ IDEA 2025.1.1.1\plugins\maven\lib\maven3\bin",
    "C:\Program Files\JetBrains\IntelliJ IDEA 2025.1.1\plugins\maven\lib\maven3\bin",
    "C:\Program Files\JetBrains\IntelliJ IDEA 2025.1\plugins\maven\lib\maven3\bin",
    "C:\Program Files\JetBrains\IntelliJ IDEA 2024.3\plugins\maven\lib\maven3\bin",
    "C:\Program Files\JetBrains\IntelliJ IDEA 2024.2\plugins\maven\lib\maven3\bin",
    # Maven descargado por mvnw.ps1 (si ya se ejecutó antes)
    "$env:USERPROFILE\.mvn\wrapper\apache-maven-3.9.6\bin",
    "$env:USERPROFILE\.mvn\wrapper\apache-maven-3.9.5\bin",
    # Instalaciones manuales comunes
    "C:\maven\bin",
    "C:\tools\maven\bin",
    "C:\Program Files\Apache\maven\bin",
    "$env:USERPROFILE\maven\bin"
)

$mavenBin = $null

Write-Host "Buscando Maven en ubicaciones conocidas..."
foreach ($path in $candidates) {
    if (Test-Path (Join-Path $path "mvn.cmd")) {
        $mavenBin = $path
        Write-Host "  ENCONTRADO: $mavenBin"
        break
    }
}

# -------------------------------------------------------
# 2. Buscar dinámicamente en toda la instalación de IntelliJ
# -------------------------------------------------------
if (-not $mavenBin) {
    Write-Host "  Buscando en todas las instalaciones de JetBrains..."
    $jetbrainsBase = "C:\Program Files\JetBrains"
    if (Test-Path $jetbrainsBase) {
        $found = Get-ChildItem $jetbrainsBase -Recurse -Filter "mvn.cmd" -ErrorAction SilentlyContinue |
                 Select-Object -First 1
        if ($found) {
            $mavenBin = $found.DirectoryName
            Write-Host "  ENCONTRADO: $mavenBin"
        }
    }
}

# -------------------------------------------------------
# 3. Si no hay Maven, descargarlo con el wrapper del proyecto
# -------------------------------------------------------
if (-not $mavenBin) {
    Write-Host ""
    Write-Host "Maven no encontrado localmente. Descargando Maven 3.9.6..."
    $wrapperScript = Join-Path $PSScriptRoot "mvnw.ps1"
    if (Test-Path $wrapperScript) {
        & $wrapperScript --version | Out-Null
    } else {
        # Descarga directa como fallback
        $MavenVersion = "3.9.6"
        $MavenCache   = Join-Path $env:USERPROFILE ".mvn\wrapper"
        $DistUrl      = "https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/$MavenVersion/apache-maven-$MavenVersion-bin.zip"
        $DistZip      = Join-Path $MavenCache "mvn-dist.zip"
        if (-not (Test-Path $MavenCache)) { New-Item -ItemType Directory -Path $MavenCache -Force | Out-Null }
        Invoke-WebRequest -Uri $DistUrl -OutFile $DistZip
        Expand-Archive -Path $DistZip -DestinationPath $MavenCache -Force
        Remove-Item $DistZip -Force
    }
    $mavenBin = "$env:USERPROFILE\.mvn\wrapper\apache-maven-3.9.6\bin"
    Write-Host "  DESCARGADO: $mavenBin"
}

# -------------------------------------------------------
# 4. Agregar al PATH del usuario de forma PERMANENTE
# -------------------------------------------------------
Write-Host ""
Write-Host "Agregando al PATH permanente del usuario..."
$userPath = [System.Environment]::GetEnvironmentVariable("PATH", [System.EnvironmentVariableTarget]::User)

if ($userPath -like "*$mavenBin*") {
    Write-Host "  Maven ya estaba en el PATH. No se requieren cambios."
} else {
    $newPath = if ($userPath) { "$userPath;$mavenBin" } else { $mavenBin }
    [System.Environment]::SetEnvironmentVariable("PATH", $newPath, [System.EnvironmentVariableTarget]::User)
    Write-Host "  PATH de usuario actualizado permanentemente."
}

# -------------------------------------------------------
# 5. Agregar al PATH de esta sesión también (efecto inmediato)
# -------------------------------------------------------
$env:PATH = "$env:PATH;$mavenBin"

# -------------------------------------------------------
# 6. Verificar
# -------------------------------------------------------
Write-Host ""
Write-Host "Verificando instalacion..."
try {
    $mvnVersion = & "$mavenBin\mvn.cmd" --version 2>&1 | Select-Object -First 2
    Write-Host $mvnVersion
    Write-Host ""
    Write-Host "============================================"
    Write-Host " OK: Maven configurado correctamente!"
    Write-Host ""
    Write-Host " IMPORTANTE: Abre una NUEVA ventana de"
    Write-Host " PowerShell para que 'mvn' funcione sin"
    Write-Host " ruta completa."
    Write-Host ""
    Write-Host " En esta ventana ya puedes usar 'mvn'"
    Write-Host " directamente ahora mismo."
    Write-Host "============================================"
} catch {
    Write-Host "Error al verificar Maven: $_"
    exit 1
}

