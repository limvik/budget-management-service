-- MySQL Script generated by MySQL Workbench
-- Fri Nov 10 13:58:12 2023
-- Model: New Model    Version: 1.0
-- MySQL Workbench Forward Engineering

SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0;
SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0;
SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION';

-- -----------------------------------------------------
-- Schema econome
-- -----------------------------------------------------

-- -----------------------------------------------------
-- Schema econome
-- -----------------------------------------------------
CREATE SCHEMA IF NOT EXISTS `econome` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci ;
USE `econome` ;

-- -----------------------------------------------------
-- Table `econome`.`users`
-- -----------------------------------------------------
DROP TABLE IF EXISTS `econome`.`users` ;

CREATE TABLE IF NOT EXISTS `econome`.`users` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `username` VARCHAR(20) NOT NULL,
    `email` VARCHAR(255) NOT NULL,
    `password` VARCHAR(255) NOT NULL,
    `minimum_daily_expense` BIGINT NULL DEFAULT 10000,
    `agree_alarm` TINYINT NULL DEFAULT 0,
    `create_time` TIMESTAMP NULL DEFAULT now(),
    `refresh_token` VARCHAR(255) NULL,
    PRIMARY KEY (`id`),
    UNIQUE INDEX `username_UNIQUE` (`username` ASC) VISIBLE,
    UNIQUE INDEX `email_UNIQUE` (`email` ASC) VISIBLE,
    UNIQUE INDEX `access_token_UNIQUE` (`refresh_token` ASC) VISIBLE)
    ENGINE = InnoDB
    DEFAULT CHARACTER SET = utf8mb4
    COLLATE = utf8mb4_unicode_ci;


-- -----------------------------------------------------
-- Table `econome`.`categories`
-- -----------------------------------------------------
DROP TABLE IF EXISTS `econome`.`categories` ;

CREATE TABLE IF NOT EXISTS `econome`.`categories` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `name` VARCHAR(32) NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE INDEX `name_UNIQUE` (`name` ASC) VISIBLE)
    ENGINE = InnoDB
    DEFAULT CHARACTER SET = utf8mb4
    COLLATE = utf8mb4_unicode_ci;


-- -----------------------------------------------------
-- Table `econome`.`budget_plans`
-- -----------------------------------------------------
DROP TABLE IF EXISTS `econome`.`budget_plans` ;

CREATE TABLE IF NOT EXISTS `econome`.`budget_plans` (
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
    REFERENCES `econome`.`users` (`id`)
    ON DELETE CASCADE
    ON UPDATE CASCADE,
    CONSTRAINT `category_id_budget_plans`
    FOREIGN KEY (`category_id`)
    REFERENCES `econome`.`categories` (`id`)
    ON DELETE CASCADE
    ON UPDATE CASCADE)
    ENGINE = InnoDB
    DEFAULT CHARACTER SET = utf8mb4
    COLLATE = utf8mb4_unicode_ci;


-- -----------------------------------------------------
-- Table `econome`.`expenses`
-- -----------------------------------------------------
DROP TABLE IF EXISTS `econome`.`expenses` ;

CREATE TABLE IF NOT EXISTS `econome`.`expenses` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `category_id` BIGINT NOT NULL,
    `datetime` TIMESTAMP NOT NULL,
    `amount` BIGINT NOT NULL,
    `memo` VARCHAR(60) NULL,
    `exclude_in_total` TINYINT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    INDEX `user_id_idx` (`user_id` ASC) VISIBLE,
    INDEX `category_id_idx` (`category_id` ASC) VISIBLE,
    CONSTRAINT `user_id_expenses`
    FOREIGN KEY (`user_id`)
    REFERENCES `econome`.`users` (`id`)
    ON DELETE CASCADE
    ON UPDATE CASCADE,
    CONSTRAINT `category_id_expenses`
    FOREIGN KEY (`category_id`)
    REFERENCES `econome`.`categories` (`id`)
    ON DELETE CASCADE
    ON UPDATE CASCADE)
    ENGINE = InnoDB
    DEFAULT CHARACTER SET = utf8mb4
    COLLATE = utf8mb4_unicode_ci;


SET SQL_MODE=@OLD_SQL_MODE;
SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS;
SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS;
