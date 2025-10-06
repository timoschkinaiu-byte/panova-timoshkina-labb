package ru.ssau.tk.pmi.operations;

import ru.ssau.tk.pmi.functions.*;
import ru.ssau.tk.pmi.functions.factory.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;


public class TabulatedDifferentialOperatorTest {
    @Test
    public void testDeriveWithArrayFactory() {
        TabulatedFunctionFactory factory = new ArrayTabulatedFunctionFactory();
        TabulatedDifferentialOperator operator = new TabulatedDifferentialOperator(factory);

        // Создаем простую линейную функцию f(x) = 2x
        double[] xValues = {0.0, 1.0, 2.0, 3.0};
        double[] yValues = {0.0, 2.0, 4.0, 6.0};
        TabulatedFunction function = factory.create(xValues, yValues);

        // Вычисляем производную (должна быть постоянной = 2)
        TabulatedFunction derivative = operator.derive(function);

        // Проверяем, что производная ≈ 2 во всех точках
        for (int i = 0; i < derivative.getCount(); i++) {
            assertEquals(derivative.getY(i), 2.0, 0.0001, "Производная должна быть равна 2");
        }
    }

    @Test
    public void testDeriveWithLinkedListFactory() {
        TabulatedFunctionFactory factory = new LinkedListTabulatedFunctionFactory();
        TabulatedDifferentialOperator operator = new TabulatedDifferentialOperator(factory);

        // Тестируем на квадратичной функции f(x) = x²
        double[] xValues = {0.0, 1.0, 2.0, 3.0};
        double[] yValues = {0.0, 1.0, 4.0, 9.0};
        TabulatedFunction function = factory.create(xValues, yValues);

        TabulatedFunction derivative = operator.derive(function);

        // Проверяем приближенные значения производной f'(x) = 2x
        assertEquals(derivative.getY(0), 1.0, 0.1);  // f'(0.5) ≈ 1.0
        assertEquals(derivative.getY(1), 3.0, 0.1);  // f'(1.5) ≈ 3.0
        assertEquals(derivative.getY(2), 5.0, 0.1);  // f'(2.5) ≈ 5.0
        assertEquals(derivative.getY(3), 5.0, 0.1);  // Последняя точка такая же как предпоследняя
    }

    @Test
    public void testDefaultConstructor() {
        TabulatedDifferentialOperator operator = new TabulatedDifferentialOperator();

        // Проверяем, что по умолчанию используется ArrayTabulatedFunctionFactory
        assertTrue(operator.getFactory() instanceof ArrayTabulatedFunctionFactory);
    }

    @Test
    public void testSetterAndGetter() {
        TabulatedDifferentialOperator operator = new TabulatedDifferentialOperator();

        // Меняем фабрику
        TabulatedFunctionFactory newFactory = new LinkedListTabulatedFunctionFactory();
        operator.setFactory(newFactory);

        // Проверяем, что фабрика изменилась
        assertSame(operator.getFactory(), newFactory);
    }

    @Test
    public void testDeriveWithDifferentFunctionTypes() {
        TabulatedDifferentialOperator operator = new TabulatedDifferentialOperator();

        // Создаем функцию через Array фабрику
        TabulatedFunctionFactory arrayFactory = new ArrayTabulatedFunctionFactory();
        double[] xValues = {0.0, 1.0, 2.0};
        double[] yValues = {0.0, 1.0, 4.0};
        TabulatedFunction arrayFunction = arrayFactory.create(xValues, yValues);

        // Вычисляем производную
        TabulatedFunction derivative = operator.derive(arrayFunction);

        // Проверяем, что производная имеет правильный тип
        assertTrue(derivative instanceof ArrayTabulatedFunction);

        // Меняем фабрику на LinkedList
        operator.setFactory(new LinkedListTabulatedFunctionFactory());
        TabulatedFunction linkedListDerivative = operator.derive(arrayFunction);

        // Проверяем, что теперь производная имеет тип LinkedList
        assertTrue(linkedListDerivative instanceof LinkedListTabulatedFunction);
    }
}