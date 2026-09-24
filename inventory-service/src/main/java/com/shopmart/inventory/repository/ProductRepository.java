package com.shopmart.inventory.repository;

import com.shopmart.inventory.domain.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Product p set p.stock = p.stock - :quantity, p.updatedAt = :now "
            + "where p.id = :id and p.stock >= :quantity")
    int deductStockAtomically(@Param("id") Long id,
                              @Param("quantity") int quantity,
                              @Param("now") LocalDateTime now);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Product p set p.stock = p.stock + :quantity, p.updatedAt = :now where p.id = :id")
    int addStockAtomically(@Param("id") Long id,
                           @Param("quantity") int quantity,
                           @Param("now") LocalDateTime now);

    @Query("select p.stock from Product p where p.id = :id")
    Optional<Integer> findStockById(@Param("id") Long id);
}
