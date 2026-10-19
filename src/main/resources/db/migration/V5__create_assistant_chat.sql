-- One row per message in an assistant conversation.
-- conversation_id groups assistant turns; it is not a FK to the direct-message conversation table.
CREATE TABLE `assistant_chat` (
    `id`              CHAR(36)    NOT NULL,
    `user_id`         CHAR(36)    NOT NULL,
    `conversation_id` CHAR(36)    NULL,
    `role`            VARCHAR(20) NOT NULL,
    `content`         TEXT        NOT NULL,
    `provider`        VARCHAR(50) NULL,
    `created_at`      TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_assistant_chat_user_created_at` (`user_id`, `created_at`),
    KEY `idx_assistant_chat_conversation_id` (`conversation_id`),
    CONSTRAINT `fk_assistant_chat_user`
        FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT `chk_assistant_chat_role` CHECK (`role` IN ('USER', 'ASSISTANT', 'SYSTEM'))
) ENGINE=InnoDB;
