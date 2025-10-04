package ru.ssau.tk.pmi.functions;

import org.junit.jupiter.api.Test;
import ru.ssau.tk.pmi.functions.ConstantFunction;
import ru.ssau.tk.pmi.functions.UnitFunction;
import ru.ssau.tk.pmi.functions.ZeroFunction;

import static org.junit.jupiter.api.Assertions.*;

public class ConstantFunctionTest {

    @Test
    public void testConstantFunctionWithDifferentValues() {
        ConstantFunction func1 = new ConstantFunction(5.5);
        ConstantFunction func2 = new ConstantFunction(-3.2);
        ConstantFunction func3 = new ConstantFunction(0.0);

        assertEquals(5.5, func1.apply(10.0), 0.0001);
        assertEquals(5.5, func1.apply(-5.0), 0.0001);

        assertEquals(-3.2, func2.apply(100.0), 0.0001);
        assertEquals(-3.2, func2.apply(0.0), 0.0001);

        assertEquals(0.0, func3.apply(7.0), 0.0001);
        assertEquals(0.0, func3.apply(-2.0), 0.0001);
    }

    @Test
    public void testConstantFunctionGetConstant() {
        ConstantFunction function = new ConstantFunction(3.14);
        assertEquals(3.14, function.getConstant(), 0.0001);
    }

    @Test
    public void testZeroFunctionAlwaysReturnsZero() {
        ZeroFunction zero = new ZeroFunction();

        assertEquals(0.0, zero.apply(0.0), 0.0001);
        assertEquals(0.0, zero.apply(10.0), 0.0001);
        assertEquals(0.0, zero.apply(-5.0), 0.0001);
        assertEquals(0.0, zero.apply(100.0), 0.0001);
    }

    @Test
    public void testUnitFunctionAlwaysReturnsOne() {
        UnitFunction unit = new UnitFunction();

        assertEquals(1.0, unit.apply(0.0), 0.0001);
        assertEquals(1.0, unit.apply(7.0), 0.0001);
        assertEquals(1.0, unit.apply(-2.0), 0.0001);
        assertEquals(1.0, unit.apply(50.0), 0.0001);
    }

    @Test
    public void testZeroFunctionGetConstant() {
        ZeroFunction zero = new ZeroFunction();
        assertEquals(0.0, zero.getConstant(), 0.0001);
    }

    @Test
    public void testUnitFunctionGetConstant() {
        UnitFunction unit = new UnitFunction();
        assertEquals(1.0, unit.getConstant(), 0.0001);
    }

    @Test
    public void testMultipleCallsConsistency() {
        ConstantFunction constFunc = new ConstantFunction(42.0);
        ZeroFunction zeroFunc = new ZeroFunction();
        UnitFunction unitFunc = new UnitFunction();
        // Проверяем, что многократные вызовы с разными аргументами
        // возвращают одно и то же значение
        for (int i = 0; i < 5; i++) {
            double testValue = i * 10 - 20; // Разные тестовые значения
            assertEquals(42.0, constFunc.apply(testValue), 0.0001);
            assertEquals(0.0, zeroFunc.apply(testValue), 0.0001);
            assertEquals(1.0, unitFunc.apply(testValue), 0.0001);
        }
    }

    @Test
    public void testNoNewFieldsInSubclasses() {
        ZeroFunction zero = new ZeroFunction();
        UnitFunction unit = new UnitFunction();

        // Проверяем, что геттер возвращает ожидаемые значения
        // без дополнительных полей в подклассах
        assertEquals(0.0, zero.getConstant(), 0.0001);
        assertEquals(1.0, unit.getConstant(), 0.0001);
    }
}
