package ru.ssau.tk.pmi.repository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import ru.ssau.tk.pmi.entity.MathFunction;
import ru.ssau.tk.pmi.entity.User;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MathFunctionRepositoryTest {

    @Mock
    private MathFunctionRepository mathFunctionRepository;

    private User testUser;
    private MathFunction testFunction1;
    private MathFunction testFunction2;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        testUser = new User("owner", "hash", "USER");
        testUser.setUserId(1L);

        testFunction1 = new MathFunction("function1", "x^2", "POLYNOMIAL", testUser);
        testFunction1.setFunctionId(1L);

        testFunction2 = new MathFunction("function2", "sin(x)", "TRIGONOMETRIC", testUser);
        testFunction2.setFunctionId(2L);
    }

    @Test
    void testFindByOwner() {
        List<MathFunction> functions = List.of(testFunction1, testFunction2);
        when(mathFunctionRepository.findByOwner(testUser)).thenReturn(functions);

        List<MathFunction> result = mathFunctionRepository.findByOwner(testUser);

        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(func -> func.getOwner().equals(testUser)));
        verify(mathFunctionRepository, times(1)).findByOwner(testUser);
    }

    @Test
    void testFindByIsPublicTrue() {
        testFunction1.setIsPublic(true);
        testFunction2.setIsPublic(false);
        List<MathFunction> publicFunctions = List.of(testFunction1);

        when(mathFunctionRepository.findByIsPublicTrue()).thenReturn(publicFunctions);

        List<MathFunction> result = mathFunctionRepository.findByIsPublicTrue();

        assertEquals(1, result.size());
        assertTrue(result.get(0).getIsPublic());
        verify(mathFunctionRepository, times(1)).findByIsPublicTrue();
    }

    @Test
    void testFindByFunctionType() {
        List<MathFunction> polynomialFunctions = List.of(testFunction1);
        when(mathFunctionRepository.findByFunctionType("POLYNOMIAL")).thenReturn(polynomialFunctions);

        List<MathFunction> result = mathFunctionRepository.findByFunctionType("POLYNOMIAL");

        assertEquals(1, result.size());
        assertEquals("POLYNOMIAL", result.get(0).getFunctionType());
        verify(mathFunctionRepository, times(1)).findByFunctionType("POLYNOMIAL");
    }

    @Test
    void testFindByFunctionNameContainingIgnoreCase() {
        List<MathFunction> functions = List.of(testFunction1);
        when(mathFunctionRepository.findByFunctionNameContainingIgnoreCase("func")).thenReturn(functions);

        List<MathFunction> result = mathFunctionRepository.findByFunctionNameContainingIgnoreCase("func");

        assertEquals(1, result.size());
        assertTrue(result.get(0).getFunctionName().toLowerCase().contains("func"));
        verify(mathFunctionRepository, times(1)).findByFunctionNameContainingIgnoreCase("func");
    }

    @Test
    void testFindFunctionsWithMinPoints() {
        List<MathFunction> functions = List.of(testFunction1);
        when(mathFunctionRepository.findFunctionsWithMinPoints(10)).thenReturn(functions);

        List<MathFunction> result = mathFunctionRepository.findFunctionsWithMinPoints(10);

        assertEquals(1, result.size());
        verify(mathFunctionRepository, times(1)).findFunctionsWithMinPoints(10);
    }
}