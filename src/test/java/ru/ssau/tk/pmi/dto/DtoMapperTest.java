
package ru.ssau.tk.pmi.dto;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
                "function_type", "POLYNOMIAL",
                "owner_id", 1L,
                "is_public", true
        );
        FunctionDto dto = DtoMapper.mapToFunctionDto(data);
        logger.info("Mapped FunctionDTO: {}", dto);
        assertTrue(dto.isPublic());
        assertEquals("POLYNOMIAL", dto.getFunctionType());
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
    }

    @Test
    void testNullMapping() {
        assertNull(DtoMapper.mapToUserDto(null));
        assertNull(DtoMapper.mapToFunctionDto(null));
        logger.info("Null mapping test passed for UserDTO and FunctionDTO");
    }
}
