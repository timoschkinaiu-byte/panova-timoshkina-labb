

package ru.ssau.tk.pmi.entity;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestPropertySource(locations = "classpath:application-test.properties")
@Transactional
class ComputedPointTest {


    @Test
    void testComputedPointCreation() {
        // Создаем пользователя и функцию
        User owner = new User();
        owner.setUsername("test_owner");
        owner.setPasswordHash("hash");
        owner.setRole("USER");

        MathFunction function = new MathFunction();
        function.setFunctionName("Test Function");
        function.setFunctionDefinition("x^2");
        function.setFunctionType("POLYNOMIAL");
        function.setIsPublic(true);
        function.setOwner(owner);

        // Создаем точку
        ComputedPoint point = new ComputedPoint();
        point.setXValue(2.5);
        point.setYValue(6.25);
        point.setFunction(function);

        // Проверяем значения
        assertEquals(2.5, point.getXValue());
        assertEquals(6.25, point.getYValue());
        assertEquals(function, point.getFunction());
        assertEquals("Test Function", point.getFunction().getFunctionName());
    }


    @Test
    void testDefaultConstructorAndSetters() {
        // Arrange & Act
        ComputedPoint point = new ComputedPoint();
        point.setPointId(1L);
        point.setXValue(2.5);
        point.setYValue(6.25);

        MathFunction function = new MathFunction();
        function.setFunctionId(10L);
        function.setFunctionName("Test Function");
        point.setFunction(function);

        // Assert
        assertEquals(1L, point.getPointId());
        assertEquals(2.5, point.getXValue());
        assertEquals(6.25, point.getYValue());
        assertEquals(function, point.getFunction());
        assertEquals(10L, point.getFunction().getFunctionId());
    }

    @Test
    void testParameterizedConstructor() {
        // Arrange
        MathFunction function = new MathFunction();
        function.setFunctionId(5L);
        function.setFunctionName("Quadratic");

        // Act
        ComputedPoint point = new ComputedPoint(3.0, 9.0, function);

        // Assert
        assertNull(point.getPointId()); // ID should be null until saved
        assertEquals(3.0, point.getXValue());
        assertEquals(9.0, point.getYValue());
        assertEquals(function, point.getFunction());
        assertEquals("Quadratic", point.getFunction().getFunctionName());
    }

    @Test
    void testEqualsAndHashCode() {
        // Arrange
        MathFunction function1 = new MathFunction();
        function1.setFunctionId(1L);

        MathFunction function2 = new MathFunction();
        function2.setFunctionId(2L);

        // Test 1: Same IDs
        ComputedPoint point1 = new ComputedPoint();
        point1.setPointId(100L);
        point1.setXValue(1.0);
        point1.setYValue(2.0);
        point1.setFunction(function1);

        ComputedPoint point2 = new ComputedPoint();
        point2.setPointId(100L);
        point2.setXValue(5.0); // Different values but same ID
        point2.setYValue(6.0);
        point2.setFunction(function2);

        // Test 2: Different IDs, same function and xValue
        ComputedPoint point3 = new ComputedPoint();
        point3.setPointId(null);
        point3.setXValue(1.0);
        point3.setYValue(2.0);
        point3.setFunction(function1);

        ComputedPoint point4 = new ComputedPoint();
        point4.setPointId(null);
        point4.setXValue(1.0);
        point4.setYValue(2.0);
        point4.setFunction(function1);

        ComputedPoint point5 = new ComputedPoint();
        point5.setPointId(null);
        point5.setXValue(1.0); // Same x but different function
        point5.setYValue(2.0);
        point5.setFunction(function2);

        // Assert
        // Same ID should be equal regardless of other fields
        assertEquals(point1, point2);
        assertEquals(point1.hashCode(), point2.hashCode());

        // Same function and xValue should be equal when no ID
        assertEquals(point3, point4);
        assertEquals(point3.hashCode(), point4.hashCode());

        // Different function should not be equal
        assertNotEquals(point3, point5);

        // Null comparison
        assertNotEquals(point1, null);

        // Self comparison
        assertEquals(point1, point1);

        // Different type
        assertNotEquals(point1, "string");
    }

    @Test
    void testToString() {
        // Arrange
        MathFunction function = new MathFunction();
        function.setFunctionId(15L);
        function.setFunctionName("Sine Function");

        ComputedPoint point = new ComputedPoint();
        point.setPointId(25L);
        point.setXValue(Math.PI);
        point.setYValue(0.0);
        point.setFunction(function);

        // Act
        String result = point.toString();

        // Assert
        assertTrue(result.contains("pointId=25"));
        assertTrue(result.contains("functionId=15"));
        assertTrue(result.contains("functionName=Sine Function"));
        assertTrue(result.contains("xValue=3.1415")); // Should contain PI
        assertTrue(result.contains("yValue=0.0"));

        // Test with null function
        ComputedPoint point2 = new ComputedPoint();
        point2.setPointId(30L);
        point2.setXValue(1.0);
        point2.setYValue(1.0);
        // function is null

        String result2 = point2.toString();
        assertTrue(result2.contains("pointId=30"));
        assertTrue(result2.contains("functionId=null"));
        assertTrue(result2.contains("functionName=null"));
    }

    @Test
    void testEdgeCasesAndNullSafety() {
        // Test with null values
        ComputedPoint point = new ComputedPoint();
        point.setXValue(null);
        point.setYValue(null);
        point.setFunction(null);

        assertNull(point.getXValue());
        assertNull(point.getYValue());
        assertNull(point.getFunction());

        // Test with extreme values
        point.setXValue(Double.MAX_VALUE);
        point.setYValue(Double.MIN_VALUE);

        assertEquals(Double.MAX_VALUE, point.getXValue());
        assertEquals(Double.MIN_VALUE, point.getYValue());

        // Test with negative values
        point.setXValue(-10.5);
        point.setYValue(-20.7);

        assertEquals(-10.5, point.getXValue());
        assertEquals(-20.7, point.getYValue());

        // Test toString with null function doesn't throw exception
        assertDoesNotThrow(() -> point.toString());
    }

}