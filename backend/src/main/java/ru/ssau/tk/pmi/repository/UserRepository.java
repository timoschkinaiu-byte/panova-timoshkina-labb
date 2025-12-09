package ru.ssau.tk.pmi.repository;

import ru.ssau.tk.pmi.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Sort;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // Автоматически генерируемые методы
    Optional<User> findByUsername(String username);
    List<User> findByRole(String role);
    List<User> findByUsernameContainingIgnoreCase(String username);
    List<User> findByUsernameContainingIgnoreCase(String username, Sort sort);
    List<User> findByRole(String role, Sort sort);
    Optional<User> findByUsernameAndEnabledTrue(String username);

    boolean existsByUsername(String username);

    // Кастомный запрос для поиска по части имени
    @Query("SELECT u FROM User u WHERE u.username LIKE %:username%")
    List<User> findByUsernameContaining(@Param("username") String username);

    // Поиск пользователей с количеством функций больше указанного
    @Query("SELECT u FROM User u WHERE SIZE(u.functions) > :minFunctions")
    List<User> findUsersWithMoreThanNFunctions(@Param("minFunctions") int minFunctions);


    @Query("SELECT u FROM User u JOIN u.functions f WHERE f.functionType = :functionType")
    List<User> findUsersByFunctionType(@Param("functionType") String functionType);

    @Query("SELECT u FROM User u WHERE u.username LIKE %:pattern% OR u.role LIKE %:pattern%")
    List<User> findByUsernameOrRoleContaining(@Param("pattern") String pattern);
}
