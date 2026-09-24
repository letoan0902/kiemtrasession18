[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

$Goc = Split-Path -Parent $PSScriptRoot
Set-Location $Goc

$DanhSachCong = 8080, 8081, 8082, 8092, 8083, 8761, 8888

function Lay-PidTheoCong([int]$cong) {
    @(Get-NetTCPConnection -LocalPort $cong -State Listen -ErrorAction SilentlyContinue |
        Select-Object -ExpandProperty OwningProcess -Unique | Where-Object { $_ -ne 0 })
}

Write-Host "== Dừng tiến trình theo cổng =="
foreach ($cong in $DanhSachCong) {
    $pids = Lay-PidTheoCong $cong
    if ($pids.Count -eq 0) {
        Write-Host "  Cổng ${cong}: không có tiến trình."
        continue
    }
    foreach ($id in $pids) {
        Write-Host "  Cổng ${cong}: dừng PID $id"
        Stop-Process -Id $id -Force -ErrorAction SilentlyContinue
    }
}

Write-Host ""
Write-Host "== Kiểm tra lại cổng =="
$conChiem = $false
for ($lan = 0; $lan -lt 5; $lan++) {
    $conChiem = $false
    foreach ($cong in $DanhSachCong) { if ((Lay-PidTheoCong $cong).Count -gt 0) { $conChiem = $true } }
    if (-not $conChiem) { break }
    Start-Sleep -Seconds 1
}

foreach ($cong in $DanhSachCong) {
    $pids = Lay-PidTheoCong $cong
    if ($pids.Count -eq 0) {
        Write-Host "  Cổng ${cong}: TRỐNG"
    } else {
        Write-Host "  Cổng ${cong}: VẪN BỊ CHIẾM bởi PID $($pids -join ' ')" -ForegroundColor Red
    }
}

Remove-Item -Path (Join-Path $Goc 'run\*.pid') -ErrorAction SilentlyContinue

if ($conChiem) {
    Write-Host "[LỖI] Còn cổng bị chiếm, hãy kiểm tra thủ công." -ForegroundColor Red
    exit 1
}
Write-Host "Đã dừng toàn bộ service."
