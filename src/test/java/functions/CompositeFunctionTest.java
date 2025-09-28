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
}