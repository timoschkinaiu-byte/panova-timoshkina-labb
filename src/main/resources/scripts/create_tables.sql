--- ============================================
 -- ТАБЛИЦА USERS (добавлены enabled, created_at, updated_at)
 -- ============================================
 CREATE TABLE users (
     user_id BIGSERIAL PRIMARY KEY,
     username VARCHAR(50) UNIQUE NOT NULL,
     password_hash VARCHAR(255) NOT NULL,
     role VARCHAR(20) NOT NULL,
     enabled BOOLEAN NOT NULL DEFAULT TRUE,
     created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
     updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
 );

 -- ============================================
 -- ТАБЛИЦА FUNCTIONS (добавлены function_type, created_at, updated_at)
 -- ============================================
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

 -- ============================================
 -- ТАБЛИЦА COMPUTED_POINTS (добавлен UNIQUE constraint)
 -- ============================================
 CREATE TABLE computed_points (
     point_id BIGSERIAL PRIMARY KEY,
     function_id BIGINT NOT NULL,
     x_value DOUBLE PRECISION NOT NULL,
     y_value DOUBLE PRECISION NOT NULL,
     FOREIGN KEY (function_id) REFERENCES functions(function_id) ON DELETE CASCADE,
     CONSTRAINT unique_function_x UNIQUE (function_id, x_value)
 );

 -- ============================================
 -- ТАБЛИЦА FUNCTIONS_ACCESS (добавлен created_at, изменён размер access_type)
 -- ============================================
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

 -- ============================================
 -- ИНДЕКСЫ ДЛЯ ПРОИЗВОДИТЕЛЬНОСТИ
 -- ============================================
 CREATE INDEX idx_users_username ON users(username);
 CREATE INDEX idx_users_role ON users(role);
 CREATE INDEX idx_functions_owner ON functions(owner_id);
 CREATE INDEX idx_functions_type ON functions(function_type);
 CREATE INDEX idx_functions_public ON functions(is_public) WHERE is_public = TRUE;
 CREATE INDEX idx_points_function ON computed_points(function_id);
 CREATE INDEX idx_points_x ON computed_points(x_value);
 CREATE INDEX idx_points_function_x ON computed_points(function_id, x_value);
 CREATE INDEX idx_access_function ON functions_access(function_id);
 CREATE INDEX idx_access_user ON functions_access(user_id);