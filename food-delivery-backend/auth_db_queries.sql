    -- 1. Create the Database manually if needed
    CREATE DATABASE IF NOT EXISTS auth_db;
    USE auth_db;

    -- 2. Create the Roles table
    CREATE TABLE roles (
        id BIGINT AUTO_INCREMENT PRIMARY KEY,
        name VARCHAR(50) NOT NULL UNIQUE
    );

    -- 3. Create the Users table
    CREATE TABLE users (
        id BIGINT AUTO_INCREMENT PRIMARY KEY,
        email VARCHAR(120) NOT NULL UNIQUE,
        password VARCHAR(255) NOT NULL,
        enabled BOOLEAN NOT NULL DEFAULT TRUE,
        created_at DATETIME NOT NULL
    );

    -- 4. Create the Join Table for the Many-to-Many relationship (User <-> Role)
    CREATE TABLE user_roles (
        user_id BIGINT NOT NULL,
        role_id BIGINT NOT NULL,
        PRIMARY KEY (user_id, role_id),
        CONSTRAINT fk_user
            FOREIGN KEY (user_id) 
            REFERENCES users(id) 
            ON DELETE CASCADE,
        CONSTRAINT fk_role
            FOREIGN KEY (role_id) 
            REFERENCES roles(id) 
            ON DELETE CASCADE
    );

    -- 5. (Optional) Insert some default Roles so they are available immediately
    INSERT INTO roles (name) VALUES 
    ('USER'), 
    ('ADMIN'), 
    ('RESTAURANT_OWNER'), 
    ('DELIVERY_PARTNER');
