package com.shopmart.payment.repository;

import com.shopmart.payment.domain.CustomerWallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CustomerWalletRepository extends JpaRepository<CustomerWallet, String> {

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = "UPDATE customer_wallets SET balance = balance - :a WHERE customer_id = :c AND balance >= :a",
            nativeQuery = true)
    int debitIfSufficient(@Param("c") String customerId, @Param("a") long amount);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = "UPDATE customer_wallets SET balance = balance + :a WHERE customer_id = :c",
            nativeQuery = true)
    int credit(@Param("c") String customerId, @Param("a") long amount);
}
