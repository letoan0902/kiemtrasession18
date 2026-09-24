[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

$Goc = Split-Path -Parent $PSScriptRoot
Set-Location $Goc

Write-Host "== docker compose down =="
docker compose down
Write-Host "Đã dừng hạ tầng Docker."
