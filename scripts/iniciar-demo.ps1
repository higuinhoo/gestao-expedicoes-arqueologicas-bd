param([switch]$SomentePreparar, [switch]$ConfiguracaoExistente)
$ErrorActionPreference = 'Stop'
$ProgressPreference = 'SilentlyContinue'
$OutputEncoding = New-Object System.Text.UTF8Encoding($false)
$taskRoot = Split-Path -Parent $PSScriptRoot
Set-Location -LiteralPath $taskRoot

function Assert-Exit([string]$Message) { if ($LASTEXITCODE -ne 0) { throw $Message } }
function Test-Docker {
    try { & docker info --format '{{.ServerVersion}}' 2>$null | Out-Null; return ($LASTEXITCODE -eq 0) }
    catch { return $false }
}
function Get-JavaVersion([string]$Executable) {
    try {
        $taskInfo = New-Object System.Diagnostics.ProcessStartInfo
        $taskInfo.FileName = $Executable; $taskInfo.Arguments = '-version'
        $taskInfo.UseShellExecute = $false; $taskInfo.CreateNoWindow = $true
        $taskInfo.RedirectStandardError = $true; $taskInfo.RedirectStandardOutput = $true
        $taskProcess = [System.Diagnostics.Process]::Start($taskInfo)
        $taskVersion = $taskProcess.StandardError.ReadToEnd() + $taskProcess.StandardOutput.ReadToEnd()
        $taskProcess.WaitForExit()
        if ($taskVersion -match 'version "(\d+)') { return [int]$Matches[1] }
    } catch { }
    return 0
}
function Find-Java {
    $taskCandidates = @()
    if ($env:JAVA_HOME) { $taskCandidates += (Join-Path $env:JAVA_HOME 'bin\java.exe') }
    $taskCommand = Get-Command java -ErrorAction SilentlyContinue
    if ($taskCommand) { $taskCandidates += $taskCommand.Source }
    $taskRuntime = Join-Path $taskRoot 'local\runtime'
    if (Test-Path -LiteralPath $taskRuntime) {
        $taskCandidates += @(Get-ChildItem -LiteralPath $taskRuntime -Filter java.exe -Recurse | ForEach-Object FullName)
    }
    foreach ($taskCandidate in $taskCandidates) {
        if ((Test-Path -LiteralPath $taskCandidate) -and (Get-JavaVersion $taskCandidate) -ge 17) { return $taskCandidate }
    }
    Write-Host 'Baixando Java 17 portatil (Adoptium). Isso e necessario apenas na primeira vez.'
    [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
    $taskArch = if ($env:PROCESSOR_ARCHITECTURE -eq 'ARM64') { 'aarch64' } else { 'x64' }
    $taskAsset = @(Invoke-RestMethod -Uri "https://api.adoptium.net/v3/assets/latest/17/hotspot?architecture=$taskArch&image_type=jre&os=windows&vendor=eclipse" -TimeoutSec 45)[0]
    if (-not $taskAsset.binary.package.checksum) { throw 'A API nao retornou um pacote Java verificavel.' }
    if (-not $taskAsset.binary.package.link.StartsWith('https://github.com/adoptium/')) { throw 'Endereco de download inesperado.' }
    New-Item -ItemType Directory -Force -Path $taskRuntime | Out-Null
    $taskZip = Join-Path $taskRuntime 'java-download.zip'
    Invoke-WebRequest -UseBasicParsing -Uri $taskAsset.binary.package.link -OutFile $taskZip -TimeoutSec 300
    if ((Get-FileHash -LiteralPath $taskZip -Algorithm SHA256).Hash.ToLower() -ne $taskAsset.binary.package.checksum.ToLower()) {
        throw 'O checksum do Java baixado nao confere. Nenhum binario sera executado.'
    }
    Expand-Archive -LiteralPath $taskZip -DestinationPath $taskRuntime -Force
    $taskJava = @(Get-ChildItem -LiteralPath $taskRuntime -Filter java.exe -Recurse | Where-Object { (Get-JavaVersion $_.FullName) -ge 17 })
    if ($taskJava.Count -eq 0) { throw 'Java 17 nao foi encontrado depois da extracao.' }
    return $taskJava[0].FullName
}

try {
    if (-not (Test-Path -LiteralPath 'dist\arqueologia-2.0.0.jar')) { throw 'JAR ausente. Baixe o pacote completo ou compile o projeto Maven.' }
    if (-not $ConfiguracaoExistente -and -not (Get-Command docker -ErrorAction SilentlyContinue)) { throw 'Instale o Docker Desktop, abra-o e execute iniciar.bat novamente.' }
    $taskJava = Find-Java
    if (-not $ConfiguracaoExistente) {
        if (-not (Get-Command docker -ErrorAction SilentlyContinue)) { throw 'Instale o Docker Desktop, abra-o e execute iniciar.bat novamente.' }
        if (-not (Test-Docker)) {
            $taskDesktop = Join-Path $env:ProgramFiles 'Docker\Docker\Docker Desktop.exe'
            if (Test-Path -LiteralPath $taskDesktop) { Start-Process -FilePath $taskDesktop -WindowStyle Hidden }
            Write-Host 'Aguardando o Docker Desktop iniciar...'
            $taskReady = $false
            for ($taskAttempt=0; $taskAttempt -lt 45; $taskAttempt++) {
                Start-Sleep -Seconds 2
                if (Test-Docker) { $taskReady=$true; break }
            }
            if (-not $taskReady) { throw 'Docker indisponivel. Abra o Docker Desktop e aguarde o mecanismo Linux ficar pronto.' }
        }
        Write-Host 'Iniciando MySQL e aguardando o banco completo...'
        & docker compose -f compose.yaml up -d --wait --wait-timeout 240
        Assert-Exit 'Nao foi possivel iniciar o banco. Confira Docker, internet e disponibilidade da porta 33319.'
        # Idempotente: atualiza somente as tres contas de demonstracao, sem recriar tabelas ou apagar cadastros.
        Get-Content -LiteralPath 'demo\07_usuarios.sql' -Raw -Encoding UTF8 | & docker compose -f compose.yaml exec -T db sh -c 'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" exec mysql -uroot --default-character-set=utf8mb4'
        Assert-Exit 'Nao foi possivel configurar as contas de demonstracao.'
        $taskPort = if ($env:ARQ_DB_PORT) { $env:ARQ_DB_PORT } else { '33319' }
        $taskUrl = "db.url=jdbc:mysql://127.0.0.1:$taskPort/arqueologia_ucb?sslMode=PREFERRED&allowPublicKeyRetrieval=true&connectionTimeZone=UTC&forceConnectionTimeZoneToSession=true`n"
        $taskConfig = Join-Path $taskRoot 'app\config.properties'
        if ((Test-Path -LiteralPath $taskConfig) -and ([IO.File]::ReadAllText($taskConfig) -ne $taskUrl)) {
            New-Item -ItemType Directory -Force -Path 'local' | Out-Null
            Copy-Item -LiteralPath $taskConfig -Destination ('local\config-anterior-' + (Get-Date -Format yyyyMMdd-HHmmssfff) + '.properties')
        }
        [IO.File]::WriteAllText($taskConfig,$taskUrl,(New-Object System.Text.UTF8Encoding($false)))
        Write-Host 'Acessos: admin/admin | catalogador/catalogador | consulta/consulta'
    } elseif (-not (Test-Path -LiteralPath 'app\config.properties')) { throw 'app/config.properties nao existe. Prepare sua conexao externa antes de usar -ConfiguracaoExistente.' }
    if ($SomentePreparar) { Write-Host 'Ambiente pronto. No NetBeans, abra app/ e execute o projeto com JDK 17+.'; exit 0 }
    Write-Host 'Abrindo a interface Swing...'
    & $taskJava '-Darq.config=app/config.properties' -jar 'dist/arqueologia-2.0.0.jar'
    Assert-Exit 'A aplicacao encerrou com erro.'
} catch { Write-Host ('ERRO: ' + $_.Exception.Message) -ForegroundColor Red; exit 1 }
