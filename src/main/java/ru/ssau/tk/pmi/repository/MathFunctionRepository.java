package ru.ssau.tk.pmi.repository;

import ru.ssau.tk.pmi.entity.MathFunction;
import ru.ssau.tk.pmi.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Sort;
import java.util.List;

@Repository
public interface MathFunctionRepository extends JpaRepository<MathFunction, Long> {

    // Автоматически генерируемые методы
    List<MathFunction> findByOwner(User owner);
    List<MathFunction> findByIsPublicTrue();
    List<MathFunction> findByFunctionNameContainingIgnoreCase(String name);
    List<MathFunction> findByFunctionNameContainingIgnoreCase(String name, Sort sort);
    List<MathFunction> findByFunctionType(String type);
    List<MathFunction> findByFunctionType(String type, Sort sort);
    // Поиск функций по владельцу и типу
    List<MathFunction> findByOwnerAndFunctionType(User owner, String functionType);

    // Кастомный запрос для поиска функций с количеством точек
    @Query("SELECT f FROM MathFunction f WHERE SIZE(f.computedPoints) >= :minPoints")
    List<MathFunction> findFunctionsWithMinPoints(@Param("minPoints") int minPoints);

    // Поиск публичных функций определенного типа
    @Query("SELECT f FROM MathFunction f WHERE f.isPublic = true AND f.functionType = :functionType")
    List<MathFunction> findPublicFunctionsByType(@Param("functionType") String functionType);

    @Query("SELECT f FROM MathFunction f WHERE f.functionName LIKE %:name% AND f.functionType = :type")
    List<MathFunction> findByNameAndType(@Param("name") String name, @Param("type") String type);

    @Query("SELECT f FROM MathFunction f WHERE f.owner.username = :username")
    List<MathFunction> findByOwnerUsername(@Param("username") String username);



    // Поиск функций доступных пользователю (свои + публичные)
    @Query("SELECT f FROM MathFunction f WHERE f.owner.userId = :userId OR f.isPublic = true")
    List<MathFunction> findAccessibleFunctions(@Param("userId") Long userId);

    // Поиск по имени с учетом доступности
    @Query("SELECT f FROM MathFunction f WHERE (f.owner.userId = :userId OR f.isPublic = true) AND LOWER(f.functionName) LIKE LOWER(CONCAT('%', :name, '%'))")
    List<MathFunction> findByFunctionNameContainingIgnoreCaseAndAccessible(@Param("name") String name, @Param("userId") Long userId);

    // Поиск по типу с учетом доступности
    @Query("SELECT f FROM MathFunction f WHERE (f.owner.userId = :userId OR f.isPublic = true) AND f.functionType = :type")
    List<MathFunction> findByFunctionTypeAndAccessible(@Param("type") String type, @Param("userId") Long userId);

    // Поиск по имени и типу с учетом доступности
    @Query("SELECT f FROM MathFunction f WHERE (f.owner.userId = :userId OR f.isPublic = true) AND LOWER(f.functionName) LIKE LOWER(CONCAT('%', :name, '%')) AND f.functionType = :type")
    List<MathFunction> findByNameAndTypeForUser(@Param("name") String name, @Param("type") String type, @Param("userId") Long userId);

    // Поиск публичных функций по имени
    @Query("SELECT f FROM MathFunction f WHERE f.isPublic = true AND LOWER(f.functionName) LIKE LOWER(CONCAT('%', :name, '%'))")
    List<MathFunction> findPublicFunctionsByName(@Param("name") String name);

    // Поиск функций пользователя по имени
    @Query("SELECT f FROM MathFunction f WHERE f.owner.userId = :userId AND LOWER(f.functionName) LIKE LOWER(CONCAT('%', :name, '%'))")
    List<MathFunction> findUserFunctionsByName(@Param("name") String name, @Param("userId") Long userId);
}