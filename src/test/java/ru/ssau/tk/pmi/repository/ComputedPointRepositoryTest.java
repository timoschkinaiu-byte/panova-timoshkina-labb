package ru.ssau.tk.pmi.repository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import ru.ssau.tk.pmi.entity.ComputedPoint;
import ru.ssau.tk.pmi.entity.MathFunction;
import ru.ssau.tk.pmi.entity.User;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ComputedPointRepositoryTest {

    @Mock
    private ComputedPointRepository computedPointRepository;

    private User testUser;
    private MathFunction testFunction;
    private ComputedPoint testPoint1;
    private ComputedPoint testPoint2;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        testUser = new User("owner", "hash", "USER");
        testUser.setUserId(1L);

        testFunction = new MathFunction("testFunc", "x^2", "POLYNOMIAL", testUser);
        testFunction.setFunctionId(1L);

        testPoint1 = new ComputedPoint(1.0, 1.0, testFunction);
        testPoint1.setPointId(1L);

        testPoint2 = new ComputedPoint(2.0, 4.0, testFunction);
        testPoint2.setPointId(2L);
    }

    @Test
    void testFindByFunction() {
        List<ComputedPoint> points = List.of(testPoint1, testPoint2);
        when(computedPointRepository.findByFunction(testFunction)).thenReturn(points);

        List<ComputedPoint> result = computedPointRepository.findByFunction(testFunction);

        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(point -> point.getFunction().equals(testFunction)));
        verify(computedPointRepository, times(1)).findByFunction(testFunction);
    }

    @Test
    void testDeleteByFunction() {
        when(computedPointRepository.deleteByFunction(testFunction)).thenReturn(2L);

        long deletedCount = computedPointRepository.deleteByFunction(testFunction);

        assertEquals(2L, deletedCount);
        verify(computedPointRepository, times(1)).deleteByFunction(testFunction);
    }

    @Test
    void testFindByXValueBetween() {
        List<ComputedPoint> points = List.of(testPoint1);
        when(computedPointRepository.findByxValueBetween(0.5, 1.5)).thenReturn(points);

        List<ComputedPoint> result = computedPointRepository.findByxValueBetween(0.5, 1.5);

        assertEquals(1, result.size());
        assertEquals(1.0, result.get(0).getXValue());
        verify(computedPointRepository, times(1)).findByxValueBetween(0.5, 1.5);
    }

    @Test
    void testFindByExactValues() {
        List<ComputedPoint> points = List.of(testPoint1);
        when(computedPointRepository.findByExactValues(1.0, 1.0)).thenReturn(points);

        List<ComputedPoint> result = computedPointRepository.findByExactValues(1.0, 1.0);

        assertEquals(1, result.size());
        assertEquals(1.0, result.get(0).getXValue());
        assertEquals(1.0, result.get(0).getYValue());
        verify(computedPointRepository, times(1)).findByExactValues(1.0, 1.0);
    }

    @Test
    void testGetFunctionPointsStatistics() {
        Object[] stats = new Object[]{2L, 1.0, 2.0, 1.0, 4.0};
        when(computedPointRepository.getFunctionPointsStatistics(testFunction)).thenReturn(stats);

        Object[] result = computedPointRepository.getFunctionPointsStatistics(testFunction);

        assertNotNull(result);
        assertEquals(5, result.length);
        assertEquals(2L, result[0]); // count
        assertEquals(1.0, result[1]); // min x
        assertEquals(2.0, result[2]); // max x
        verify(computedPointRepository, times(1)).getFunctionPointsStatistics(testFunction);
    }
}