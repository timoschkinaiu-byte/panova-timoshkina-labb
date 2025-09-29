package functions;
import org.junit.jupiter.api.Test;
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
        assertEquals(0, function.floorIndexOfX(0.5));  // меньше первого - возвращает 0
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
    public void testApplySinglePoint() {
        double[] xValues = {5.0};
        double[] yValues = {25.0};
        ArrayTabulatedFunction function = new ArrayTabulatedFunction(xValues, yValues);
        assertEquals(25.0, function.apply(5.0), 0.0001);
        assertEquals(25.0, function.apply(0.0), 0.0001);
        assertEquals(25.0, function.apply(10.0), 0.0001);
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
}