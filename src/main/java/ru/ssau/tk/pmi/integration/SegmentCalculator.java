package ru.ssau.tk.pmi.integration;

import ru.ssau.tk.pmi.functions.TabulatedFunction;
import java.util.concurrent.Callable;

public class SegmentCalculator implements Callable<Double> {
    private final TabulatedFunction func;
    private final int firstSegment;
    private final int lastSegment;

    public SegmentCalculator(TabulatedFunction func, int firstSegment, int lastSegment) {
        this.func = func;
        this.firstSegment = firstSegment;
        this.lastSegment = lastSegment;
    }

    @Override
    public Double call() {
        double segmentSum = 0.0;

        for (int i = firstSegment; i <= lastSegment; i++) {
            double segmentStart = func.getX(i);
            double segmentEnd = func.getX(i + 1);
            double segmentHeight = func.getY(i + 1); // правые прямоугольники

            segmentSum += (segmentEnd - segmentStart) * segmentHeight;
        }

        String threadName = Thread.currentThread().getName();
        System.out.printf("Segment %d-%d calculated by %s: %.6f%n",
                firstSegment, lastSegment, threadName, segmentSum);

        return segmentSum;
    }
}