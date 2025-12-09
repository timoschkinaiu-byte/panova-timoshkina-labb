package ru.ssau.tk.pmi.repository;

import ru.ssau.tk.pmi.entity.ComputedPoint;
import ru.ssau.tk.pmi.entity.MathFunction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ComputedPointRepository extends JpaRepository<ComputedPoint, Long> {

    // Стандартные методы по именам (Spring Data JPA)
    List<ComputedPoint> findByFunction(MathFunction function);
    long deleteByFunction(MathFunction function);
    List<ComputedPoint> findByxValueBetween(Double xFrom, Double xTo);
    List<ComputedPoint> findByyValueBetween(Double yFrom, Double yTo);


    // Кастомные JPQL-запросы
    @Query("SELECT cp FROM ComputedPoint cp WHERE cp.xValue = :xValue AND cp.yValue = :yValue")
    List<ComputedPoint> findByExactValues(@Param("xValue") Double xValue, @Param("yValue") Double yValue);

    @Query("SELECT cp FROM ComputedPoint cp WHERE cp.function.functionName = :functionName")
    List<ComputedPoint> findByFunctionName(@Param("functionName") String functionName);

    @Query("SELECT cp FROM ComputedPoint cp WHERE cp.function = :function AND cp.xValue BETWEEN :xFrom AND :xTo ORDER BY cp.xValue")
    List<ComputedPoint> findByFunctionAndXValueBetween(
            @Param("function") MathFunction function,
            @Param("xFrom") Double xFrom,
            @Param("xTo") Double xTo
    );


    @Query("SELECT COUNT(cp), MIN(cp.xValue), MAX(cp.xValue), MIN(cp.yValue), MAX(cp.yValue) " +
            "FROM ComputedPoint cp WHERE cp.function = :function")
    Object[] getFunctionPointsStatistics(@Param("function") MathFunction function);

    @Query("SELECT cp FROM ComputedPoint cp WHERE cp.function = :function AND cp.yValue = :yValue")
    List<ComputedPoint> findByFunctionAndyValue(@Param("function") MathFunction function,
                                                @Param("yValue") Double yValue);

    @Query("SELECT cp FROM ComputedPoint cp WHERE cp.function = :function ORDER BY cp.xValue ASC")
    List<ComputedPoint> findByFunctionOrderByxValue(@Param("function") MathFunction function);

}

