CREATE TABLE admin_audit_log (
    id             BIGSERIAL PRIMARY KEY,
    admin_email    VARCHAR(255) NOT NULL,
    action         VARCHAR(50)  NOT NULL,
    target_user_id BIGINT,
    target_email   VARCHAR(255),
    details        VARCHAR(512),
    timestamp      TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_audit_log_timestamp   ON admin_audit_log (timestamp DESC);
CREATE INDEX idx_audit_log_admin_email ON admin_audit_log (admin_email);
