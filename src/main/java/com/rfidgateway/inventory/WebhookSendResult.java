package com.rfidgateway.inventory;

import java.util.LinkedHashMap;
import java.util.Map;

public class WebhookSendResult {

    private final boolean success;
    private final int httpStatus;
    private final String url;
    private final int attempts;
    private final String detail;

    private WebhookSendResult(boolean success, int httpStatus, String url, int attempts, String detail) {
        this.success = success;
        this.httpStatus = httpStatus;
        this.url = url;
        this.attempts = attempts;
        this.detail = detail;
    }

    public static WebhookSendResult ok(String url, int httpStatus, int attempts, String detail) {
        return new WebhookSendResult(true, httpStatus, url, attempts, detail);
    }

    public static WebhookSendResult fail(String url, int httpStatus, int attempts, String detail) {
        return new WebhookSendResult(false, httpStatus, url, attempts, detail);
    }

    public boolean isSuccess() {
        return success;
    }

    public int getHttpStatus() {
        return httpStatus;
    }

    public String getUrl() {
        return url;
    }

    public int getAttempts() {
        return attempts;
    }

    public String getDetail() {
        return detail;
    }

    public String toUserMessage() {
        if (success) {
            String msg = "POST OK → " + url + " (HTTP " + httpStatus + ", intento " + attempts + ")";
            if (detail != null && !detail.isBlank()) {
                msg += ". Respuesta: " + detail;
            }
            return msg;
        }
        String msg = "POST falló → " + url;
        if (httpStatus > 0) {
            msg += " (HTTP " + httpStatus + ")";
        }
        if (attempts > 0) {
            msg += ", tras " + attempts + " intento(s)";
        }
        if (detail != null && !detail.isBlank()) {
            msg += ": " + detail;
        }
        return msg;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("success", success);
        m.put("httpStatus", httpStatus);
        m.put("url", url);
        m.put("attempts", attempts);
        m.put("detail", detail);
        m.put("message", toUserMessage());
        return m;
    }
}
