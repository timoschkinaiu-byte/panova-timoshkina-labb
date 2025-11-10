package ru.ssau.tk.pmi.repository.manual;

import org.junit.jupiter.api.*;
import java.sql.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

public class JdbcFunctionAccessDaoTest {
    private Connection connection;
    private JdbcFunctionAccessDao accessDao;
    @BeforeEach
    void setUp() throws Exception {
        // Подключение к реальной БД
        String url = "jdbc:postgresql://localhost:5432/lab_db";
        String username = "postgres";
        String password = "user";
        connection = DriverManager.getConnection(url, username, password);
        accessDao = new JdbcFunctionAccessDao(connection);
        // Очистка таблиц
        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate("DELETE FROM functions_access");
            stmt.executeUpdate("DELETE FROM functions");
            stmt.executeUpdate("DELETE FROM users");
        }
        // Создадим пользователя и функцию для теста
        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate("INSERT INTO users (user_id, username, password_hash, role) VALUES (1, 'test_user', '123', 'USER')");
            stmt.executeUpdate("INSERT INTO functions (function_id, function_name, function_definition, function_type, owner_id, is_public) " +
                    "VALUES (1, 'f1', 'x^2', 'POLYNOMIAL', 1, true)");

        }
    }
    @AfterEach
    void tearDown() throws Exception {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }
    @Test
    void testCRUD() {
        Long functionId = 1L;
        Long userId = 1L;
        String accessType = "READ";
        // INSERT
        Long accessId = accessDao.insertAccess(functionId, userId, accessType);
        assertNotNull(accessId);
        // SELECT by ID
        Map<String, Object> access = accessDao.getAccessById(accessId);
        assertNotNull(access);
        assertEquals("READ", access.get("access_type"));
        // UPDATE
        accessDao.updateAccess(accessId, "WRITE");
        Map<String, Object> updated = accessDao.getAccessById(accessId);
        assertEquals("WRITE", updated.get("access_type"));
        // SELECT by function & user
        List<Map<String, Object>> list = accessDao.getAccessByFunctionAndUser(functionId, userId);
        assertFalse(list.isEmpty());
        assertEquals(accessId, list.get(0).get("access_id"));
        // DELETE
        accessDao.deleteAccess(accessId);
        Map<String, Object> deleted = accessDao.getAccessById(accessId);
        assertNull(deleted);
    }
}
