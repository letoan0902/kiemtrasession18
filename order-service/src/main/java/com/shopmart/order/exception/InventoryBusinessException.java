package com.shopmart.order.exception;

public abstract class InventoryBusinessException extends RuntimeException {

    protected InventoryBusinessException(String message) {
        super(message);
    }
}
