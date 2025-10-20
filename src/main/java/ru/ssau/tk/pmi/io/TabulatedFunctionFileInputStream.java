package ru.ssau.tk.pmi.io;

import ru.ssau.tk.pmi.functions.TabulatedFunction;
import ru.ssau.tk.pmi.functions.factory.ArrayTabulatedFunctionFactory;
import ru.ssau.tk.pmi.functions.factory.LinkedListTabulatedFunctionFactory;
import ru.ssau.tk.pmi.operations.TabulatedDifferentialOperator;
import java.io.*;

public class TabulatedFunctionFileInputStream {

    public static void main(String[] args) {

        //чтение из файла
        try(FileInputStream fileInput = new FileInputStream("input/binary function.bin");
            BufferedInputStream BufFileInput = new BufferedInputStream(fileInput);
        ){
            ArrayTabulatedFunctionFactory factory = new ArrayTabulatedFunctionFactory();
            TabulatedFunction func = FunctionsIO.readTabulatedFunction(BufFileInput, factory);
            System.out.println(factory.toString());

        }
        catch (IOException e) {
            e.printStackTrace();
        }

        //чтение из консоли
        System.out.println("\nВведите размер и значения функции:");

        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            int count = Integer.parseInt(reader.readLine());


            double[] xValues = new double[count];
            double[] yValues = new double[count];

            for (int i = 0; i < count; i++) {
                String line = reader.readLine();
                String[] parts = line.split(" ");
                xValues[i] = Double.parseDouble(parts[0]);
                yValues[i] = Double.parseDouble(parts[1]);
            }


            LinkedListTabulatedFunctionFactory linkedListFactory = new LinkedListTabulatedFunctionFactory();
            TabulatedFunction consoleFunction = linkedListFactory.create(xValues, yValues);

            // Вычисляем производную
            TabulatedDifferentialOperator operator = new TabulatedDifferentialOperator();
            TabulatedFunction derivative = operator.derive(consoleFunction);

            System.out.println("Производная функции:");
            System.out.println(derivative.toString());

        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
