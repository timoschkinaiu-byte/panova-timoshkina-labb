package ru.ssau.tk.pmi.io;

import ru.ssau.tk.pmi.functions.ArrayTabulatedFunction;
import ru.ssau.tk.pmi.functions.TabulatedFunction;
import ru.ssau.tk.pmi.functions.factory.ArrayTabulatedFunctionFactory;
import ru.ssau.tk.pmi.operations.TabulatedDifferentialOperator;

import java.io.*;

public class ArrayTabulatedFunctionSerialization {
    public static void main(String[] args) {
        double[] xValues = {0.0, 1.0, 2.0, 3.0, 4.0};
        double[] yValues = {0.0, 1.0, 4.0, 9.0, 16.0}; // f(x) = x^2
        ArrayTabulatedFunction function = new ArrayTabulatedFunction(xValues, yValues);
        TabulatedDifferentialOperator differentialOperator = new TabulatedDifferentialOperator(new ArrayTabulatedFunctionFactory());
        TabulatedFunction firstDerivative = differentialOperator.derive(function);
        TabulatedFunction secondDerivative = differentialOperator.derive(firstDerivative);
        try (FileOutputStream fileOutputStream = new FileOutputStream("output/serialized array functions.bin");
             BufferedOutputStream bufferedStream = new BufferedOutputStream(fileOutputStream)) {

            FunctionsIO.serialize(bufferedStream, function);
            FunctionsIO.serialize(bufferedStream, firstDerivative);
            FunctionsIO.serialize(bufferedStream, secondDerivative);

        } catch (IOException e) {
            e.printStackTrace();
        }
        try (FileInputStream fileInputStream = new FileInputStream("output/serialized array functions.bin"); BufferedInputStream bufferedStream = new BufferedInputStream(fileInputStream)) {
            TabulatedFunction deserializedFunction = FunctionsIO.deserialize(bufferedStream);
            TabulatedFunction deserializedFirstDerivative = FunctionsIO.deserialize(bufferedStream);
            TabulatedFunction deserializedSecondDerivative = FunctionsIO.deserialize(bufferedStream);
            System.out.println(deserializedFunction.toString());
            System.out.println(deserializedFirstDerivative.toString());
            System.out.println(deserializedSecondDerivative.toString());

        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
}
