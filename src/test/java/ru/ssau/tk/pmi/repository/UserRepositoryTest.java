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
import java.util.Optional;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UserRepositoryTest {

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

        // Очистка базы перед тестами
        try (Session session = sessionFactory.openSession()) {
            session.beginTransaction();
            session.createMutationQuery("DELETE FROM ComputedPoint").executeUpdate();
            session.createMutationQuery("DELETE FROM FunctionAccess").executeUpdate();
            session.createMutationQuery("DELETE FROM MathFunction").executeUpdate();
            session.createMutationQuery("DELETE FROM User").executeUpdate();
            session.getTransaction().commit();
        }
    }

    @Test
    void testFindByUsername_RealDatabase() {
        // Подготовка данных
        try (Session session = sessionFactory.openSession()) {
            session.beginTransaction();
            User testUser = new User("test_user_1", "hash_1", "USER");
            session.persist(testUser);
            session.getTransaction().commit();
        }

        // Тестируем поиск по имени пользователя
        try (Session session = sessionFactory.openSession()) {
            User user = session.createQuery("FROM User WHERE username = :username", User.class)
                    .setParameter("username", "test_user_1")
                    .uniqueResult();

            assertNotNull(user);
            assertEquals("test_user_1", user.getUsername());
            assertEquals("USER", user.getRole());
        }
    }


    @Test
    void testFindByRole_RealDatabase() {
        try (Session session = sessionFactory.openSession()) {
            session.beginTransaction();


            User adminUser = new User("admin_user", "hash_admin", "ADMIN");
            User regularUser = new User("regular_user", "hash_user", "USER");
            session.persist(adminUser);
            session.persist(regularUser);
            session.flush();

            List<User> admins = session.createQuery("FROM User WHERE role = :role", User.class)
                    .setParameter("role", "ADMIN")
                    .getResultList();

            assertEquals(1, admins.size());
            assertEquals("admin_user", admins.get(0).getUsername());

            session.getTransaction().rollback();
        }
    }

    @Test
    void testSaveAndFindById_RealDatabase() {
        User newUser = new User("new_test_user", "new_hash", "USER");

        // Сохраняем пользователя
        try (Session session = sessionFactory.openSession()) {
            session.beginTransaction();
            session.persist(newUser);
            session.getTransaction().commit();
        }

        // Ищем по ID
        try (Session session = sessionFactory.openSession()) {
            User foundUser = session.find(User.class, newUser.getUserId());

            assertNotNull(foundUser);
            assertEquals("new_test_user", foundUser.getUsername());
            assertEquals("USER", foundUser.getRole());
        }
    }

    @AfterAll
    void tearDown() {
        if (sessionFactory != null) {
            sessionFactory.close();
        }
    }
}