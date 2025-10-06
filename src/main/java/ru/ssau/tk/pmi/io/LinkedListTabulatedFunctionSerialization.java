package ru.ssau.tk.pmi.io;

import ru.ssau.tk.pmi.functions.TabulatedFunction;
import ru.ssau.tk.pmi.functions.LinkedListTabulatedFunction;
import ru.ssau.tk.pmi.operations.TabulatedDifferentialOperator;
import java.io.*;

public class LinkedListTabulatedFunctionSerialization {

    public static void main(String[] args) {

        double[] xValues = {1.0, 2.0, 3.0, 4.0, 5.0};
        double[] yValues = {1.5, 4.6, 9.8, 56.0, 25.0};
        TabulatedFunction originalFunction = new LinkedListTabulatedFunction(xValues, yValues);

        // Создаем дифференциальный оператор
        TabulatedDifferentialOperator operator = new TabulatedDifferentialOperator();

        // Находим первую и вторую производные
        TabulatedFunction firstDerivative = operator.derive(originalFunction);
        TabulatedFunction secondDerivative = operator.derive(firstDerivative);

        // Сериализация функций в файл
        try (FileOutputStream fileStream = new FileOutputStream("output/serialized linked list functions.bin");
             BufferedOutputStream bufferedStream = new BufferedOutputStream(fileStream)) {

            FunctionsIO.serialize(bufferedStream, originalFunction);
            FunctionsIO.serialize(bufferedStream, firstDerivative);
            FunctionsIO.serialize(bufferedStream, secondDerivative);

            System.out.println("Функции успешно сериализованы в файл");

        } catch (IOException e) {
            e.printStackTrace();
        }

        // Десериализация функций из файла
        try (FileInputStream fileStream = new FileInputStream("output/serialized linked list functions.bin");
             BufferedInputStream bufferedStream = new BufferedInputStream(fileStream)) {

            TabulatedFunction deserializedOriginal = FunctionsIO.deserialize(bufferedStream);
            TabulatedFunction deserializedFirstDerivative = FunctionsIO.deserialize(bufferedStream);
            TabulatedFunction deserializedSecondDerivative = FunctionsIO.deserialize(bufferedStream);

            // Вывод функций в консоль
            System.out.println("\nИсходная функция:");
            System.out.println(deserializedOriginal.toString());

            System.out.println("\nПервая производная:");
            System.out.println(deserializedFirstDerivative.toString());

            System.out.println("\nВторая производная:");
            System.out.println(deserializedSecondDerivative.toString());

        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
}