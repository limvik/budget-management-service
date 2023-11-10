CREATE TABLE IF NOT EXISTS `users` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `username` VARCHAR(20) NOT NULL,
    `email` VARCHAR(255) NOT NULL,
    `password` VARCHAR(255) NOT NULL,
    `minimum_daily_expense` BIGINT NULL DEFAULT 10000,
    `agree_alarm` BOOLEAN NULL DEFAULT 0,
    `create_time` TIMESTAMP NULL DEFAULT now(),
    `refresh_token` VARCHAR(255) NULL,
    PRIMARY KEY (`id`),
    UNIQUE INDEX `username_UNIQUE` (`username` ASC) VISIBLE,
    UNIQUE INDEX `email_UNIQUE` (`email` ASC) VISIBLE,
    UNIQUE INDEX `access_token_UNIQUE` (`refresh_token` ASC) VISIBLE)
    ENGINE = InnoDB
    DEFAULT CHARACTER SET = utf8mb4
    COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `categories` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `name` VARCHAR(32) NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE INDEX `name_UNIQUE` (`name` ASC) VISIBLE)
    ENGINE = InnoDB
    DEFAULT CHARACTER SET = utf8mb4
    COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `budget_plans` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `category_id` BIGINT NOT NULL,
    `date` DATE NOT NULL,
    `amount` BIGINT NOT NULL,
    PRIMARY KEY (`id`),
    INDEX `id_idx` (`user_id` ASC) VISIBLE,
    INDEX `category_id_idx` (`category_id` ASC) VISIBLE,
    CONSTRAINT `user_id_budget_plans`
    FOREIGN KEY (`user_id`)
    REFERENCES `users` (`id`)
    ON DELETE CASCADE
    ON UPDATE CASCADE,
    CONSTRAINT `category_id_budget_plans`
    FOREIGN KEY (`category_id`)
    REFERENCES `categories` (`id`)
    ON DELETE CASCADE
    ON UPDATE CASCADE)
    ENGINE = InnoDB
    DEFAULT CHARACTER SET = utf8mb4
    COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `expenses` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `category_id` BIGINT NOT NULL,
    `datetime` TIMESTAMP NOT NULL,
    `amount` BIGINT NOT NULL,
    `memo` VARCHAR(60) NULL,
    `exclude_in_total` BOOLEAN NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    INDEX `user_id_idx` (`user_id` ASC) VISIBLE,
    INDEX `category_id_idx` (`category_id` ASC) VISIBLE,
    CONSTRAINT `user_id_expenses`
    FOREIGN KEY (`user_id`)
    REFERENCES `users` (`id`)
    ON DELETE CASCADE
    ON UPDATE CASCADE,
    CONSTRAINT `category_id_expenses`
    FOREIGN KEY (`category_id`)
    REFERENCES `categories` (`id`)
    ON DELETE CASCADE
    ON UPDATE CASCADE)
    ENGINE = InnoDB
    DEFAULT CHARACTER SET = utf8mb4
    COLLATE = utf8mb4_unicode_ci;