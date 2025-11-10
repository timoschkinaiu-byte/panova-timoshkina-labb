package ru.ssau.tk.pmi.repository.manual;

import org.junit.jupiter.api.*;
import java.sql.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

public class JdbcFunctionDaoTest {
    private Connection connection;
    private JdbcFunctionDao functionDao;
    @BeforeEach
    void setUp() throws Exception {
        // Подключение к твоей реальной базе PostgreSQL
        String url = "jdbc:postgresql://localhost:5432/lab_db";
        String username = "postgres";
        String password = "user";
        connection = DriverManager.getConnection(url, username, password);
        functionDao = new JdbcFunctionDao(connection);
        // Очистим таблицу перед тестом
        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate("DELETE FROM functions");
        }
        // ⚠️ Убедимся, что есть пользователь с ID=1
        try (Statement stmt = connection.createStatement()) {
            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM users WHERE user_id = 1");
            rs.next();
            if (rs.getInt(1) == 0) {
                stmt.executeUpdate("INSERT INTO users (user_id, username, password_hash, role) VALUES (1, 'test_user', '123', 'USER')");
            }
        }
    }
    @AfterEach
    void tearDown() throws Exception {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }
    private String randomString(int len) {
        String chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
        Random random = new Random();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < len; i++) sb.append(chars.charAt(random.nextInt(chars.length())));
        return sb.toString();
    }
    @Test
    void testCRUD() {
        String name = randomString(6);
        String def = "x^2";
        String type = "POLYNOMIAL";
        Long ownerId = 1L;
        boolean isPublic = true;
        // INSERT
        Long id = functionDao.insertFunction(name, def, type, ownerId, isPublic);
        assertNotNull(id);
        // SELECT
        Map<String, Object> func = functionDao.getFunctionById(id);
        assertNotNull(func);
        assertEquals(name, func.get("function_name"));
        // UPDATE
        String newName = randomString(8);
        String newDef = "sin(x)";
        String newType = "TRIGONOMETRIC";
        boolean newIsPublic = false;
        functionDao.updateFunction(id, newName, newDef, newType, newIsPublic);
        Map<String, Object> updated = functionDao.getFunctionById(id);
        assertEquals(newName, updated.get("function_name"));
        assertEquals(newDef, updated.get("function_definition"));
        assertEquals(newType, updated.get("function_type"));
        assertEquals(newIsPublic, updated.get("is_public"));
        // DELETE
        functionDao.deleteFunction(id);
        Map<String, Object> deleted = functionDao.getFunctionById(id);
        assertNull(deleted);
    }
}
