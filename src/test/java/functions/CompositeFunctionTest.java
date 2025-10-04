package functions;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
public class CompositeFunctionTest {
    static class PlusOneFunction implements MathFunction {
        public double apply(double x) {
            return x + 1;
        }
    }
    static class TimesTwoFunction implements MathFunction {
        public double apply(double x) {
            return x * 2;
        }
    }
    static class MinusFiveFunction implements MathFunction {
        public double apply(double x) {
            return x - 5;
        }
    }
    static class TimesThreeFunction implements MathFunction {
        public double apply(double x) {
            return x * 3;
        }
    }
    static class PlusTwoFunction implements MathFunction {
        public double apply(double x) {
            return x + 2;
        }
    }
    static class DivideByTwoFunction implements MathFunction {
        public double apply(double x) {
            return x / 2;
        }
    }
    static class MinusThreeFunction implements MathFunction {
        public double apply(double x) {
            return x - 3;
        }
    }
    static class CosFunction implements MathFunction {
        public double apply(double x) {
            return Math.cos(x);
        }
    }
    @Test
    public void testSimpleAndThen() {
        MathFunction f = new SqrFunction();
        MathFunction g = new IdentityFunction();

        MathFunction composite = f.andThen(g);
        assertEquals(25.0, composite.apply(5.0), 0.0001);
        assertEquals(9.0, composite.apply(3.0), 0.0001);
    }
    @Test
    public void testDoubleComposition() {
        MathFunction f = new SqrFunction();
        MathFunction g = new PlusOneFunction();
        MathFunction h = new TimesTwoFunction();
        MathFunction composite = f.andThen(g).andThen(h);
        assertEquals(52.0, composite.apply(5.0), 0.0001);
        assertEquals(20.0, composite.apply(3.0), 0.0001);
    }
    @Test
    public void testTripleComposition() {
        MathFunction f = new SqrFunction();
        MathFunction g = new PlusOneFunction();
        MathFunction h = new TimesTwoFunction();
        MathFunction i = new MinusFiveFunction();

        MathFunction composite = f.andThen(g).andThen(h).andThen(i);
        assertEquals(47.0, composite.apply(5.0), 0.0001);
        assertEquals(15.0, composite.apply(3.0), 0.0001);
    }
    @Test
    public void testWithConstantFunction() {
        MathFunction constant = new ConstantFunction(5.0);
        MathFunction sqr = new SqrFunction();
        MathFunction composite = constant.andThen(sqr);
        assertEquals(25.0, composite.apply(10.0), 0.0001);
        assertEquals(25.0, composite.apply(-5.0), 0.0001);
    }
    @Test
    public void testWithZeroFunction() {
        MathFunction zero = new ZeroFunction();
        MathFunction sqr = new SqrFunction();
        MathFunction composite = zero.andThen(sqr);
        assertEquals(0.0, composite.apply(100.0), 0.0001);
        assertEquals(0.0, composite.apply(-50.0), 0.0001);
    }
    @Test
    public void testChainWithDifferentFunctions() {
        MathFunction f = new TimesThreeFunction();
        MathFunction g = new PlusTwoFunction();
        MathFunction h = new DivideByTwoFunction();
        MathFunction composite = f.andThen(g).andThen(h);
        assertEquals(8.5, composite.apply(5.0), 0.0001);
        assertEquals(4.0, composite.apply(2.0), 0.0001);
    }
    @Test
    public void testCompositeFunctionStructure() {
        MathFunction f = new SqrFunction();
        MathFunction g = new IdentityFunction();
        CompositeFunction composite = f.andThen(g);
        assertInstanceOf(CompositeFunction.class, composite);
        assertEquals(f, composite.getFirstFunction());
        assertEquals(g, composite.getSecondFunction());
    }
    @Test
    public void testMultipleAndThenCalls() {
        MathFunction f = new PlusOneFunction();
        MathFunction g = new TimesTwoFunction();
        MathFunction h = new MinusThreeFunction();
        MathFunction composite1 = f.andThen(g).andThen(h);
        assertEquals(9.0, composite1.apply(5.0), 0.0001);
    }
    @Test
    public void testComplexMathematicalChain() {
        MathFunction sqr = new SqrFunction();
        MathFunction plusOne = new PlusOneFunction();
        MathFunction cos = new CosFunction();
        MathFunction composite = sqr.andThen(plusOne).andThen(cos);
        double result = composite.apply(0.0);
        assertEquals(Math.cos(1.0), result, 0.0001);
    }

    @Test
    public void testWithBSpline() {
        double[] nodes = {0.0, 0.0, 1.0, 2.0, 2.0};
        double[] weights = {1.0, 2.0, 3.0};
        MathFunction spline = new BSplineFunction(nodes, 1, weights);
        MathFunction square = new SqrFunction();

        CompositeFunction composite = new CompositeFunction(square, spline);

        double result = composite.apply(1.0);
        assertTrue(result >= 0.0);
    }

    @Test
    public void testComplexNestedComposition() {

        MathFunction addOne = x -> x + 1;
        MathFunction multiplyTwo = x -> x * 2;

        MathFunction square = new SqrFunction();

        CompositeFunction inner = new CompositeFunction(addOne, multiplyTwo);
        CompositeFunction outer = new CompositeFunction(inner, square);

        assertEquals(4.0, outer.apply(0.0), 1e-9);
        assertEquals(16.0, outer.apply(1.0), 1e-9);
        assertEquals(36.0, outer.apply(2.0), 1e-9);
    }

    @Test
    public void testMultipleComposition() {
        MathFunction addOne = x -> x + 1;
        MathFunction multiplyTwo = x -> x * 2;
        MathFunction subtractThree = x -> x - 3;

        CompositeFunction firstComposite = new CompositeFunction(addOne, multiplyTwo);
        CompositeFunction finalComposite = new CompositeFunction(firstComposite, subtractThree);

        assertEquals(-1.0, finalComposite.apply(0.0), 1e-9);  // ((0+1)*2)-3 = -1
        assertEquals(1.0, finalComposite.apply(1.0), 1e-9);   // ((1+1)*2)-3 = 1
        assertEquals(5.0, finalComposite.apply(2.0), 1e-9);   // ((2+1)*2)-3 = 3
    }
    @Test
    public  void testTwoArrayTabledFunctions(){
        double[] xValues1 = {1,2,3};
        double[] yValues1 = {10,20,30};
        double[] xValues2 = {10,20,30};
        double[] yValues2 = {100,200,300};
        ArrayTabulatedFunction func1 =  new ArrayTabulatedFunction(xValues1,yValues1);
        ArrayTabulatedFunction func2 =  new ArrayTabulatedFunction(xValues2,yValues2);
        CompositeFunction func = new CompositeFunction(func1,func2);
        assertEquals(100, func.apply(1), 0.001);
    }
    @Test
    public  void testArrayAndLinkedFunctions(){
        double [] xValues1 = {1,2,3,4};
        double [] yValues1 = {10,20,30,40};
        double [] xValues2 = {1,2,3,4};
        double [] yValues2 = {100,200,300,400};
        ArrayTabulatedFunction func1 = new ArrayTabulatedFunction(xValues1,yValues1);
        LinkedListTabulatedFunction func2 = new LinkedListTabulatedFunction(xValues2,yValues2);
        CompositeFunction func = new CompositeFunction(func1,func2);
        assertEquals(1000, func.apply(1), 0.001);
    }

    @Test
    public void testInterpolationConsistencyBetweenImplementations() {

        double[] xValues = {0.5, 1.7, 2.9, 4.1, 5.3};
        double[] yValues = {2.3, 4.1, 1.8, 5.6, 3.2};

        ArrayTabulatedFunction arrayFunc = new ArrayTabulatedFunction(xValues, yValues);
        LinkedListTabulatedFunction linkedListFunc = new LinkedListTabulatedFunction(xValues, yValues);

        double testX1 = 1.2;
        double testX2 = 3.5;
        double testX3 = 4.8;

        assertEquals(arrayFunc.apply(testX1), linkedListFunc.apply(testX1), 1e-9);
        assertEquals(arrayFunc.apply(testX2), linkedListFunc.apply(testX2), 1e-9);
        assertEquals(arrayFunc.apply(testX3), linkedListFunc.apply(testX3), 1e-9);
    }


    @Test
    public void testIndexSearchConsistency() {
        // Проверка согласованности поиска индексов
        double[] xValues = {1.3, 2.7, 4.2, 5.8, 7.1};
        double[] yValues = {2.1, 4.8, 1.3, 5.9, 3.4};

        ArrayTabulatedFunction arrayFunc = new ArrayTabulatedFunction(xValues, yValues);
        LinkedListTabulatedFunction linkedListFunc = new LinkedListTabulatedFunction(xValues, yValues);

        assertEquals(arrayFunc.indexOfX(2.7), linkedListFunc.indexOfX(2.7));
        assertEquals(arrayFunc.indexOfX(5.8), linkedListFunc.indexOfX(5.8));
        assertEquals(arrayFunc.indexOfX(3.0), linkedListFunc.indexOfX(3.0)); // не существует

        assertEquals(arrayFunc.indexOfY(4.8), linkedListFunc.indexOfY(4.8));
        assertEquals(arrayFunc.indexOfY(3.4), linkedListFunc.indexOfY(3.4));
        assertEquals(arrayFunc.indexOfY(6.0), linkedListFunc.indexOfY(6.0)); // не существует

        double testX1 = 3.5;
        double testX2 = 6.5;

        assertEquals(arrayFunc.apply(testX1), linkedListFunc.apply(testX1), 1e-9);
        assertEquals(arrayFunc.apply(testX2), linkedListFunc.apply(testX2), 1e-9);
    }

    @Test
    public void testSinglePointFunctionHandling() {
        //Проверка обработки функции с одной точкой
        double[] singleX = {2.5};
        double[] singleY = {7.3};

        ArrayTabulatedFunction arraySingle = new ArrayTabulatedFunction(singleX, singleY);
        LinkedListTabulatedFunction linkedListSingle = new LinkedListTabulatedFunction(singleX, singleY);


        assertEquals(7.3, arraySingle.apply(0.0), 1e-9);
        assertEquals(7.3, arraySingle.apply(2.5), 1e-9);
        assertEquals(7.3, arraySingle.apply(5.0), 1e-9);

        assertEquals(linkedListSingle.apply(0.0), arraySingle.apply(0.0), 1e-9);
        assertEquals(linkedListSingle.apply(2.5), arraySingle.apply(2.5), 1e-9);
        assertEquals(linkedListSingle.apply(5.0), arraySingle.apply(5.0), 1e-9);
    }
}