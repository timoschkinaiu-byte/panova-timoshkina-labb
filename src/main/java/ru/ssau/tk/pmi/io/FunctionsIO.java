package ru.ssau.tk.pmi.io;

import ru.ssau.tk.pmi.functions.Point;
import ru.ssau.tk.pmi.functions.TabulatedFunction;
import ru.ssau.tk.pmi.functions.factory.TabulatedFunctionFactory;

import java.io.*;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;

public final class FunctionsIO {
    private FunctionsIO() {
        throw new UnsupportedOperationException();
    }

    public static void writeTabulatedFunction(BufferedWriter writer, TabulatedFunction function) {
        PrintWriter printWriter = new PrintWriter(writer);
        printWriter.println(function.getCount());
        for (Point point : function) {
            printWriter.printf("%f %f\n", point.x, point.y);
        }
        printWriter.flush();
    }

    public static TabulatedFunction readTabulatedFunction(BufferedReader reader, TabulatedFunctionFactory factory) throws IOException {
            String line = reader.readLine();
            int count = Integer.parseInt(line.trim());
            double[] xValues = new double[count];
            double[] yValues = new double[count];
            NumberFormat formatter = NumberFormat.getInstance(Locale.forLanguageTag("ru"));
            for (int i = 0; i < count; i++) {
                line = reader.readLine();
                String[] parts = line.split(" ");
                try {
                    xValues[i] = formatter.parse(parts[0]).doubleValue();
                    yValues[i] = formatter.parse(parts[1]).doubleValue();
                } catch (ParseException e) {
                    throw new IOException();
                }
            }
            return factory.create(xValues, yValues);

    }
    public static void serialize(BufferedOutputStream stream, TabulatedFunction function) throws IOException {
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(stream);
        objectOutputStream.writeObject(function);
        objectOutputStream.flush();
    }
}