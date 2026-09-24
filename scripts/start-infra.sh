#!/usr/bin/env bash
set -u

GOC="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$GOC" || exit 1

HAN_CHOT_GIAY=180

buoc() { echo; echo "== $* =="; }
loi() { echo "[LỖI] $*" >&2; exit 1; }

buoc "Bước 1/4: docker compose up -d"
docker compose up -d || loi "docker compose up thất bại. Docker Desktop đã chạy chưa?"

buoc "Bước 2/4: chờ Kafka trả lời lệnh kafka-topics --list"
bat_dau=$(date +%s)
until docker exec shopmart-kafka kafka-topics --bootstrap-server localhost:9092 --list >/dev/null 2>&1; do
    da_qua=$(( $(date +%s) - bat_dau ))
    [ "$da_qua" -ge "$HAN_CHOT_GIAY" ] && loi "Kafka chưa sẵn sàng sau ${HAN_CHOT_GIAY} giây"
    echo "  Kafka chưa sẵn sàng (${da_qua} giây), thử lại sau 3 giây ..."
    sleep 3
done
echo "  Kafka đã trả lời."

buoc "Bước 3/4: chờ topic order và payment được tạo (container kafka-init)"
bat_dau=$(date +%s)
while true; do
    danh_sach="$(docker exec shopmart-kafka kafka-topics --bootstrap-server localhost:9092 --list 2>/dev/null | tr -d '\r')"
    co_order=$(echo "$danh_sach" | grep -cx "order")
    co_payment=$(echo "$danh_sach" | grep -cx "payment")
    if [ "$co_order" -ge 1 ] && [ "$co_payment" -ge 1 ]; then
        break
    fi
    da_qua=$(( $(date +%s) - bat_dau ))
    [ "$da_qua" -ge "$HAN_CHOT_GIAY" ] && loi "Chưa thấy đủ topic order và payment sau ${HAN_CHOT_GIAY} giây. Xem: docker logs shopmart-kafka-init"
    echo "  Chưa đủ topic (order=${co_order}, payment=${co_payment}), thử lại sau 3 giây ..."
    sleep 3
done
echo "  Đã có đủ hai topic:"
docker exec shopmart-kafka kafka-topics --bootstrap-server localhost:9092 --describe --topic order 2>/dev/null | head -n 1
docker exec shopmart-kafka kafka-topics --bootstrap-server localhost:9092 --describe --topic payment 2>/dev/null | head -n 1

buoc "Bước 4/4: chờ Redis trả PONG"
bat_dau=$(date +%s)
while true; do
    tra_loi="$(docker exec shopmart-redis redis-cli ping 2>/dev/null | tr -d '\r')"
    [ "$tra_loi" = "PONG" ] && break
    da_qua=$(( $(date +%s) - bat_dau ))
    [ "$da_qua" -ge 60 ] && loi "Redis chưa trả PONG sau 60 giây"
    echo "  Redis chưa sẵn sàng, thử lại sau 2 giây ..."
    sleep 2
done
echo "  Redis trả lời: PONG"

buoc "Hạ tầng đã sẵn sàng"
docker compose ps
