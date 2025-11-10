package ru.ssau.tk.pmi.dto;
import java.util.Map;

public class DtoMapper {

    public static UserDto mapToUserDto(Map<String, Object> map) {
        if (map == null || map.isEmpty()) return null;

        Long userId = getLongSafe(map, "user_id");
        String username = getStringSafe(map, "username");
        String passwordHash = getStringSafe(map, "password_hash");
        String role = getStringSafe(map, "role");

        if (userId == null || username == null || passwordHash == null || role == null) {
            return null;
        }

        return new UserDto(userId, username, passwordHash, role);
    }

    public static FunctionDto mapToFunctionDto(Map<String, Object> map) {
        if (map == null || map.isEmpty()) return null;

        Long functionId = getLongSafe(map, "function_id");
        String functionName = getStringSafe(map, "function_name");
        String functionDefinition = getStringSafe(map, "function_definition");
        Long ownerId = getLongSafe(map, "owner_id");
        Boolean isPublic = getBooleanSafe(map, "is_public");

        if (functionId == null || functionName == null || functionDefinition == null ||
                ownerId == null || isPublic == null) {
            return null;
        }

        return new FunctionDto(functionId, functionName, functionDefinition, ownerId, isPublic);
    }

    public static ComputedPointDto mapToComputedPointDto(Map<String, Object> map) {
        if (map == null || map.isEmpty()) return null;

        Long pointId = getLongSafe(map, "point_id");
        Long functionId = getLongSafe(map, "function_id");
        Double xValue = getDoubleSafe(map, "x_value");
        Double yValue = getDoubleSafe(map, "y_value");

        if (pointId == null || functionId == null || xValue == null || yValue == null) {
            return null;
        }

        return new ComputedPointDto(pointId, functionId, xValue, yValue);
    }

    public static FunctionAccessDto mapToAccessDto(Map<String, Object> map) {
        if (map == null || map.isEmpty()) return null;

        Long accessId = getLongSafe(map, "access_id");
        Long functionId = getLongSafe(map, "function_id");
        Long userId = getLongSafe(map, "user_id");
        String accessType = getStringSafe(map, "access_type");

        if (accessId == null || functionId == null || userId == null || accessType == null) {
            return null;
        }

        return new FunctionAccessDto(accessId, functionId, userId, accessType);
    }

    // Вспомогательные методы для безопасного получения значений
    private static Long getLongSafe(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Long) return (Long) value;
        if (value instanceof Integer) return ((Integer) value).longValue();
        if (value instanceof Number) return ((Number) value).longValue();
        return null;
    }

    private static String getStringSafe(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value != null ? value.toString() : null;
    }

    private static Double getDoubleSafe(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Double) return (Double) value;
        if (value instanceof Float) return ((Float) value).doubleValue();
        if (value instanceof Integer) return ((Integer) value).doubleValue();
        if (value instanceof Long) return ((Long) value).doubleValue();
        if (value instanceof Number) return ((Number) value).doubleValue();
        return null;
    }

    private static Boolean getBooleanSafe(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Boolean) return (Boolean) value;
        if (value instanceof String) return Boolean.parseBoolean((String) value);
        if (value instanceof Number) return ((Number) value).intValue() != 0;
        return null;
    }
}
