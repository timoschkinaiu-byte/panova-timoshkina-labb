package ru.ssau.tk.pmi.functions;
import org.junit.jupiter.api.Test;
import ru.ssau.tk.pmi.exceptions.InterpolationException;
import ru.ssau.tk.pmi.functions.ArrayTabulatedFunction;
import ru.ssau.tk.pmi.functions.MathFunction;
import ru.ssau.tk.pmi.functions.SqrFunction;
import ru.ssau.tk.pmi.functions.Point;
import java.util.Iterator;
import static org.junit.jupiter.api.Assertions.*;
public class ArrayTabulatedFunctionTest {
    @Test
    public void testConstructorWithArrays() {
        double[] xValues = {1.0, 2.0, 3.0};
        double[] yValues = {10.0, 20.0, 30.0};
        ArrayTabulatedFunction function = new ArrayTabulatedFunction(xValues, yValues);
        assertEquals(3, function.getCount());
        assertEquals(1.0, function.leftBound(), 0.0001);
        assertEquals(3.0, function.rightBound(), 0.0001);
        assertEquals(1.0, function.getX(0), 0.0001);
        assertEquals(2.0, function.getX(1), 0.0001);
        assertEquals(3.0, function.getX(2), 0.0001);
        assertEquals(10.0, function.getY(0), 0.0001);
        assertEquals(20.0, function.getY(1), 0.0001);
        assertEquals(30.0, function.getY(2), 0.0001);
    }
    @Test
    public void testConstructorWithMathFunction() {
        MathFunction source = new SqrFunction();
        ArrayTabulatedFunction function = new ArrayTabulatedFunction(source, 0.0, 4.0, 5);
        assertEquals(5, function.getCount());
        assertEquals(0.0, function.leftBound(), 0.0001);
        assertEquals(4.0, function.rightBound(), 0.0001);
        assertEquals(0.0, function.getY(0), 0.0001);
        assertEquals(4.0, function.getY(2), 0.0001);
        assertEquals(16.0, function.getY(4), 0.0001);
    }
    @Test
    public void testConstructorWithReversedBounds() {
        MathFunction source = new SqrFunction();
        ArrayTabulatedFunction function = new ArrayTabulatedFunction(source, 4.0, 0.0, 5);

        assertEquals(5, function.getCount());
        assertEquals(0.0, function.leftBound(), 0.0001);
        assertEquals(4.0, function.rightBound(), 0.0001);
    }
    @Test
    public void testConstructorWithEqualBounds() {
        MathFunction source = new SqrFunction();
        ArrayTabulatedFunction function = new ArrayTabulatedFunction(source, 3.0, 3.0, 4);
        assertEquals(4, function.getCount());
        assertEquals(3.0, function.leftBound(), 0.0001);
        assertEquals(3.0, function.rightBound(), 0.0001);
        assertEquals(9.0, function.getY(0), 0.0001);
        assertEquals(9.0, function.getY(3), 0.0001);
    }
    @Test
    public void testSetY() {
        double[] xValues = {1.0, 2.0, 3.0};
        double[] yValues = {10.0, 20.0, 30.0};
        ArrayTabulatedFunction function = new ArrayTabulatedFunction(xValues, yValues);
        function.setY(1, 25.0);
        assertEquals(25.0, function.getY(1), 0.0001);
    }
    @Test
    public void testIndexOfX() {
        double[] xValues = {1.0, 2.0, 3.0};
        double[] yValues = {10.0, 20.0, 30.0};
        ArrayTabulatedFunction function = new ArrayTabulatedFunction(xValues, yValues);
        assertEquals(0, function.indexOfX(1.0));
        assertEquals(1, function.indexOfX(2.0));
        assertEquals(2, function.indexOfX(3.0));
        assertEquals(-1, function.indexOfX(4.0));
    }
    @Test
    public void testIndexOfY() {
        double[] xValues = {1.0, 2.0, 3.0};
        double[] yValues = {10.0, 20.0, 30.0};
        ArrayTabulatedFunction function = new ArrayTabulatedFunction(xValues, yValues);
        assertEquals(0, function.indexOfY(10.0));
        assertEquals(1, function.indexOfY(20.0));
        assertEquals(2, function.indexOfY(30.0));
        assertEquals(-1, function.indexOfY(40.0));
    }
    @Test
    public void testFloorIndexOfX() {
        double[] xValues = {1.0, 2.0, 3.0, 4.0};
        double[] yValues = {10.0, 20.0, 30.0, 40.0};
        ArrayTabulatedFunction function = new ArrayTabulatedFunction(xValues, yValues);
        assertEquals(0, function.floorIndexOfX(1.5));  // между 1.0 и 2.0
        assertEquals(1, function.floorIndexOfX(2.5));  // между 2.0 и 3.0
        assertEquals(2, function.floorIndexOfX(3.5));  // между 3.0 и 4.0
        assertEquals(4, function.floorIndexOfX(4.0));  // равно последнему - возвращает последний интервал
        assertEquals(4, function.floorIndexOfX(5.0));  // больше последнего - возвращает последний интервал
    }
    @Test
    public void testApplyInterpolation() {
        double[] xValues = {0.0, 2.0, 4.0};
        double[] yValues = {0.0, 4.0, 16.0};
        ArrayTabulatedFunction function = new ArrayTabulatedFunction(xValues, yValues);
        assertEquals(2.0, function.apply(1.0), 0.0001);
        assertEquals(10.0, function.apply(3.0), 0.0001);
    }
    @Test
    public void testApplyExtrapolation() {
        double[] xValues = {1.0, 2.0, 3.0};
        double[] yValues = {1.0, 4.0, 9.0};
        ArrayTabulatedFunction function = new ArrayTabulatedFunction(xValues, yValues);
        double leftResult = function.apply(0.0);
        double rightResult = function.apply(4.0);
        assertTrue(leftResult < 1.0);
        assertTrue(rightResult > 9.0);
    }

    @Test
    public void testApplyExactMatch() {
        double[] xValues = {1.0, 2.0, 3.0};
        double[] yValues = {10.0, 20.0, 30.0};
        ArrayTabulatedFunction function = new ArrayTabulatedFunction(xValues, yValues);
        assertEquals(10.0, function.apply(1.0), 0.0001);
        assertEquals(20.0, function.apply(2.0), 0.0001);
        assertEquals(30.0, function.apply(3.0), 0.0001);
    }

    @Test
    public void insertTestMiddle(){
        double[] xValues = {1.0, 2.0, 3.0};
        double[] yValues = {10.0, 20.0, 30.0};
        ArrayTabulatedFunction func = new ArrayTabulatedFunction(xValues, yValues);

        func.insert(2.5, 56);
        assertEquals(2.5,func.getX(2));
        assertEquals(3.0,func.getX(3));
        assertEquals(56,func.getY(2));
        assertEquals(20,func.getY(1));
    }


    @Test
    public void insertTestEnd(){
        double[] xValues = {1.0, 2.0, 3.0};
        double[] yValues = {10.0, 20.0, 30.0};
        ArrayTabulatedFunction func = new ArrayTabulatedFunction(xValues, yValues);

        func.insert(6.8, 45);
        assertEquals(6.8,func.getX(3));
        assertEquals(3.0,func.getX(2));
        assertEquals(45,func.getY(3));
        assertEquals(20,func.getY(1));
    }

    @Test
    public void insertTestBagging(){
        double[] xValues = {1.0, 2.0, 3.0};
        double[] yValues = {10.0, 20.0, 30.0};
        ArrayTabulatedFunction func = new ArrayTabulatedFunction(xValues, yValues);

        func.insert(0.8, 45);
        assertEquals(0.8,func.getX(0));
        assertEquals(2.0,func.getX(2));
        assertEquals(45,func.getY(0));
        assertEquals(10,func.getY(1));
    }


    @Test
    public void testInsertMultipleValues() {

        double[] xValues = {2.0, 4.0};
        double[] yValues = {4.0, 8.0};
        ArrayTabulatedFunction func = new ArrayTabulatedFunction(xValues, yValues);

        func.insert(1.0, 2.0);
        func.insert(3.0, 6.0);
        func.insert(5.0, 10.0);

        assertEquals(1.0,func.getX(0));
        assertEquals(3.0,func.getX(2));
        assertEquals(8,func.getY(3));
        assertEquals(10,func.getY(4));
    }
    @Test
    void testRemove_ValidIndex_RemovesElement() {
        ArrayTabulatedFunction function = new ArrayTabulatedFunction(
                new double[]{1.0, 2.0, 3.0, 4.0},
                new double[]{10.0, 20.0, 30.0, 40.0}
        );
        function.remove(1);
        assertEquals(3, function.getCount());
        assertEquals(1.0, function.getX(0));
        assertEquals(3.0, function.getX(1));
        assertEquals(4.0, function.getX(2));
    }
    @Test
    void testRemove_FirstElement_WorksCorrectly() {
        ArrayTabulatedFunction function = new ArrayTabulatedFunction(
                new double[]{1.0, 2.0, 3.0},
                new double[]{10.0, 20.0, 30.0}
        );
        function.remove(0);
        assertEquals(2, function.getCount());
        assertEquals(2.0, function.getX(0));
        assertEquals(3.0, function.getX(1));
    }
    @Test
    public void testInterpolateThrowsExceptionWhenXLessThan() {
        double[] xValues = {1.0, 2.0, 3.0};
        double[] yValues = {1.0, 4.0, 9.0};
        ArrayTabulatedFunction function = new ArrayTabulatedFunction(xValues, yValues);
        assertThrows(InterpolationException.class, () -> {
            function.interpolate(0.5, 0); // x < leftNode.x
        });
    }
    @Test
    public void testInterpolateThrowsExceptionWhenXGreaterThan() {
        double[] xValues = {1.0, 2.0, 3.0};
        double[] yValues = {1.0, 4.0, 9.0};
        ArrayTabulatedFunction function = new ArrayTabulatedFunction(xValues, yValues);
        assertThrows(InterpolationException.class, () -> {
            function.interpolate(2.5, 0);
        });
    }
    @Test
    public void testIteratorWithWhileLoop() {
        double[] xValues = {1.0, 2.0, 3.0, 4.0};
        double[] yValues = {1.0, 4.0, 9.0, 16.0};
        ArrayTabulatedFunction function = new ArrayTabulatedFunction(xValues, yValues);
        Iterator<Point> iterator = function.iterator();
        double[] expectedX = {1.0, 2.0, 3.0, 4.0};
        double[] expectedY = {1.0, 4.0, 9.0, 16.0};
        int index = 0;
        while (iterator.hasNext()) {
            Point point = iterator.next();
            assertEquals(expectedX[index], point.x, 1e-10);
            assertEquals(expectedY[index], point.y, 1e-10);
            index++;
        }
        assertEquals(4, index); // Проверяем, что прошли все 4 точки
        assertFalse(iterator.hasNext()); // Проверяем, что итератор завершен
    }
    @Test
    public void testIteratorWithForLoop() {
        double[] xValues = {1.0, 2.0, 3.0, 4.0};
        double[] yValues = {1.0, 4.0, 9.0, 16.0};
        ArrayTabulatedFunction function = new ArrayTabulatedFunction(xValues, yValues);
        int index = 0;
        for (Point point : function) {
            assertEquals(xValues[index], point.x, 1e-10);
            assertEquals(yValues[index], point.y, 1e-10);
            index++;
        }
        assertEquals(4, index); // Проверяем, что прошли все 4 точки
    }


    @Test
    public void testConstructorWithInvalidLength() {
        double[] xValues = {1.0};
        double[] yValues = {9.0};

        assertThrows(IllegalArgumentException.class, () -> {
            new ArrayTabulatedFunction(xValues, yValues);
        });
    }

    @Test
    public void testConstructorWithSourceInvalidLength(){

        assertThrows(IllegalArgumentException.class, ()->{
            new ArrayTabulatedFunction(new IdentityFunction(), 3, 4, 1);
        });
    }

    @Test
    public void testGetXWithInvalidIndex() {
        double[] xValues = {1.0, 2.0, 3.0};
        double[] yValues = {4.0, 5.0, 6.0};
        ArrayTabulatedFunction function = new ArrayTabulatedFunction(xValues, yValues);

        assertThrows(IllegalArgumentException.class, () -> function.getX(-1));
        assertThrows(IllegalArgumentException.class, () -> function.getX(3));
    }

    @Test
    public void testGetYWithInvalidIndex() {
        double[] xValues = {1.0, 2.0, 3.0};
        double[] yValues = {4.0, 5.0, 6.0};
        ArrayTabulatedFunction function = new ArrayTabulatedFunction(xValues, yValues);

        assertThrows(IllegalArgumentException.class, () -> function.getY(-1));
        assertThrows(IllegalArgumentException.class, () -> function.getY(3));
    }

    @Test
    public void testSetYWithInvalidIndex() {
        double[] xValues = {1.0, 2.0, 3.0};
        double[] yValues = {4.0, 5.0, 6.0};
        ArrayTabulatedFunction function = new ArrayTabulatedFunction(xValues, yValues);

        assertThrows(IllegalArgumentException.class, () -> function.setY(-1, 10.0));
        assertThrows(IllegalArgumentException.class, () -> function.setY(3, 10.0));
    }

    @Test
    public void testFloorIndexOfXWithXLessThanLeftBound() {
        double[] xValues = {1.0, 2.0, 3.0};
        double[] yValues = {4.0, 5.0, 6.0};
        ArrayTabulatedFunction function = new ArrayTabulatedFunction(xValues, yValues);

        assertThrows(IllegalArgumentException.class, () -> function.floorIndexOfX(0.5));
    }


    @Test
    public void testRemoveWithInvalidIndex() {
        double[] xValues = {1.0, 2.0, 3.0};
        double[] yValues = {4.0, 5.0, 6.0};
        ArrayTabulatedFunction function = new ArrayTabulatedFunction(xValues, yValues);

        assertThrows(IllegalArgumentException.class, () -> function.remove(-1));
        assertThrows(IllegalArgumentException.class, () -> function.remove(3));
    }

    @Test
    public void testGetNodeWithInvalidIndex() {
        double[] xValues = {1.0, 2.0, 3.0};
        double[] yValues = {4.0, 5.0, 6.0};
        ArrayTabulatedFunction function = new ArrayTabulatedFunction(xValues, yValues);

        assertThrows(IllegalArgumentException.class, () -> function.getX(-1));
        assertThrows(IllegalArgumentException.class, () -> function.getX(5));
    }


}