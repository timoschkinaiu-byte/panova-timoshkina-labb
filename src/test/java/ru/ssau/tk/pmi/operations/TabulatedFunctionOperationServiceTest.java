package ru.ssau.tk.pmi.operations;
import ru.ssau.tk.pmi.functions.ArrayTabulatedFunction;
import ru.ssau.tk.pmi.functions.LinkedListTabulatedFunction;
import ru.ssau.tk.pmi.functions.Point;
import ru.ssau.tk.pmi.functions.TabulatedFunction;
import ru.ssau.tk.pmi.functions.factory.ArrayTabulatedFunctionFactory;
import ru.ssau.tk.pmi.functions.factory.LinkedListTabulatedFunctionFactory;
import ru.ssau.tk.pmi.exceptions.InconsistentFunctionsException;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class TabulatedFunctionOperationServiceTest {

    @Test
    public void testAsPoints() {
        double[] xValues = {1.0, 2.0, 3.0};
        double[] yValues = {10.0, 20.0, 30.0};
        ArrayTabulatedFunction function = new ArrayTabulatedFunction(xValues, yValues);
        Point[] points = TabulatedFunctionOperationService.asPoints(function);
        assertEquals(3, points.length);
        assertEquals(1.0, points[0].x, 1e-10);
        assertEquals(10.0, points[0].y, 1e-10);
        assertEquals(2.0, points[1].x, 1e-10);
        assertEquals(20.0, points[1].y, 1e-10);
        assertEquals(3.0, points[2].x, 1e-10);
        assertEquals(30.0, points[2].y, 1e-10);
    }

    @Test
    public void testAsPointsWithSinglePoint() {
        double[] xValues = {5.0, 6.0};
        double[] yValues = {25.0, 36.0};
        ArrayTabulatedFunction function = new ArrayTabulatedFunction(xValues, yValues);
        Point[] points = TabulatedFunctionOperationService.asPoints(function);
        assertEquals(2, points.length); 
        assertEquals(5.0, points[0].x, 1e-10);
        assertEquals(25.0, points[0].y, 1e-10);
        assertEquals(6.0, points[1].x, 1e-10);
        assertEquals(36.0, points[1].y, 1e-10);
    }

    // НОВЫЕ ТЕСТЫ ДЛЯ МЕТОДОВ СЛОЖЕНИЯ И ВЫЧИТАНИЯ

    @Test
    public void testAddWithArrayFactory() {
        TabulatedFunctionOperationService service = new TabulatedFunctionOperationService(new ArrayTabulatedFunctionFactory());

        double[] xValues = {1.0, 2.0, 3.0};
        double[] y1 = {10.0, 20.0, 30.0};
        double[] y2 = {5.0, 10.0, 15.0};

        ArrayTabulatedFunction f1 = new ArrayTabulatedFunction(xValues, y1);
        ArrayTabulatedFunction f2 = new ArrayTabulatedFunction(xValues, y2);

        // Тестируем оба метода add
        TabulatedFunction result1 = service.add(f1, f2);
        TabulatedFunction result2 = service.add2(f1, f2);

        assertEquals(15.0, result1.getY(0), 1e-10);
        assertEquals(30.0, result1.getY(1), 1e-10);
        assertEquals(45.0, result1.getY(2), 1e-10);

        assertEquals(15.0, result2.getY(0), 1e-10);
        assertEquals(30.0, result2.getY(1), 1e-10);
        assertEquals(45.0, result2.getY(2), 1e-10);

        assertTrue(result1 instanceof ArrayTabulatedFunction);
        assertTrue(result2 instanceof ArrayTabulatedFunction);
    }


    @Test
    public void testInconsistentFunctionsException_DifferentCount() {
        TabulatedFunctionOperationService service = new TabulatedFunctionOperationService();

        double[] x1 = {1.0, 2.0};
        double[] x2 = {1.0, 2.0, 3.0};
        double[] y1 = {10.0, 20.0};
        double[] y2 = {5.0, 10.0, 15.0};

        ArrayTabulatedFunction f1 = new ArrayTabulatedFunction(x1, y1);
        ArrayTabulatedFunction f2 = new ArrayTabulatedFunction(x2, y2);

        // Оба метода должны бросать исключение
        assertThrows(InconsistentFunctionsException.class, () -> service.add(f1, f2));
        assertThrows(InconsistentFunctionsException.class, () -> service.add2(f1, f2));
        assertThrows(InconsistentFunctionsException.class, () -> service.subtraction(f1, f2));
        assertThrows(InconsistentFunctionsException.class, () -> service.subtraction2(f1, f2));
    }

    @Test
    public void testInconsistentFunctionsException_DifferentXValues() {
        TabulatedFunctionOperationService service = new TabulatedFunctionOperationService();

        double[] x1 = {1.0, 2.0, 3.0};
        double[] x2 = {1.0, 2.5, 3.0}; // разные X
        double[] y1 = {10.0, 20.0, 30.0};
        double[] y2 = {5.0, 10.0, 15.0};

        ArrayTabulatedFunction f1 = new ArrayTabulatedFunction(x1, y1);
        ArrayTabulatedFunction f2 = new ArrayTabulatedFunction(x2, y2);

        assertThrows(InconsistentFunctionsException.class, () -> service.add(f1, f2));
        assertThrows(InconsistentFunctionsException.class, () -> service.add2(f1, f2));
    }

    // ТЕСТ ДЛЯ ФАБРИКИ ПО УМОЛЧАНИЮ

    @Test
    public void testDefaultFactoryIsArray() {
        // Конструктор без параметров должен использовать Array фабрику
        TabulatedFunctionOperationService service = new TabulatedFunctionOperationService();

        double[] xValues = {1.0, 2.0};
        double[] y1 = {5.0, 10.0};
        double[] y2 = {2.0, 4.0};

        ArrayTabulatedFunction f1 = new ArrayTabulatedFunction(xValues, y1);
        ArrayTabulatedFunction f2 = new ArrayTabulatedFunction(xValues, y2);

        TabulatedFunction result = service.add(f1, f2);

        // Результат должен быть ArrayTabulatedFunction
        assertTrue(result instanceof ArrayTabulatedFunction);
        assertEquals(7.0, result.getY(0), 1e-10);
        assertEquals(14.0, result.getY(1), 1e-10);
    }

    // ТЕСТ НА ИДЕНТИЧНОСТЬ РЕЗУЛЬТАТОВ СТАРЫХ И НОВЫХ МЕТОДОВ

    @Test
    public void testOldAndNewMethodsProduceSameResults() {
        TabulatedFunctionOperationService service = new TabulatedFunctionOperationService();

        double[] xValues = {1.0, 2.0, 3.0};
        double[] y1 = {10.0, 20.0, 30.0};
        double[] y2 = {5.0, 10.0, 15.0};

        ArrayTabulatedFunction f1 = new ArrayTabulatedFunction(xValues, y1);
        LinkedListTabulatedFunction f2 = new LinkedListTabulatedFunction(xValues, y2);

        TabulatedFunction oldAdd = service.add(f1, f2);
        TabulatedFunction newAdd = service.add2(f1, f2);

        TabulatedFunction oldSub = service.subtraction(f1, f2);
        TabulatedFunction newSub = service.subtraction2(f1, f2);
        for (int i = 0; i < f1.getCount(); i++) {
            assertEquals(oldAdd.getY(i), newAdd.getY(i), 1e-10, "add и add2 должны давать одинаковые результаты");
            assertEquals(oldSub.getY(i), newSub.getY(i), 1e-10, "subtraction и subtraction2 должны давать одинаковые результаты");
        }
    }

    //тесты умножения и деления
    @Test
    public void testAddWithArrayFunctions() {
        TabulatedFunctionOperationService service = new TabulatedFunctionOperationService(new ArrayTabulatedFunctionFactory());

        double[] xValues = {0.0, 1.0, 2.0, 3.0};
        double[] yValues1 = {1.0, 3.0, 5.0, 7.0};
        double[] yValues2 = {2.0, 4.0, 6.0, 8.0};

        TabulatedFunction f1 = new ArrayTabulatedFunction(xValues, yValues1);
        TabulatedFunction f2 = new ArrayTabulatedFunction(xValues, yValues2);

        TabulatedFunction result = service.add(f1, f2);

        assertEquals(4, result.getCount());
        assertEquals(3.0, result.getY(0), 0.0001);
        assertEquals(7.0, result.getY(1), 0.0001);
        assertEquals(11.0, result.getY(2), 0.0001);
        assertEquals(15.0, result.getY(3), 0.0001);
        assertInstanceOf(ArrayTabulatedFunction.class, result);
    }

    @Test
    public void testAddWithLinkedListFunctions() {
        TabulatedFunctionOperationService service = new TabulatedFunctionOperationService(new LinkedListTabulatedFunctionFactory());

        double[] xValues = {0.0, 1.0, 2.0, 3.0};
        double[] yValues1 = {1.0, 3.0, 5.0, 7.0};
        double[] yValues2 = {2.0, 4.0, 6.0, 8.0};

        TabulatedFunction f1 = new LinkedListTabulatedFunction(xValues, yValues1);
        TabulatedFunction f2 = new LinkedListTabulatedFunction(xValues, yValues2);

        TabulatedFunction result = service.add(f1, f2);

        assertEquals(4, result.getCount());
        assertEquals(3.0, result.getY(0), 0.0001);
        assertEquals(7.0, result.getY(1), 0.0001);
        assertEquals(11.0, result.getY(2), 0.0001);
        assertEquals(15.0, result.getY(3), 0.0001);
        assertInstanceOf(LinkedListTabulatedFunction.class, result);
    }

    @Test
    public void testAddWithMixedFunctionTypes() {
        TabulatedFunctionOperationService service = new TabulatedFunctionOperationService(new ArrayTabulatedFunctionFactory());

        double[] xValues = {0.0, 1.0, 2.0, 3.0};
        double[] yValues1 = {1.0, 3.0, 5.0, 7.0};
        double[] yValues2 = {2.0, 4.0, 6.0, 8.0};

        TabulatedFunction f1 = new ArrayTabulatedFunction(xValues, yValues1);
        TabulatedFunction f2 = new LinkedListTabulatedFunction(xValues, yValues2);

        TabulatedFunction result = service.add(f1, f2);

        assertEquals(4, result.getCount());
        assertEquals(3.0, result.getY(0), 0.0001);
        assertEquals(7.0, result.getY(1), 0.0001);
        assertEquals(11.0, result.getY(2), 0.0001);
        assertEquals(15.0, result.getY(3), 0.0001);
        assertInstanceOf(ArrayTabulatedFunction.class, result);
    }

    @Test
    public void testMultiplicationWithArrayFunctions() {
        TabulatedFunctionOperationService service = new TabulatedFunctionOperationService(new ArrayTabulatedFunctionFactory());

        double[] xValues = {0.0, 1.0, 2.0, 3.0};
        double[] yValues1 = {1.0, 3.0, 5.0, 7.0};
        double[] yValues2 = {2.0, 4.0, 6.0, 8.0};

        TabulatedFunction f1 = new ArrayTabulatedFunction(xValues, yValues1);
        TabulatedFunction f2 = new ArrayTabulatedFunction(xValues, yValues2);

        TabulatedFunction result = service.multiplication(f1, f2);

        assertEquals(4, result.getCount());
        assertEquals(2.0, result.getY(0), 0.0001);
        assertEquals(12.0, result.getY(1), 0.0001);
        assertEquals(30.0, result.getY(2), 0.0001);
        assertEquals(56.0, result.getY(3), 0.0001);
        assertInstanceOf(ArrayTabulatedFunction.class, result);
    }

    @Test
    public void testMultiplicationWithLinkedListFunctions() {
        TabulatedFunctionOperationService service = new TabulatedFunctionOperationService(new LinkedListTabulatedFunctionFactory());

        double[] xValues = {0.0, 1.0, 2.0, 3.0};
        double[] yValues1 = {1.0, 3.0, 5.0, 7.0};
        double[] yValues2 = {2.0, 4.0, 6.0, 8.0};

        TabulatedFunction f1 = new LinkedListTabulatedFunction(xValues, yValues1);
        TabulatedFunction f2 = new LinkedListTabulatedFunction(xValues, yValues2);

        TabulatedFunction result = service.multiplication(f1, f2);

        assertEquals(4, result.getCount());
        assertEquals(2.0, result.getY(0), 0.0001);
        assertEquals(12.0, result.getY(1), 0.0001);
        assertEquals(30.0, result.getY(2), 0.0001);
        assertEquals(56.0, result.getY(3), 0.0001);
        assertInstanceOf(LinkedListTabulatedFunction.class, result);
    }

    @Test
    public void testAddThrowsExceptionWithDifferentLengths() {
        TabulatedFunctionOperationService service = new TabulatedFunctionOperationService();

        double[] xValues1 = {0.0, 1.0, 2.0, 3.0};
        double[] yValues1 = {1.0, 3.0, 5.0, 7.0};
        double[] xValues2 = {0.0, 1.0};
        double[] yValues2 = {1.0, 2.0};

        TabulatedFunction f1 = new ArrayTabulatedFunction(xValues1, yValues1);
        TabulatedFunction f2 = new ArrayTabulatedFunction(xValues2, yValues2);

        assertThrows(InconsistentFunctionsException.class, () -> service.add(f1, f2));
    }

    @Test
    public void testMultiplicationThrowsExceptionWithDifferentXValues() {
        TabulatedFunctionOperationService service = new TabulatedFunctionOperationService();

        double[] xValues1 = {0.0, 1.0, 2.0, 3.0};
        double[] yValues1 = {1.0, 3.0, 5.0, 7.0};
        double[] xValues2 = {0.0, 1.5, 2.0, 3.0};
        double[] yValues2 = {2.0, 4.0, 6.0, 8.0};

        TabulatedFunction f1 = new ArrayTabulatedFunction(xValues1, yValues1);
        TabulatedFunction f2 = new ArrayTabulatedFunction(xValues2, yValues2);

        assertThrows(InconsistentFunctionsException.class, () -> service.multiplication(f1, f2));
    }

    @Test
    public void testAdd2WithArrayFunctions() {
        TabulatedFunctionOperationService service = new TabulatedFunctionOperationService(new ArrayTabulatedFunctionFactory());

        double[] xValues = {0.0, 1.0, 2.0, 3.0};
        double[] yValues1 = {1.0, 3.0, 5.0, 7.0};
        double[] yValues2 = {2.0, 4.0, 6.0, 8.0};

        TabulatedFunction f1 = new ArrayTabulatedFunction(xValues, yValues1);
        TabulatedFunction f2 = new ArrayTabulatedFunction(xValues, yValues2);

        TabulatedFunction result = service.add2(f1, f2);

        assertEquals(4, result.getCount());
        assertEquals(3.0, result.getY(0), 0.0001);
        assertEquals(7.0, result.getY(1), 0.0001);
        assertEquals(11.0, result.getY(2), 0.0001);
        assertEquals(15.0, result.getY(3), 0.0001);
        assertInstanceOf(ArrayTabulatedFunction.class, result);
    }

    @Test
    public void testMultiplication2WithLinkedListFunctions() {
        TabulatedFunctionOperationService service = new TabulatedFunctionOperationService(new LinkedListTabulatedFunctionFactory());

        double[] xValues = {0.0, 1.0, 2.0, 3.0};
        double[] yValues1 = {1.0, 3.0, 5.0, 7.0};
        double[] yValues2 = {2.0, 4.0, 6.0, 8.0};

        TabulatedFunction f1 = new LinkedListTabulatedFunction(xValues, yValues1);
        TabulatedFunction f2 = new LinkedListTabulatedFunction(xValues, yValues2);

        TabulatedFunction result = service.multiplication2(f1, f2);

        assertEquals(4, result.getCount());
        assertEquals(2.0, result.getY(0), 0.0001);
        assertEquals(12.0, result.getY(1), 0.0001);
        assertEquals(30.0, result.getY(2), 0.0001);
        assertEquals(56.0, result.getY(3), 0.0001);
        assertTrue(result instanceof LinkedListTabulatedFunction);
    }

    @Test
    public void testAddAndAdd2GiveSameResult() {
        TabulatedFunctionOperationService service = new TabulatedFunctionOperationService();

        double[] xValues = {0.0, 1.0, 2.0, 3.0};
        double[] yValues1 = {1.0, 3.0, 5.0, 7.0};
        double[] yValues2 = {2.0, 4.0, 6.0, 8.0};

        TabulatedFunction f1 = new ArrayTabulatedFunction(xValues, yValues1);
        TabulatedFunction f2 = new ArrayTabulatedFunction(xValues, yValues2);

        TabulatedFunction result1 = service.add(f1, f2);
        TabulatedFunction result2 = service.add2(f1, f2);

        assertEquals(result1.getCount(), result2.getCount());
        for (int i = 0; i < result1.getCount(); i++) {
            assertEquals(result1.getX(i), result2.getX(i), 0.0001);
            assertEquals(result1.getY(i), result2.getY(i), 0.0001);
        }
    }

    @Test
    public void testMultiplicationAndMultiplication2GiveSameResult() {
        TabulatedFunctionOperationService service = new TabulatedFunctionOperationService();

        double[] xValues = {0.0, 1.0, 2.0, 3.0};
        double[] yValues1 = {1.0, 3.0, 5.0, 7.0};
        double[] yValues2 = {2.0, 4.0, 6.0, 8.0};

        TabulatedFunction f1 = new LinkedListTabulatedFunction(xValues, yValues1);
        TabulatedFunction f2 = new LinkedListTabulatedFunction(xValues, yValues2);

        TabulatedFunction result1 = service.multiplication(f1, f2);
        TabulatedFunction result2 = service.multiplication2(f1, f2);

        assertEquals(result1.getCount(), result2.getCount());
        for (int i = 0; i < result1.getCount(); i++) {
            assertEquals(result1.getX(i), result2.getX(i), 0.0001);
            assertEquals(result1.getY(i), result2.getY(i), 0.0001);
        }
    }

}