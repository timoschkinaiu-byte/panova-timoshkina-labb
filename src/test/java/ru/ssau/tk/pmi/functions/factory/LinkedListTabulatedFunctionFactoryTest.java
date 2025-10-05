package ru.ssau.tk.pmi.functions.factory;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import ru.ssau.tk.pmi.functions.TabulatedFunction;
import ru.ssau.tk.pmi.functions.LinkedListTabulatedFunction;

class LinkedListTabulatedFunctionFactoryTest {

    @Test
    public void testLinkedListTabulatedFunctionFactory() {
        TabulatedFunctionFactory factory = new LinkedListTabulatedFunctionFactory();
        double[] xValues = {1.0, 21.0, 3.7};
        double[] yValues = {8.0, 5.0, 6.0};

        TabulatedFunction function = factory.create(xValues, yValues);

        assertTrue(function instanceof LinkedListTabulatedFunction);
    }
}