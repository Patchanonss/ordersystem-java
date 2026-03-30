package com.notificationsystem.notificationservice.dto;

public class InventoryResultEvent {
    private String orderId;
    private boolean successful;

    public InventoryResultEvent() {
    }

    public InventoryResultEvent(String orderId, boolean successful) {
        this.orderId = orderId;
        this.successful = successful;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public boolean isSuccessful() {
        return successful;
    }

    public void setSuccessful(boolean successful) {
        this.successful = successful;
    }
}
