package ru.ssau.tk.pmi.integration;

import ru.ssau.tk.pmi.functions.TabulatedFunction;

public class SingleThreadIntegralSolver implements IntegralSolver {

    @Override
    public double computeIntegral(TabulatedFunction func) {
        double total = 0.0;
        int segments = func.getCount() - 1;

        for (int i = 0; i < segments; i++) {
            double leftX = func.getX(i);
            double rightX = func.getX(i + 1);
            double leftY = func.getY(i);
            double rightY = func.getY(i + 1);

            // Метод прямоугольников (правых)
            total += (rightX - leftX) * rightY;
        }

        return total;
    }
}