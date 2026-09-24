$ErrorActionPreference = 'Continue'
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$OutputEncoding = [System.Text.Encoding]::UTF8

$Goc = Split-Path -Parent $PSScriptRoot
Set-Location $Goc

$HanChotGiay = 180

function Buoc([string]$noiDung) { Write-Host ""; Write-Host "== $noiDung ==" }
function Loi([string]$noiDung) { Write-Host "[LỖI] $noiDung" -ForegroundColor Red; exit 1 }

Buoc "Bước 1/4: docker compose up -d"
docker compose up -d
if ($LASTEXITCODE -ne 0) { Loi "docker compose up thất bại. Docker Desktop đã chạy chưa?" }

Buoc "Bước 2/4: chờ Kafka trả lời lệnh kafka-topics --list"
$batDau = Get-Date
while ($true) {
    docker exec shopmart-kafka kafka-topics --bootstrap-server localhost:9092 --list *> $null
    if ($LASTEXITCODE -eq 0) { break }
    $daQua = [int]((Get-Date) - $batDau).TotalSeconds
    if ($daQua -ge $HanChotGiay) { Loi "Kafka chưa sẵn sàng sau $HanChotGiay giây" }
    Write-Host "  Kafka chưa sẵn sàng ($daQua giây), thử lại sau 3 giây ..."
    Start-Sleep -Seconds 3
}
Write-Host "  Kafka đã trả lời."

Buoc "Bước 3/4: chờ topic order và payment được tạo (container kafka-init)"
$batDau = Get-Date
while ($true) {
    $danhSach = @(docker exec shopmart-kafka kafka-topics --bootstrap-server localhost:9092 --list 2>$null |
        ForEach-Object { $_.Trim() })
    $coOrder = $danhSach -contains 'order'
    $coPayment = $danhSach -contains 'payment'
    if ($coOrder -and $coPayment) { break }
    $daQua = [int]((Get-Date) - $batDau).TotalSeconds
    if ($daQua -ge $HanChotGiay) {
        Loi "Chưa thấy đủ topic order và payment sau $HanChotGiay giây. Xem: docker logs shopmart-kafka-init"
    }
    Write-Host "  Chưa đủ topic (order=$coOrder, payment=$coPayment), thử lại sau 3 giây ..."
    Start-Sleep -Seconds 3
}
Write-Host "  Đã có đủ hai topic:"
docker exec shopmart-kafka kafka-topics --bootstrap-server localhost:9092 --describe --topic order 2>$null | Select-Object -First 1
docker exec shopmart-kafka kafka-topics --bootstrap-server localhost:9092 --describe --topic payment 2>$null | Select-Object -First 1

Buoc "Bước 4/4: chờ Redis trả PONG"
$batDau = Get-Date
while ($true) {
    $traLoi = (docker exec shopmart-redis redis-cli ping 2>$null | Out-String).Trim()
    if ($traLoi -eq 'PONG') { break }
    $daQua = [int]((Get-Date) - $batDau).TotalSeconds
    if ($daQua -ge 60) { Loi "Redis chưa trả PONG sau 60 giây" }
    Write-Host "  Redis chưa sẵn sàng, thử lại sau 2 giây ..."
    Start-Sleep -Seconds 2
}
Write-Host "  Redis trả lời: PONG"

Buoc "Hạ tầng đã sẵn sàng"
docker compose ps
