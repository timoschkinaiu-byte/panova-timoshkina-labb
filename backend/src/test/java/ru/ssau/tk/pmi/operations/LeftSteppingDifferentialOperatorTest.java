package ru.ssau.tk.pmi.operations;

import org.junit.jupiter.api.Test;
import ru.ssau.tk.pmi.functions.MathFunction;
import ru.ssau.tk.pmi.functions.SqrFunction;
import ru.ssau.tk.pmi.functions.IdentityFunction;

import static org.junit.jupiter.api.Assertions.*;

public class LeftSteppingDifferentialOperatorTest {

    @Test
    public void testLeftSteppingDifferentialOperatorWithSqrFunction() {
        double step = 0.1;
        LeftSteppingDifferentialOperator operator = new LeftSteppingDifferentialOperator(step);
        MathFunction sqrFunction = new SqrFunction();
        MathFunction derivative = operator.derive(sqrFunction);
        double x = 2.0;
        double expected = (sqrFunction.apply(x) - sqrFunction.apply(x - step)) / step;
        double result = derivative.apply(x);
        assertEquals(expected, result, 1e-10);
        assertEquals(4.0, result, 0.1); // 2x при x=2 должно быть около 4.0
    }

    @Test
    public void testLeftSteppingDifferentialOperatorWithIdentityFunction() {
        double step = 0.5;
        LeftSteppingDifferentialOperator operator = new LeftSteppingDifferentialOperator(step);
        MathFunction identityFunction = new IdentityFunction(); // f(x) = x
        MathFunction derivative = operator.derive(identityFunction);
        // Для f(x) = x производная f'(x) = 1
        double x = 5.0;
        double result = derivative.apply(x);
        assertEquals(1.0, result, 1e-10); // Должно быть точно 1.0 для линейной функции
    }

}