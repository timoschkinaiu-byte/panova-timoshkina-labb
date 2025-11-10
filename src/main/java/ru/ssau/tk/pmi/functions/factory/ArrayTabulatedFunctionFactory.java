package ru.ssau.tk.pmi.functions.factory;
import ru.ssau.tk.pmi.functions.TabulatedFunction;
import ru.ssau.tk.pmi.functions.ArrayTabulatedFunction;

public class ArrayTabulatedFunctionFactory implements TabulatedFunctionFactory{

    public TabulatedFunction create(double[] xValues, double[] yValues){
        return new ArrayTabulatedFunction(xValues, yValues);
    }
}
