package ru.ssau.tk.pmi.io;

import ru.ssau.tk.pmi.functions.TabulatedFunction;
import ru.ssau.tk.pmi.functions.factory.ArrayTabulatedFunctionFactory;
import ru.ssau.tk.pmi.functions.factory.LinkedListTabulatedFunctionFactory;
import ru.ssau.tk.pmi.functions.factory.TabulatedFunctionFactory;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
public class TabulatedFunctionFileReader {
    public static void main(String[] args) {
        try (FileReader fileReader1 = new FileReader("input/function.txt");
             BufferedReader reader1 = new BufferedReader(fileReader1);
             FileReader fileReader2 = new FileReader("input/function.txt");
             BufferedReader reader2 = new BufferedReader(fileReader2)) {
            TabulatedFunctionFactory arrayFactory = new ArrayTabulatedFunctionFactory();
            TabulatedFunctionFactory linkedListFactory = new LinkedListTabulatedFunctionFactory();
            // Читаем ArrayTabulatedFunction из первого потока
            TabulatedFunction arrayFunction = FunctionsIO.readTabulatedFunction(reader1, arrayFactory);
            System.out.println("ArrayTabulatedFunction:");
            System.out.println(arrayFunction.toString());
            TabulatedFunction linkedListFunction = FunctionsIO.readTabulatedFunction(reader2, linkedListFactory);
            System.out.println("LinkedListTabulatedFunction:");
            System.out.println(linkedListFunction.toString());

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
