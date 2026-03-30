package com.notificationsystem.notificationservice.dto;

public class NotificationEvent {
    private String orderId;
    private String emailAddress;
    private String message;

    public NotificationEvent() {
    }

    public NotificationEvent(String orderId, String emailAddress, String message) {
        this.orderId = orderId;
        this.emailAddress = emailAddress;
        this.message = message;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getEmailAddress() {
        return emailAddress;
    }

    public void setEmailAddress(String emailAddress) {
        this.emailAddress = emailAddress;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
