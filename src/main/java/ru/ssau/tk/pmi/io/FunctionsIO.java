package ru.ssau.tk.pmi.io;

import ru.ssau.tk.pmi.functions.Point;
import ru.ssau.tk.pmi.functions.TabulatedFunction;
import ru.ssau.tk.pmi.functions.factory.TabulatedFunctionFactory;

import ru.ssau.tk.pmi.functions.factory.TabulatedFunctionFactory;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;
import java.io.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class FunctionsIO {
    private static final Logger logger = LogManager.getLogger(FunctionsIO.class);

    private FunctionsIO(){
        throw new UnsupportedOperationException();
    }
    public static void writeTabulatedFunction(BufferedWriter writer, TabulatedFunction function){
        logger.info("Запись функции в текстовый файл, точек: {}", function.getCount());
        PrintWriter printWriter = new PrintWriter(writer);
        printWriter.println(function.getCount());
        for(Point point: function){
            printWriter.printf("%f %f\n", point.x, point.y);
        }
        printWriter.flush();
        logger.debug("Функция записана успешно");

    }
    public static TabulatedFunction readTabulatedFunction(BufferedReader reader, TabulatedFunctionFactory factory) throws IOException {
        logger.info("Чтение функции из текстового файла");
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
                logger.error("Ошибка парсинга чисел в строке: {}", line);
                throw new IOException();
            }
        }
        TabulatedFunction result = factory.create(xValues, yValues);
        logger.info("Функция прочитана успешно");
        return result;
    }

    public static void writeTabulatedFunction(BufferedOutputStream outputStream, TabulatedFunction function) throws IOException{
        DataOutputStream out = new DataOutputStream(outputStream);
        out.writeInt(function.getCount());
        for(Point point : function){
            out.writeDouble(point.x);
            out.writeDouble(point.y);
        }
        out.flush();
    }
    public static void serialize(BufferedOutputStream stream, TabulatedFunction function) throws IOException {
        logger.info("Сериализация функции");
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(stream);
        objectOutputStream.writeObject(function);
        objectOutputStream.flush();
        logger.debug("Сериализация завершена");
    }

    public static TabulatedFunction deserialize(BufferedInputStream stream) throws IOException, ClassNotFoundException{
        ObjectInputStream objectInputStream = new ObjectInputStream(stream);
        return (TabulatedFunction) objectInputStream.readObject();
    }


    public static TabulatedFunction readTabulatedFunction(BufferedInputStream inputStream, TabulatedFunctionFactory factory)
            throws IOException {

        logger.info("Чтение функции из бинарного файла");
        DataInputStream dataIn = new DataInputStream(inputStream);

        int count = dataIn.readInt();

        double[] xValues = new double[count];
        double[] yValues = new double[count];

        for (int i = 0; i < count; i++) {
            xValues[i] = dataIn.readDouble();
            yValues[i] = dataIn.readDouble();
        }

        TabulatedFunction result = factory.create(xValues, yValues);
        logger.info("Бинарное чтение завершено");
        return result;
    }

}
