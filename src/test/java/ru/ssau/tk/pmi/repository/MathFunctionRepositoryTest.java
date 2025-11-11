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
class MathFunctionRepositoryTest {

    private SessionFactory sessionFactory;
    private User testUser;

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

            // Создаем тестового пользователя
            testUser = new User("function_owner", "hash_owner", "USER");
            session.persist(testUser);
            session.getTransaction().commit();
        }
    }

    @Test
    void testFindByFunctionType_RealDatabase() {
        // Подготовка данных
        try (Session session = sessionFactory.openSession()) {
            session.beginTransaction();
            MathFunction polyFunction = new MathFunction("poly_func", "x^2", "ПОЛИНОМ", testUser);
            MathFunction trigFunction = new MathFunction("trig_func", "sin(x)", "ТРИГОНОМЕТРИЧЕСКАЯ", testUser);
            session.persist(polyFunction);
            session.persist(trigFunction);
            session.flush();

            // Тестируем поиск по типу функции
            List<MathFunction> polyFunctions = session.createQuery(
                            "FROM MathFunction WHERE functionType = :type", MathFunction.class)
                    .setParameter("type", "ПОЛИНОМ")
                    .getResultList();

            assertEquals(1, polyFunctions.size());
            assertEquals("poly_func", polyFunctions.get(0).getFunctionName());

            session.getTransaction().rollback();
        }
    }

    @Test
    void testFindByOwner_RealDatabase() {
        try (Session session = sessionFactory.openSession()) {
            // Начинаем транзакцию, которая откатится после теста
            session.beginTransaction();

            // Подготовка данных
            MathFunction func1 = new MathFunction("func_1", "x^2", "ПОЛИНОМ", testUser);
            MathFunction func2 = new MathFunction("func_2", "sin(x)", "ТРИГОНОМЕТРИЧЕСКАЯ", testUser);
            session.persist(func1);
            session.persist(func2);
            session.flush(); // Принудительно сохраняем в БД

            // Тестируем поиск функций по владельцу
            List<MathFunction> userFunctions = session.createQuery(
                            "FROM MathFunction WHERE owner.userId = :ownerId", MathFunction.class)
                    .setParameter("ownerId", testUser.getUserId())
                    .getResultList();

            assertEquals(2, userFunctions.size(),
                    "Должно быть 2 функции у пользователя. Найдено: " + userFunctions.size());
            assertTrue(userFunctions.stream().allMatch(func ->
                    func.getOwner().getUserId().equals(testUser.getUserId())));

            // Откатываем транзакцию - данные не сохранятся в БД
            session.getTransaction().rollback();
        }
    }


    @Test
    void testFindByFunctionNameContaining_RealDatabase() {
        // Подготовка данных
        try (Session session = sessionFactory.openSession()) {
            session.beginTransaction();
            MathFunction quadraticFunc = new MathFunction("Квадратичная_функция", "x^2", "ПОЛИНОМ", testUser);
            MathFunction linearFunc = new MathFunction("Линейная_функция", "2x+1", "ПОЛИНОМ", testUser);
            session.persist(quadraticFunc);
            session.persist(linearFunc);
            session.getTransaction().commit();
        }

        // Тестируем поиск по части имени
        try (Session session = sessionFactory.openSession()) {
            List<MathFunction> functions = session.createQuery(
                            "FROM MathFunction WHERE functionName LIKE :pattern", MathFunction.class)
                    .setParameter("pattern", "%Квадратичная%")
                    .getResultList();

            assertEquals(1, functions.size());
            assertEquals("Квадратичная_функция", functions.get(0).getFunctionName());
        }
    }

    @AfterAll
    void tearDown() {
        if (sessionFactory != null) {
            sessionFactory.close();
        }
    }
}