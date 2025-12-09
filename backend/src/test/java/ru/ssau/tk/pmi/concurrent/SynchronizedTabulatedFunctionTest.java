
package ru.ssau.tk.pmi.concurrent;

import org.junit.jupiter.api.Test;
import ru.ssau.tk.pmi.functions.TabulatedFunction;
import ru.ssau.tk.pmi.functions.ArrayTabulatedFunction;
import ru.ssau.tk.pmi.functions.LinkedListTabulatedFunction;
import ru.ssau.tk.pmi.functions.Point;
import static org.junit.jupiter.api.Assertions.*;
import java.util.Iterator;
import java.util.NoSuchElementException;

public class SynchronizedTabulatedFunctionTest {

    @Test
    public void testSynchronizedArrayFunction() {
        double[] xValues = {1.0, 2.0, 3.0};
        double[] yValues = {4.0, 5.0, 6.0};

        TabulatedFunction arrayFunction = new ArrayTabulatedFunction(xValues, yValues);
        SynchronizedTabulatedFunction syncFunction = new SynchronizedTabulatedFunction(arrayFunction);

        assertEquals(3, syncFunction.getCount());
        assertEquals(1.0, syncFunction.getX(0), 1e-9);
        assertEquals(4.0, syncFunction.getY(0), 1e-9);
        assertEquals(1.0, syncFunction.leftBound(), 1e-9);
        assertEquals(3.0, syncFunction.rightBound(), 1e-9);
    }

    @Test
    public void testSynchronizedLinkedListFunction() {
        double[] xValues = {1.0, 2.0, 3.0};
        double[] yValues = {4.0, 5.0, 6.0};

        TabulatedFunction linkedListFunction = new LinkedListTabulatedFunction(xValues, yValues);
        SynchronizedTabulatedFunction syncFunction = new SynchronizedTabulatedFunction(linkedListFunction);

        assertEquals(3, syncFunction.getCount());
        assertEquals(1, syncFunction.indexOfX(2.0));
        assertEquals(0, syncFunction.indexOfY(4.0));
    }

    @Test
    public void testSetY() {
        double[] xValues = {1.0, 2.0, 3.0};
        double[] yValues = {4.0, 5.0, 6.0};

        TabulatedFunction arrayFunction = new ArrayTabulatedFunction(xValues, yValues);
        SynchronizedTabulatedFunction syncFunction = new SynchronizedTabulatedFunction(arrayFunction);

        syncFunction.setY(1, 10.0);
        assertEquals(10.0, syncFunction.getY(1), 1e-9);
    }

    @Test
    public void testApply() {
        double[] xValues = {1.0, 2.0, 3.0};
        double[] yValues = {4.0, 5.0, 6.0};

        TabulatedFunction arrayFunction = new ArrayTabulatedFunction(xValues, yValues);
        SynchronizedTabulatedFunction syncFunction = new SynchronizedTabulatedFunction(arrayFunction);

        assertEquals(4.0, syncFunction.apply(1.0), 1e-9);
        assertEquals(5.0, syncFunction.apply(2.0), 1e-9);
        assertEquals(6.0, syncFunction.apply(3.0), 1e-9);
    }

    @Test
    public void testIteratorWithArrayFunction() {
        double[] xValues = {1.0, 2.0, 3.0};
        double[] yValues = {4.0, 5.0, 6.0};

        TabulatedFunction arrayFunction = new ArrayTabulatedFunction(xValues, yValues);
        SynchronizedTabulatedFunction syncFunction = new SynchronizedTabulatedFunction(arrayFunction);

        int index = 0;
        for (Point point : syncFunction) {
            assertEquals(xValues[index], point.x, 1e-9);
            assertEquals(yValues[index], point.y, 1e-9);
            index++;
        }
        assertEquals(3, index);
    }

    @Test
    public void testIteratorWithLinkedListFunction() {
        double[] xValues = {0.5, 1.5, 2.5};
        double[] yValues = {1.0, 2.0, 3.0};

        TabulatedFunction linkedListFunction = new LinkedListTabulatedFunction(xValues, yValues);
        SynchronizedTabulatedFunction syncFunction = new SynchronizedTabulatedFunction(linkedListFunction);

        Iterator<Point> iterator = syncFunction.iterator();
        assertTrue(iterator.hasNext());

        Point point1 = iterator.next();
        assertEquals(0.5, point1.x, 1e-9);
        assertEquals(1.0, point1.y, 1e-9);

        Point point2 = iterator.next();
        assertEquals(1.5, point2.x, 1e-9);
        assertEquals(2.0, point2.y, 1e-9);

        Point point3 = iterator.next();
        assertEquals(2.5, point3.x, 1e-9);
        assertEquals(3.0, point3.y, 1e-9);

        assertFalse(iterator.hasNext());
    }

    @Test
    public void testIteratorOnTwoPointsFunction() {

        double[] xValues = {1.0, 2.0};
        double[] yValues = {3.0, 4.0};

        TabulatedFunction arrayFunction = new ArrayTabulatedFunction(xValues, yValues);
        SynchronizedTabulatedFunction syncFunction = new SynchronizedTabulatedFunction(arrayFunction);

        Iterator<Point> iterator = syncFunction.iterator();
        assertTrue(iterator.hasNext());
        iterator.next();
        iterator.next();
        assertFalse(iterator.hasNext());
    }

    @Test
    public void testIteratorThrowsExceptionWhenNoMoreElements() {
        double[] xValues = {1.0, 2.0};
        double[] yValues = {3.0, 4.0};

        TabulatedFunction arrayFunction = new ArrayTabulatedFunction(xValues, yValues);
        SynchronizedTabulatedFunction syncFunction = new SynchronizedTabulatedFunction(arrayFunction);

        Iterator<Point> iterator = syncFunction.iterator();
        iterator.next();
        iterator.next();

        assertThrows(NoSuchElementException.class, () -> iterator.next());
    }

    @Test
    public void testIteratorIsIndependentCopy() {
        double[] xValues = {1.0, 2.0, 3.0};
        double[] yValues = {4.0, 5.0, 6.0};

        TabulatedFunction arrayFunction = new ArrayTabulatedFunction(xValues, yValues);
        SynchronizedTabulatedFunction syncFunction = new SynchronizedTabulatedFunction(arrayFunction);


        Iterator<Point> iterator = syncFunction.iterator();

        syncFunction.setY(1, 10.0);

        Point point1 = iterator.next();
        assertEquals(1.0, point1.x, 1e-9);
        assertEquals(4.0, point1.y, 1e-9);

        Point point2 = iterator.next();
        assertEquals(2.0, point2.x, 1e-9);
        assertEquals(5.0, point2.y, 1e-9);
    }

    @Test
    public void testMultipleIteratorsAreIndependent() {
        double[] xValues = {1.0, 2.0};
        double[] yValues = {3.0, 4.0};

        TabulatedFunction arrayFunction = new ArrayTabulatedFunction(xValues, yValues);
        SynchronizedTabulatedFunction syncFunction = new SynchronizedTabulatedFunction(arrayFunction);

        Iterator<Point> iterator1 = syncFunction.iterator();
        Iterator<Point> iterator2 = syncFunction.iterator();

        Point point1FromIterator1 = iterator1.next();
        Point point1FromIterator2 = iterator2.next();

        assertEquals(point1FromIterator1.x, point1FromIterator2.x, 1e-9);
        assertEquals(point1FromIterator1.y, point1FromIterator2.y, 1e-9);

        iterator1.next();
        assertFalse(iterator1.hasNext());
        assertTrue(iterator2.hasNext());
    }
    @Test
    public void testDoSynchronouslyWithReturnValue() {
        TabulatedFunction baseFunction = new ArrayTabulatedFunction(new double[]{1, 2, 3}, new double[]{1, 2, 3});
        SynchronizedTabulatedFunction syncFunction = new SynchronizedTabulatedFunction(baseFunction);

        Double result = syncFunction.doSynchronously(func -> {
            return func.getX(0) + func.getY(0);
        });

        assertEquals(result, 2.0);
    }
    @Test
    public void testDoSynchronouslyWithVoid() {
        TabulatedFunction baseFunction = new ArrayTabulatedFunction(new double[]{1, 2, 3}, new double[]{1, 2, 3});
        SynchronizedTabulatedFunction syncFunction = new SynchronizedTabulatedFunction(baseFunction);
        Void result = syncFunction.doSynchronously(func -> {
            func.setY(0, 10.0);
            return null;
        });
        assertNull(result);
        assertEquals(syncFunction.getY(0), 10.0);
    }
}



