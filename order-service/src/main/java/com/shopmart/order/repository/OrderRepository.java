package com.shopmart.order.repository;

import com.shopmart.order.domain.OrderEntity;
import com.shopmart.order.domain.OrderStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderRepository extends JpaRepository<OrderEntity, Long> {

    Optional<OrderEntity> findByOrderId(String orderId);

    List<OrderEntity> findAllByOrderByCreatedAtDesc();

    List<OrderEntity> findTop100ByStatusAndReservedAtBeforeOrderByReservedAtAsc(OrderStatus status,
                                                                               Instant deadline);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update OrderEntity o set o.status = :to, o.updatedAt = :now "
            + "where o.orderId = :orderId and o.status = :from")
    int transition(@Param("orderId") String orderId,
                   @Param("from") OrderStatus from,
                   @Param("to") OrderStatus to,
                   @Param("now") Instant now);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update OrderEntity o set o.status = :to, o.failureReason = :reason, o.updatedAt = :now "
            + "where o.orderId = :orderId and o.status = :from")
    int transitionWithReason(@Param("orderId") String orderId,
                             @Param("from") OrderStatus from,
                             @Param("to") OrderStatus to,
                             @Param("reason") String reason,
                             @Param("now") Instant now);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update OrderEntity o set o.status = com.shopmart.order.domain.OrderStatus.INVENTORY_RESERVED, "
            + "o.inventoryInstance = :instance, o.reservedAt = :now, o.updatedAt = :now "
            + "where o.orderId = :orderId and o.status = com.shopmart.order.domain.OrderStatus.PENDING")
    int markReserved(@Param("orderId") String orderId,
                     @Param("instance") String instance,
                     @Param("now") Instant now);
}
