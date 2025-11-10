package ru.ssau.tk.pmi.integration;

import ru.ssau.tk.pmi.functions.TabulatedFunction;

public interface IntegralSolver {
    double computeIntegral(TabulatedFunction func);
}
