package ru.ssau.tk.pmi.integration;
import ru.ssau.tk.pmi.functions.TabulatedFunction;
import java.util.concurrent.*;
import java.util.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MultiThreadIntegralSolver implements IntegralSolver {
    private final int workerCount;
    private static final Logger logger = LogManager.getLogger(MultiThreadIntegralSolver.class);

    public MultiThreadIntegralSolver(int workerCount) {
        logger.info("Создание MultiThreadIntegralSolver с {} потоками", workerCount);
        this.workerCount = Math.max(1, workerCount);
    }

    public MultiThreadIntegralSolver() {
        int processors = Runtime.getRuntime().availableProcessors();
        logger.info("Создание MultiThreadIntegralSolver с {} потоками (по умолчанию)", processors);
        this.workerCount = processors;
    }

    @Override
    public double computeIntegral(TabulatedFunction func) {
        logger.info("Вычисление интеграла для функции с {} точками, потоков: {}", func.getCount(), workerCount);

        int totalSegments = func.getCount() - 1;
        if (totalSegments < 1) {
            logger.warn("Недостаточно точек для вычисления интеграла");
            return 0.0;
        }

        ExecutorService workers = Executors.newFixedThreadPool(workerCount);
        List<Future<Double>> pendingResults = new ArrayList<>();

        int baseSegmentsPerWorker = totalSegments / workerCount;
        int extraSegments = totalSegments % workerCount;

        logger.debug("Распределение сегментов: базово {}, дополнительно {}", baseSegmentsPerWorker, extraSegments);

        int currentStart = 0;
        for (int workerIndex = 0; workerIndex < workerCount; workerIndex++) {
            int segmentsForThisWorker = baseSegmentsPerWorker;
            if (workerIndex < extraSegments) {
                segmentsForThisWorker++;
            }

            if (segmentsForThisWorker == 0) break;

            int currentEnd = currentStart + segmentsForThisWorker - 1;
            if (currentEnd >= totalSegments) {
                currentEnd = totalSegments - 1;
            }

            SegmentCalculator task = new SegmentCalculator(func, currentStart, currentEnd);
            pendingResults.add(workers.submit(task));
            currentStart = currentEnd + 1;
        }

        double finalResult = 0.0;
        for (Future<Double> pending : pendingResults) {
            try {
                finalResult += pending.get();
            } catch (InterruptedException | ExecutionException e) {
                workers.shutdown();
                throw new RuntimeException("Integration failed", e);
            }
        }

        workers.shutdown();
        logger.info("Интеграл вычислен: {}", finalResult);
        return finalResult;
    }
}