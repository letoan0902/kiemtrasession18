[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

$Goc = Split-Path -Parent $PSScriptRoot
Set-Location $Goc

$Gradle = $env:GRADLE
if (-not $Gradle) {
    $Gradle = Join-Path $env:USERPROFILE '.gradle\wrapper\dists\gradle-8.14.3-bin\cv11ve7ro1n3o1j4so8xd9n66\gradle-8.14.3\bin\gradle.bat'
}
if (-not (Test-Path $Gradle)) {
    Write-Host "[LỖI] Không tìm thấy Gradle tại $Gradle. Đặt biến môi trường GRADLE để chỉ đường dẫn khác." -ForegroundColor Red
    exit 1
}

Write-Host "== Build sáu module bằng $Gradle =="
& $Gradle ':config-server:build' ':eureka-server:build' ':api-gateway:build' `
    ':order-service:build' ':inventory-service:build' ':payment-service:build'
$ma = $LASTEXITCODE

Write-Host ""
if ($ma -eq 0) {
    Write-Host "Build thành công. Các tệp jar:"
    foreach ($m in 'config-server', 'eureka-server', 'api-gateway', 'order-service', 'inventory-service', 'payment-service') {
        $jar = Join-Path $Goc "$m\build\libs\$m.jar"
        if (Test-Path $jar) {
            Write-Host ("  {0}  {1:N0} byte" -f $jar, (Get-Item $jar).Length)
        } else {
            Write-Host "  Thiếu $jar"
        }
    }
} else {
    Write-Host "[LỖI] Build thất bại với mã $ma" -ForegroundColor Red
}
exit $ma
