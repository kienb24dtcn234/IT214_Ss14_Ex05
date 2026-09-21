package com.storex.combo.model;

// Yêu cầu đặt combo. Trường "scenario" chỉ dùng để DEMO ép lỗi ở bước nào.
public class ComboRequest {
    private String orderId;
    private String userId;
    private double amount;

    // SUCCESS | HOTEL_FAIL | PAYMENT_FAIL | HOTEL_TIMEOUT
    private String scenario = "SUCCESS";

    public ComboRequest() {
    }

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public String getScenario() { return scenario; }
    public void setScenario(String scenario) { this.scenario = scenario; }
}
