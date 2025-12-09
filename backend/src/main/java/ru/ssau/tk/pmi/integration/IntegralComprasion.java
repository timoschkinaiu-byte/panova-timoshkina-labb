package ru.ssau.tk.pmi.integration;

import ru.ssau.tk.pmi.functions.TabulatedFunction;
import ru.ssau.tk.pmi.functions.ArrayTabulatedFunction;

class IntegralComparison {
    public static void main(String[] args) {
        double[] xPoints = {0.0, 0.5, 1.0, 1.5, 2.0, 2.5, 3.0};
        double[] yPoints = {0.0, 0.25, 1.0, 2.25, 4.0, 6.25, 9.0};

        TabulatedFunction testFunction = new ArrayTabulatedFunction(xPoints, yPoints);

        System.out.println("Сравнение методов вычисления интеграла (f(x) = x²)");

        IntegralSolver[] solvers = {
                new SingleThreadIntegralSolver(),
                new MultiThreadIntegralSolver(2),
                new MultiThreadIntegralSolver(4)
        };

        String[] names = {"Однопоточный", "Двухпоточный", "Четырехпоточный"};

        for (int i = 0; i < solvers.length; i++) {
            long start = System.nanoTime();
            double value = solvers[i].computeIntegral(testFunction);
            long duration = System.nanoTime() - start;

            System.out.printf("%s: результат = %.6f, время = %d нс%n",
                    names[i], value, duration);
        }
    }
}

