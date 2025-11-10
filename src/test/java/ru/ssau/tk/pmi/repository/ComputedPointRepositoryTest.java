package ru.ssau.tk.pmi.repository;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.junit.jupiter.api.*;
import ru.ssau.tk.pmi.entity.User;
import ru.ssau.tk.pmi.entity.MathFunction;
import ru.ssau.tk.pmi.entity.ComputedPoint;
import ru.ssau.tk.pmi.entity.FunctionAccess;

import java.util.List;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ComputedPointRepositoryTest {

    private SessionFactory sessionFactory;
    private User testUser;
    private MathFunction testFunction;

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

        // Очистка и подготовка данных
        try (Session session = sessionFactory.openSession()) {
            session.beginTransaction();
            session.createMutationQuery("DELETE FROM ComputedPoint").executeUpdate();
            session.createMutationQuery("DELETE FROM FunctionAccess").executeUpdate();
            session.createMutationQuery("DELETE FROM MathFunction").executeUpdate();
            session.createMutationQuery("DELETE FROM User").executeUpdate();

            // Создаем тестового пользователя и функцию
            testUser = new User("point_owner", "hash_owner", "USER");
            testFunction = new MathFunction("test_function", "x^2", "ПОЛИНОМ", testUser);
            session.persist(testUser);
            session.persist(testFunction);
            session.getTransaction().commit();
        }
    }

    @Test
    void testFindByFunction_RealDatabase() {
        // Подготовка данных
        try (Session session = sessionFactory.openSession()) {
            session.beginTransaction();
            ComputedPoint point1 = new ComputedPoint(1.0, 1.0, testFunction);
            ComputedPoint point2 = new ComputedPoint(2.0, 4.0, testFunction);
            session.persist(point1);
            session.persist(point2);
            session.flush();

            List<ComputedPoint> points = session.createQuery(
                            "FROM ComputedPoint WHERE function.functionId = :functionId", ComputedPoint.class)
                    .setParameter("functionId", testFunction.getFunctionId())
                    .getResultList();

            assertEquals(2, points.size());
            assertTrue(points.stream().allMatch(point ->
                    point.getFunction().getFunctionId().equals(testFunction.getFunctionId())));
            session.getTransaction().rollback();
        }
    }

    @Test
    void testFindByXValueBetween_RealDatabase() {
        // Подготовка данных
        try (Session session = sessionFactory.openSession()) {
            session.beginTransaction();
            ComputedPoint point1 = new ComputedPoint(1.0, 1.0, testFunction);
            ComputedPoint point2 = new ComputedPoint(5.0, 25.0, testFunction);
            ComputedPoint point3 = new ComputedPoint(10.0, 100.0, testFunction);
            session.persist(point1);
            session.persist(point2);
            session.persist(point3);
            session.flush();

            List<ComputedPoint> points = session.createQuery(
                            "FROM ComputedPoint WHERE xValue BETWEEN :from AND :to", ComputedPoint.class)
                    .setParameter("from", 2.0)
                    .setParameter("to", 8.0)
                    .getResultList();

            assertEquals(1, points.size());
            assertEquals(5.0, points.get(0).getXValue());
            assertEquals(25.0, points.get(0).getYValue());

            session.getTransaction().rollback();
        }
    }

    @Test
    void testFindByYValue_RealDatabase() {
        // Подготовка данных
        try (Session session = sessionFactory.openSession()) {
            session.beginTransaction();
            ComputedPoint point1 = new ComputedPoint(2.0, 4.0, testFunction);
            ComputedPoint point2 = new ComputedPoint(3.0, 9.0, testFunction);
            ComputedPoint point3 = new ComputedPoint(4.0, 16.0, testFunction);
            session.persist(point1);
            session.persist(point2);
            session.persist(point3);
            session.flush();

            List<ComputedPoint> points = session.createQuery(
                            "FROM ComputedPoint WHERE yValue = :yValue", ComputedPoint.class)
                    .setParameter("yValue", 9.0)
                    .getResultList();

            assertEquals(1, points.size());
            assertEquals(3.0, points.get(0).getXValue());
            assertEquals(9.0, points.get(0).getYValue());
            session.getTransaction().rollback();
        }
    }

    @AfterAll
    void tearDown() {
        if (sessionFactory != null) {
            sessionFactory.close();
        }
    }
}