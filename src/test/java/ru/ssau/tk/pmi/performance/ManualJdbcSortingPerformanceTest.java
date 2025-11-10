package ru.ssau.tk.pmi.performance;

import org.junit.jupiter.api.*;
import ru.ssau.tk.pmi.repository.manual.*;
import ru.ssau.tk.pmi.dto.*;

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
    void setUp() throws SQLException {
        connection = DriverManager.getConnection(
                "jdbc:postgresql://localhost:5432/lab_db",
                "postgres",
                "user"
        );

        functionDao = new JdbcFunctionDao(connection);
        searchService = new SearchServiceImpl(connection, functionDao);

        generateTestData();
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

    @Test
    void testAccessSortingByType() {
        System.out.println("=== ТЕСТ 5: СОРТИРОВКА ПРАВ ДОСТУПА ПО ТИПУ (JDBC) ===");

        List<Long> ascTimes = new ArrayList<>();

        for (int i = 0; i < TEST_RUNS; i++) {
            long start = System.nanoTime();
            List<FunctionAccessDto> accessAsc = searchService.searchByFieldSorted(
                    FunctionAccessDto.class, "user_id", testUserIds.get(0), "access_type", true);
            long ascTime = System.nanoTime() - start;
            ascTimes.add(ascTime);
        }

        printSortingStats("Сортировка прав доступа по типу", ascTimes);
    }

    // Дополнительные тесты для сравнения производительности

    @Test
    void testDirectSqlSortingPerformance() {
        System.out.println("=== ПРЯМЫЕ SQL ЗАПРОСЫ ДЛЯ СОРТИРОВКИ (JDBC) ===");

        // Тест 1: Прямой SQL для сортировки пользователей
        measureDirectSqlSorting("Пользователи (прямой SQL)",
                "SELECT * FROM users WHERE role = ? ORDER BY username ASC", "ADMIN");

        // Тест 2: Прямой SQL для сортировки функций
        measureDirectSqlSorting("Функции (прямой SQL)",
                "SELECT * FROM functions WHERE is_public = ? ORDER BY function_name ASC", true);

        // Тест 3: Прямой SQL для сортировки точек
        measureDirectSqlSorting("Точки (прямой SQL)",
                "SELECT * FROM computed_points WHERE function_id = ? ORDER BY x_value ASC", testFunctionIds.get(0));
    }

    private void measureDirectSqlSorting(String testName, String sql, Object param) {
        List<Long> times = new ArrayList<>();

        for (int i = 0; i < TEST_RUNS; i++) {
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setObject(1, param);
                long start = System.nanoTime();
                ResultSet rs = stmt.executeQuery();
                // Читаем все результаты для точного измерения
                while (rs.next()) {
                    rs.getObject(1); // Просто читаем данные
                }
                long time = System.nanoTime() - start;
                times.add(time);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        printSortingStats(testName, times);
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

        // Создание таблицы результатов для GitHub
        createResultsTable();
    }

    private void createResultsTable() {
        System.out.println("\n=== ТАБЛИЦА РЕЗУЛЬТАТОВ ДЛЯ GITHUB ===");
        System.out.println("| Тест | Среднее время (мс) | Минимальное время (мс) | Максимальное время (мс) |");
        System.out.println("|------|-------------------|----------------------|----------------------|");
        System.out.println("| Сортировка пользователей по имени | - | - | - |");
        System.out.println("| Сортировка функций по имени | - | - | - |");
        System.out.println("| Сортировка функций по владельцу | - | - | - |");
        System.out.println("| Сортировка точек по X | - | - | - |");
        System.out.println("| Сортировка прав доступа по типу | - | - | - |");
        System.out.println("| Пользователи (прямой SQL) | - | - | - |");
        System.out.println("| Функции (прямой SQL) | - | - | - |");
        System.out.println("| Точки (прямой SQL) | - | - | - |");

        System.out.println("\n**Параметры тестирования:**");
        System.out.println("- База данных: PostgreSQL");
        System.out.println("- Размер данных: " + DATA_SIZE + " записей в каждой таблице");
        System.out.println("- Количество запусков: " + TEST_RUNS + " на тест");
        System.out.println("- Реализация: JDBC (Manual)");
    }
}
