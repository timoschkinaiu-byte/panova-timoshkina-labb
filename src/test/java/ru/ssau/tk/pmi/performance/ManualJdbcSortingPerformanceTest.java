
package ru.ssau.tk.pmi.performance;

import org.junit.jupiter.api.*;
import ru.ssau.tk.pmi.repository.manual.*;
import ru.ssau.tk.pmi.dto.*;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ManualJdbcSortingPerformanceTest {

    private Connection connection;
    private SearchServiceImpl searchService;
    private JdbcFunctionDao functionDao;

    private static final int TEST_RUNS = 50;
    private static final int DATA_SIZE = 10000;

    private List<Long> testUserIds;
    private List<Long> testFunctionIds;
    private AtomicLong userCounter = new AtomicLong(1000000);

    @BeforeAll
    void setUp() throws Exception {
        connection = DriverManager.getConnection(
                "jdbc:postgresql://localhost:5432/lab_db",
                "postgres",
                "user"
        );

        initializeDatabase();

        functionDao = new JdbcFunctionDao(connection);
        searchService = new SearchServiceImpl(connection, functionDao);

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
        System.out.println("Генерация тестовых данных для сортировки JDBC (10к+ записей)...");

        // Очистка таблиц
        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate("DELETE FROM computed_points");
            stmt.executeUpdate("DELETE FROM functions_access");
            stmt.executeUpdate("DELETE FROM functions");
            stmt.executeUpdate("DELETE FROM users");
        }

        JdbcUserDao userDao = new JdbcUserDao(connection);
        JdbcComputedPointDao pointDao = new JdbcComputedPointDao(connection);
        JdbcFunctionAccessDao accessDao = new JdbcFunctionAccessDao(connection);

        // Генерация пользователей с разными именами для сортировки
        testUserIds = new ArrayList<>();
        String[] firstNames = {"Альфа", "Бета", "Гамма", "Дельта", "Эпсилон", "Дзета", "Эта", "Тета", "Йота", "Каппа"};
        String[] lastNames = {"Иванов", "Петров", "Сидоров", "Кузнецов", "Смирнов", "Попов", "Васильев", "Федоров", "Морозов", "Волков"};

        for (int i = 0; i < DATA_SIZE; i++) {
            String firstName = firstNames[i % firstNames.length];
            String lastName = lastNames[i % lastNames.length];
            String username = firstName.toLowerCase() + "_" + lastName.toLowerCase() + "_" + i;
            userDao.insertUser(username, "hash_" + i, i % 2 == 0 ? "ADMIN" : "USER");

            var user = userDao.getUserByUsername(username);
            if (user != null) {
                testUserIds.add((Long) user.get("user_id"));
            }
        }

        // Генерация функций с разными именами для сортировки
        testFunctionIds = new ArrayList<>();
        String[] functionNames = {"Квадратичная", "Синус", "Косинус", "Экспонента", "Логарифм", "Кубическая", "Тангенс", "Корень", "Модуль", "Гиперболическая"};

        for (int i = 0; i < DATA_SIZE; i++) {
            Long ownerId = testUserIds.get(i % testUserIds.size());
            String functionName = functionNames[i % functionNames.length] + "_Функция_" + i;
            Long functionId = functionDao.insertFunction(functionName, "x^" + (i % 5), ownerId, i % 3 == 0);
            if (functionId != null) {
                testFunctionIds.add(functionId);
            }
        }


        // Генерация точек с разными значениями X для сортировки
        for (int i = 0; i < DATA_SIZE; i++) {
            Long functionId = testFunctionIds.get(i % testFunctionIds.size());
            double xValue = (double) (DATA_SIZE - i); // Обратный порядок для тестирования сортировки
            pointDao.insertComputedPoint(functionId, xValue, xValue * xValue);
        }

        // Генерация прав доступа
        for (int i = 0; i < DATA_SIZE; i++) {
            Long functionId = testFunctionIds.get(i % testFunctionIds.size());
            Long userId = testUserIds.get((i + 1) % testUserIds.size());
            String accessType = i % 3 == 0 ? "READ" : i % 3 == 1 ? "WRITE" : "EXECUTE";
            accessDao.insertAccess(functionId, userId, accessType);
        }

        System.out.println("Данные для сортировки JDBC сгенерированы:");
        System.out.println("- Пользователей: " + testUserIds.size());
        System.out.println("- Функций: " + testFunctionIds.size());
        System.out.println("- Точек: " + DATA_SIZE);
        System.out.println("- Прав доступа: " + DATA_SIZE);
        System.out.println();
    }

    // ОСТАВИЛ ТОЛЬКО 4 ТЕСТА КАК В HIBERNATE
    @Test
    void testUserSortingByName() {
        System.out.println("=== ТЕСТ 1: СОРТИРОВКА ПОЛЬЗОВАТЕЛЕЙ ПО ИМЕНИ (JDBC) ===");

        List<Long> ascTimes = new ArrayList<>();

        for (int i = 0; i < TEST_RUNS; i++) {
            long start = System.nanoTime();
            List<UserDto> usersAsc = searchService.searchByFieldSorted(
                    UserDto.class, "role", "ADMIN", "username", true);
            long ascTime = System.nanoTime() - start;
            ascTimes.add(ascTime);
        }

        printSortingStats("Сортировка пользователей по имени", ascTimes);
    }

    @Test
    void testFunctionSortingByName() {
        System.out.println("=== ТЕСТ 2: СОРТИРОВКА ФУНКЦИЙ ПО ИМЕНИ (JDBC) ===");

        List<Long> ascTimes = new ArrayList<>();

        for (int i = 0; i < TEST_RUNS; i++) {
            long start = System.nanoTime();
            List<FunctionDto> functionsAsc = searchService.searchByFieldSorted(
                    FunctionDto.class, "is_public", true, "function_name", true);
            long ascTime = System.nanoTime() - start;
            ascTimes.add(ascTime);
        }

        printSortingStats("Сортировка функций по имени", ascTimes);
    }

    @Test
    void testFunctionSortingByOwner() {
        System.out.println("=== ТЕСТ 3: СОРТИРОВКА ФУНКЦИЙ ПО ВЛАДЕЛЬЦУ (JDBC) ===");

        List<Long> ascTimes = new ArrayList<>();

        for (int i = 0; i < TEST_RUNS; i++) {
            long start = System.nanoTime();
            List<FunctionDto> functionsAsc = searchService.searchByFieldSorted(
                    FunctionDto.class, "is_public", true, "owner_id", true);
            long ascTime = System.nanoTime() - start;
            ascTimes.add(ascTime);
        }

        printSortingStats("Сортировка функций по владельцу", ascTimes);
    }

    @Test
    void testPointSortingByX() {
        System.out.println("=== ТЕСТ 4: СОРТИРОВКА ТОЧЕК ПО X (JDBC) ===");

        List<Long> ascTimes = new ArrayList<>();

        for (int i = 0; i < TEST_RUNS; i++) {
            long start = System.nanoTime();
            List<ComputedPointDto> pointsAsc = searchService.searchByFieldSorted(
                    ComputedPointDto.class, "function_id", testFunctionIds.get(0), "x_value", true);
            long ascTime = System.nanoTime() - start;
            ascTimes.add(ascTime);
        }

        printSortingStats("Сортировка точек по X", ascTimes);
    }

    private void printSortingStats(String operation, List<Long> times) {
        double avg = times.stream().mapToLong(Long::longValue).average().orElse(0) / 1_000_000.0;
        double min = times.stream().mapToLong(Long::longValue).min().orElse(0) / 1_000_000.0;
        double max = times.stream().mapToLong(Long::longValue).max().orElse(0) / 1_000_000.0;


        System.out.println("Операция: " + operation);
        System.out.printf("Среднее время: %.3f мс\n", avg);
        System.out.printf("Минимальное время: %.3f мс\n", min);
        System.out.printf("Максимальное время: %.3f мс\n", max);
        System.out.println("Количество тестов: " + TEST_RUNS);
        System.out.println();
    }

    @AfterAll
    void tearDown() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }

        System.out.println("=== РЕЗУЛЬТАТЫ ТЕСТИРОВАНИЯ СКОРОСТИ СОРТИРОВКИ JDBC ===");
        System.out.println("Тестирование завершено. Размер каждой таблицы: " + DATA_SIZE + " записей");
        System.out.println("Количество запусков на тест: " + TEST_RUNS);
    }
}