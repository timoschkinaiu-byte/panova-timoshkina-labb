package ru.ssau.tk.pmi.dto;
import java.util.Map;

public class DtoMapper {

    public static UserDto mapToUserDto(Map<String, Object> map) {
        if (map == null) return null;
        return new UserDto(
                (Long) map.get("user_id"),
                (String) map.get("username"),
                (String) map.get("password_hash"),
                (String) map.get("role")
        );
    }

    public static FunctionDto mapToFunctionDto(Map<String, Object> map) {
        if (map == null) return null;
        return new FunctionDto(
                (Long) map.get("function_id"),
                (String) map.get("function_name"),
                (String) map.get("function_definition"),
                (String) map.get("function_type"),
                (Long) map.get("owner_id"),
                map.get("is_public") instanceof Boolean ? (Boolean) map.get("is_public")
                        : Boolean.parseBoolean(map.get("is_public").toString())
        );
    }

    public static ComputedPointDto mapToComputedPointDto(Map<String, Object> map) {
        if (map == null) return null;
        return new ComputedPointDto(
                (Long) map.get("point_id"),
                (Long) map.get("function_id"),
                ((Number) map.get("x_value")).doubleValue(),
                ((Number) map.get("y_value")).doubleValue()
        );
    }

    public static FunctionAccessDto mapToAccessDto(Map<String, Object> map) {
        if (map == null) return null;
        return new FunctionAccessDto(
                (Long) map.get("access_id"),
                (Long) map.get("function_id"),
                (Long) map.get("user_id"),
                (String) map.get("access_type")
        );
    }
}
