-- Restaurant service database (MySQL 8+)
CREATE DATABASE IF NOT EXISTS restaurant_db
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE restaurant_db;

-- Schema mirrors JPA entities (can be used for manual provisioning when ddl-auto=none)

CREATE TABLE IF NOT EXISTS restaurants (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    owner_id BIGINT NOT NULL,
    name VARCHAR(200) NOT NULL,
    description VARCHAR(2000),
    status VARCHAR(32) NOT NULL,
    address_line1 VARCHAR(255),
    city VARCHAR(120),
    cuisine_type VARCHAR(120),
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP(6)
);

CREATE TABLE IF NOT EXISTS categories (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    restaurant_id BIGINT NOT NULL,
    name VARCHAR(120) NOT NULL,
    CONSTRAINT fk_categories_restaurant FOREIGN KEY (restaurant_id) REFERENCES restaurants (id) ON DELETE CASCADE,
    CONSTRAINT uq_category_per_restaurant UNIQUE (restaurant_id, name)
);

CREATE TABLE IF NOT EXISTS menu_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    restaurant_id BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    name VARCHAR(200) NOT NULL,
    description VARCHAR(2000),
    price DECIMAL(12, 2) NOT NULL,
    food_type VARCHAR(32) NOT NULL,
    is_available BIT(1) NOT NULL DEFAULT 1,
    image_url VARCHAR(1024),
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_menu_items_restaurant FOREIGN KEY (restaurant_id) REFERENCES restaurants (id) ON DELETE CASCADE,
    CONSTRAINT fk_menu_items_category FOREIGN KEY (category_id) REFERENCES categories (id) ON DELETE CASCADE
);

-- Seed sample data (ids may differ if tables already populated; safe for empty DB)

INSERT INTO restaurants (owner_id, name, description, status, address_line1, city, cuisine_type)
SELECT 1, 'Demo Bistro', 'Sample restaurant for local development', 'ACTIVE', '1 Market Street', 'Bengaluru', 'CONTINENTAL'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM restaurants WHERE name = 'Demo Bistro');

SET @rid := (SELECT id FROM restaurants WHERE name = 'Demo Bistro' LIMIT 1);

INSERT INTO categories (restaurant_id, name)
SELECT @rid, 'Mains'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM categories WHERE restaurant_id = @rid AND name = 'Mains');

INSERT INTO categories (restaurant_id, name)
SELECT @rid, 'Drinks'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM categories WHERE restaurant_id = @rid AND name = 'Drinks');

SET @cat_mains := (SELECT id FROM categories WHERE restaurant_id = @rid AND name = 'Mains' LIMIT 1);
SET @cat_drinks := (SELECT id FROM categories WHERE restaurant_id = @rid AND name = 'Drinks' LIMIT 1);

INSERT INTO menu_items (restaurant_id, category_id, name, description, price, food_type, is_available, image_url)
SELECT @rid, @cat_mains, 'Margherita Pizza', 'Classic tomato and mozzarella', 299.00, 'VEG', 1, NULL
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM menu_items WHERE restaurant_id = @rid AND name = 'Margherita Pizza');

INSERT INTO menu_items (restaurant_id, category_id, name, description, price, food_type, is_available, image_url)
SELECT @rid, @cat_mains, 'Chicken Alfredo', 'Creamy pasta with grilled chicken', 349.00, 'NON_VEG', 1, NULL
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM menu_items WHERE restaurant_id = @rid AND name = 'Chicken Alfredo');

INSERT INTO menu_items (restaurant_id, category_id, name, description, price, food_type, is_available, image_url)
SELECT @rid, @cat_drinks, 'Cola', 'Chilled soft drink', 60.00, 'VEG', 1, NULL
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM menu_items WHERE restaurant_id = @rid AND name = 'Cola');
