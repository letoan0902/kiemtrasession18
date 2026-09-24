package com.shopmart.payment.service;

import com.shopmart.payment.domain.CustomerWallet;
import com.shopmart.payment.domain.PaymentTransaction;
import com.shopmart.payment.domain.TransactionStatus;
import com.shopmart.payment.domain.TransactionType;
import com.shopmart.payment.event.OrderEvent;
import com.shopmart.payment.event.PaymentEvent;
import com.shopmart.payment.repository.CustomerWalletRepository;
import com.shopmart.payment.repository.PaymentTransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Optional;

@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final CustomerWalletRepository walletRepository;
    private final PaymentTransactionRepository transactionRepository;
    private final TransactionTemplate transactionTemplate;

    public PaymentService(CustomerWalletRepository walletRepository,
                          PaymentTransactionRepository transactionRepository,
                          PlatformTransactionManager transactionManager) {
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    public SagaOutcome charge(OrderEvent event, boolean chaosFail) {
        try {
            return transactionTemplate.execute(status -> doCharge(event, chaosFail));
        } catch (DataIntegrityViolationException race) {
            log.info("[SAGA][{}] Trùng khóa (order_id, CHARGE) khi chèn: đây là sự kiện lặp, KHÔNG trừ tiền lần hai",
                    event.orderId());
            return transactionTemplate.execute(status -> duplicateCharge(event,
                    transactionRepository.findByOrderIdAndType(event.orderId(), TransactionType.CHARGE).orElseThrow()));
        }
    }

    private SagaOutcome doCharge(OrderEvent event, boolean chaosFail) {
        String orderId = event.orderId();

        Optional<PaymentTransaction> existing = transactionRepository.findByOrderIdAndType(orderId, TransactionType.CHARGE);
        if (existing.isPresent()) {
            return duplicateCharge(event, existing.get());
        }

        long amount = event.totalAmount() == null ? 0L : event.totalAmount();

        PaymentTransaction charge = transactionRepository.saveAndFlush(
                PaymentTransaction.processing(orderId, event.customerId(), amount, TransactionType.CHARGE));
        log.info("[SAGA][{}] Đã ghi bản ghi CHARGE id={} trạng thái PROCESSING, số tiền {} đồng",
                orderId, charge.getId(), amount);

        String failure = precondition(event, amount, chaosFail);
        if (failure == null) {
            int rows = walletRepository.debitIfSufficient(event.customerId(), amount);
            if (rows != 1) {
                failure = explainDebitFailure(event.customerId(), amount);
            }
        }

        if (failure != null) {
            charge.markFailed(failure);
            transactionRepository.save(charge);
            log.error("[SAGA][{}] Trừ tiền THẤT BẠI cho khách {}: {} (luồng {})", orderId, event.customerId(), failure,
                    Thread.currentThread().getName());
            return new SagaOutcome(SagaOutcome.Kind.CHARGE_FAILED, PaymentEvent.of(PaymentEvent.PAYMENT_FAILED,
                    orderId, event.customerId(), event.productId(), event.quantity(), amount, failure));
        }

        charge.markCompleted(null);
        transactionRepository.save(charge);
        long balanceAfter = walletRepository.findById(event.customerId()).map(CustomerWallet::getBalance).orElse(-1L);
        log.info("[SAGA][{}] Trừ tiền THÀNH CÔNG {} đồng từ ví {}, số dư còn {} đồng (luồng {})",
                orderId, amount, event.customerId(), balanceAfter, Thread.currentThread().getName());
        return new SagaOutcome(SagaOutcome.Kind.CHARGE_COMPLETED, PaymentEvent.of(PaymentEvent.PAYMENT_COMPLETED,
                orderId, event.customerId(), event.productId(), event.quantity(), amount, null));
    }

    private String precondition(OrderEvent event, long amount, boolean chaosFail) {
        if (event.customerId() == null || event.customerId().isBlank()) {
            return "Sự kiện thiếu mã khách hàng";
        }
        if (amount <= 0) {
            return "Số tiền cần thanh toán không hợp lệ: " + amount;
        }
        if (chaosFail) {
            return "Chế độ hỗn loạn 'fail' đang bật: giả lập cổng thanh toán từ chối giao dịch";
        }
        if (event.wantsSimulatedFailure()) {
            return "Khách hàng yêu cầu giả lập thanh toán thất bại (simulatePaymentFailure = true)";
        }
        return null;
    }

    private String explainDebitFailure(String customerId, long amount) {
        return walletRepository.findById(customerId)
                .map(w -> "Số dư không đủ: cần " + amount + " đồng, ví " + customerId + " chỉ còn "
                        + w.getBalance() + " đồng")
                .orElse("Không tìm thấy ví của khách hàng " + customerId);
    }

    private SagaOutcome duplicateCharge(OrderEvent event, PaymentTransaction charge) {
        String orderId = event.orderId();
        if (transactionRepository.findByOrderIdAndType(orderId, TransactionType.REFUND).isPresent()) {
            log.info("[SAGA][{}] Bỏ qua ORDER_CREATED lặp: đơn đã được hoàn tiền, KHÔNG trừ tiền lần hai và không phát lại",
                    orderId);
            return new SagaOutcome(SagaOutcome.Kind.CHARGE_DUPLICATE, null);
        }
        PaymentEvent replay = switch (charge.getStatus()) {
            case COMPLETED -> PaymentEvent.of(PaymentEvent.PAYMENT_COMPLETED, orderId, charge.getCustomerId(),
                    event.productId(), event.quantity(), charge.getAmount(), null);
            case FAILED -> PaymentEvent.of(PaymentEvent.PAYMENT_FAILED, orderId, charge.getCustomerId(),
                    event.productId(), event.quantity(), charge.getAmount(), charge.getReason());
            case PROCESSING -> null;
        };
        log.info("[SAGA][{}] Bỏ qua ORDER_CREATED lặp: CHARGE id={} đã ở trạng thái {}, KHÔNG trừ tiền lần hai{}",
                orderId, charge.getId(), charge.getStatus(), replay == null ? "" : ", phát lại " + replay.eventType());
        return new SagaOutcome(SagaOutcome.Kind.CHARGE_DUPLICATE, replay);
    }

    public SagaOutcome refund(OrderEvent event) {
        try {
            return transactionTemplate.execute(status -> doRefund(event));
        } catch (DataIntegrityViolationException race) {
            log.info("[SAGA][{}] Trùng khóa (order_id, REFUND) khi chèn: đây là sự kiện lặp, KHÔNG hoàn tiền lần hai",
                    event.orderId());
            return transactionTemplate.execute(status -> duplicateRefund(event,
                    transactionRepository.findByOrderIdAndType(event.orderId(), TransactionType.REFUND).orElseThrow()));
        }
    }

    private SagaOutcome doRefund(OrderEvent event) {
        String orderId = event.orderId();

        Optional<PaymentTransaction> existingRefund = transactionRepository.findByOrderIdAndType(orderId, TransactionType.REFUND);
        if (existingRefund.isPresent()) {
            return duplicateRefund(event, existingRefund.get());
        }

        Optional<PaymentTransaction> charge = transactionRepository.findByOrderIdAndType(orderId, TransactionType.CHARGE);
        if (charge.isEmpty() || charge.get().getStatus() != TransactionStatus.COMPLETED) {
            String why = charge.isEmpty() ? "chưa từng trừ tiền đơn này"
                    : "lần trừ tiền ở trạng thái " + charge.get().getStatus() + ", không có đồng nào bị trừ";
            log.info("[SAGA][{}] Nhận ORDER_CANCELLED nhưng không có gì để hoàn: {}", orderId, why);
            return new SagaOutcome(SagaOutcome.Kind.REFUND_NOTHING, null);
        }

        PaymentTransaction chargeTx = charge.get();
        String customerId = chargeTx.getCustomerId();
        long amount = chargeTx.getAmount();

        PaymentTransaction refund = transactionRepository.saveAndFlush(
                PaymentTransaction.processing(orderId, customerId, amount, TransactionType.REFUND));

        int rows = walletRepository.credit(customerId, amount);
        if (rows != 1) {
            throw new IllegalStateException("Không tìm thấy ví " + customerId + " để hoàn tiền đơn " + orderId);
        }

        String reason = "Hoàn tiền do đơn bị hủy" + (event.reason() == null ? "" : ": " + event.reason());
        refund.markCompleted(reason);
        transactionRepository.save(refund);
        long balanceAfter = walletRepository.findById(customerId).map(CustomerWallet::getBalance).orElse(-1L);
        log.info("[SAGA][{}] HOÀN TIỀN thành công {} đồng vào ví {}, số dư sau hoàn {} đồng; "
                        + "bản ghi CHARGE id={} được giữ nguyên để kiểm toán (luồng {})",
                orderId, amount, customerId, balanceAfter, chargeTx.getId(), Thread.currentThread().getName());
        return new SagaOutcome(SagaOutcome.Kind.REFUND_COMPLETED, PaymentEvent.of(PaymentEvent.PAYMENT_REFUNDED,
                orderId, customerId, event.productId(), event.quantity(), amount, reason));
    }

    private SagaOutcome duplicateRefund(OrderEvent event, PaymentTransaction refund) {
        PaymentEvent replay = refund.getStatus() == TransactionStatus.COMPLETED
                ? PaymentEvent.of(PaymentEvent.PAYMENT_REFUNDED, event.orderId(), refund.getCustomerId(),
                event.productId(), event.quantity(), refund.getAmount(), refund.getReason())
                : null;
        log.info("[SAGA][{}] Bỏ qua ORDER_CANCELLED lặp: đã hoàn tiền trước đó (REFUND id={}), KHÔNG hoàn lần hai",
                event.orderId(), refund.getId());
        return new SagaOutcome(SagaOutcome.Kind.REFUND_DUPLICATE, replay);
    }
}
