package ru.ssau.tk.pmi.io;
import ru.ssau.tk.pmi.functions.ArrayTabulatedFunction;
import ru.ssau.tk.pmi.functions.LinkedListTabulatedFunction;
import ru.ssau.tk.pmi.functions.TabulatedFunction;

import java.io.*;

public class TabulatedFunctionFileOutputStream {
    public static void main(String[] args){
        double[] xValues = {1.0, 2.0, 3.0, 4.0};
        double[] yValues = {1.0, 4.0, 9.0, 16.0};
        TabulatedFunction arrayFunction = new ArrayTabulatedFunction(xValues, yValues);
        TabulatedFunction linkedListFunction = new LinkedListTabulatedFunction(xValues, yValues);

        try(FileOutputStream outArray = new FileOutputStream("output/array function.bin");
            FileOutputStream outLinkedList = new FileOutputStream("output/linked list function.bin");
            BufferedOutputStream BufOutArray = new BufferedOutputStream(outArray);
            BufferedOutputStream BufOutLinkedList = new BufferedOutputStream(outLinkedList)
        ){
            FunctionsIO.writeTabulatedFunction(BufOutArray, arrayFunction);
            FunctionsIO.writeTabulatedFunction(BufOutLinkedList, linkedListFunction);
        }
        catch (IOException e) {
            e.printStackTrace();
        }

    }
}
