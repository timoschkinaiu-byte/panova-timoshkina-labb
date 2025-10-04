package functions;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class SimpleIterationMethodTest {

    // Вспомогательные классы функций
    static class CosFunction implements MathFunction {
        public double apply(double x) {
            return Math.cos(x);
        }
    }

    static class SqrtFunction implements MathFunction {
        public double apply(double x) {
            return (x + 2.0 / x) / 2.0;
        }
    }

    static class IdentityFunction implements MathFunction {
        public double apply(double x) {
            return x;
        }
    }

    static class ConstantFunction implements MathFunction {
        public double apply(double x) {
            return 42.0;
        }
    }

    static class SlowConvergenceFunction implements MathFunction {
        public double apply(double x) {
            return x + 0.001;
        }
    }

    static class LinearFunction implements MathFunction {
        public double apply(double x) {
            return 0.5 * x + 1;
        }
    }

    @Test
    public void testSolveCosEquation() {
        MathFunction cosFunction = new CosFunction();
        SimpleIterationMethod solver = new SimpleIterationMethod(0.0001, 1000, cosFunction);
        double result = solver.apply(0.5);

        assertEquals(0.739085, result, 0.001);
    }

    @Test
    public void testSolveSquareRootOfTwo() {
        MathFunction sqrtFunction = new SqrtFunction();
        SimpleIterationMethod solver = new SimpleIterationMethod(0.0001, 1000, sqrtFunction);
        double result = solver.apply(1.0);

        assertEquals(1.41421356, result, 0.0001);
    }

    @Test
    public void testIdentityFunctionConvergesImmediately() {
        MathFunction identity = new IdentityFunction();
        SimpleIterationMethod solver = new SimpleIterationMethod(0.0001, 1000, identity);
        double result = solver.apply(5.0);

        assertEquals(5.0, result, 0.0001);
    }

    @Test
    public void testConstantFunction() {
        MathFunction constant = new ConstantFunction();
        SimpleIterationMethod solver = new SimpleIterationMethod(0.0001, 1000, constant);
        double result = solver.apply(10.0);

        assertEquals(42.0, result, 0.0001);
    }

    @Test
    public void testRespectsPrecision() {
        MathFunction cosFunction = new CosFunction();
        SimpleIterationMethod solver = new SimpleIterationMethod(0.01, 1000, cosFunction);
        double result = solver.apply(0.5);

        double next = cosFunction.apply(result);
        assertTrue(Math.abs(next - result) < 0.02);
    }

    @Test
    public void testMaxIterationsReached() {
        MathFunction slowConvergence = new SlowConvergenceFunction();
        SimpleIterationMethod solver = new SimpleIterationMethod(0.0001, 10, slowConvergence);
        double result = solver.apply(0.0);

        assertTrue(result >= 0.0 && result <= 0.1);
    }

    @Test
    public void testDifferentInitialGuess() {
        MathFunction cosFunction = new CosFunction();
        SimpleIterationMethod solver = new SimpleIterationMethod(0.0001, 1000, cosFunction);

        double result1 = solver.apply(0.1);
        double result2 = solver.apply(1.0);

        assertEquals(result1, result2, 0.001);
    }

    @Test
    public void testLinearFunction() {
        MathFunction linear = new LinearFunction();
        SimpleIterationMethod solver = new SimpleIterationMethod(0.0001, 1000, linear);
        double result = solver.apply(0.0);

        assertEquals(2.0, result, 0.0001);
    }
}