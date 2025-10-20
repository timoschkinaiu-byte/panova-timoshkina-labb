package ru.ssau.tk.pmi.concurrent;

import ru.ssau.tk.pmi.functions.TabulatedFunction;
import ru.ssau.tk.pmi.functions.UnitFunction;
import ru.ssau.tk.pmi.functions.LinkedListTabulatedFunction;
import java.util.LinkedList;
import java.util.List;

public class MultiplyingTaskExecutor {
    public static void main(String[] args) throws InterruptedException {
        // Создаем табулированную функцию с реализацией в виде связного списка
        TabulatedFunction function = new LinkedListTabulatedFunction(new UnitFunction(), 1, 1000, 1000);
        List<Thread> threads = new LinkedList<>();
        for (int i = 0; i < 10; i++) {
            MultiplyingTask task = new MultiplyingTask(function);
            Thread thread = new Thread(task);
            threads.add(thread);
        }
        for (Thread thread : threads) {
            thread.start();
        }
        Thread.sleep(2000);
        System.out.println("Табулированная функция после выполнения:");
        for (int i = 0; i < function.getCount(); i++) {
            System.out.printf("x = %.1f, y = %.1f%n", function.getX(i), function.getY(i));
        }
    }
}
