package ru.ssau.tk.pmi.io;

import ru.ssau.tk.pmi.functions.ArrayTabulatedFunction;
import ru.ssau.tk.pmi.functions.LinkedListTabulatedFunction;
import ru.ssau.tk.pmi.functions.TabulatedFunction;
import java.io.*;

public class TabulatedFunctionFileWriter {
    public static void main(String[] args) {
        double[] xValues = {1.0, 2.0, 3.0, 4.0};
        double[] yValues = {1.0, 4.0, 9.0, 16.0};
        TabulatedFunction arrayFunction = new ArrayTabulatedFunction(xValues, yValues);
        TabulatedFunction linkedListFunction = new LinkedListTabulatedFunction(xValues, yValues);
        try (
                FileWriter arrayWriter = new FileWriter("output/array function.txt");
                FileWriter linkedListWriter = new FileWriter("output/linked list function.txt");
                BufferedWriter bufferedArrayWriter = new BufferedWriter(arrayWriter);
                BufferedWriter bufferedLinkedListWriter = new BufferedWriter(linkedListWriter)
        ) {
            FunctionsIO.writeTabulatedFunction(bufferedArrayWriter, arrayFunction);
            FunctionsIO.writeTabulatedFunction(bufferedLinkedListWriter, linkedListFunction);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
