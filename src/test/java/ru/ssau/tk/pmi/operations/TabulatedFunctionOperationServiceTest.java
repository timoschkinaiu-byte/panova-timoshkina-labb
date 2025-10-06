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

    // Ваши существующие тесты
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
}