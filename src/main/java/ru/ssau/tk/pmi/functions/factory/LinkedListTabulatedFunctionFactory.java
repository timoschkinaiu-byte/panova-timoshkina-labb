package ru.ssau.tk.pmi.functions.factory;
import ru.ssau.tk.pmi.functions.TabulatedFunction;
import ru.ssau.tk.pmi.functions.LinkedListTabulatedFunction;

public class LinkedListTabulatedFunctionFactory implements TabulatedFunctionFactory{
    public TabulatedFunction create(double[] xValues, double[] yValues){
        return new LinkedListTabulatedFunction(xValues, yValues);
    }
}
