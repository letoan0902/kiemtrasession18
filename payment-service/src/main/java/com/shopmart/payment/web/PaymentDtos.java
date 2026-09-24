package com.shopmart.payment.web;

import com.shopmart.payment.domain.CustomerWallet;
import com.shopmart.payment.domain.PaymentTransaction;
import com.shopmart.payment.service.ChaosService;

import java.time.Instant;

public final class PaymentDtos {

    private PaymentDtos() {
    }

    public record WalletResponse(String customerId, String customerName, Long balance) {
        public static WalletResponse from(CustomerWallet w) {
            return new WalletResponse(w.getCustomerId(), w.getCustomerName(), w.getBalance());
        }
    }

    public record TransactionResponse(Long id, String orderId, String customerId, Long amount, String type,
                                      String status, String reason, Instant createdAt, Instant updatedAt) {
        public static TransactionResponse from(PaymentTransaction t) {
            return new TransactionResponse(t.getId(), t.getOrderId(), t.getCustomerId(), t.getAmount(),
                    t.getType().name(), t.getStatus().name(), t.getReason(), t.getCreatedAt(), t.getUpdatedAt());
        }
    }

    public record ChaosResponse(String mode, int delaySeconds, String message) {
        public static ChaosResponse from(ChaosService.ChaosState s) {
            String message = switch (s.mode()) {
                case NORMAL -> "Thanh toán hoạt động bình thường";
                case FAIL -> "Mọi lần trừ tiền sẽ thất bại";
                case SLOW -> "Trễ " + s.delaySeconds() + " giây (không chặn) rồi mới trừ tiền";
            };
            return new ChaosResponse(s.mode().apiName(), s.delaySeconds(), message);
        }
    }

    public record HealthResponse(String status, String configSource) {
    }

    public record ErrorResponse(Instant timestamp, int status, String error, String message, String path) {
    }
}
