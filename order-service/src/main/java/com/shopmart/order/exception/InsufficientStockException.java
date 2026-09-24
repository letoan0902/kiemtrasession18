package com.shopmart.order.exception;

public class InsufficientStockException extends InventoryBusinessException {

    public InsufficientStockException(String message) {
        super(message);
    }
}
