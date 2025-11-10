package ru.ssau.tk.pmi.search;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Sort;
import ru.ssau.tk.pmi.entity.MathFunction;
import ru.ssau.tk.pmi.entity.User;
import ru.ssau.tk.pmi.repository.ComputedPointRepository;
import ru.ssau.tk.pmi.repository.MathFunctionRepository;
import ru.ssau.tk.pmi.repository.UserRepository;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class springSearchServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private MathFunctionRepository mathFunctionRepository;

    @Mock
    private ComputedPointRepository computedPointRepository;

    private springSearchService searchService;

    private User testUser1;
    private User testUser2;
    private MathFunction testFunction1;
    private MathFunction testFunction2;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        searchService = new springSearchService();

        // Устанавливаем моки через рефлексию (так как в SearchService нет сеттеров)
        setField(searchService, "userRepository", userRepository);
        setField(searchService, "mathFunctionRepository", mathFunctionRepository);
        setField(searchService, "computedPointRepository", computedPointRepository);

        // Создаем тестовые данные
        testUser1 = new User("john_doe", "hash1", "USER");
        testUser1.setUserId(1L);

        testUser2 = new User("alice_smith", "hash2", "ADMIN");
        testUser2.setUserId(2L);

        testFunction1 = new MathFunction("quadratic", "x^2", "POLYNOMIAL", testUser1);
        testFunction1.setFunctionId(1L);

        testFunction2 = new MathFunction("sine_wave", "sin(x)", "TRIGONOMETRIC", testUser2);
        testFunction2.setFunctionId(2L);
    }

    @Test
    void testSearchWithSortingByUsernameAscending() {
        // Given
        List<User> mockUsers = List.of(testUser1, testUser2);
        when(userRepository.findByUsernameContainingIgnoreCase(eq("john"), any(Sort.class)))
                .thenReturn(mockUsers);

        // When
        List<Object> result = searchService.searchWithSorting("USERNAME", "john", "username", true);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(userRepository, times(1)).findByUsernameContainingIgnoreCase(eq("john"), any(Sort.class));
    }

    @Test
    void testSearchWithSortingByUsernameDescending() {
        // Given
        List<User> mockUsers = List.of(testUser2, testUser1); // В обратном порядке для DESC
        when(userRepository.findByUsernameContainingIgnoreCase(eq("smith"), any(Sort.class)))
                .thenReturn(mockUsers);

        // When
        List<Object> result = searchService.searchWithSorting("USERNAME", "smith", "username", false);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(userRepository, times(1)).findByUsernameContainingIgnoreCase(eq("smith"), any(Sort.class));
    }

    @Test
    void testSearchWithSortingByFunctionName() {
        // Given
        List<MathFunction> mockFunctions = List.of(testFunction1, testFunction2);
        when(mathFunctionRepository.findByFunctionNameContainingIgnoreCase(eq("quad"), any(Sort.class)))
                .thenReturn(mockFunctions);

        // When
        List<Object> result = searchService.searchWithSorting("FUNCTION_NAME", "quad", "functionName", true);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.get(0) instanceof MathFunction);
        verify(mathFunctionRepository, times(1)).findByFunctionNameContainingIgnoreCase(eq("quad"), any(Sort.class));
    }

    @Test
    void testSearchWithSortingByFunctionType() {
        // Given
        List<MathFunction> mockFunctions = List.of(testFunction1);
        when(mathFunctionRepository.findByFunctionType(eq("POLYNOMIAL"), any(Sort.class))).thenReturn(mockFunctions);

        // When
        List<Object> result = searchService.searchWithSorting("FUNCTION_TYPE", "POLYNOMIAL", "functionType", true);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("POLYNOMIAL", ((MathFunction) result.get(0)).getFunctionType());
        verify(mathFunctionRepository, times(1)).findByFunctionType(eq("POLYNOMIAL"), any(Sort.class));
    }

    @Test
    void testSearchWithSortingDefaultCase() {
        // Given
        when(userRepository.findByUsernameContainingIgnoreCase("unknown")).thenReturn(List.of(testUser1));

        // When - используем неизвестное поле, чтобы попасть в default case
        List<Object> result = searchService.searchWithSorting("UNKNOWN_FIELD", "test", "someField", true);

        // Then - должен вызваться singleSearch
        assertNotNull(result);
        assertEquals(1, result.size());
        // Здесь мы проверяем, что был вызов singleSearch, но для этого нужно мокировать singleSearch
    }

    @Test
    void testSearchWithSortingCaseInsensitive() {
        // Given
        List<User> mockUsers = List.of(testUser1);
        when(userRepository.findByUsernameContainingIgnoreCase(eq("john"), any(Sort.class))).thenReturn(mockUsers);

        // When - используем разный регистр для поля
        List<Object> result1 = searchService.searchWithSorting("username", "john", "username", true);
        List<Object> result2 = searchService.searchWithSorting("USERNAME", "john", "username", true);
        List<Object> result3 = searchService.searchWithSorting("UserName", "john", "username", true);

        // Then - все варианты должны работать одинаково
        assertNotNull(result1);
        assertNotNull(result2);
        assertNotNull(result3);
        verify(userRepository, times(3)).findByUsernameContainingIgnoreCase(eq("john"), any(Sort.class));
    }

    @Test
    void testSearchWithSortingEmptyResults() {
        // Given
        when(mathFunctionRepository.findByFunctionNameContainingIgnoreCase(eq("nonexistent"), any(Sort.class))).thenReturn(List.of());

        // When
        List<Object> result = searchService.searchWithSorting("FUNCTION_NAME", "nonexistent", "functionName", true);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(mathFunctionRepository, times(1)).findByFunctionNameContainingIgnoreCase(eq("nonexistent"), any(Sort.class));
    }

    // Вспомогательный метод для установки полей через рефлексию
    private void setField(Object target, String fieldName, Object value) {
        try {
            var field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set field: " + fieldName, e);
        }
    }
}