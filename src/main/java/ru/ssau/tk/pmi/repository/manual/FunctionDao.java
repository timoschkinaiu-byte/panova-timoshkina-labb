package ru.ssau.tk.pmi.repository.manual;
import java.util.List;
import java.util.Map;
public interface FunctionDao {
    Long insertFunction(String functionName, String functionDefinition, String functionType, Long ownerId, boolean isPublic);
    Map<String, Object> getFunctionById(Long id);
    List<Map<String, Object>> getAllFunctions();
    void updateFunction(Long id, String name, String definition, String functionType, boolean isPublic);
    void deleteFunction(Long id);
}

