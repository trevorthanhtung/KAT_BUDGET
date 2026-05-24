param(
  [int]$Port = 5173
)

$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $projectRoot

$nodePath = "C:\Program Files\nodejs\node.exe"
$npmPath = "C:\Program Files\nodejs\npm.cmd"
$viteScript = Join-Path $projectRoot "node_modules\vite\bin\vite.js"

if (-not (Test-Path $nodePath)) {
  throw "Khong tim thay Node.js tai C:\Program Files\nodejs\node.exe"
}

if (-not (Test-Path "$projectRoot\node_modules")) {
  if (-not (Test-Path $npmPath)) {
    throw "Khong tim thay npm.cmd tai C:\Program Files\nodejs\npm.cmd"
  }
  Write-Host "Dang cai dependencies..."
  & $npmPath install
}

if (-not (Test-Path $viteScript)) {
  if (-not (Test-Path $npmPath)) {
    throw "Khong tim thay vite va npm.cmd de cai lai dependencies."
  }
  Write-Host "Dang cai lai dependencies do thieu vite..."
  & $npmPath install
}

Write-Host "Dang chay dev server tren port $Port (neu bi trung port, vite se tu chon port khac)."
& $nodePath $viteScript --host 0.0.0.0 --port $Port
