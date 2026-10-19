-- Notifications
CREATE TABLE `email_template` (
    `id`               CHAR(36)     NOT NULL,
    `name`             VARCHAR(100) NOT NULL,
    `subject_template` VARCHAR(255) NOT NULL,
    `html_content`     TEXT         NOT NULL,
    `enabled`          BOOLEAN      NOT NULL DEFAULT TRUE,
    `created_at`       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uq_email_template_name` (`name`)
) ENGINE=InnoDB;

CREATE TABLE `email_log` (
    `id`            CHAR(36)     NOT NULL,
    `template_id`   CHAR(36)     NULL,
    `recipient_to`  JSON         NOT NULL,
    `recipient_cc`  JSON         NULL,
    `recipient_bcc` JSON         NULL,
    `subject`       VARCHAR(255) NOT NULL,
    `status`        VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    `error_message` TEXT         NULL,
    `created_at`    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `sent_at`       TIMESTAMP    NULL DEFAULT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_email_log_template_id` (`template_id`),
    KEY `idx_email_log_status_created_at` (`status`, `created_at`),
    CONSTRAINT `fk_email_log_template`
        FOREIGN KEY (`template_id`) REFERENCES `email_template` (`id`)
        ON DELETE SET NULL ON UPDATE CASCADE,
    CONSTRAINT `chk_email_log_status` CHECK (`status` IN ('PENDING', 'SENT', 'FAILED'))
) ENGINE=InnoDB;

CREATE TABLE `audit_log` (
    `id`            CHAR(36)     NOT NULL,
    `event_id`      CHAR(36)     NOT NULL,
    `actor_user_id` CHAR(36)     NULL,
    `action`        VARCHAR(100) NOT NULL,
    `entity_type`   VARCHAR(100) NOT NULL,
    `entity_id`     VARCHAR(100) NOT NULL,
    `old_values`    JSON         NULL,
    `new_values`    JSON         NULL,
    `metadata`      JSON         NULL,
    `created_at`    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uq_audit_log_event_id` (`event_id`),
    KEY `idx_audit_log_actor_created_at` (`actor_user_id`, `created_at`),
    KEY `idx_audit_log_entity_created_at` (`entity_type`, `entity_id`, `created_at`)
) ENGINE=InnoDB;
