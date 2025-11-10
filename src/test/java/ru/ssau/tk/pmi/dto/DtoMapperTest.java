package ru.ssau.tk.pmi.dto;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class DtoMapperTest {

    private static final Logger logger = LoggerFactory.getLogger(DtoMapperTest.class);

    @Test
    void testUserDtoMapping() {
        Map<String, Object> data = Map.of(
                "user_id", 1L,
                "username", "testuser",
                "password_hash", "hash",
                "role", "ADMIN"
        );
        UserDto dto = DtoMapper.mapToUserDto(data);
        logger.info("Mapped UserDTO: {}", dto);
        assertEquals("testuser", dto.getUsername());
        assertEquals("ADMIN", dto.getRole());
    }

    @Test
    void testFunctionDtoMapping() {
        Map<String, Object> data = Map.of(
                "function_id", 10L,
                "function_name", "f(x)=x^2",
                "function_definition", "x*x",
                "owner_id", 1L,
                "is_public", true
        );
        FunctionDto dto = DtoMapper.mapToFunctionDto(data);
        logger.info("Mapped FunctionDTO: {}", dto);
        assertTrue(dto.isPublic());
        assertEquals("f(x)=x^2", dto.getFunctionName());
        assertEquals(1L, dto.getOwnerId());
    }

    @Test
    void testFunctionDtoMappingWithBooleanString() {
        Map<String, Object> data = Map.of(
                "function_id", 10L,
                "function_name", "f(x)=x^2",
                "function_definition", "x*x",
                "owner_id", 1L,
                "is_public", "true"  // тестируем строковое значение
        );
        FunctionDto dto = DtoMapper.mapToFunctionDto(data);
        logger.info("Mapped FunctionDTO with string boolean: {}", dto);
        assertTrue(dto.isPublic());
    }

    @Test
    void testComputedPointDtoMapping() {
        Map<String, Object> data = Map.of(
                "point_id", 5L,
                "function_id", 10L,
                "x_value", 2.0,
                "y_value", 4.0
        );
        ComputedPointDto dto = DtoMapper.mapToComputedPointDto(data);
        logger.info("Mapped ComputedPointDTO: {}", dto);
        assertEquals(2.0, dto.getXValue());
        assertEquals(4.0, dto.getYValue());
        assertEquals(10L, dto.getFunctionId());
    }

    @Test
    void testComputedPointDtoMappingWithIntegerValues() {
        Map<String, Object> data = Map.of(
                "point_id", 5L,
                "function_id", 10L,
                "x_value", 2,  // Integer вместо Double
                "y_value", 4   // Integer вместо Double
        );
        ComputedPointDto dto = DtoMapper.mapToComputedPointDto(data);
        logger.info("Mapped ComputedPointDTO with integer values: {}", dto);
        assertEquals(2.0, dto.getXValue());
        assertEquals(4.0, dto.getYValue());
    }

    @Test
    void testAccessDtoMapping() {
        Map<String, Object> data = Map.of(
                "access_id", 20L,
                "function_id", 10L,
                "user_id", 1L,
                "access_type", "READ"
        );
        FunctionAccessDto dto = DtoMapper.mapToAccessDto(data);
        logger.info("Mapped FunctionAccessDTO: {}", dto);
        assertEquals("READ", dto.getAccessType());
        assertEquals(10L, dto.getFunctionId());
        assertEquals(1L, dto.getUserId());
    }

    @Test
    void testNullMapping() {
        assertNull(DtoMapper.mapToUserDto(null));
        assertNull(DtoMapper.mapToFunctionDto(null));
        assertNull(DtoMapper.mapToComputedPointDto(null));
        assertNull(DtoMapper.mapToAccessDto(null));
        logger.info("Null mapping test passed for all DTOs");
    }

    @Test
    void testEmptyMapMapping() {
        Map<String, Object> emptyMap = new HashMap<>(); // Используем HashMap вместо Map.of()

        UserDto userDto = DtoMapper.mapToUserDto(emptyMap);
        FunctionDto functionDto = DtoMapper.mapToFunctionDto(emptyMap);
        ComputedPointDto pointDto = DtoMapper.mapToComputedPointDto(emptyMap);
        FunctionAccessDto accessDto = DtoMapper.mapToAccessDto(emptyMap);

        assertNull(userDto);
        assertNull(functionDto);
        assertNull(pointDto);
        assertNull(accessDto);
        logger.info("Empty map mapping test passed");
    }

    @Test
    void testPartialMapMapping() {
        // Map с отсутствующими обязательными полями
        Map<String, Object> partialMap = new HashMap<>();
        partialMap.put("function_id", 10L);
        partialMap.put("function_name", "test");
        // Нет owner_id и is_public

        FunctionDto dto = DtoMapper.mapToFunctionDto(partialMap);
        assertNull(dto); // Должен вернуть null из-за отсутствующих полей
        logger.info("Partial map mapping test passed");
    }
}
