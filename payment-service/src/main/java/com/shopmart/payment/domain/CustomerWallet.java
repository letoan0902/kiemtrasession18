package com.shopmart.payment.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "customer_wallets")
public class CustomerWallet {

    @Id
    @Column(name = "customer_id", length = 64)
    private String customerId;

    @Column(name = "customer_name", nullable = false, length = 200)
    private String customerName;

    @Column(name = "balance", nullable = false)
    private Long balance;

    protected CustomerWallet() {
    }

    public CustomerWallet(String customerId, String customerName, Long balance) {
        this.customerId = customerId;
        this.customerName = customerName;
        this.balance = balance;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public Long getBalance() {
        return balance;
    }

    public void setBalance(Long balance) {
        this.balance = balance;
    }
}
