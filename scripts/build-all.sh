#!/usr/bin/env bash
set -u

GOC="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$GOC" || exit 1

GRADLE="${GRADLE:-/c/Users/gigabyte/.gradle/wrapper/dists/gradle-8.14.3-bin/cv11ve7ro1n3o1j4so8xd9n66/gradle-8.14.3/bin/gradle}"
if [ ! -x "$GRADLE" ] && ! command -v "$GRADLE" >/dev/null 2>&1; then
    echo "[LỖI] Không tìm thấy Gradle tại $GRADLE. Đặt biến môi trường GRADLE để chỉ đường dẫn khác." >&2
    exit 1
fi

echo "== Build sáu module bằng $GRADLE =="
"$GRADLE" :config-server:build :eureka-server:build :api-gateway:build \
          :order-service:build :inventory-service:build :payment-service:build
ma=$?

echo
if [ "$ma" -eq 0 ]; then
    echo "Build thành công. Các tệp jar:"
    for m in config-server eureka-server api-gateway order-service inventory-service payment-service; do
        ls -l "$m/build/libs/$m.jar" 2>/dev/null || echo "  Thiếu $m/build/libs/$m.jar"
    done
else
    echo "[LỖI] Build thất bại với mã $ma" >&2
fi
exit "$ma"
