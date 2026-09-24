package com.shopmart.payment.repository;

import com.shopmart.payment.domain.PaymentTransaction;
import com.shopmart.payment.domain.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {

    Optional<PaymentTransaction> findByOrderIdAndType(String orderId, TransactionType type);

    List<PaymentTransaction> findByOrderIdOrderByIdAsc(String orderId);

    List<PaymentTransaction> findAllByOrderByIdAsc();
}
