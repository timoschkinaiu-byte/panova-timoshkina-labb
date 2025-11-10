package ru.ssau.tk.pmi.repository.manual;

import java.util.List;
import java.util.Map;
public interface ComputedPointDao {
    Long insertComputedPoint(Long functionId, double xValue, double yValue);
    Map<String, Object> getComputedPointById(Long id);
    List<Map<String, Object>> getComputedPointsByFunctionId(Long functionId);
    List<Map<String, Object>> getAllComputedPoints();
    void updateComputedPoint(Long id, double xValue, double yValue);
    void deleteComputedPoint(Long id);
}
