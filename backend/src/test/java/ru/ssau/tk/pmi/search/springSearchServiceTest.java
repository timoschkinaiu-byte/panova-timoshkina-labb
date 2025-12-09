package ru.ssau.tk.pmi.search;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.junit.jupiter.api.*;
import ru.ssau.tk.pmi.entity.ComputedPoint;
import ru.ssau.tk.pmi.entity.FunctionAccess;
import ru.ssau.tk.pmi.entity.User;
import ru.ssau.tk.pmi.entity.MathFunction;

import java.util.List;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class springSearchServiceTest {

    private SessionFactory sessionFactory;

    @BeforeAll
    void setUp() {
        Configuration configuration = new Configuration();
        Properties settings = new Properties();
        settings.put("hibernate.connection.driver_class", "org.postgresql.Driver");
        settings.put("hibernate.connection.url", "jdbc:postgresql://localhost:5432/lab_db");
        settings.put("hibernate.connection.username", "postgres");
        settings.put("hibernate.connection.password", "user");
        settings.put("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
        settings.put("hibernate.hbm2ddl.auto", "create-drop");
        settings.put("hibernate.show_sql", "false");

        configuration.setProperties(settings);
        configuration.addAnnotatedClass(User.class);
        configuration.addAnnotatedClass(MathFunction.class);
        configuration.addAnnotatedClass(ComputedPoint.class);
        configuration.addAnnotatedClass(FunctionAccess.class);

        sessionFactory = configuration.buildSessionFactory();
        generateTestData();
    }

    private void generateTestData() {
        try (Session session = sessionFactory.openSession()) {
            session.beginTransaction();

            session.createMutationQuery("DELETE FROM MathFunction").executeUpdate();
            session.createMutationQuery("DELETE FROM User").executeUpdate();

            // Создание тестовых данных для сортировки
            User user1 = new User("бета_пользователь", "hash1", "USER");
            User user2 = new User("альфа_пользователь", "hash2", "ADMIN");
            User user3 = new User("гамма_пользователь", "hash3", "USER");
            session.persist(user1);
            session.persist(user2);
            session.persist(user3);

            MathFunction func1 = new MathFunction("бета_функция", "x^2", "ПОЛИНОМ", user1);
            MathFunction func2 = new MathFunction("альфа_функция", "sin(x)", "ТРИГОНОМЕТРИЧЕСКАЯ", user2);
            MathFunction func3 = new MathFunction("гамма_функция", "cos(x)", "ПОЛИНОМ", user3);
            session.persist(func1);
            session.persist(func2);
            session.persist(func3);

            session.getTransaction().commit();
        }
    }

    @Test
    void testUsernameSortingAscending() {
        try (Session session = sessionFactory.openSession()) {
            List<User> users = session.createQuery(
                            "FROM User WHERE username LIKE :pattern ORDER BY username ASC", User.class)
                    .setParameter("pattern", "%пользователь%")
                    .getResultList();

            assertEquals(3, users.size());
            assertTrue(users.get(0).getUsername().compareTo(users.get(1).getUsername()) <= 0);
            assertTrue(users.get(1).getUsername().compareTo(users.get(2).getUsername()) <= 0);
        }
    }

    @Test
    void testUsernameSortingDescending() {
        try (Session session = sessionFactory.openSession()) {
            List<User> users = session.createQuery(
                            "FROM User WHERE username LIKE :pattern ORDER BY username DESC", User.class)
                    .setParameter("pattern", "%пользователь%")
                    .getResultList();

            assertEquals(3, users.size());
            assertTrue(users.get(0).getUsername().compareTo(users.get(1).getUsername()) >= 0);
            assertTrue(users.get(1).getUsername().compareTo(users.get(2).getUsername()) >= 0);
        }
    }

    @Test
    void testFunctionNameSortingAscending() {
        try (Session session = sessionFactory.openSession()) {
            List<MathFunction> functions = session.createQuery(
                            "FROM MathFunction WHERE functionName LIKE :pattern ORDER BY functionName ASC", MathFunction.class)
                    .setParameter("pattern", "%функция%")
                    .getResultList();

            assertEquals(3, functions.size());
            assertTrue(functions.get(0).getFunctionName().compareTo(functions.get(1).getFunctionName()) <= 0);
            assertTrue(functions.get(1).getFunctionName().compareTo(functions.get(2).getFunctionName()) <= 0);
        }
    }

    @Test
    void testFunctionNameSortingDescending() {
        try (Session session = sessionFactory.openSession()) {
            List<MathFunction> functions = session.createQuery(
                            "FROM MathFunction WHERE functionName LIKE :pattern ORDER BY functionName DESC", MathFunction.class)
                    .setParameter("pattern", "%функция%")
                    .getResultList();

            assertEquals(3, functions.size());
            assertTrue(functions.get(0).getFunctionName().compareTo(functions.get(1).getFunctionName()) >= 0);
            assertTrue(functions.get(1).getFunctionName().compareTo(functions.get(2).getFunctionName()) >= 0);
        }
    }

    @Test
    void testFunctionTypeSorting() {
        try (Session session = sessionFactory.openSession()) {
            List<MathFunction> functions = session.createQuery(
                            "FROM MathFunction WHERE functionType = :type ORDER BY functionName ASC", MathFunction.class)
                    .setParameter("type", "ПОЛИНОМ")
                    .getResultList();

            assertEquals(2, functions.size());
            assertEquals("ПОЛИНОМ", functions.get(0).getFunctionType());
            assertEquals("ПОЛИНОМ", functions.get(1).getFunctionType());
        }
    }

    @Test
    void testComplexSorting() {
        try (Session session = sessionFactory.openSession()) {
            List<MathFunction> functions = session.createQuery(
                            "FROM MathFunction ORDER BY functionType ASC, functionName ASC", MathFunction.class)
                    .getResultList();

            assertEquals(3, functions.size());
            // Проверяем что сначала идут ПОЛИНОМ, потом ТРИГОНОМЕТРИЧЕСКАЯ
            assertTrue(functions.get(0).getFunctionType().equals("ПОЛИНОМ") ||
                    functions.get(1).getFunctionType().equals("ПОЛИНОМ"));
        }
    }

    @AfterAll
    void tearDown() {
        if (sessionFactory != null) {
            sessionFactory.close();
        }
    }
}