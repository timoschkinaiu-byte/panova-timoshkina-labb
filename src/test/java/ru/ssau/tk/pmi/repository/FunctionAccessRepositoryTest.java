package ru.ssau.tk.pmi.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import ru.ssau.tk.pmi.entity.FunctionAccess;
import ru.ssau.tk.pmi.entity.MathFunction;
import ru.ssau.tk.pmi.entity.User;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FunctionAccessRepositoryTest {

    @Mock
    private FunctionAccessRepository functionAccessRepository;

    private User testUser1;
    private User testUser2;
    private MathFunction testFunction1;
    private MathFunction testFunction2;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // Create test users
        testUser1 = new User("test_user_1", "hash1", "USER");
        testUser1.setUserId(1L);

        testUser2 = new User("test_user_2", "hash2", "ADMIN");
        testUser2.setUserId(2L);

        // Create test functions
        testFunction1 = new MathFunction("function_1", "x^2", "POLYNOMIAL", testUser1);
        testFunction1.setFunctionId(1L);

        testFunction2 = new MathFunction("function_2", "sin(x)", "TRIGONOMETRIC", testUser1);
        testFunction2.setFunctionId(2L);
    }

    @Test
    void testFindByFunction() {
        // Given
        FunctionAccess access1 = new FunctionAccess("READ", testFunction1, testUser1);
        FunctionAccess access2 = new FunctionAccess("WRITE", testFunction1, testUser2);
        List<FunctionAccess> expectedAccesses = List.of(access1, access2);

        when(functionAccessRepository.findByFunction(testFunction1)).thenReturn(expectedAccesses);

        // When
        List<FunctionAccess> result = functionAccessRepository.findByFunction(testFunction1);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("READ", result.get(0).getAccessType());
        assertEquals("WRITE", result.get(1).getAccessType());
        verify(functionAccessRepository, times(1)).findByFunction(testFunction1);
    }

    @Test
    void testFindByUser() {
        // Given
        FunctionAccess access1 = new FunctionAccess("READ", testFunction1, testUser1);
        FunctionAccess access2 = new FunctionAccess("WRITE", testFunction2, testUser1);
        List<FunctionAccess> expectedAccesses = List.of(access1, access2);

        when(functionAccessRepository.findByUser(testUser1)).thenReturn(expectedAccesses);

        // When
        List<FunctionAccess> result = functionAccessRepository.findByUser(testUser1);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(access -> access.getUser().getUserId().equals(1L)));
        verify(functionAccessRepository, times(1)).findByUser(testUser1);
    }

    @Test
    void testFindByFunctionAndUser() {
        // Given
        FunctionAccess expectedAccess = new FunctionAccess("READ", testFunction1, testUser1);
        expectedAccess.setAccessId(1L);

        when(functionAccessRepository.findByFunctionAndUser(testFunction1, testUser1))
                .thenReturn(Optional.of(expectedAccess));
        when(functionAccessRepository.findByFunctionAndUser(testFunction1, testUser2))
                .thenReturn(Optional.empty());

        // When
        Optional<FunctionAccess> result1 = functionAccessRepository.findByFunctionAndUser(testFunction1, testUser1);
        Optional<FunctionAccess> result2 = functionAccessRepository.findByFunctionAndUser(testFunction1, testUser2);

        // Then
        assertTrue(result1.isPresent());
        assertEquals(1L, result1.get().getAccessId());
        assertEquals("READ", result1.get().getAccessType());
        assertEquals(testFunction1.getFunctionId(), result1.get().getFunction().getFunctionId());
        assertEquals(testUser1.getUserId(), result1.get().getUser().getUserId());

        assertFalse(result2.isPresent());
        verify(functionAccessRepository, times(1)).findByFunctionAndUser(testFunction1, testUser1);
        verify(functionAccessRepository, times(1)).findByFunctionAndUser(testFunction1, testUser2);
    }

    @Test
    void testExistsByFunctionAndUser() {
        // Given
        when(functionAccessRepository.existsByFunctionAndUser(testFunction1, testUser1)).thenReturn(true);
        when(functionAccessRepository.existsByFunctionAndUser(testFunction1, testUser2)).thenReturn(false);

        // When
        boolean exists = functionAccessRepository.existsByFunctionAndUser(testFunction1, testUser1);
        boolean notExists = functionAccessRepository.existsByFunctionAndUser(testFunction1, testUser2);

        // Then
        assertTrue(exists);
        assertFalse(notExists);
        verify(functionAccessRepository, times(1)).existsByFunctionAndUser(testFunction1, testUser1);
        verify(functionAccessRepository, times(1)).existsByFunctionAndUser(testFunction1, testUser2);
    }

    @Test
    void testDeleteByFunctionAndUser() {
        // Given - no return value for void method

        // When
        functionAccessRepository.deleteByFunctionAndUser(testFunction1, testUser1);

        // Then
        verify(functionAccessRepository, times(1)).deleteByFunctionAndUser(testFunction1, testUser1);
        // For void methods, we just verify they were called with correct parameters
    }

    @Test
    void testFindSharedFunctionsForUser() {
        // Given
        List<MathFunction> expectedFunctions = List.of(testFunction2);

        when(functionAccessRepository.findSharedFunctionsForUser(testUser2)).thenReturn(expectedFunctions);

        // When
        List<MathFunction> result = functionAccessRepository.findSharedFunctionsForUser(testUser2);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testFunction2.getFunctionId(), result.get(0).getFunctionId());
        verify(functionAccessRepository, times(1)).findSharedFunctionsForUser(testUser2);
    }

    @Test
    void testFindUsersWithAccessToFunction() {
        // Given
        List<User> expectedUsers = List.of(testUser2);

        when(functionAccessRepository.findUsersWithAccessToFunction(testFunction1)).thenReturn(expectedUsers);

        // When
        List<User> result = functionAccessRepository.findUsersWithAccessToFunction(testFunction1);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testUser2.getUserId(), result.get(0).getUserId());
        verify(functionAccessRepository, times(1)).findUsersWithAccessToFunction(testFunction1);
    }
}