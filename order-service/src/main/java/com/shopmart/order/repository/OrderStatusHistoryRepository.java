package com.shopmart.order.repository;

import com.shopmart.order.domain.OrderStatusHistory;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderStatusHistoryRepository extends JpaRepository<OrderStatusHistory, Long> {

    List<OrderStatusHistory> findByOrderIdOrderByIdAsc(String orderId);

    boolean existsByOrderIdAndNote(String orderId, String note);
}
