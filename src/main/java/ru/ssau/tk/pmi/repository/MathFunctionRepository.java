package ru.ssau.tk.pmi.repository;

import ru.ssau.tk.pmi.entity.MathFunction;
import ru.ssau.tk.pmi.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface MathFunctionRepository extends JpaRepository<MathFunction, Long> {

    // Автоматически генерируемые методы
    List<MathFunction> findByOwner(User owner);
    List<MathFunction> findByIsPublicTrue();
    List<MathFunction> findByFunctionType(String functionType);
    List<MathFunction> findByFunctionNameContainingIgnoreCase(String name);

    // Поиск функций по владельцу и типу
    List<MathFunction> findByOwnerAndFunctionType(User owner, String functionType);

    // Кастомный запрос для поиска функций с количеством точек
    @Query("SELECT f FROM MathFunction f WHERE SIZE(f.computedPoints) >= :minPoints")
    List<MathFunction> findFunctionsWithMinPoints(@Param("minPoints") int minPoints);

    // Поиск публичных функций определенного типа
    @Query("SELECT f FROM MathFunction f WHERE f.isPublic = true AND f.functionType = :functionType")
    List<MathFunction> findPublicFunctionsByType(@Param("functionType") String functionType);
}