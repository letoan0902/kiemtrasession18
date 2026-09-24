package com.shopmart.inventory.exception;

public class ReservationNotFoundException extends RuntimeException {

    public ReservationNotFoundException(String orderId) {
        super("Không có bản ghi trừ kho của đơn " + orderId);
    }
}
