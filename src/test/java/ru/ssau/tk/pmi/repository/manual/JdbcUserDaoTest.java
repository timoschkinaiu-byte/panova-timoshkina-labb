package ru.ssau.tk.pmi.repository.manual;

import org.junit.jupiter.api.*;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.*;
import java.util.Map;
import java.util.Random;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class JdbcUserDaoTest {

    private Connection connection;
    private JdbcUserDao userDao;

    @BeforeAll
    void setUpDatabase() throws Exception {

        String url = "jdbc:postgresql://localhost:5432/lab_db2";
        String username = "postgres";
        String password = "user";

        connection = DriverManager.getConnection(url, username, password);
        initializeDatabase();
        userDao = new JdbcUserDao(connection);
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

    @BeforeEach
    void clearTable() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate("DELETE FROM users");
        }
    }

    @Test
    void testUserCRUD() {
        Random random = new Random();
        String userName = "user_" + random.nextInt(10000);
        String passwordHash = "hash_" + random.nextInt(10000);
        String role = "USER";

        // 🔹 INSERT
        userDao.insertUser(userName, passwordHash, role);

        // 🔹 SELECT BY USERNAME
        Map<String, Object> inserted = userDao.getUserByUsername(userName);
        Assertions.assertNotNull(inserted, "User should exist after insert");
        Long userId = ((Number) inserted.get("user_id")).longValue();

        Assertions.assertEquals(userName, inserted.get("username"));
        Assertions.assertEquals(passwordHash, inserted.get("password_hash"));
        Assertions.assertEquals(role, inserted.get("role"));

        // 🔹 UPDATE
        String newName = userName + "_updated";
        String newPass = passwordHash + "_new";
        String newRole = "ADMIN";
        userDao.updateUser(userId, newName, newPass, newRole);

        Map<String, Object> updated = userDao.getUserById(userId);
        Assertions.assertEquals(newName, updated.get("username"));
        Assertions.assertEquals(newPass, updated.get("password_hash"));
        Assertions.assertEquals(newRole, updated.get("role"));

        // 🔹 DELETE
        userDao.deleteUser(userId);
        Map<String, Object> deleted = userDao.getUserById(userId);
        Assertions.assertNull(deleted, "User should be deleted");
    }

    @AfterAll
    void tearDown() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }
}


