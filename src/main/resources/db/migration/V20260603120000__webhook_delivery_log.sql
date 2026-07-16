CREATE TABLE webhook_delivery_log (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    system_id       UUID NOT NULL,
    event_id        UUID,
    event_type      VARCHAR(32) NOT NULL,
    status          VARCHAR(20) NOT NULL,
    attempt         INT NOT NULL DEFAULT 1,
    http_status     INT,
    target_url      TEXT,
    payload_json    TEXT NOT NULL,
    error_message   TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_webhook_delivery_system_time ON webhook_delivery_log (system_id, created_at DESC);
