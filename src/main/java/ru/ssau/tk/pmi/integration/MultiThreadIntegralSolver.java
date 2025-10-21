package ru.ssau.tk.pmi.integration;

import ru.ssau.tk.pmi.functions.TabulatedFunction;
import java.util.concurrent.*;
import java.util.*;

public class MultiThreadIntegralSolver implements IntegralSolver {
    private final int workerCount;

    public MultiThreadIntegralSolver(int workerCount) {
        this.workerCount = Math.max(1, workerCount);
    }

    public MultiThreadIntegralSolver() {
        this(Runtime.getRuntime().availableProcessors()); // Исправлено здесь
    }

    @Override
    public double computeIntegral(TabulatedFunction func) {
        int totalSegments = func.getCount() - 1;
        if (totalSegments < 1) {
            return 0.0;
        }

        ExecutorService workers = Executors.newFixedThreadPool(workerCount);
        List<Future<Double>> pendingResults = new ArrayList<>();

        int baseSegmentsPerWorker = totalSegments / workerCount;
        int extraSegments = totalSegments % workerCount;

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
        return finalResult;
    }
}
