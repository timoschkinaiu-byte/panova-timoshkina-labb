
package ru.ssau.tk.pmi.entity;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FunctionTest {

    @Test
    void testFunctionEntityCreation() {
        // Arrange
        User owner = new User("owner", "hash", "USER");
        owner.setUserId(1L);

        // Act
        MathFunction function = new MathFunction("Test Function", "x^2 + 1", "POLYNOMIAL", owner);

        // Assert
        assertNotNull(function);
        assertEquals("Test Function", function.getFunctionName());
        assertEquals("x^2 + 1", function.getFunctionDefinition());
        assertEquals("POLYNOMIAL", function.getFunctionType());
        assertEquals(owner, function.getOwner());
        assertFalse(function.getIsPublic());
        assertNotNull(function.getCreatedAt());
        assertNotNull(function.getUpdatedAt());
    }

    @Test
    void testFunctionEntityCollectionsInitialization() {
        // Arrange
        User owner = new User("owner", "hash", "USER");
        MathFunction function = new MathFunction();

        // Act & Assert
        assertNotNull(function.getComputedPoints());
        assertNotNull(function.getFunctionAccesses());
        assertTrue(function.getComputedPoints().isEmpty());
        assertTrue(function.getFunctionAccesses().isEmpty());
    }
}