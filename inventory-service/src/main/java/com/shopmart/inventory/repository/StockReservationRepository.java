package com.shopmart.inventory.repository;

import com.shopmart.inventory.domain.ReservationStatus;
import com.shopmart.inventory.domain.StockReservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface StockReservationRepository extends JpaRepository<StockReservation, Long> {

    Optional<StockReservation> findByOrderId(String orderId);

    @Modifying(flushAutomatically = true)
    @Query(value = "INSERT IGNORE INTO stock_reservations "
            + "(order_id, product_id, quantity, status, created_at, updated_at) "
            + "VALUES (:orderId, :productId, :quantity, 'DEDUCTED', :now, :now)",
            nativeQuery = true)
    int insertIfAbsent(@Param("orderId") String orderId,
                       @Param("productId") Long productId,
                       @Param("quantity") int quantity,
                       @Param("now") LocalDateTime now);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update StockReservation r set r.status = :to, r.updatedAt = :now "
            + "where r.orderId = :orderId and r.status = :from")
    int transitionStatus(@Param("orderId") String orderId,
                         @Param("from") ReservationStatus from,
                         @Param("to") ReservationStatus to,
                         @Param("now") LocalDateTime now);
}
