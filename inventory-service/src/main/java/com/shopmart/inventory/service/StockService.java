package com.shopmart.inventory.service;

import com.shopmart.inventory.config.CacheConfig;
import com.shopmart.inventory.domain.ReservationStatus;
import com.shopmart.inventory.domain.StockReservation;
import com.shopmart.inventory.exception.InsufficientStockException;
import com.shopmart.inventory.exception.ProductNotFoundException;
import com.shopmart.inventory.exception.ReservationMismatchException;
import com.shopmart.inventory.exception.ReservationNotFoundException;
import com.shopmart.inventory.repository.ProductRepository;
import com.shopmart.inventory.repository.StockReservationRepository;
import com.shopmart.inventory.support.InstanceIdentity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class StockService {

    private static final Logger log = LoggerFactory.getLogger(StockService.class);

    private final ProductRepository productRepository;
    private final StockReservationRepository reservationRepository;
    private final InstanceIdentity instance;

    public StockService(ProductRepository productRepository,
                        StockReservationRepository reservationRepository,
                        InstanceIdentity instance) {
        this.productRepository = productRepository;
        this.reservationRepository = reservationRepository;
        this.instance = instance;
    }

    @Transactional
    @CacheEvict(cacheNames = CacheConfig.PRODUCTS_CACHE, key = "#productId")
    public DeductResult deduct(Long productId, String orderId, int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Số lượng trừ kho phải lớn hơn 0");
        }
        LocalDateTime now = LocalDateTime.now();

        int inserted = reservationRepository.insertIfAbsent(orderId, productId, quantity, now);
        if (inserted == 0) {
            StockReservation existing = reservationRepository.findByOrderId(orderId)
                    .orElseThrow(() -> new IllegalStateException(
                            "Ràng buộc duy nhất báo trùng nhưng không đọc được bản ghi trừ kho của đơn " + orderId));
            int remaining = productRepository.findStockById(existing.getProductId()).orElse(0);
            log.info("[SAGA][{}] bỏ qua trừ kho vì đơn đã được xử lý trước đó (sản phẩm {}, số lượng {}, "
                            + "trạng thái {}), tồn kho hiện tại {}, bản {}",
                    orderId, existing.getProductId(), existing.getQuantity(), existing.getStatus(),
                    remaining, instance.servedBy());
            return new DeductResult(orderId, existing.getProductId(), existing.getQuantity(), remaining, true);
        }

        int updated = productRepository.deductStockAtomically(productId, quantity, now);
        if (updated == 0) {
            Optional<Integer> current = productRepository.findStockById(productId);
            if (current.isEmpty()) {
                log.error("[SAGA][{}] trừ kho thất bại: không có sản phẩm {}, bản {}",
                        orderId, productId, instance.servedBy());
                throw new ProductNotFoundException(productId);
            }
            log.error("[SAGA][{}] trừ kho thất bại: sản phẩm {} cần {} nhưng chỉ còn {}, quay lui giao dịch, bản {}",
                    orderId, productId, quantity, current.get(), instance.servedBy());
            throw new InsufficientStockException(productId, quantity, current.get());
        }

        int remaining = productRepository.findStockById(productId).orElse(0);
        log.info("[SAGA][{}] trừ kho thành công sản phẩm {} số lượng {}, tồn kho còn {}, bản {}",
                orderId, productId, quantity, remaining, instance.servedBy());
        return new DeductResult(orderId, productId, quantity, remaining, false);
    }

    @Transactional
    @CacheEvict(cacheNames = CacheConfig.PRODUCTS_CACHE, key = "#result.productId()",
            condition = "#result != null && #result.productId() != null")
    public RestoreResult restore(String orderId, Long expectedProductId, String trigger) {
        Optional<StockReservation> found = reservationRepository.findByOrderId(orderId);
        if (found.isEmpty()) {
            String reason = "Không có bản ghi trừ kho của đơn, không có gì để hoàn";
            log.info("[SAGA][{}] bỏ qua hoàn kho ({}): không có bản ghi trừ kho, bản {}",
                    orderId, trigger, instance.servedBy());
            return new RestoreResult(orderId, null, null, null, false, reason);
        }

        StockReservation reservation = found.get();
        Long productId = reservation.getProductId();
        if (expectedProductId != null && !expectedProductId.equals(productId)) {
            throw new ReservationMismatchException(orderId, productId, expectedProductId);
        }

        if (reservation.getStatus() == ReservationStatus.RESTORED) {
            int current = productRepository.findStockById(productId).orElse(0);
            log.info("[SAGA][{}] bỏ qua vì đã hoàn trước đó ({}), sản phẩm {} tồn kho {}, bản {}",
                    orderId, trigger, productId, current, instance.servedBy());
            return new RestoreResult(orderId, productId, reservation.getQuantity(), current, false,
                    "Đơn đã được hoàn kho trước đó");
        }

        LocalDateTime now = LocalDateTime.now();
        int transitioned = reservationRepository.transitionStatus(orderId,
                ReservationStatus.DEDUCTED, ReservationStatus.RESTORED, now);
        if (transitioned == 0) {
            int current = productRepository.findStockById(productId).orElse(0);
            log.info("[SAGA][{}] bỏ qua vì đã hoàn trước đó bởi luồng khác ({}), sản phẩm {} tồn kho {}, bản {}",
                    orderId, trigger, productId, current, instance.servedBy());
            return new RestoreResult(orderId, productId, reservation.getQuantity(), current, false,
                    "Đơn đã được hoàn kho trước đó");
        }

        int added = productRepository.addStockAtomically(productId, reservation.getQuantity(), now);
        if (added == 0) {
            throw new ProductNotFoundException(productId);
        }
        int remaining = productRepository.findStockById(productId).orElse(0);
        log.info("[SAGA][{}] đã hoàn {} sản phẩm (mã {}) vào kho, tồn kho còn {} ({}), bản {}",
                orderId, reservation.getQuantity(), productId, remaining, trigger, instance.servedBy());
        return new RestoreResult(orderId, productId, reservation.getQuantity(), remaining, true,
                "Đã hoàn kho");
    }

    @Transactional(readOnly = true)
    public StockReservation getReservation(String orderId) {
        return reservationRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ReservationNotFoundException(orderId));
    }
}
