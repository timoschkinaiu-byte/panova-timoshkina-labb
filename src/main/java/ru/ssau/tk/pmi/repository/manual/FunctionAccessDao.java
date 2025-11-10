package ru.ssau.tk.pmi.repository.manual;

import java.util.List;
import java.util.Map;

public interface FunctionAccessDao {
    Long insertAccess(Long functionId, Long userId, String accessType);
    Map<String, Object> getAccessById(Long accessId);
    List<Map<String, Object>> getAccessByFunctionAndUser(Long functionId, Long userId);
    List<Map<String, Object>> getAllAccess();
    void updateAccess(Long accessId, String accessType);
    void deleteAccess(Long accessId);
}
