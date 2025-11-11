/*package ru.ssau.tk.pmi.repository.manual;

import org.junit.jupiter.api.*;

import java.nio.file.Files;
import java.nio.file.Paths;
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
        initializeDatabase();
        accessDao = new JdbcFunctionAccessDao(connection);

    }

    private void initializeDatabase() throws Exception {
        // Чтение SQL скрипта
        String sqlScript = new String(Files.readAllBytes(
                Paths.get(getClass().getClassLoader().getResource("setup-test-db.sql").toURI())
        ));

        try (Statement stmt = connection.createStatement()) {
            // Выполнение скрипта построчно
            String[] statements = sqlScript.split(";");
            for (String statement : statements) {
                if (!statement.trim().isEmpty()) {
                    stmt.execute(statement.trim());
                }
            }
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
}*/



package ru.ssau.tk.pmi.repository.manual;

import org.junit.jupiter.api.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class JdbcFunctionAccessDaoTest {

    private static final Logger logger = LogManager.getLogger(JdbcFunctionAccessDaoTest.class);
    private Connection connection;
    private JdbcFunctionAccessDao functionAccessDao;

    @BeforeAll
    void setup() throws Exception {
        // Подключение к БД
        connection = DriverManager.getConnection(
                "jdbc:postgresql://localhost:5432/lab_db",
                "postgres",
                "user"
        );

        // Инициализация схемы БД
        initializeDatabase();

        functionAccessDao = new JdbcFunctionAccessDao(connection);
        logger.info("Test DB initialized for FunctionAccessDao");
    }

    private void initializeDatabase() throws Exception {
        // Чтение SQL скрипта
        String sqlScript = new String(Files.readAllBytes(
                Paths.get(getClass().getClassLoader().getResource("setup-test-db.sql").toURI())
        ));

        try (Statement stmt = connection.createStatement()) {
            // Выполнение скрипта
            String[] statements = sqlScript.split(";");
            for (String statement : statements) {
                if (!statement.trim().isEmpty()) {
                    stmt.execute(statement.trim());
                }
            }
        }
        logger.info("Database schema initialized");
    }

    @BeforeEach
    void clearData() throws SQLException {
        // Очистка данных перед каждым тестом
        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate("DELETE FROM functions_access");
            stmt.executeUpdate("DELETE FROM computed_points");
            stmt.executeUpdate("DELETE FROM functions");
            stmt.executeUpdate("DELETE FROM users");

            // Создание тестовых данных
            stmt.executeUpdate("INSERT INTO users (user_id, username, password_hash, role) VALUES (1, 'owner_user', '123', 'USER')");
            stmt.executeUpdate("INSERT INTO users (user_id, username, password_hash, role) VALUES (2, 'shared_user', '456', 'USER')");

            stmt.executeUpdate("INSERT INTO functions (function_id, function_name, function_definition, function_type, owner_id, is_public) " +
                    "VALUES (1, 'shared_func', 'x^2', 'POLYNOMIAL', 1, false)");
        }
    }

    @Test
    void testFunctionAccessCRUD() {
        // INSERT
        Long accessId = functionAccessDao.insertAccess(1L, 2L, "READ");
        Assertions.assertNotNull(accessId);

        // SELECT by ID
        Map<String, Object> fetched = functionAccessDao.getAccessById(accessId);
        Assertions.assertNotNull(fetched);
        Assertions.assertEquals("READ", fetched.get("access_type"));
        Assertions.assertEquals(1L, fetched.get("function_id"));
        Assertions.assertEquals(2L, fetched.get("user_id"));

        // SELECT by Function and User
        List<Map<String, Object>> accessList = functionAccessDao.getAccessByFunctionAndUser(1L, 2L);
        Assertions.assertFalse(accessList.isEmpty());
        Assertions.assertEquals(accessId, accessList.get(0).get("access_id"));

        // UPDATE
        functionAccessDao.updateAccess(accessId, "WRITE");
        Map<String, Object> updated = functionAccessDao.getAccessById(accessId);
        Assertions.assertEquals("WRITE", updated.get("access_type"));

        // GET ALL
        List<Map<String, Object>> allAccess = functionAccessDao.getAllAccess();
        Assertions.assertFalse(allAccess.isEmpty());

        // DELETE
        functionAccessDao.deleteAccess(accessId);
        Map<String, Object> deleted = functionAccessDao.getAccessById(accessId);
        Assertions.assertNull(deleted);

        logger.info("FunctionAccess CRUD test passed");
    }

    @Test
    void testMultipleAccessRecords() {
        // Создаем несколько записей доступа
        functionAccessDao.insertAccess(1L, 2L, "READ");
        functionAccessDao.insertAccess(1L, 1L, "WRITE");

        List<Map<String, Object>> allAccess = functionAccessDao.getAllAccess();
        Assertions.assertEquals(2, allAccess.size());
    }

    @AfterAll
    void cleanup() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
        logger.info("Connection closed");
    }
}
