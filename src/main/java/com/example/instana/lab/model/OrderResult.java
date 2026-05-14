package com.example.instana.lab.model;

public class OrderResult {

    private String status;
    private String orderId;
    private String customerId;
    private String scenario;
    private String paymentStatus;
    private String inventoryStatus;
    private int queryRows;
    private long queryMillis;
    private long elapsedMillis;
    private boolean traceActive;
    private String traceId;
    private String spanId;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public String getScenario() {
        return scenario;
    }

    public void setScenario(String scenario) {
        this.scenario = scenario;
    }

    public String getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(String paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public String getInventoryStatus() {
        return inventoryStatus;
    }

    public void setInventoryStatus(String inventoryStatus) {
        this.inventoryStatus = inventoryStatus;
    }

    public int getQueryRows() {
        return queryRows;
    }

    public void setQueryRows(int queryRows) {
        this.queryRows = queryRows;
    }

    public long getQueryMillis() {
        return queryMillis;
    }

    public void setQueryMillis(long queryMillis) {
        this.queryMillis = queryMillis;
    }

    public long getElapsedMillis() {
        return elapsedMillis;
    }

    public void setElapsedMillis(long elapsedMillis) {
        this.elapsedMillis = elapsedMillis;
    }

    public boolean isTraceActive() {
        return traceActive;
    }

    public void setTraceActive(boolean traceActive) {
        this.traceActive = traceActive;
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    public String getSpanId() {
        return spanId;
    }

    public void setSpanId(String spanId) {
        this.spanId = spanId;
    }
}
