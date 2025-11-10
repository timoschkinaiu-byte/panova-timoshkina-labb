/*package ru.ssau.tk.pmi.repository.manual;

import org.junit.jupiter.api.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.*;
import java.util.List;
import java.util.Map;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class JdbcComputedPointDaoTest {

    private static final Logger logger = LogManager.getLogger(JdbcComputedPointDaoTest.class);

    private Connection connection;
    private JdbcComputedPointDao computedPointDao;

    @BeforeAll
    void setup() throws SQLException {
        connection = DriverManager.getConnection(
                "jdbc:postgresql://localhost:5432/lab_db",
                "postgres",
                "user"
        );
        computedPointDao = new JdbcComputedPointDao(connection);

        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate("DELETE FROM computed_points");
            stmt.executeUpdate("DELETE FROM functions");
            stmt.executeUpdate("DELETE FROM users");

            // создаём пользователя и функцию БЕЗ function_type
            stmt.executeUpdate("INSERT INTO users (user_id, username, password_hash, role) VALUES (1, 'test_user', '123', 'USER')");
            stmt.executeUpdate("INSERT INTO functions (function_id, function_name, function_definition, owner_id, is_public) " +
                    "VALUES (1, 'f1', 'x^2', 1, true)");
        }
        logger.info("Test DB initialized");
    }

    @Test
    void testComputedPointCRUD() {
        Long pointId = computedPointDao.insertComputedPoint(1L, 2.0, 4.0);
        Assertions.assertNotNull(pointId);

        Map<String, Object> fetched = computedPointDao.getComputedPointById(pointId);
        Assertions.assertEquals(2.0, fetched.get("x_value"));
        Assertions.assertEquals(4.0, fetched.get("y_value"));

        computedPointDao.updateComputedPoint(pointId, 3.0, 9.0);
        Map<String, Object> updated = computedPointDao.getComputedPointById(pointId);
        Assertions.assertEquals(3.0, updated.get("x_value"));
        Assertions.assertEquals(9.0, updated.get("y_value"));

        List<Map<String, Object>> list = computedPointDao.getComputedPointsByFunctionId(1L);
        Assertions.assertFalse(list.isEmpty());

        computedPointDao.deleteComputedPoint(pointId);
        Assertions.assertNull(computedPointDao.getComputedPointById(pointId));

        logger.info("ComputedPoint CRUD test passed");
    }

    @AfterAll
    void cleanup() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
        logger.info("Connection closed");
    }
}*/




package ru.ssau.tk.pmi.repository.manual;

import org.junit.jupiter.api.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Path;
import java.sql.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class JdbcComputedPointDaoTest {

    private static final Logger logger = LogManager.getLogger(JdbcComputedPointDaoTest.class);
    private Connection connection;
    private JdbcComputedPointDao computedPointDao;

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

        computedPointDao = new JdbcComputedPointDao(connection);
        logger.info("Test DB initialized");
    }

    private void initializeDatabase() throws Exception {
        // Чтение SQL скрипта

        Path sqlPath = Paths.get("src/test/resources/setup-test-db.sql");
        String sqlScript = Files.readString(sqlPath);

        try (Statement stmt = connection.createStatement()) {
            // Выполнение скрипта построчно
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
            stmt.executeUpdate("DELETE FROM computed_points");
            stmt.executeUpdate("DELETE FROM functions_access");
            stmt.executeUpdate("DELETE FROM functions");
            stmt.executeUpdate("DELETE FROM users");

            // Создание тестовых данных
            stmt.executeUpdate("INSERT INTO users (user_id, username, password_hash, role) VALUES (1, 'test_user', '123', 'USER')");
            stmt.executeUpdate("INSERT INTO functions (function_id, function_name, function_definition, owner_id, is_public) " +
                    "VALUES (1, 'f1', 'x^2', 1, true)");
        }
    }

    @Test
    void testComputedPointCRUD() {
        Long pointId = computedPointDao.insertComputedPoint(1L, 2.0, 4.0);
        Assertions.assertNotNull(pointId);

        Map<String, Object> fetched = computedPointDao.getComputedPointById(pointId);
        Assertions.assertEquals(2.0, fetched.get("x_value"));
        Assertions.assertEquals(4.0, fetched.get("y_value"));

        computedPointDao.updateComputedPoint(pointId, 3.0, 9.0);
        Map<String, Object> updated = computedPointDao.getComputedPointById(pointId);
        Assertions.assertEquals(3.0, updated.get("x_value"));
        Assertions.assertEquals(9.0, updated.get("y_value"));

        List<Map<String, Object>> list = computedPointDao.getComputedPointsByFunctionId(1L);
        Assertions.assertFalse(list.isEmpty());

        computedPointDao.deleteComputedPoint(pointId);
        Assertions.assertNull(computedPointDao.getComputedPointById(pointId));

        logger.info("ComputedPoint CRUD test passed");
    }

    @AfterAll
    void cleanup() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
        logger.info("Connection closed");
    }
}
