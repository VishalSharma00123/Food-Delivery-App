-- User Service schema (optional manual DDL; Hibernate ddl-auto=update creates tables in dev)

CREATE DATABASE IF NOT EXISTS user_db;
USE user_db;

CREATE TABLE IF NOT EXISTS user_profiles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    auth_user_id BIGINT NOT NULL UNIQUE,
    display_name VARCHAR(160) NOT NULL,
    phone VARCHAR(32),
    contact_email VARCHAR(255),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS delivery_addresses (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_profile_id BIGINT NOT NULL,
    label VARCHAR(64) NOT NULL,
    line1 VARCHAR(255) NOT NULL,
    line2 VARCHAR(255),
    city VARCHAR(120) NOT NULL,
    postal_code VARCHAR(32) NOT NULL,
    default_address BIT(1) NOT NULL DEFAULT 0,
    CONSTRAINT fk_delivery_profile FOREIGN KEY (user_profile_id) REFERENCES user_profiles (id)
);

CREATE INDEX idx_delivery_profile ON delivery_addresses (user_profile_id);
