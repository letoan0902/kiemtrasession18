package com.shopmart.order.exception;

public class InventoryUnavailableException extends RuntimeException {

    private final boolean circuitOpen;

    public InventoryUnavailableException(String message) {
        this(message, false, null);
    }

    public InventoryUnavailableException(String message, boolean circuitOpen, Throwable cause) {
        super(message, cause);
        this.circuitOpen = circuitOpen;
    }

    public boolean isCircuitOpen() {
        return circuitOpen;
    }
}
