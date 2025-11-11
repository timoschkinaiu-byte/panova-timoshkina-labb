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
public class JdbcFunctionDaoTest {

    private static final Logger logger = LogManager.getLogger(JdbcFunctionDaoTest.class);
    private Connection connection;
    private JdbcFunctionDao functionDao;

    @BeforeEach
    void setUp() throws Exception {
        String url = "jdbc:postgresql://localhost:5432/lab_db";
        String username = "postgres";
        String password = "user";
        connection = DriverManager.getConnection(url, username, password);
        initializeDatabase();
        functionDao = new JdbcFunctionDao(connection);
        try (Statement stmt = connection.createStatement()) {
            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM users WHERE user_id = 1");
            rs.next();
            if (rs.getInt(1) == 0) {
                stmt.executeUpdate("INSERT INTO users (user_id, username, password_hash, role) VALUES (1, 'test_user', '123', 'USER')");
            }
        }
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



    @Test
    void testFunctionCRUD() {
        Long functionId = functionDao.insertFunction("test_func", "x^3", 1L, true);
        Assertions.assertNotNull(functionId);

        // SELECT by ID
        Map<String, Object> fetched = functionDao.getFunctionById(functionId);
        Assertions.assertNotNull(fetched);
        Assertions.assertEquals("test_func", fetched.get("function_name"));
        Assertions.assertEquals("x^3", fetched.get("function_definition"));
        // UPDATE
        functionDao.updateFunction(functionId, "updated_func", "x^4", false);
        Map<String, Object> updated = functionDao.getFunctionById(functionId);
        Assertions.assertEquals("updated_func", updated.get("function_name"));
        Assertions.assertEquals("x^4", updated.get("function_definition"));
        Assertions.assertEquals(false, updated.get("is_public"));

        // GET ALL
        List<Map<String, Object>> allFunctions = functionDao.getAllFunctions();
        Assertions.assertFalse(allFunctions.isEmpty());

        // DELETE
        functionDao.deleteFunction(functionId);
        Map<String, Object> deleted = functionDao.getFunctionById(functionId);
        Assertions.assertNull(deleted);

        logger.info("Function CRUD test passed");
    }


    @AfterAll
    void cleanup() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
        logger.info("Connection closed");
    }
}