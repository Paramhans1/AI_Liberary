package com.ai.library.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for payment integration.
 */
@Component
@ConfigurationProperties(prefix = "payment.stripe")
public class PaymentProperties {
    /**
     * Enables Stripe payment gateway (creates StripePaymentGateway bean).
     */
    private boolean enabled = false;

    /**
     * Enables the webhook controller to receive Stripe webhooks.
     */
    private boolean webhookEnabled = false;

    /**
     * The signing secret for Stripe webhook verification (whsec_...)
     */
    private String webhookSecret = "";
    /**
     * Tolerance window for webhook timestamps in seconds (default 300 = 5 minutes)
     */
    private long webhookToleranceSeconds = 300;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isWebhookEnabled() {
        return webhookEnabled;
    }

    public void setWebhookEnabled(boolean webhookEnabled) {
        this.webhookEnabled = webhookEnabled;
    }

    public String getWebhookSecret() {
        return webhookSecret;
    }

    public void setWebhookSecret(String webhookSecret) {
        this.webhookSecret = webhookSecret;
    }

    public long getWebhookToleranceSeconds() { return webhookToleranceSeconds; }
    public void setWebhookToleranceSeconds(long webhookToleranceSeconds) { this.webhookToleranceSeconds = webhookToleranceSeconds; }
}
