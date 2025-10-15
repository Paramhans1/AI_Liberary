package com.ai.library.payment;

public class PaymentResult {
    private boolean success;
    private String provider;
    private String transactionId;
    private String message;

    public PaymentResult() {}

    public PaymentResult(boolean success, String provider, String transactionId, String message) {
        this.success = success;
        this.provider = provider;
        this.transactionId = transactionId;
        this.message = message;
    }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
