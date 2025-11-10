package ru.ssau.tk.pmi.performance;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.junit.jupiter.api.*;
import ru.ssau.tk.pmi.entity.User;
import ru.ssau.tk.pmi.entity.MathFunction;
import ru.ssau.tk.pmi.entity.ComputedPoint;
import ru.ssau.tk.pmi.entity.FunctionAccess;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FrameworkSortingPerformanceTest {

    private SessionFactory sessionFactory;
    private static final int TEST_RUNS = 50;
    private static final int DATA_SIZE = 10000;

    private List<User> testUsers;
    private List<MathFunction> testFunctions;
    private List<ComputedPoint> testPoints;
    private List<FunctionAccess> testAccesses;

    @BeforeAll
    void setUp() {
        Configuration configuration = new Configuration();
        Properties settings = new Properties();
        settings.put("hibernate.connection.driver_class", "org.postgresql.Driver");
        settings.put("hibernate.connection.url", "jdbc:postgresql://localhost:5432/lab_db");
        settings.put("hibernate.connection.username", "postgres");
        settings.put("hibernate.connection.password", "user");
        settings.put("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
        settings.put("hibernate.show_sql", "false");
        settings.put("hibernate.hbm2ddl.auto", "create-drop");
        settings.put("hibernate.jdbc.batch_size", "100");
        settings.put("hibernate.order_inserts", "true");

        configuration.setProperties(settings);
        configuration.addAnnotatedClass(User.class);
        configuration.addAnnotatedClass(MathFunction.class);
        configuration.addAnnotatedClass(ComputedPoint.class);
        configuration.addAnnotatedClass(FunctionAccess.class);

        sessionFactory = configuration.buildSessionFactory();

        generateTestData();
    }

    private void generateTestData() {
        System.out.println("Генерация тестовых данных для сортировки (10к+ записей в каждой таблице)...");

        try (Session session = sessionFactory.openSession()) {
            session.beginTransaction();

            // Очистка всех таблиц
            session.createMutationQuery("DELETE FROM ComputedPoint").executeUpdate();
            session.createMutationQuery("DELETE FROM FunctionAccess").executeUpdate();
            session.createMutationQuery("DELETE FROM MathFunction").executeUpdate();
            session.createMutationQuery("DELETE FROM User").executeUpdate();
            session.getTransaction().commit();
        }

        // Генерация 10к пользователей с разными именами для сортировки
        testUsers = new ArrayList<>();
        try (Session session = sessionFactory.openSession()) {
            session.beginTransaction();
            String[] firstNames = {"Альфа", "Бета", "Гамма", "Дельта", "Эпсилон", "Дзета", "Эта", "Тета", "Йота", "Каппа"};
            String[] lastNames = {"Иванов", "Петров", "Сидоров", "Кузнецов", "Смирнов", "Попов", "Васильев", "Федоров", "Морозов", "Волков"};

            for (int i = 0; i < DATA_SIZE; i++) {
                String firstName = firstNames[i % firstNames.length];
                String lastName = lastNames[i % lastNames.length];
                String username = firstName.toLowerCase() + "_" + lastName.toLowerCase() + "_" + i;
                User user = new User(username, "hash_" + i, i % 2 == 0 ? "ADMIN" : "USER");
                session.persist(user);
                testUsers.add(user);
                if (i % 100 == 0) session.flush();
            }
            session.getTransaction().commit();
        }

        // Генерация 10к функций с разными именами и типами для сортировки
        testFunctions = new ArrayList<>();
        try (Session session = sessionFactory.openSession()) {
            session.beginTransaction();
            String[] functionTypes = {"ПОЛИНОМ", "ТРИГОНОМЕТРИЧЕСКАЯ", "ЭКСПОНЕНЦИАЛЬНАЯ", "ЛОГАРИФМИЧЕСКАЯ", "РАЦИОНАЛЬНАЯ"};
            String[] functionNames = {"Квадратичная", "Синус", "Косинус", "Экспонента", "Логарифм", "Кубическая", "Тангенс", "Корень", "Модуль", "Гиперболическая"};

            for (int i = 0; i < DATA_SIZE; i++) {
                User owner = testUsers.get(i % DATA_SIZE);
                String functionName = functionNames[i % functionNames.length] + "_Функция_" + i;
                String functionType = functionTypes[i % functionTypes.length];
                String definition = "x^" + (i % 5);

                MathFunction function = new MathFunction(functionName, definition, functionType, owner);
                function.setIsPublic(i % 3 == 0);
                session.persist(function);
                testFunctions.add(function);
                if (i % 100 == 0) session.flush();
            }
            session.getTransaction().commit();
        }

        // Генерация 10к точек для сортировки по X
        testPoints = new ArrayList<>();
        try (Session session = sessionFactory.openSession()) {
            session.beginTransaction();
            for (int i = 0; i < DATA_SIZE; i++) {
                MathFunction function = testFunctions.get(i % DATA_SIZE);
                // Создаем точки с разными значениями X для сортировки
                double xValue = (double) (DATA_SIZE - i); // Обратный порядок для тестирования сортировки
                ComputedPoint point = new ComputedPoint(xValue, xValue * xValue, function);
                session.persist(point);
                testPoints.add(point);
                if (i % 100 == 0) session.flush();
            }
            session.getTransaction().commit();
        }

        // Генерация 10к прав доступа
        testAccesses = new ArrayList<>();
        try (Session session = sessionFactory.openSession()) {
            session.beginTransaction();
            for (int i = 0; i < DATA_SIZE; i++) {
                MathFunction function = testFunctions.get(i % DATA_SIZE);
                User user = testUsers.get((i + 1) % DATA_SIZE);
                String accessType = i % 3 == 0 ? "READ" : i % 3 == 1 ? "WRITE" : "EXECUTE";
                FunctionAccess access = new FunctionAccess(accessType, function, user);
                session.persist(access);
                testAccesses.add(access);
                if (i % 100 == 0) session.flush();
            }
            session.getTransaction().commit();
        }

        System.out.println("Данные для сортировки сгенерированы:");
        System.out.println("- Пользователей: " + testUsers.size());
        System.out.println("- Функций: " + testFunctions.size());
        System.out.println("- Точек: " + testPoints.size());
        System.out.println("- Прав доступа: " + testAccesses.size());
        System.out.println();
    }

    @Test
    void testUserSortingByName() {
        System.out.println("=== ТЕСТ 1: СОРТИРОВКА ПОЛЬЗОВАТЕЛЕЙ ПО ИМЕНИ ===");

        List<Long> ascTimes = new ArrayList<>();

        for (int i = 0; i < TEST_RUNS; i++) {
            try (Session session = sessionFactory.openSession()) {
                long start = System.nanoTime();
                List<User> usersAsc = session.createQuery(
                                "FROM User ORDER BY username ASC", User.class)
                        .getResultList();
                long ascTime = System.nanoTime() - start;
                ascTimes.add(ascTime);
            }
        }

        printSortingStats("Сортировка пользователей по имени", ascTimes);
    }

    @Test
    void testFunctionSortingByName() {
        System.out.println("=== ТЕСТ 2: СОРТИРОВКА ФУНКЦИЙ ПО ИМЕНИ ===");

        List<Long> ascTimes = new ArrayList<>();

        for (int i = 0; i < TEST_RUNS; i++) {
            try (Session session = sessionFactory.openSession()) {
                long start = System.nanoTime();
                List<MathFunction> functionsAsc = session.createQuery(
                                "FROM MathFunction ORDER BY functionName ASC", MathFunction.class)
                        .getResultList();
                long ascTime = System.nanoTime() - start;
                ascTimes.add(ascTime);
            }
        }

        printSortingStats("Сортировка функций по имени", ascTimes);
    }

    @Test
    void testFunctionSortingByType() {
        System.out.println("=== ТЕСТ 3: СОРТИРОВКА ФУНКЦИЙ ПО ТИПУ ===");

        List<Long> ascTimes = new ArrayList<>();

        for (int i = 0; i < TEST_RUNS; i++) {
            try (Session session = sessionFactory.openSession()) {
                long start = System.nanoTime();
                List<MathFunction> functionsAsc = session.createQuery(
                                "FROM MathFunction ORDER BY functionType ASC", MathFunction.class)
                        .getResultList();
                long ascTime = System.nanoTime() - start;
                ascTimes.add(ascTime);
            }
        }

        printSortingStats("Сортировка функций по типу", ascTimes);
    }

    @Test
    void testPointSortingByX() {
        System.out.println("=== ТЕСТ 4: СОРТИРОВКА ТОЧЕК ПО X ===");

        List<Long> ascTimes = new ArrayList<>();

        for (int i = 0; i < TEST_RUNS; i++) {
            try (Session session = sessionFactory.openSession()) {
                long start = System.nanoTime();
                List<ComputedPoint> pointsAsc = session.createQuery(
                                "FROM ComputedPoint ORDER BY xValue ASC", ComputedPoint.class)
                        .getResultList();
                long ascTime = System.nanoTime() - start;
                ascTimes.add(ascTime);
            }
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
    void tearDown() {
        if (sessionFactory != null) {
            sessionFactory.close();
        }

        System.out.println("=== РЕЗУЛЬТАТЫ ТЕСТИРОВАНИЯ СКОРОСТИ СОРТИРОВКИ ===");
        System.out.println("Тестирование завершено. Размер каждой таблицы: " + DATA_SIZE + " записей");
    }
}