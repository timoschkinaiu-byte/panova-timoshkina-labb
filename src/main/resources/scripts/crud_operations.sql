-- ============================================
-- USERS ОПЕРАЦИИ
-- ============================================

-- INSERT (добавлены enabled, created_at, updated_at)
INSERT INTO users (username, password_hash, role, enabled, created_at, updated_at)
VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- SELECT по ID (все поля)
SELECT user_id, username, password_hash, role, enabled, created_at, updated_at
FROM users WHERE user_id = ?;

-- SELECT по username (все поля)
SELECT user_id, username, password_hash, role, enabled, created_at, updated_at
FROM users WHERE username = ?;

-- SELECT админа (все поля)
SELECT user_id, username, password_hash, role, enabled, created_at, updated_at
FROM users WHERE username = 'admin';

-- UPDATE (все поля + updated_at)
UPDATE users
SET username = ?, password_hash = ?, role = ?, enabled = ?, updated_at = CURRENT_TIMESTAMP
WHERE user_id = ?;

-- DELETE
DELETE FROM users WHERE user_id = ?;

-- ДОПОЛНИТЕЛЬНЫЕ:
-- SELECT все пользователи (для админа)
SELECT user_id, username, role, enabled, created_at
FROM users;

-- Проверка существования username
SELECT COUNT(*) FROM users WHERE username = ?;

-- ============================================
-- FUNCTIONS ОПЕРАЦИИ
-- ============================================

-- INSERT (добавлен function_type, created_at, updated_at)
INSERT INTO functions (function_name, function_definition, function_type, owner_id, is_public, created_at, updated_at)
VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
RETURNING function_id;

-- SELECT по ID (все поля)
SELECT function_id, function_name, function_definition, function_type, owner_id, is_public, created_at, updated_at
FROM functions WHERE function_id = ?;

-- SELECT функции пользователя
SELECT function_id, function_name, function_definition, function_type, is_public, created_at, updated_at
FROM functions WHERE owner_id = ?;

-- UPDATE (все поля + updated_at)
UPDATE functions
SET function_name = ?, function_definition = ?, function_type = ?, is_public = ?, updated_at = CURRENT_TIMESTAMP
WHERE function_id = ?;

-- DELETE
DELETE FROM functions WHERE function_id = ?;

-- ДОПОЛНИТЕЛЬНЫЕ:
-- SELECT все функции
SELECT function_id, function_name, function_definition, function_type, owner_id, is_public, created_at, updated_at
FROM functions;

-- SELECT публичные функции
SELECT function_id, function_name, function_definition, function_type, owner_id, is_public, created_at, updated_at
FROM functions WHERE is_public = true;

-- Подсчёт функций пользователя
SELECT COUNT(*) FROM functions WHERE owner_id = ?;

-- ============================================
-- COMPUTED_POINTS ОПЕРАЦИИ
-- ============================================

-- INSERT (остаётся тот же, но будет проверяться UNIQUE constraint)
INSERT INTO computed_points (function_id, x_value, y_value)
VALUES (?, ?, ?)
RETURNING point_id;

-- SELECT по ID
SELECT point_id, function_id, x_value, y_value
FROM computed_points WHERE point_id = ?;

-- SELECT точки функции
SELECT point_id, function_id, x_value, y_value
FROM computed_points WHERE function_id = ?
ORDER BY x_value;

-- UPDATE
UPDATE computed_points
SET x_value = ?, y_value = ?
WHERE point_id = ?;

-- DELETE
DELETE FROM computed_points WHERE point_id = ?;

-- ДОПОЛНИТЕЛЬНЫЕ:
-- SELECT все точки
SELECT point_id, function_id, x_value, y_value
FROM computed_points;

-- Подсчёт точек функции
SELECT COUNT(*) FROM computed_points WHERE function_id = ?;

-- DELETE все точки функции
DELETE FROM computed_points WHERE function_id = ?;

-- ============================================
-- FUNCTIONS_ACCESS ОПЕРАЦИИ
-- ============================================

-- INSERT (добавлен created_at)
INSERT INTO functions_access (function_id, user_id, access_type, created_at)
VALUES (?, ?, ?, CURRENT_TIMESTAMP)
RETURNING access_id;

-- SELECT по ID (все поля)
SELECT access_id, function_id, user_id, access_type, created_at
FROM functions_access WHERE access_id = ?;

-- SELECT доступ по функции и пользователю
SELECT access_id, function_id, user_id, access_type, created_at
FROM functions_access WHERE function_id = ? AND user_id = ?;

-- UPDATE
UPDATE functions_access
SET access_type = ?
WHERE access_id = ?;

-- DELETE
DELETE FROM functions_access WHERE access_id = ?;

-- ДОПОЛНИТЕЛЬНЫЕ:
-- SELECT все доступы
SELECT access_id, function_id, user_id, access_type, created_at
FROM functions_access;

-- SELECT доступы к функции
SELECT access_id, function_id, user_id, access_type, created_at
FROM functions_access WHERE function_id = ?;

-- SELECT доступы пользователя
SELECT access_id, function_id, user_id, access_type, created_at
FROM functions_access WHERE user_id = ?;

-- DELETE доступ по функции и пользователю
DELETE FROM functions_access WHERE function_id = ? AND user_id = ?;