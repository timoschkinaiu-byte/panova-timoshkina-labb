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
class FunctionAccessRepositoryTest {

    private SessionFactory sessionFactory;
    private User testUser1;
    private User testUser2;
    private MathFunction testFunction1;
    private MathFunction testFunction2;

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

            // Создаем тестовых пользователей и функции
            testUser1 = new User("user1", "hash1", "USER");
            testUser2 = new User("user2", "hash2", "ADMIN");
            testFunction1 = new MathFunction("function1", "x^2", "ПОЛИНОМ", testUser1);
            testFunction2 = new MathFunction("function2", "sin(x)", "ТРИГОНОМЕТРИЧЕСКАЯ", testUser1);

            session.persist(testUser1);
            session.persist(testUser2);
            session.persist(testFunction1);
            session.persist(testFunction2);
            session.getTransaction().commit();
        }
    }

    @Test
    void testFindByFunction_Database() {
        // Подготовка данных - создаем права доступа
        try (Session session = sessionFactory.openSession()) {
            session.beginTransaction();
            FunctionAccess access1 = new FunctionAccess("READ", testFunction1, testUser1);
            FunctionAccess access2 = new FunctionAccess("WRITE", testFunction1, testUser2);
            session.persist(access1);
            session.persist(access2);
            session.flush();

        // Тестируем поиск прав доступа по функции
            List<FunctionAccess> accesses = session.createQuery(
                            "FROM FunctionAccess WHERE function.functionId = :functionId", FunctionAccess.class)
                    .setParameter("functionId", testFunction1.getFunctionId())
                    .getResultList();

            assertEquals(2, accesses.size());
            assertTrue(accesses.stream().allMatch(access ->
                    access.getFunction().getFunctionId().equals(testFunction1.getFunctionId())));

            // Проверяем типы доступа
            assertTrue(accesses.stream().anyMatch(access -> "READ".equals(access.getAccessType())));
            assertTrue(accesses.stream().anyMatch(access -> "WRITE".equals(access.getAccessType())));
            session.getTransaction().rollback();
        }
    }

    @Test
    void testFindByUser_Database() {
        // Подготовка данных - создаем права доступа
        try (Session session = sessionFactory.openSession()) {
            session.beginTransaction();
            FunctionAccess access1 = new FunctionAccess("READ", testFunction1, testUser1);
            FunctionAccess access2 = new FunctionAccess("EXECUTE", testFunction2, testUser1);
            session.persist(access1);
            session.persist(access2);
            session.flush();


            List<FunctionAccess> userAccesses = session.createQuery(
                            "FROM FunctionAccess WHERE user.userId = :userId", FunctionAccess.class)
                    .setParameter("userId", testUser1.getUserId())
                    .getResultList();

            assertEquals(2, userAccesses.size());
            assertTrue(userAccesses.stream().allMatch(access ->
                    access.getUser().getUserId().equals(testUser1.getUserId())));

            // Проверяем разные функции
            assertTrue(userAccesses.stream().anyMatch(access ->
                    access.getFunction().getFunctionId().equals(testFunction1.getFunctionId())));
            assertTrue(userAccesses.stream().anyMatch(access ->
                    access.getFunction().getFunctionId().equals(testFunction2.getFunctionId())));
            session.getTransaction().rollback();
        }
    }

    @Test
    void testFindByFunctionAndUser_Database() {
        // Подготовка данных - создаем права доступа
        try (Session session = sessionFactory.openSession()) {
            session.beginTransaction();
            FunctionAccess access1 = new FunctionAccess("READ", testFunction1, testUser1);
            FunctionAccess access2 = new FunctionAccess("WRITE", testFunction1, testUser2);
            session.persist(access1);
            session.persist(access2);
            session.flush();

            FunctionAccess access = session.createQuery(
                            "FROM FunctionAccess WHERE function.functionId = :functionId AND user.userId = :userId",
                            FunctionAccess.class)
                    .setParameter("functionId", testFunction1.getFunctionId())
                    .setParameter("userId", testUser1.getUserId())
                    .uniqueResult();

            assertNotNull(access);
            assertEquals("READ", access.getAccessType());
            assertEquals(testFunction1.getFunctionId(), access.getFunction().getFunctionId());
            assertEquals(testUser1.getUserId(), access.getUser().getUserId());
            session.getTransaction().rollback();
        }
    }

    @Test
    void testExistsByFunctionAndUser_Database() {
        // Подготовка данных - создаем права доступа
        try (Session session = sessionFactory.openSession()) {
            session.beginTransaction();
            FunctionAccess access = new FunctionAccess("READ", testFunction1, testUser1);
            session.persist(access);
            session.flush();

            // Проверяем существующий доступ
            Long countExists = session.createQuery(
                            "SELECT COUNT(*) FROM FunctionAccess WHERE function.functionId = :functionId AND user.userId = :userId",
                            Long.class)
                    .setParameter("functionId", testFunction1.getFunctionId())
                    .setParameter("userId", testUser1.getUserId())
                    .uniqueResult();

            assertTrue(countExists > 0);

            // Проверяем несуществующий доступ
            Long countNotExists = session.createQuery(
                            "SELECT COUNT(*) FROM FunctionAccess WHERE function.functionId = :functionId AND user.userId = :userId",
                            Long.class)
                    .setParameter("functionId", testFunction1.getFunctionId())
                    .setParameter("userId", testUser2.getUserId())
                    .uniqueResult();

            assertEquals(0, countNotExists);

            session.getTransaction().rollback();
        }
    }
}