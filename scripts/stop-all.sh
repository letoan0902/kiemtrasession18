#!/usr/bin/env bash
set -u

GOC="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$GOC" || exit 1

DANH_SACH_CONG="8080 8081 8082 8092 8083 8761 8888"

pid_theo_cong() {
    netstat -ano 2>/dev/null | tr -d '\r' | awk -v p=":$1" \
        '$1=="TCP" && $4=="LISTENING" && substr($2, length($2)-length(p)+1)==p && $5!="0" {print $5}' | sort -u
}

echo "== Dừng tiến trình theo cổng =="
for cong in $DANH_SACH_CONG; do
    pids="$(pid_theo_cong "$cong")"
    if [ -z "$pids" ]; then
        echo "  Cổng $cong: không có tiến trình."
        continue
    fi
    for pid in $pids; do
        echo "  Cổng $cong: dừng PID $pid"
        powershell -NoProfile -Command "Stop-Process -Id $pid -Force -ErrorAction SilentlyContinue" >/dev/null 2>&1
    done
done

echo
echo "== Kiểm tra lại cổng =="
con_chiem=0
for lan in 1 2 3 4 5; do
    con_chiem=0
    for cong in $DANH_SACH_CONG; do
        [ -n "$(pid_theo_cong "$cong")" ] && con_chiem=1
    done
    [ "$con_chiem" -eq 0 ] && break
    sleep 1
done

for cong in $DANH_SACH_CONG; do
    pids="$(pid_theo_cong "$cong")"
    if [ -z "$pids" ]; then
        echo "  Cổng $cong: TRỐNG"
    else
        echo "  Cổng $cong: VẪN BỊ CHIẾM bởi PID $(echo $pids)"
    fi
done

rm -f run/*.pid 2>/dev/null

if [ "$con_chiem" -ne 0 ]; then
    echo "[LỖI] Còn cổng bị chiếm, hãy kiểm tra thủ công." >&2
    exit 1
fi
echo "Đã dừng toàn bộ service."
