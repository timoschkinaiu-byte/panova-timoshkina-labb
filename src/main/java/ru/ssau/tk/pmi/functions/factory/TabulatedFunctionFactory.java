package ru.ssau.tk.pmi.functions.factory;

import ru.ssau.tk.pmi.functions.TabulatedFunction;

public interface TabulatedFunctionFactory {
    TabulatedFunction create(double[] xValues, double[] yValues);

}
