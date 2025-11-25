-- USERS
INSERT INTO users (username, password_hash, role) VALUES (?, ?, ?);
SELECT * FROM users WHERE user_id = ?;
SELECT * FROM users WHERE username = ?;
SELECT * FROM users WHERE username = 'admin';
UPDATE users SET username = ?, password_hash = ?, role = ? WHERE user_id = ?;
UPDATE users SET role = 'ADMIN' WHERE username = 'admin';
DELETE FROM users WHERE user_id = ?;

-- FUNCTIONS
INSERT INTO functions (function_name, function_definition, owner_id, is_public) VALUES (?, ?, ?, ?);
SELECT * FROM functions WHERE function_id = ?;
SELECT * FROM functions WHERE owner_id = ?;
UPDATE functions SET function_name = ?, function_definition = ?, is_public = ? WHERE function_id = ?;
DELETE FROM functions WHERE function_id = ?;

-- COMPUTED_POINTS
INSERT INTO computed_points (function_id, x_value, y_value) VALUES (?, ?, ?);
SELECT * FROM computed_points WHERE point_id = ?;
SELECT * FROM computed_points WHERE function_id = ?;
UPDATE computed_points SET x_value = ?, y_value = ? WHERE point_id = ?;
DELETE FROM computed_points WHERE point_id = ?;

-- FUNCTIONS_ACCESS
INSERT INTO functions_access (function_id, user_id, access_type) VALUES (?, ?, ?);
SELECT * FROM functions_access WHERE access_id = ?;
SELECT * FROM functions_access WHERE function_id = ? AND user_id = ?;
UPDATE functions_access SET access_type = ? WHERE access_id = ?;
DELETE FROM functions_access WHERE access_id = ?;

-- ДОПОЛНИТЕЛЬНЫЕ ДЛЯ ПОЛНОЙ РЕАЛИЗАЦИИ ВСЕХ МЕТОДОВ:
-- USERS дополнительные
SELECT * FROM users;
SELECT COUNT(*) FROM users WHERE username = ?;

-- FUNCTIONS дополнительные
INSERT INTO functions (function_name, function_definition, owner_id, is_public) VALUES (?, ?, ?, ?) RETURNING function_id;
SELECT * FROM functions;
SELECT * FROM functions WHERE is_public = true;
SELECT COUNT(*) FROM functions WHERE owner_id = ?;

-- COMPUTED_POINTS дополнительные
INSERT INTO computed_points (function_id, x_value, y_value) VALUES (?, ?, ?) RETURNING point_id;
SELECT * FROM computed_points;
SELECT COUNT(*) FROM computed_points WHERE function_id = ?;
DELETE FROM computed_points WHERE function_id = ?;

-- FUNCTIONS_ACCESS дополнительные
INSERT INTO functions_access (function_id, user_id, access_type) VALUES (?, ?, ?) RETURNING access_id;
SELECT * FROM functions_access;
SELECT * FROM functions_access WHERE function_id = ?;
SELECT * FROM functions_access WHERE user_id = ?;
DELETE FROM functions_access WHERE function_id = ? AND user_id = ?;