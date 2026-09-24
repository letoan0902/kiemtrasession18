#!/usr/bin/env bash
set -u

GOC="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$GOC" || exit 1

THU_MUC_RUN="run"
HAN_CHOT_SERVICE=150
HAN_CHOT_EUREKA=120
JAVA_OPTS="-Dfile.encoding=UTF-8 -Dsun.stdout.encoding=UTF-8 -Dsun.stderr.encoding=UTF-8"

mkdir -p "$THU_MUC_RUN"

buoc() { echo; echo "== $* =="; }
loi() { echo "[LỖI] $*" >&2; exit 1; }

ma_http() {
    curl -s -o /dev/null -w "%{http_code}" --max-time 3 "$1" 2>/dev/null || true
}

cong_dang_nghe() {
    netstat -ano 2>/dev/null | tr -d '\r' | awk -v p=":$1" '$1=="TCP" && $4=="LISTENING" && substr($2, length($2)-length(p)+1)==p {found=1} END {exit found?0:1}'
}

khoi_dong() {
    local module="$1" ten_log="$2" url="$3"
    shift 3
    local jar="$module/build/libs/$module.jar"
    local log="$THU_MUC_RUN/$ten_log.log"
    local pid_file="$THU_MUC_RUN/$ten_log.pid"

    buoc "Khởi động $ten_log"
    [ -f "$jar" ] || loi "Không thấy $jar. Hãy chạy scripts/build-all.sh trước."

    nohup java $JAVA_OPTS -jar "$jar" "$@" > "$log" 2>&1 &
    local pid=$!
    disown "$pid" 2>/dev/null || true
    local winpid=""
    [ -r "/proc/$pid/winpid" ] && winpid="$(cat "/proc/$pid/winpid")"
    echo "$pid ${winpid}" > "$pid_file"
    echo "  Tiến trình: pid $pid (Windows pid ${winpid:-không rõ}), log: $log"
    echo "  Chờ $url trả 200 (hạn ${HAN_CHOT_SERVICE} giây) ..."

    local bat_dau
    bat_dau=$(date +%s)
    while true; do
        local ma
        ma="$(ma_http "$url")"
        if [ "$ma" = "200" ]; then
            echo "  $ten_log đã sẵn sàng sau $(( $(date +%s) - bat_dau )) giây."
            return 0
        fi
        if ! kill -0 "$pid" 2>/dev/null; then
            echo "  Tiến trình $ten_log đã thoát. 30 dòng log cuối:" >&2
            tail -n 30 "$log" >&2
            loi "$ten_log không khởi động được"
        fi
        local da_qua=$(( $(date +%s) - bat_dau ))
        if [ "$da_qua" -ge "$HAN_CHOT_SERVICE" ]; then
            tail -n 30 "$log" >&2
            loi "$ten_log chưa sẵn sàng sau ${HAN_CHOT_SERVICE} giây (mã HTTP cuối: $ma)"
        fi
        sleep 2
    done
}

buoc "Kiểm tra cổng trống"
for cong in 8888 8761 8082 8092 8083 8081 8080; do
    if cong_dang_nghe "$cong"; then
        loi "Cổng $cong đang bị chiếm. Hãy chạy scripts/stop-all.sh trước."
    fi
done
echo "  Mọi cổng đều trống."

khoi_dong config-server     config-server           "http://localhost:8888/application/default"
khoi_dong eureka-server     eureka-server           "http://localhost:8761/actuator/health"
khoi_dong inventory-service inventory-service-8082  "http://localhost:8082/api/inventory/health"
khoi_dong inventory-service inventory-service-8092  "http://localhost:8092/api/inventory/health" --server.port=8092
khoi_dong payment-service   payment-service         "http://localhost:8083/api/payment/health"
khoi_dong order-service     order-service           "http://localhost:8081/api/order/health"
khoi_dong api-gateway       api-gateway             "http://localhost:8080/actuator/health"

buoc "Chờ Eureka ghi nhận đủ instance"
CAN_CO="order-service:8081 payment-service:8083 api-gateway:8080 inventory-service:8082 inventory-service:8092"
bat_dau=$(date +%s)
while true; do
    registry="$(curl -s --max-time 5 -H "Accept: application/json" http://localhost:8761/eureka/apps 2>/dev/null)"
    thieu=""
    for id in $CAN_CO; do
        echo "$registry" | grep -q "\"instanceId\":\"$id\"" || thieu="$thieu $id"
    done
    if [ -z "$thieu" ]; then
        echo "  Eureka đã có đủ: $CAN_CO"
        break
    fi
    da_qua=$(( $(date +%s) - bat_dau ))
    [ "$da_qua" -ge "$HAN_CHOT_EUREKA" ] && loi "Sau ${HAN_CHOT_EUREKA} giây Eureka vẫn thiếu:$thieu"
    echo "  Còn thiếu:$thieu (${da_qua} giây), thử lại sau 3 giây ..."
    sleep 3
done

buoc "Toàn hệ thống đã sẵn sàng"
echo "  Gateway:       http://localhost:8080"
echo "  Eureka:        http://localhost:8761"
echo "  Config Server: http://localhost:8888/order-service/default"
echo "  Log:           $GOC/$THU_MUC_RUN/"
