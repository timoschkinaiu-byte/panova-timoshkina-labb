package ru.ssau.tk.pmi.performance;

import org.junit.jupiter.api.*;
import ru.ssau.tk.pmi.repository.manual.*;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ManualJdbcPerformanceTest {

    private Connection connection;
    private JdbcUserDao userDao;
    private JdbcFunctionDao functionDao;
    private JdbcComputedPointDao computedPointDao;
    private JdbcFunctionAccessDao accessDao;

    private static final int TEST_RUNS = 50;
    private static final int DATA_SIZE = 10000;

    private List<Long> testUserIds;
    private List<Long> testFunctionIds;
    private List<Long> testPointIds;
    private List<Long> testAccessIds;

    private AtomicLong userCounter = new AtomicLong(1000000); // для уникальных username

    @BeforeAll
    void setUp() throws Exception{
        connection = DriverManager.getConnection(
                "jdbc:postgresql://localhost:5432/lab_db",
                "postgres",
                "user"
        );

        initializeDatabase();

        userDao = new JdbcUserDao(connection);
        functionDao = new JdbcFunctionDao(connection);
        computedPointDao = new JdbcComputedPointDao(connection);
        accessDao = new JdbcFunctionAccessDao(connection);

        generateTestData();
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

    private void generateTestData() throws SQLException {
        System.out.println("Генерация тестовых данных для JDBC (10к+ записей в каждой таблице)...");

        // Очистка всех таблиц
        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate("DELETE FROM computed_points");
            stmt.executeUpdate("DELETE FROM functions_access");
            stmt.executeUpdate("DELETE FROM functions");
            stmt.executeUpdate("DELETE FROM users");
        }

        // Генерация 10к пользователей
        testUserIds = new ArrayList<>();
        for (int i = 0; i < DATA_SIZE; i++) {
            String username = "jdbc_user_" + i;
            String passwordHash = "hash_" + i;
            String role = i % 2 == 0 ? "ADMIN" : "USER";

            userDao.insertUser(username, passwordHash, role);

            var user = userDao.getUserByUsername(username);
            if (user != null) {
                testUserIds.add((Long) user.get("user_id"));
            }
        }

        // Генерация 10к функций
        testFunctionIds = new ArrayList<>();
        for (int i = 0; i < DATA_SIZE; i++) {
            Long ownerId = testUserIds.get(i % testUserIds.size());
            Long functionId = functionDao.insertFunction(
                    "jdbc_func_" + i,
                    "x^" + (i % 5),
                    ownerId,
                    i % 3 == 0
            );
            if (functionId != null) {
                testFunctionIds.add(functionId);
            }
        }

        // Генерация 10к точек
        testPointIds = new ArrayList<>();
        for (int i = 0; i < DATA_SIZE; i++) {
            Long functionId = testFunctionIds.get(i % testFunctionIds.size());
            Long pointId = computedPointDao.insertComputedPoint(functionId, (double) i, (double) i * i);
            if (pointId != null) {
                testPointIds.add(pointId);
            }
        }

        // Генерация 10к прав доступа
        testAccessIds = new ArrayList<>();
        for (int i = 0; i < DATA_SIZE; i++) {
            Long functionId = testFunctionIds.get(i % testFunctionIds.size());
            Long userId = testUserIds.get((i + 1) % testUserIds.size());
            String accessType = i % 3 == 0 ? "READ" : i % 3 == 1 ? "WRITE" : "EXECUTE";
            Long accessId = accessDao.insertAccess(functionId, userId, accessType);
            if (accessId != null) {
                testAccessIds.add(accessId);
            }
        }

        System.out.println("Данные сгенерированы для JDBC:");
        System.out.println("- Пользователей: " + testUserIds.size());
        System.out.println("- Функций: " + testFunctionIds.size());
        System.out.println("- Точек: " + testPointIds.size());
        System.out.println("- Прав доступа: " + testAccessIds.size());
        System.out.println();
    }

    // ==================== ТЕСТЫ ПОИСКА ====================

    @Test
    void testSearchPerformance() {
        System.out.println("=== JDBC: СКОРОСТЬ ПОИСКА ===");

        // Поиск пользователя по username
        measureSearchPerformance("User", () -> {
            userDao.getUserByUsername("jdbc_user_" + (DATA_SIZE / 2));
        });

        // Поиск функции по ID
        measureSearchPerformance("MathFunction", () -> {
            functionDao.getFunctionById(testFunctionIds.get(DATA_SIZE / 2));
        });

        // Поиск точек по function_id
        measureSearchPerformance("ComputedPoint", () -> {
            computedPointDao.getComputedPointsByFunctionId(testFunctionIds.get(DATA_SIZE / 2));
        });

        // Поиск прав доступа по ID
        measureSearchPerformance("FunctionAccess", () -> {
            accessDao.getAccessById(testAccessIds.get(DATA_SIZE / 2));
        });
    }

    // ==================== ТЕСТЫ ДОБАВЛЕНИЯ ====================

    @Test
    void testInsertPerformance() {
        System.out.println("=== JDBC: СКОРОСТЬ ДОБАВЛЕНИЯ ===");

        // Добавление пользователя (без возврата ID)
        measureInsertPerformance("User", () -> {
            String uniqueUsername = "new_user_" + userCounter.getAndIncrement();
            userDao.insertUser(uniqueUsername, "new_hash", "USER");
            return null;
        });

        // Добавление функции
        measureInsertPerformance("MathFunction", () -> {
            Long ownerId = testUserIds.get(0);
            String uniqueFuncName = "new_func_" + userCounter.getAndIncrement();
            return functionDao.insertFunction(uniqueFuncName, "x^2", ownerId, true);
        });

        // Добавление точки
        measureInsertPerformance("ComputedPoint", () -> {
            Long functionId = testFunctionIds.get(0);
            return computedPointDao.insertComputedPoint(functionId, 999.0, 999.0);
        });

        // Добавление права доступа
        measureInsertPerformance("FunctionAccess", () -> {
            Long functionId = testFunctionIds.get(0);
            Long userId = testUserIds.get(1);
            return accessDao.insertAccess(functionId, userId, "READ");
        });
    }

    // ==================== ТЕСТЫ ОБНОВЛЕНИЯ ====================

    @Test
    void testUpdatePerformance() {
        System.out.println("=== JDBC: СКОРОСТЬ ОБНОВЛЕНИЯ ===");

        // Обновление пользователя - только пароль и роль (не username)
        measureUpdatePerformance("User", (id) -> {
            String uniqueUsername = "updated_user_" + userCounter.getAndIncrement();
            userDao.updateUser(id, uniqueUsername, "updated_hash", "UPDATED");
        });

        // Обновление функции
        measureUpdatePerformance("MathFunction", (id) -> {
            String uniqueName = "updated_func_" + userCounter.getAndIncrement();
            functionDao.updateFunction(id, uniqueName, "updated_def", false);
        });

        // Обновление точки
        measureUpdatePerformance("ComputedPoint", (id) -> {
            computedPointDao.updateComputedPoint(id, 888.0, 888.0);
        });

        // Обновление права доступа
        measureUpdatePerformance("FunctionAccess", (id) -> {
            accessDao.updateAccess(id, "UPDATED");
        });
    }

    // ==================== ТЕСТЫ УДАЛЕНИЯ ====================

    @Test
    void testDeletePerformance() {
        System.out.println("=== JDBC: СКОРОСТЬ УДАЛЕНИЯ ===");

        // Удаление пользователя (создаем временные данные)
        measureDeletePerformance("User", () -> {
            String uniqueUsername = "temp_user_" + userCounter.getAndIncrement();
            userDao.insertUser(uniqueUsername, "temp_hash", "USER");
            var user = userDao.getUserByUsername(uniqueUsername);
            return user != null ? (Long) user.get("user_id") : null;
        });

        // Удаление функции (создаем временные данные)
        measureDeletePerformance("MathFunction", () -> {
            Long ownerId = testUserIds.get(0);
            String uniqueFuncName = "temp_func_" + userCounter.getAndIncrement();
            return functionDao.insertFunction(uniqueFuncName, "x^2", ownerId, true);
        });

        // Удаление точки (создаем временные данные)
        measureDeletePerformance("ComputedPoint", () -> {
            Long functionId = testFunctionIds.get(0);
            return computedPointDao.insertComputedPoint(functionId, 777.0, 777.0);
        });

        // Удаление права доступа (создаем временные данные)
        measureDeletePerformance("FunctionAccess", () -> {
            Long functionId = testFunctionIds.get(0);
            Long userId = testUserIds.get(1);
            return accessDao.insertAccess(functionId, userId, "TEMP");
        });
    }

    // ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ

    private void measureSearchPerformance(String tableName, SearchOperation operation) {
        List<Long> times = new ArrayList<>();

        for (int i = 0; i < TEST_RUNS; i++) {
            long start = System.nanoTime();
            operation.execute();
            long time = System.nanoTime() - start;
            times.add(time);
        }

        printPerformanceStats("Поиск в " + tableName, times);
    }

    private void measureInsertPerformance(String tableName, InsertOperation operation) {
        List<Long> times = new ArrayList<>();

        for (int i = 0; i < TEST_RUNS; i++) {
            long start = System.nanoTime();
            operation.execute();
            long time = System.nanoTime() - start;
            times.add(time);
        }

        printPerformanceStats("Добавление в " + tableName, times);
    }

    private void measureUpdatePerformance(String tableName, UpdateOperation operation) {
        List<Long> times = new ArrayList<>();

        for (int i = 0; i < TEST_RUNS; i++) {
            Long id = getRandomId(tableName);
            if (id != null) {
                long start = System.nanoTime();
                operation.execute(id);
                long time = System.nanoTime() - start;
                times.add(time);
            }
        }

        printPerformanceStats("Обновление в " + tableName, times);
    }

    private void measureDeletePerformance(String tableName, TempInsertOperation operation) {
        List<Long> times = new ArrayList<>();

        for (int i = 0; i < TEST_RUNS; i++) {
            Long id = operation.execute();

            if (id != null) {
                long start = System.nanoTime();
                deleteEntity(tableName, id);
                long time = System.nanoTime() - start;
                times.add(time);
            }
        }

        printPerformanceStats("Удаление из " + tableName, times);
    }

    private void deleteEntity(String tableName, Long id) {
        try {
            switch (tableName) {
                case "User": userDao.deleteUser(id); break;
                case "MathFunction": functionDao.deleteFunction(id); break;
                case "ComputedPoint": computedPointDao.deleteComputedPoint(id); break;
                case "FunctionAccess": accessDao.deleteAccess(id); break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Long getRandomId(String tableName) {
        Random random = new Random();
        List<Long> list = switch (tableName) {
            case "User" -> testUserIds;
            case "MathFunction" -> testFunctionIds;
            case "ComputedPoint" -> testPointIds;
            case "FunctionAccess" -> testAccessIds;
            default -> new ArrayList<Long>();
        };

        return list.isEmpty() ? null : list.get(random.nextInt(list.size()));
    }

    private void printPerformanceStats(String operation, List<Long> times) {
        if (times.isEmpty()) return;

        double avg = times.stream().mapToLong(Long::longValue).average().orElse(0) / 1_000_000.0;
        double min = times.stream().mapToLong(Long::longValue).min().orElse(0) / 1_000_000.0;
        double max = times.stream().mapToLong(Long::longValue).max().orElse(0) / 1_000_000.0;

        System.out.printf("%-35s - Среднее: %8.3f ms, Мин: %6.3f ms, Макс: %6.3f ms%n",
                operation, avg, min, max);
    }

    @FunctionalInterface
    interface SearchOperation {
        void execute();
    }

    @FunctionalInterface
    interface InsertOperation {
        Long execute();
    }

    @FunctionalInterface
    interface TempInsertOperation {
        Long execute();
    }

    @FunctionalInterface
    interface UpdateOperation {
        void execute(Long id);
    }

    @AfterAll
    void tearDown() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }

        System.out.println("\n=== ИТОГИ ТЕСТИРОВАНИЯ JDBC ===");
        System.out.println("Протестировано 4 типа операций на 4 таблицах");
        System.out.println("Размер каждой таблицы: " + DATA_SIZE + " записей");
        System.out.println("Количество запусков на операцию: " + TEST_RUNS);
    }
}
