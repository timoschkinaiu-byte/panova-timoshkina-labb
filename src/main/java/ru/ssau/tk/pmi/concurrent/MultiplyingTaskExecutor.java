package ru.ssau.tk.pmi.concurrent;

import ru.ssau.tk.pmi.functions.TabulatedFunction;
import ru.ssau.tk.pmi.functions.UnitFunction;
import ru.ssau.tk.pmi.functions.LinkedListTabulatedFunction;
import java.util.*;

public class MultiplyingTaskExecutor {
    public static void main(String[] args) {
        // Создаем табулированную функцию с реализацией в виде связного списка
        TabulatedFunction function = new LinkedListTabulatedFunction(new UnitFunction(), 1, 1000, 1000);

        // Набор для хранения активных потоков
        Collection<Thread> activeThreads = Collections.synchronizedCollection(new LinkedList<>());
        List<Thread> threads = new LinkedList<>();
        int numberOfThreads = 10;

        // Создаем задачи и потоки
        for (int i = 0; i < numberOfThreads; i++) {
            MultiplyingTask task = new MultiplyingTask(function);
            Thread thread = new Thread(task);
            threads.add(thread);
            activeThreads.add(thread);
        }

        // Стартуем все потоки
        for (Thread thread : threads) {
            thread.start();
        }

        System.out.println("Ожидаем завершения всех потоков...");

        // Активное ожидание завершения всех потоков
        while (!activeThreads.isEmpty()) {
            List<Thread> threadsToRemove = new ArrayList<>();
            for (Thread thread : activeThreads) {
                if (!thread.isAlive()) {
                    threadsToRemove.add(thread);
                }
            }
            // Удаляем завершенные потоки
            activeThreads.removeAll(threadsToRemove);
        }
        System.out.println("Табулированная функция после выполнения:");
        for (int i = 0; i < function.getCount(); i++) {
            System.out.printf("x = %.1f, y = %.1f%n", function.getX(i), function.getY(i));
        }
    }
}