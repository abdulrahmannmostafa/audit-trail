-- Identity and application roles
CREATE TABLE `user` (
    `id`           CHAR(36)     NOT NULL,
    `email`        VARCHAR(255) NOT NULL,
    `display_name` VARCHAR(120) NOT NULL,
    `avatar_url`   VARCHAR(500) NULL,
    `status`       VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    `created_at`   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uq_user_email` (`email`),
    CONSTRAINT `chk_user_status` CHECK (`status` IN ('ACTIVE', 'DISABLED'))
) ENGINE=InnoDB;

CREATE TABLE `user_identity` (
    `id`         CHAR(36)     NOT NULL,
    `user_id`    CHAR(36)     NOT NULL,
    `provider`   VARCHAR(50)  NOT NULL,
    `issuer`     VARCHAR(500) NOT NULL,
    `subject`    VARCHAR(255) NOT NULL,
    `created_at` TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uq_user_identity_issuer_subject` (`issuer`, `subject`),
    KEY `idx_user_identity_user_id` (`user_id`),
    CONSTRAINT `fk_user_identity_user`
        FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB;

CREATE TABLE `app_role` (
    `id`   INT UNSIGNED NOT NULL AUTO_INCREMENT,
    `name` VARCHAR(50)  NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uq_app_role_name` (`name`)
) ENGINE=InnoDB;

CREATE TABLE `user_role` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT,
    `user_id`     CHAR(36)     NOT NULL,
    `role_id`     INT UNSIGNED NOT NULL,
    `assigned_at` TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uq_user_role_user_role` (`user_id`, `role_id`),
    KEY `idx_user_role_role_id` (`role_id`),
    CONSTRAINT `fk_user_role_user`
        FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT `fk_user_role_role`
        FOREIGN KEY (`role_id`) REFERENCES `app_role` (`id`)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB;
