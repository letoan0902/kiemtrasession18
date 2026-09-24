[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

$Goc = Split-Path -Parent $PSScriptRoot
Set-Location $Goc

$ThuMucRun = Join-Path $Goc 'run'
$HanChotService = 150
$HanChotEureka = 120
$JavaOpts = '-Dfile.encoding=UTF-8 -Dsun.stdout.encoding=UTF-8 -Dsun.stderr.encoding=UTF-8'

New-Item -ItemType Directory -Force -Path $ThuMucRun | Out-Null

function Buoc([string]$noiDung) { Write-Host ""; Write-Host "== $noiDung ==" }
function Loi([string]$noiDung) { Write-Host "[LỖI] $noiDung" -ForegroundColor Red; exit 1 }

function Lay-MaHttp([string]$url) {
    try {
        $r = Invoke-WebRequest -Uri $url -UseBasicParsing -TimeoutSec 3 -ErrorAction Stop
        return [int]$r.StatusCode
    } catch {
        if ($_.Exception.Response) { return [int]$_.Exception.Response.StatusCode }
        return 0
    }
}

function Lay-PidTheoCong([int]$cong) {
    @(Get-NetTCPConnection -LocalPort $cong -State Listen -ErrorAction SilentlyContinue |
        Select-Object -ExpandProperty OwningProcess -Unique | Where-Object { $_ -ne 0 })
}

function Khoi-Dong([string]$module, [string]$tenLog, [int]$cong, [string]$url, [string]$thamSoThem = '') {
    $jar = Join-Path $Goc "$module\build\libs\$module.jar"
    $log = Join-Path $ThuMucRun "$tenLog.log"
    $pidFile = Join-Path $ThuMucRun "$tenLog.pid"

    Buoc "Khởi động $tenLog"
    if (-not (Test-Path $jar)) { Loi "Không thấy $jar. Hãy chạy scripts\build-all.ps1 trước." }

    $lenh = "java $JavaOpts -jar `"$jar`" $thamSoThem > `"$log`" 2>&1"
    $p = Start-Process -FilePath 'cmd.exe' -ArgumentList '/c', $lenh -WindowStyle Hidden -PassThru
    Write-Host "  Tiến trình cmd: pid $($p.Id), log: $log"
    Write-Host "  Chờ $url trả 200 (hạn $HanChotService giây) ..."

    $batDau = Get-Date
    while ($true) {
        $ma = Lay-MaHttp $url
        if ($ma -eq 200) {
            $giay = [int]((Get-Date) - $batDau).TotalSeconds
            $javaPid = (Lay-PidTheoCong $cong) -join ' '
            Set-Content -Path $pidFile -Value $javaPid -Encoding ascii
            Write-Host "  $tenLog đã sẵn sàng sau $giay giây (java pid $javaPid)."
            return
        }
        if ($p.HasExited) {
            Write-Host "  Tiến trình $tenLog đã thoát. 30 dòng log cuối:" -ForegroundColor Red
            Get-Content $log -Tail 30 -Encoding UTF8
            Loi "$tenLog không khởi động được"
        }
        $daQua = [int]((Get-Date) - $batDau).TotalSeconds
        if ($daQua -ge $HanChotService) {
            Get-Content $log -Tail 30 -Encoding UTF8
            Loi "$tenLog chưa sẵn sàng sau $HanChotService giây (mã HTTP cuối: $ma)"
        }
        Start-Sleep -Seconds 2
    }
}

Buoc "Kiểm tra cổng trống"
foreach ($cong in 8888, 8761, 8082, 8092, 8083, 8081, 8080) {
    if ((Lay-PidTheoCong $cong).Count -gt 0) { Loi "Cổng $cong đang bị chiếm. Hãy chạy scripts\stop-all.ps1 trước." }
}
Write-Host "  Mọi cổng đều trống."

Khoi-Dong 'config-server'     'config-server'          8888 'http://localhost:8888/application/default'
Khoi-Dong 'eureka-server'     'eureka-server'          8761 'http://localhost:8761/actuator/health'
Khoi-Dong 'inventory-service' 'inventory-service-8082' 8082 'http://localhost:8082/api/inventory/health'
Khoi-Dong 'inventory-service' 'inventory-service-8092' 8092 'http://localhost:8092/api/inventory/health' '--server.port=8092'
Khoi-Dong 'payment-service'   'payment-service'        8083 'http://localhost:8083/api/payment/health'
Khoi-Dong 'order-service'     'order-service'          8081 'http://localhost:8081/api/order/health'
Khoi-Dong 'api-gateway'       'api-gateway'            8080 'http://localhost:8080/actuator/health'

Buoc "Chờ Eureka ghi nhận đủ instance"
$canCo = @('order-service:8081', 'payment-service:8083', 'api-gateway:8080', 'inventory-service:8082', 'inventory-service:8092')
$batDau = Get-Date
while ($true) {
    $coMat = @()
    try {
        $reg = Invoke-RestMethod -Uri 'http://localhost:8761/eureka/apps' -Headers @{ Accept = 'application/json' } -TimeoutSec 5
        foreach ($app in @($reg.applications.application)) {
            foreach ($inst in @($app.instance)) { $coMat += $inst.instanceId }
        }
    } catch { }
    $thieu = @($canCo | Where-Object { $coMat -notcontains $_ })
    if ($thieu.Count -eq 0) {
        Write-Host "  Eureka đã có đủ: $($canCo -join ', ')"
        break
    }
    $daQua = [int]((Get-Date) - $batDau).TotalSeconds
    if ($daQua -ge $HanChotEureka) { Loi "Sau $HanChotEureka giây Eureka vẫn thiếu: $($thieu -join ', ')" }
    Write-Host "  Còn thiếu: $($thieu -join ', ') ($daQua giây), thử lại sau 3 giây ..."
    Start-Sleep -Seconds 3
}

Buoc "Toàn hệ thống đã sẵn sàng"
Write-Host "  Gateway:       http://localhost:8080"
Write-Host "  Eureka:        http://localhost:8761"
Write-Host "  Config Server: http://localhost:8888/order-service/default"
Write-Host "  Log:           $ThuMucRun"
