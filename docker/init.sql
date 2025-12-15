-- ============================================
-- FINAL WORKING DATABASE SCRIPT
-- ============================================

-- 1. Удалить всё
DROP TABLE IF EXISTS functions_access CASCADE;
DROP TABLE IF EXISTS computed_points CASCADE;
DROP TABLE IF EXISTS functions CASCADE;
DROP TABLE IF EXISTS users CASCADE;

-- 2. Создать таблицы
CREATE TABLE users (
    user_id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE functions (
    function_id BIGSERIAL PRIMARY KEY,
    function_name VARCHAR(100) NOT NULL,
    function_definition TEXT NOT NULL,
    function_type VARCHAR(20) NOT NULL DEFAULT 'TABULATED',
    owner_id BIGINT NOT NULL,
    is_public BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (owner_id) REFERENCES users(user_id) ON DELETE CASCADE,
    CONSTRAINT check_function_type CHECK (function_type IN ('TABULATED', 'MATH', 'COMPOSITE'))
);

CREATE TABLE computed_points (
    point_id BIGSERIAL PRIMARY KEY,
    function_id BIGINT NOT NULL,
    x_value DOUBLE PRECISION NOT NULL,
    y_value DOUBLE PRECISION NOT NULL,
    FOREIGN KEY (function_id) REFERENCES functions(function_id) ON DELETE CASCADE,
    CONSTRAINT unique_function_x UNIQUE (function_id, x_value)
);

CREATE TABLE functions_access (
    access_id BIGSERIAL PRIMARY KEY,
    function_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    access_type VARCHAR(10) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (function_id) REFERENCES functions(function_id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    CONSTRAINT unique_function_user UNIQUE (function_id, user_id),
    CONSTRAINT check_access_type CHECK (access_type IN ('READ', 'WRITE'))
);

-- 3. Добавить admin с РАБОЧИМ хэшем (из твоего вывода!)
INSERT INTO users (username, password_hash, role, enabled)
VALUES (
    'admin',
    '$2a$10$2ry1HYhzvbXJcv0rRkfnt.37V6r69qUUCY8kvizQepEQqX4.g7jnm',
    'ADMIN',
    true
);