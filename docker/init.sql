CREATE TABLE IF NOT EXISTS users (
    user_id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL
);

CREATE TABLE IF NOT EXISTS functions (
    function_id BIGSERIAL PRIMARY KEY,
    function_name VARCHAR(100) NOT NULL,
    function_definition TEXT NOT NULL,
    owner_id BIGINT NOT NULL,
    is_public BOOLEAN DEFAULT FALSE,
    FOREIGN KEY (owner_id) REFERENCES users(user_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS computed_points (
    point_id BIGSERIAL PRIMARY KEY,
    function_id BIGINT NOT NULL,
    x_value DOUBLE PRECISION NOT NULL,
    y_value DOUBLE PRECISION NOT NULL,
    FOREIGN KEY (function_id) REFERENCES functions(function_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS functions_access (
    access_id BIGSERIAL PRIMARY KEY,
    function_id BIGSERIAL NOT NULL,
    user_id BIGINT NOT NULL,
    access_type VARCHAR(20) NOT NULL,
    FOREIGN KEY (function_id) REFERENCES functions(function_id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

-- Создаем администратора
INSERT INTO users (username, password_hash, role)
VALUES ('admin', 'hashed_admin123', 'ADMIN')
ON CONFLICT (username) DO NOTHING;