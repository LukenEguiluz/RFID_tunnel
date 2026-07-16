package com.rfidgateway.inventory;

import java.util.LinkedHashMap;
import java.util.Map;

public class WebhookManualDispatchResult {

    private final Map<String, Object> payload;
    private final WebhookSendResult webhook;

    public WebhookManualDispatchResult(Map<String, Object> payload, WebhookSendResult webhook) {
        this.payload = payload;
        this.webhook = webhook;
    }

    public Map<String, Object> getPayload() {
        return payload;
    }

    public WebhookSendResult getWebhook() {
        return webhook;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("payload", payload);
        m.put("webhook", webhook.toMap());
        m.put("ok", webhook.isSuccess());
        return m;
    }
}
