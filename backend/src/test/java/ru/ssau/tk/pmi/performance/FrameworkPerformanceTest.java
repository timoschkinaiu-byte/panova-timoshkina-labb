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
class FrameworkPerformanceTest {

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
        settings.put("hibernate.order_updates", "true");

        configuration.setProperties(settings);
        configuration.addAnnotatedClass(User.class);
        configuration.addAnnotatedClass(MathFunction.class);
        configuration.addAnnotatedClass(ComputedPoint.class);
        configuration.addAnnotatedClass(FunctionAccess.class);

        sessionFactory = configuration.buildSessionFactory();

        generateTestData();
    }

    private void generateTestData() {
        System.out.println("Генерация тестовых данных (10к+ записей в каждой таблице)...");

        try (Session session = sessionFactory.openSession()) {
            session.beginTransaction();

            // Очистка всех таблиц
            session.createMutationQuery("DELETE FROM ComputedPoint").executeUpdate();
            session.createMutationQuery("DELETE FROM FunctionAccess").executeUpdate();
            session.createMutationQuery("DELETE FROM MathFunction").executeUpdate();
            session.createMutationQuery("DELETE FROM User").executeUpdate();
            session.getTransaction().commit();
        }

        // Генерация 10к пользователей
        testUsers = new ArrayList<>();
        try (Session session = sessionFactory.openSession()) {
            session.beginTransaction();
            for (int i = 0; i < DATA_SIZE; i++) {
                User user = new User("user_" + i, "hash_" + i, i % 2 == 0 ? "ADMIN" : "USER");
                session.persist(user);
                testUsers.add(user);
                if (i % 100 == 0) session.flush();
            }
            session.getTransaction().commit();
        }

        // Генерация 10к функций
        testFunctions = new ArrayList<>();
        try (Session session = sessionFactory.openSession()) {
            session.beginTransaction();
            for (int i = 0; i < DATA_SIZE; i++) {
                User owner = testUsers.get(i % DATA_SIZE);
                MathFunction function = new MathFunction("func_" + i, "x^" + (i % 5),
                        i % 4 == 0 ? "ПОЛИНОМ" : "ТРИГОНОМЕТРИЧЕСКАЯ", owner);
                function.setIsPublic(i % 3 == 0);
                session.persist(function);
                testFunctions.add(function);
                if (i % 100 == 0) session.flush();
            }
            session.getTransaction().commit();
        }

        // Генерация 10к точек
        testPoints = new ArrayList<>();
        try (Session session = sessionFactory.openSession()) {
            session.beginTransaction();
            for (int i = 0; i < DATA_SIZE; i++) {
                MathFunction function = testFunctions.get(i % DATA_SIZE);
                ComputedPoint point = new ComputedPoint((double) i, (double) i * i, function);
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
                User user = testUsers.get((i + 1) % DATA_SIZE); // Другой пользователь
                String accessType = i % 3 == 0 ? "READ" : i % 3 == 1 ? "WRITE" : "EXECUTE";
                FunctionAccess access = new FunctionAccess(accessType, function, user);
                session.persist(access);
                testAccesses.add(access);
                if (i % 100 == 0) session.flush();
            }
            session.getTransaction().commit();
        }

        System.out.println("Данные сгенерированы:");
        System.out.println("- Пользователей: " + testUsers.size());
        System.out.println("- Функций: " + testFunctions.size());
        System.out.println("- Точек: " + testPoints.size());
        System.out.println("- Прав доступа: " + testAccesses.size());
        System.out.println();
    }

    // ==================== ТЕСТЫ ПОИСКА ====================


    @Test
    void testSearchPerformance() {
        System.out.println("=== СКОРОСТЬ ПОИСКА ===");

        // Поиск в таблице User
        measureSearchPerformance("User", "FROM User WHERE username = :param",
                "user_" + (DATA_SIZE / 2), "param");

        // Поиск в таблице MathFunction
        measureSearchPerformance("MathFunction", "FROM MathFunction WHERE functionName = :param",
                "func_" + (DATA_SIZE / 2), "param");

        // Поиск в таблице ComputedPoint
        measureSearchPerformance("ComputedPoint", "FROM ComputedPoint WHERE xValue = :param",
                (double) (DATA_SIZE / 2), "param");

        // Поиск в таблице FunctionAccess - ИСПРАВЛЕНО
        measureSearchPerformance("FunctionAccess", "FROM FunctionAccess WHERE accessType = :param",
                "READ", "param");
    }

    // ==================== ТЕСТЫ ДОБАВЛЕНИЯ ====================

    @Test
    void testInsertPerformance() {
        System.out.println("=== СКОРОСТЬ ДОБАВЛЕНИЯ ===");

        // Добавление в таблицу User
        measureInsertPerformance("User", () -> {
            User user = new User("new_user_" + System.currentTimeMillis(), "new_hash", "USER");
            return user;
        });

        // Добавление в таблицу MathFunction
        measureInsertPerformance("MathFunction", () -> {
            User owner = testUsers.get(0);
            MathFunction function = new MathFunction("new_func_" + System.currentTimeMillis(),
                    "x^2", "ПОЛИНОМ", owner);
            return function;
        });

        // Добавление в таблицу ComputedPoint
        measureInsertPerformance("ComputedPoint", () -> {
            MathFunction function = testFunctions.get(0);
            ComputedPoint point = new ComputedPoint(999.0, 999.0, function);
            return point;
        });

        // Добавление в таблицу FunctionAccess
        measureInsertPerformance("FunctionAccess", () -> {
            MathFunction function = testFunctions.get(0);
            User user = testUsers.get(1);
            FunctionAccess access = new FunctionAccess("READ", function, user);
            return access;
        });
    }

    // ==================== ТЕСТЫ ОБНОВЛЕНИЯ ====================

    @Test
    void testUpdatePerformance() {
        System.out.println("=== СКОРОСТЬ ОБНОВЛЕНИЯ ===");

        // Обновление в таблице User
        measureUpdatePerformance("User", (session, id) -> {
            User user = session.find(User.class, id);
            if (user != null) user.setRole("UPDATED");
        });

        // Обновление в таблице MathFunction
        measureUpdatePerformance("MathFunction", (session, id) -> {
            MathFunction function = session.find(MathFunction.class, id);
            if (function != null) function.setIsPublic(true);
        });

        // Обновление в таблице ComputedPoint
        measureUpdatePerformance("ComputedPoint", (session, id) -> {
            ComputedPoint point = session.find(ComputedPoint.class, id);
            if (point != null) point.setYValue(9999.0);
        });

        // Обновление в таблице FunctionAccess
        measureUpdatePerformance("FunctionAccess", (session, id) -> {
            FunctionAccess access = session.find(FunctionAccess.class, id);
            if (access != null) access.setAccessType("UPDATED");
        });
    }

    // ==================== ТЕСТЫ УДАЛЕНИЯ ====================

    @Test
    void testDeletePerformance() {
        System.out.println("=== СКОРОСТЬ УДАЛЕНИЯ ===");

        // Удаление из таблицы User (создаем временные данные)
        measureDeletePerformance("User", () -> {
            User user = new User("temp_user_" + System.currentTimeMillis(), "temp_hash", "USER");
            return user;
        });

        // Удаление из таблицы MathFunction (создаем временные данные)
        measureDeletePerformance("MathFunction", () -> {
            User owner = testUsers.get(0);
            MathFunction function = new MathFunction("temp_func_" + System.currentTimeMillis(),
                    "x^2", "ПОЛИНОМ", owner);
            return function;
        });

        // Удаление из таблицы ComputedPoint (создаем временные данные)
        measureDeletePerformance("ComputedPoint", () -> {
            MathFunction function = testFunctions.get(0);
            ComputedPoint point = new ComputedPoint(888.0, 888.0, function);
            return point;
        });

        // Удаление из таблицы FunctionAccess (создаем временные данные)
        measureDeletePerformance("FunctionAccess", () -> {
            MathFunction function = testFunctions.get(0);
            User user = testUsers.get(1);
            FunctionAccess access = new FunctionAccess("TEMP", function, user);
            return access;
        });
    }

    // ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ


    private void measureSearchPerformance(String tableName, String hql, Object parameter, String paramName) {
        List<Long> times = new ArrayList<>();

        for (int i = 0; i < TEST_RUNS; i++) {
            try (Session session = sessionFactory.openSession()) {
                long start = System.nanoTime();

                var query = session.createQuery(hql, Object.class)
                        .setParameter(paramName, parameter);

                if (tableName.equals("FunctionAccess")) {
                    query.getResultList(); // Для FunctionAccess может быть несколько результатов
                } else {
                    query.uniqueResult(); // Для остальных - один результат
                }

                long time = System.nanoTime() - start;
                times.add(time);
            }
        }

        printPerformanceStats("Поиск в " + tableName, times);
    }

    private void measureInsertPerformance(String tableName, EntityCreator creator) {
        List<Long> times = new ArrayList<>();

        for (int i = 0; i < TEST_RUNS; i++) {
            try (Session session = sessionFactory.openSession()) {
                session.beginTransaction();

                Object entity = creator.create();
                long start = System.nanoTime();
                session.persist(entity);
                session.flush();
                long time = System.nanoTime() - start;

                session.getTransaction().rollback();
                times.add(time);
            }
        }

        printPerformanceStats("Добавление в " + tableName, times);
    }

    private void measureUpdatePerformance(String tableName, EntityUpdater updater) {
        List<Long> times = new ArrayList<>();

        for (int i = 0; i < TEST_RUNS; i++) {
            try (Session session = sessionFactory.openSession()) {
                session.beginTransaction();

                Long id = getRandomId(tableName);
                long start = System.nanoTime();
                updater.update(session, id);
                session.flush();
                long time = System.nanoTime() - start;

                session.getTransaction().rollback();
                times.add(time);
            }
        }

        printPerformanceStats("Обновление в " + tableName, times);
    }

    private void measureDeletePerformance(String tableName, EntityCreator creator) {
        List<Long> times = new ArrayList<>();

        for (int i = 0; i < TEST_RUNS; i++) {
            try (Session session = sessionFactory.openSession()) {
                session.beginTransaction();

                // Создаем и сохраняем сущность
                Object entity = creator.create();
                session.persist(entity);
                session.flush();

                // Измеряем удаление
                long start = System.nanoTime();
                session.remove(entity);
                session.flush();
                long time = System.nanoTime() - start;

                session.getTransaction().rollback();
                times.add(time);
            }
        }

        printPerformanceStats("Удаление из " + tableName, times);
    }

    private Long getRandomId(String tableName) {
        switch (tableName) {
            case "User": return testUsers.get(TEST_RUNS % DATA_SIZE).getUserId();
            case "MathFunction": return testFunctions.get(TEST_RUNS % DATA_SIZE).getFunctionId();
            case "ComputedPoint": return testPoints.get(TEST_RUNS % DATA_SIZE).getPointId();
            case "FunctionAccess": return testAccesses.get(TEST_RUNS % DATA_SIZE).getAccessId();
            default: return 1L;
        }
    }

    private void printPerformanceStats(String operation, List<Long> times) {
        double avg = times.stream().mapToLong(Long::longValue).average().orElse(0) / 1_000_000.0;
        double min = times.stream().mapToLong(Long::longValue).min().orElse(0) / 1_000_000.0;
        double max = times.stream().mapToLong(Long::longValue).max().orElse(0) / 1_000_000.0;

        System.out.printf("%-35s - Среднее: %8.3f ms, Мин: %6.3f ms, Макс: %6.3f ms%n",
                operation, avg, min, max);
    }

    @FunctionalInterface
    interface EntityCreator {
        Object create();
    }

    @FunctionalInterface
    interface EntityUpdater {
        void update(Session session, Long id);
    }

    @AfterAll
    void tearDown() {
        if (sessionFactory != null) {
            sessionFactory.close();
        }

        System.out.println("\n=== ИТОГИ ТЕСТИРОВАНИЯ ===");
        System.out.println("Протестировано 4 типа операций на 4 таблицах");
        System.out.println("Размер каждой таблицы: " + DATA_SIZE + " записей");
    }
}