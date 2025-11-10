package ru.ssau.tk.pmi.operations;

import ru.ssau.tk.pmi.functions.*;
import ru.ssau.tk.pmi.functions.factory.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import ru.ssau.tk.pmi.concurrent.SynchronizedTabulatedFunction;

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

    @Test
    public void testDeriveSynchronously() {
        // Создаем исходную функцию
        double[] xValues = {1, 2, 3, 4};
        double[] yValues = {1, 4, 9, 16};
        ArrayTabulatedFunction function = new ArrayTabulatedFunction(xValues, yValues);

        // Создаем оператор
        TabulatedDifferentialOperator operator = new TabulatedDifferentialOperator();

        // Вычисляем производную
        TabulatedFunction derived = operator.deriveSynchronously(function);

        // Проверяем результаты
        assertEquals(3, derived.getY(0), 1e-9); // Производная в точке x=1
        assertEquals(5, derived.getY(1), 1e-9); // Производная в точке x=2
        assertEquals(7, derived.getY(2), 1e-9); // Производная в точке x=3
    }

    @Test
    public void testDeriveSynchronously_AlreadySynchronized() {
        // Создаем исходную функцию и оборачиваем её в синхронизированную обёртку
        double[] xValues = {1, 2, 3};
        double[] yValues = {2, 4, 6};
        TabulatedFunction baseFunction = new ArrayTabulatedFunction(xValues, yValues);
        SynchronizedTabulatedFunction synchronizedFunction = new SynchronizedTabulatedFunction(baseFunction);

        // Создаем оператор
        TabulatedDifferentialOperator operator = new TabulatedDifferentialOperator();

        // Вычисляем производную
        TabulatedFunction derived = operator.deriveSynchronously(synchronizedFunction);

        // Проверяем корректность вычислений (вместо проверки через рефлексию)
        assertEquals(2, derived.getY(0), 1e-9); // Производная должна быть 2
        assertEquals(2, derived.getY(1), 1e-9); // Производная должна быть 2
        assertEquals(2, derived.getY(2), 1e-9); // Производная должна быть 2
    }

    @Test
    public void testDeriveSynchronously_CompareWithRegularDerive() {
        // Создаем исходную функцию
        double[] xValues = {0, 1, 2, 3};
        double[] yValues = {0, 1, 8, 27};
        TabulatedFunction function = new ArrayTabulatedFunction(xValues, yValues);

        // Создаем оператор
        TabulatedDifferentialOperator operator = new TabulatedDifferentialOperator();

        // Вычисляем производную обоими методами
        TabulatedFunction derivedRegular = operator.derive(function);
        TabulatedFunction derivedSynchronously = operator.deriveSynchronously(function);

        // Проверяем, что результаты одинаковы
        assertEquals(derivedSynchronously.getCount(), derivedRegular.getCount());

        for (int i = 0; i < derivedRegular.getCount(); i++) {
            assertEquals(derivedSynchronously.getX(i), derivedRegular.getX(i), 1e-9);
            assertEquals(derivedSynchronously.getY(i), derivedRegular.getY(i), 1e-9);
        }
    }
}