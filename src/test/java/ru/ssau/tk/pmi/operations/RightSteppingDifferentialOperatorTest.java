package ru.ssau.tk.pmi.operations;

import org.junit.jupiter.api.Test;
import ru.ssau.tk.pmi.functions.MathFunction;
import ru.ssau.tk.pmi.functions.SqrFunction;
import ru.ssau.tk.pmi.functions.IdentityFunction;

import static org.junit.jupiter.api.Assertions.*;

public class RightSteppingDifferentialOperatorTest {
    @Test
    public void testRightSteppingDifferentialOperatorWithSqrFunction() {
        double step = 0.1;
        RightSteppingDifferentialOperator operator = new RightSteppingDifferentialOperator(step);
        MathFunction sqrFunction = new SqrFunction();
        MathFunction derivative = operator.derive(sqrFunction);
        double x = 3.0;
        double result = derivative.apply(x);
        assertEquals(6.1, result, 1e-10);
    }

    @Test
    public void testRightSteppingDifferentialOperatorWithIdentityFunction() {
        double step = 0.5;
        RightSteppingDifferentialOperator operator = new RightSteppingDifferentialOperator(step);
        MathFunction identityFunction = new IdentityFunction(); // f(x) = x
        MathFunction derivative = operator.derive(identityFunction);
        double x = 7.0;
        double result = derivative.apply(x);
        assertEquals(1.0, result, 1e-10);
    }


}