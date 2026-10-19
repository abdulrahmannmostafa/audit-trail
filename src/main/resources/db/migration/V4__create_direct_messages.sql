-- Direct messages
CREATE TABLE `conversation` (
    `id`         CHAR(36)    NOT NULL,
    `type`       VARCHAR(20) NOT NULL DEFAULT 'DIRECT',
    `created_at` TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    CONSTRAINT `chk_conversation_type` CHECK (`type` IN ('DIRECT'))
) ENGINE=InnoDB;

CREATE TABLE `conversation_participant` (
    `id`              BIGINT    NOT NULL AUTO_INCREMENT,
    `conversation_id` CHAR(36)  NOT NULL,
    `user_id`         CHAR(36)  NOT NULL,
    `joined_at`       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `last_read_at`    TIMESTAMP NULL DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uq_conversation_participant_conversation_user` (`conversation_id`, `user_id`),
    KEY `idx_conversation_participant_user_id` (`user_id`),
    CONSTRAINT `fk_conversation_participant_conversation`
        FOREIGN KEY (`conversation_id`) REFERENCES `conversation` (`id`)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT `fk_conversation_participant_user`
        FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB;

CREATE TABLE `message` (
    `id`              CHAR(36)  NOT NULL,
    `conversation_id` CHAR(36)  NOT NULL,
    `sender_user_id`  CHAR(36)  NOT NULL,
    `content`         TEXT      NOT NULL,
    `created_at`      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `edited_at`       TIMESTAMP NULL DEFAULT NULL,
    `deleted_at`      TIMESTAMP NULL DEFAULT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_message_conversation_created_at` (`conversation_id`, `created_at`),
    KEY `idx_message_sender_user_id` (`sender_user_id`),
    CONSTRAINT `fk_message_conversation`
        FOREIGN KEY (`conversation_id`) REFERENCES `conversation` (`id`)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT `fk_message_sender`
        FOREIGN KEY (`sender_user_id`) REFERENCES `user` (`id`)
        ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB;
