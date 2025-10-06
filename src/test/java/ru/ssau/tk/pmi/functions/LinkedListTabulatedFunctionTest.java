package ru.ssau.tk.pmi.functions;

import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.BeforeEach;

import static org.junit.jupiter.api.Assertions.assertEquals;

import ru.ssau.tk.pmi.functions.*;
import ru.ssau.tk.pmi.functions.LinkedListTabulatedFunction.Node;

import static org.junit.jupiter.api.Assertions.*;
import ru.ssau.tk.pmi.exceptions.InterpolationException;

import java.util.Iterator;
public class LinkedListTabulatedFunctionTest {

    private LinkedListTabulatedFunction function;
    private final double[] xValues = {1.3, 2.7, 4.1, 5.5, 6.9};
    private final double[] yValues = {3.8, 7.2, 9.6, 12.4, 15.1};

    @BeforeEach
    public void setUp() {
        function = new LinkedListTabulatedFunction(xValues, yValues);
    }

    // Тесты конструкторов
    @Test
    public void testConstructorFromArrays_ValidData_CreatesCorrectList() {
        assertEquals(5, function.getCount());
        assertEquals(1.3, function.leftBound(), 1e-9);
        assertEquals(6.9, function.rightBound(), 1e-9);
    }

    @Test
    public void testConstructorFromFunction_UniformSampling_CreatesCorrectList() {
        MathFunction square = new SqrFunction();
        LinkedListTabulatedFunction func = new LinkedListTabulatedFunction(square, 1.5, 3.5, 4);

        assertEquals(4, func.getCount());
        assertEquals(1.5, func.leftBound(), 1e-9);
        assertEquals(3.5, func.rightBound(), 1e-9);
        assertEquals(2.25, func.getY(0), 1e-9);
        assertEquals(12.25, func.getY(3), 1e-9);
    }

    @Test
    public void testConstructorFromFunction_ReversedBounds_CorrectsOrder() {
        MathFunction identity = new IdentityFunction();
        LinkedListTabulatedFunction func = new LinkedListTabulatedFunction(identity, 2.4, 7.2, 4);

        assertEquals(2.4, func.leftBound(), 1e-9);
        assertEquals(7.2, func.rightBound(), 1e-9);
    }

    @Test
    public void testConstructorFromFunction_SinglePoint_AllValuesEqual() {
        MathFunction constant = new ConstantFunction(8.7);
        LinkedListTabulatedFunction func = new LinkedListTabulatedFunction(constant, 3.3, 3.3, 4);

        assertEquals(4, func.getCount());
        assertEquals(3.3, func.getX(0), 1e-9);
        assertEquals(3.3, func.getX(1), 1e-9);
        assertEquals(3.3, func.getX(2), 1e-9);
        assertEquals(3.3, func.getX(3), 1e-9);
        assertEquals(8.7, func.getY(0), 1e-9);
        assertEquals(8.7, func.getY(1), 1e-9);
        assertEquals(8.7, func.getY(2), 1e-9);
        assertEquals(8.7, func.getY(3), 1e-9);
    }

    // Тесты основных методов доступа
    @Test
    public void testGetCount_AfterCreation_ReturnsCorrectCount() {
        assertEquals(5, function.getCount());
    }

    @Test
    public void testGetX_ValidIndex_ReturnsCorrectValue() {
        assertEquals(1.3, function.getX(0), 1e-9);
        assertEquals(2.7, function.getX(1), 1e-9);
        assertEquals(4.1, function.getX(2), 1e-9);
        assertEquals(5.5, function.getX(3), 1e-9);
        assertEquals(6.9, function.getX(4), 1e-9);
    }

    @Test
    public void testGetY_ValidIndex_ReturnsCorrectValue() {
        assertEquals(3.8, function.getY(0), 1e-9);
        assertEquals(7.2, function.getY(1), 1e-9);
        assertEquals(9.6, function.getY(2), 1e-9);
        assertEquals(12.4, function.getY(3), 1e-9);
        assertEquals(15.1, function.getY(4), 1e-9);
    }

    @Test
    public void testSetY_ValidIndex_ModifiesValue() {
        function.setY(2, 11.3);
        assertEquals(11.3, function.getY(2), 1e-9);
        // Проверяем что другие значения не изменились
        assertEquals(3.8, function.getY(0), 1e-9);
        assertEquals(7.2, function.getY(1), 1e-9);
        assertEquals(12.4, function.getY(3), 1e-9);
    }

    // Тесты граничных значений
    @Test
    public void testLeftBound_ReturnsFirstX() {
        assertEquals(1.3, function.leftBound(), 1e-9);
    }

    @Test
    public void testRightBound_ReturnsLastX() {
        assertEquals(6.9, function.rightBound(), 1e-9);
    }

    // Тесты поиска
    @Test
    public void testIndexOfX_ExistingValue_ReturnsCorrectIndex() {
        assertEquals(0, function.indexOfX(1.3));
        assertEquals(1, function.indexOfX(2.7));
        assertEquals(3, function.indexOfX(5.5));
        assertEquals(4, function.indexOfX(6.9));
    }

    @Test
    public void testIndexOfX_NonExistingValue_ReturnsMinusOne() {
        assertEquals(-1, function.indexOfX(1.0));
        assertEquals(-1, function.indexOfX(3.5));
        assertEquals(-1, function.indexOfX(7.2));
    }

    @Test
    public void testIndexOfY_ExistingValue_ReturnsCorrectIndex() {
        assertEquals(0, function.indexOfY(3.8));
        assertEquals(1, function.indexOfY(7.2));
        assertEquals(3, function.indexOfY(12.4));
        assertEquals(4, function.indexOfY(15.1));
    }

    @Test
    public void testIndexOfY_NonExistingValue_ReturnsMinusOne() {
        assertEquals(-1, function.indexOfY(2.5));
        assertEquals(-1, function.indexOfY(8.9));
        assertEquals(-1, function.indexOfY(16.0));
    }

    // Тесты floorIndexOfX

    @Test
    public void testFloorIndexOfX_GreaterThanAll_ReturnsCount() {
        assertEquals(5, function.floorIndexOfX(7.5));
    }

    @Test
    public void testFloorIndexOfX_BetweenNodes_ReturnsLeftIndex() {
        assertEquals(0, function.floorIndexOfX(1.8));
        assertEquals(1, function.floorIndexOfX(3.2));
        assertEquals(2, function.floorIndexOfX(4.7));
        assertEquals(3, function.floorIndexOfX(6.1));
    }

    @Test
    public void testFloorIndexOfX_ExactMatch_ReturnsNodeIndex() {
        assertEquals(0, function.floorIndexOfX(1.3));
        assertEquals(2, function.floorIndexOfX(4.1));
        assertEquals(4, function.floorIndexOfX(6.9));
    }

    // Тесты интерполяции
    @Test
    public void testInterpolate_BetweenNodes_ReturnsLinearValue() {
        double result = function.interpolate(3.4, 1);
        double expected = 7.2 + (9.6 - 7.2) * (3.4 - 2.7) / (4.1 - 2.7);
        assertEquals(expected, result, 1e-9);
    }

    @Test
    public void testInterpolate_AnotherInterval_ReturnsCorrectValue() {
        // Между 4.1 и 5.5: x=4.8
        double result = function.interpolate(4.8, 2);
        double expected = 9.6 + (12.4 - 9.6) * (4.8 - 4.1) / (5.5 - 4.1);
        assertEquals(expected, result, 1e-9);
    }

    // Тесты экстраполяции
    @Test
    public void testExtrapolateLeft_OutsideLeft_ReturnsCorrectValue() {
        // Экстраполяция слева от 1.3
        double result = function.extrapolateLeft(0.5);
        double expected = 3.8 + (7.2 - 3.8) * (0.5 - 1.3) / (2.7 - 1.3);
        assertEquals(expected, result, 1e-9);
    }

    @Test
    public void testExtrapolateRight_OutsideRight_ReturnsCorrectValue() {
        // Экстраполяция справа от 6.9
        double result = function.extrapolateRight(7.8);
        double expected = 12.4 + (15.1 - 12.4) * (7.8 - 5.5) / (6.9 - 5.5);
        assertEquals(expected, result, 1e-9);
    }

    // Тесты метода apply()
    @Test
    public void testApply_ExactNodeMatch_ReturnsNodeValue() {
        assertEquals(3.8, function.apply(1.3), 1e-9);
        assertEquals(7.2, function.apply(2.7), 1e-9);
        assertEquals(15.1, function.apply(6.9), 1e-9);
    }

    @Test
    public void testApply_BetweenNodes_ReturnsInterpolatedValue() {
        // Между 2.7 и 4.1
        double result = function.apply(3.4);
        double expected = 7.2 + (9.6 - 7.2) * (3.4 - 2.7) / (4.1 - 2.7);
        assertEquals(expected, result, 1e-9);
    }

    @Test
    public void testApply_LeftExtrapolation_ReturnsExtrapolatedValue() {
        double result = function.apply(0.8);
        double expected = 3.8 + (7.2 - 3.8) * (0.8 - 1.3) / (2.7 - 1.3);
        assertEquals(expected, result, 1e-9);
    }

    @Test
    public void testApply_RightExtrapolation_ReturnsExtrapolatedValue() {
        double result = function.apply(7.3);
        double expected = 12.4 + (15.1 - 12.4) * (7.3 - 5.5) / (6.9 - 5.5);
        assertEquals(expected, result, 1e-9);
    }

    // Тесты специальных случаев
    @Test
    public void testTwoNodeList_AllMethodsWork() {
        double[] twoX = {2.1, 5.7};
        double[] twoY = {4.3, 11.9};
        LinkedListTabulatedFunction twoNodeFunc = new LinkedListTabulatedFunction(twoX, twoY);

        assertEquals(2, twoNodeFunc.getCount());
        assertEquals(2.1, twoNodeFunc.leftBound(), 1e-9);
        assertEquals(5.7, twoNodeFunc.rightBound(), 1e-9);

        // Проверяем интерполяцию
        double interpolated = twoNodeFunc.apply(3.9);
        double expected = 4.3 + (11.9 - 4.3) * (3.9 - 2.1) / (5.7 - 2.1);
        assertEquals(expected, interpolated, 1e-9);
    }

    @Test
    public void testFloorNodeOfX_VariousCases_ReturnsCorrectNode() {
        Node node = function.floorNodeOfX(1.8);
        assertNotNull(node);
        assertEquals(1.3, node.x, 1e-9);

        node = function.floorNodeOfX(3.2);
        assertEquals(2.7, node.x, 1e-9);

        node = function.floorNodeOfX(6.1);
        assertEquals(5.5, node.x, 1e-9);
    }

    @Test
    public void testApply_WithOptimizedSearch_ReturnsCorrectValues() {
        assertEquals(3.8, function.apply(1.3), 1e-9);
        assertEquals(8.4, function.apply(3.4), 1e-9);

        double leftExtrapolation = function.apply(0.5);
        double rightExtrapolation = function.apply(7.8);
        assertTrue(leftExtrapolation < 3.8);
        assertTrue(rightExtrapolation > 15.1);
    }

    @Test
    public void testApply_ExactMatch_ReturnsNodeValueWithoutInterpolation() {
        assertEquals(7.2, function.apply(2.7), 1e-9);
        assertEquals(12.4, function.apply(5.5), 1e-9);
    }


    @Test
    void testInsertAtBeginning() {
        // Создаем: (1,1) → (3,9) → (5,25)
        LinkedListTabulatedFunction function = new LinkedListTabulatedFunction(
                new double[]{1.0, 3.0, 5.0},
                new double[]{1.0, 9.0, 25.0}
        );
        function.insert(-1.0, 1.0);  // Вставляем -1,1 в начало
        assertEquals(4, function.getCount());
        assertEquals(-1.0, function.getX(0));
        assertEquals(1.0, function.getX(1));
        assertEquals(1.0, function.getY(0));
    }
    @Test
    void testInsertAtEnd() {
        LinkedListTabulatedFunction function = new LinkedListTabulatedFunction(
                new double[]{1.0, 3.0, 5.0},
                new double[]{1.0, 9.0, 25.0}
        );
        function.insert(7.0, 49.0);  // Вставляем 7,49 в конец
        assertEquals(4, function.getCount());
        assertEquals(5.0, function.getX(2));
        assertEquals(7.0, function.getX(3));
        assertEquals(49.0, function.getY(3));

    }
    @Test
    void testInsertInMiddle() {
        LinkedListTabulatedFunction function = new LinkedListTabulatedFunction(
                new double[]{1.0, 3.0, 5.0},
                new double[]{1.0, 9.0, 25.0}
        );
        function.insert(2.0, 4.0);  // Вставляем 2,4 между 1 и 3
        assertEquals(4, function.getCount());
        assertEquals(1.0, function.getX(0));
        assertEquals(2.0, function.getX(1));
        assertEquals(3.0, function.getX(2));
        assertEquals(5.0, function.getX(3));
        assertEquals(4.0, function.getY(1));
    }
    @Test
    void testUpdateExistingX() {
        LinkedListTabulatedFunction function = new LinkedListTabulatedFunction(
                new double[]{1.0, 3.0, 5.0},
                new double[]{1.0, 9.0, 25.0}
        );
        function.insert(3.0, 100.0);  // Обновляем существующий X=3.0
        assertEquals(3, function.getCount());  // Количество не изменилось
        assertEquals(3.0, function.getX(1));   // X остался тот же
        assertEquals(100.0, function.getY(1)); // Y обновился
    }

    @Test
    public void removeTestMiddle() {
        double[] xValue = {2.1, 5.7, 8};
        double[] yValue = {4.3, 11.9, 78};
        LinkedListTabulatedFunction func = new LinkedListTabulatedFunction(xValue, yValue);

        func.remove(1);
        assertEquals(2, func.getCount());
        assertEquals(8, func.getX(1), 1e-9);
        assertEquals(78, func.getY(1), 1e-9);
    }

    @Test
    public void testRemoveFromBeginning() {
        // Удаление первого элемента
        double[] xValues = {1.5, 2.5, 3.5};
        double[] yValues = {15.0, 25.0, 35.0};
        LinkedListTabulatedFunction function = new LinkedListTabulatedFunction(xValues, yValues);

        function.remove(0);

        assertEquals(2, function.getCount());
        assertEquals(2.5, function.getX(0), 1e-9);
        assertEquals(3.5, function.getX(1), 1e-9);
        assertEquals(25.0, function.getY(0), 1e-9);
        assertEquals(35.0, function.getY(1), 1e-9);
    }


    @Test
    public void testRemoveFromEnd() {
        //Удаление последнего элемента
        double[] xValues = {1.2, 2.4, 3.6};
        double[] yValues = {12.0, 24.0, 36.0};
        LinkedListTabulatedFunction function = new LinkedListTabulatedFunction(xValues, yValues);

        function.remove(2);

        assertEquals(2, function.getCount());
        assertEquals(1.2, function.getX(0), 1e-9);
        assertEquals(2.4, function.getX(1), 1e-9);
        assertEquals(12.0, function.getY(0), 1e-9);
        assertEquals(24.0, function.getY(1), 1e-9);
    }


    @Test
    public void testInterpolateThrowsExceptionWhenXLessThanLeftNode() {
        double[] xValues = {1.0, 2.0, 3.0};
        double[] yValues = {1.0, 4.0, 9.0};
        LinkedListTabulatedFunction function = new LinkedListTabulatedFunction(xValues, yValues);
        assertThrows(InterpolationException.class, () -> {
            function.interpolate(0.5, 0); // x < leftNode.x
        });
    }
    @Test
    public void testInterpolateThrowsExceptionWhenXGreaterThanRightNode() {
        double[] xValues = {1.0, 2.0, 3.0};
        double[] yValues = {1.0, 4.0, 9.0};
        LinkedListTabulatedFunction function = new LinkedListTabulatedFunction(xValues, yValues);
        assertThrows(InterpolationException.class, () -> {
            function.interpolate(2.5, 0); // x > rightNode.x
        });
    }



    @Test
    public void testConstructorWithInvalidLength() {
        double[] xValues = {1.0};
        double[] yValues = {9.0};

        assertThrows(IllegalArgumentException.class, () -> {
            new LinkedListTabulatedFunction(xValues, yValues);
        });
    }

    @Test
    public void testConstructorWithSourceInvalidLength(){

        assertThrows(IllegalArgumentException.class, ()->{
            new LinkedListTabulatedFunction(new IdentityFunction(), 3, 4, 1);
        });
    }

    @Test
    public void testGetXWithInvalidIndex() {
        double[] xValues = {1.0, 2.0, 3.0};
        double[] yValues = {4.0, 5.0, 6.0};
        LinkedListTabulatedFunction function = new LinkedListTabulatedFunction(xValues, yValues);

        assertThrows(IllegalArgumentException.class, () -> function.getX(-1));
        assertThrows(IllegalArgumentException.class, () -> function.getX(3));
    }

    @Test
    public void testGetYWithInvalidIndex() {
        double[] xValues = {1.0, 2.0, 3.0};
        double[] yValues = {4.0, 5.0, 6.0};
        LinkedListTabulatedFunction function = new LinkedListTabulatedFunction(xValues, yValues);

        assertThrows(IllegalArgumentException.class, () -> function.getY(-1));
        assertThrows(IllegalArgumentException.class, () -> function.getY(3));
    }

    @Test
    public void testSetYWithInvalidIndex() {
        double[] xValues = {1.0, 2.0, 3.0};
        double[] yValues = {4.0, 5.0, 6.0};
        LinkedListTabulatedFunction function = new LinkedListTabulatedFunction(xValues, yValues);

        assertThrows(IllegalArgumentException.class, () -> function.setY(-1, 10.0));
        assertThrows(IllegalArgumentException.class, () -> function.setY(3, 10.0));
    }

    @Test
    public void testFloorIndexOfXWithXLessThanLeftBound() {
        double[] xValues = {1.0, 2.0, 3.0};
        double[] yValues = {4.0, 5.0, 6.0};
        LinkedListTabulatedFunction function = new LinkedListTabulatedFunction(xValues, yValues);

        assertThrows(IllegalArgumentException.class, () -> function.floorIndexOfX(0.5));
    }

    @Test
    public void testFloorNodeOfXWithXLessThanLeftBound() {
        double[] xValues = {1.0, 2.0, 3.0};
        double[] yValues = {4.0, 5.0, 6.0};
        LinkedListTabulatedFunction function = new LinkedListTabulatedFunction(xValues, yValues);

        assertThrows(IllegalArgumentException.class, () -> function.floorNodeOfX(0.5));
    }

    @Test
    public void testRemoveWithInvalidIndex() {
        double[] xValues = {1.0, 2.0, 3.0};
        double[] yValues = {4.0, 5.0, 6.0};
        LinkedListTabulatedFunction function = new LinkedListTabulatedFunction(xValues, yValues);

        assertThrows(IllegalArgumentException.class, () -> function.remove(-1));
        assertThrows(IllegalArgumentException.class, () -> function.remove(3));
    }

    @Test
    public void testGetNodeWithInvalidIndex() {
        double[] xValues = {1.0, 2.0, 3.0};
        double[] yValues = {4.0, 5.0, 6.0};
        LinkedListTabulatedFunction function = new LinkedListTabulatedFunction(xValues, yValues);

        assertThrows(IllegalArgumentException.class, () -> function.getX(-1));
        assertThrows(IllegalArgumentException.class, () -> function.getX(5));
    }


    //тесты итератора
    @Test
    public void testIteratorWithWhileLoop() {
        double[] xValues = {1.0, 2.0, 3.0};
        double[] yValues = {4.0, 5.0, 6.0};
        LinkedListTabulatedFunction function = new LinkedListTabulatedFunction(xValues, yValues);

        Iterator<Point> iterator = function.iterator();
        int index = 0;
        while (iterator.hasNext()) {
            Point point = iterator.next();
            assertEquals(xValues[index], point.x, 0.0001);
            assertEquals(yValues[index], point.y,0.0001);
            index++;
        }
        assertEquals(3, index);
    }

    @Test
    public void testIteratorWithForEachLoop() {
        double[] xValues = {1.0, 2.0, 3.0};
        double[] yValues = {4.0, 5.0, 6.0};
        LinkedListTabulatedFunction function = new LinkedListTabulatedFunction(xValues, yValues);

        int index = 0;
        for (Point point : function) {
            assertEquals(xValues[index],point.x,  0.0001);
            assertEquals(yValues[index], point.y,0.0001);
            index++;
        }
        assertEquals(3, index);
    }

    }
