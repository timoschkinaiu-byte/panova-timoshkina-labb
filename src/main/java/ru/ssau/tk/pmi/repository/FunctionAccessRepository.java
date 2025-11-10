package ru.ssau.tk.pmi.repository;

import ru.ssau.tk.pmi.entity.FunctionAccess;
import ru.ssau.tk.pmi.entity.MathFunction;
import ru.ssau.tk.pmi.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface FunctionAccessRepository extends JpaRepository<FunctionAccess, Long> {

    // Автоматически генерируемые методы
    List<FunctionAccess> findByFunction(MathFunction function);
    List<FunctionAccess> findByUser(User user);
    Optional<FunctionAccess> findByFunctionAndUser(MathFunction function, User user);

    // Проверка существования доступа
    boolean existsByFunctionAndUser(MathFunction function, User user);

    // Поиск всех функций, к которым пользователь имеет доступ (кроме своих)
    @Query("SELECT fa.function FROM FunctionAccess fa WHERE fa.user = :user AND fa.function.owner != :user")
    List<MathFunction> findSharedFunctionsForUser(@Param("user") User user);

    // Поиск пользователей с доступом к функции (кроме владельца)
    @Query("SELECT fa.user FROM FunctionAccess fa WHERE fa.function = :function AND fa.function.owner != fa.user")
    List<User> findUsersWithAccessToFunction(@Param("function") MathFunction function);

    // Удаление доступа по функции и пользователю
    void deleteByFunctionAndUser(MathFunction function, User user);
}
