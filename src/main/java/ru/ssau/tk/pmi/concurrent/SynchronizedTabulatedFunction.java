package ru.ssau.tk.pmi.concurrent;

import ru.ssau.tk.pmi.functions.TabulatedFunction;
import ru.ssau.tk.pmi.functions.Point;
import ru.ssau.tk.pmi.operations.TabulatedFunctionOperationService;

import java.util.Iterator;
import java.util.NoSuchElementException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SynchronizedTabulatedFunction implements TabulatedFunction {
    private final TabulatedFunction func;
    private static final Logger logger = LogManager.getLogger(SynchronizedTabulatedFunction.class);

    public SynchronizedTabulatedFunction(TabulatedFunction func){
        this.func = func;
    }

    @Override
    public synchronized int getCount() {
        return func.getCount();
    }

    @Override
    public synchronized double getX(int index) {
        return func.getX(index);
    }

    @Override
    public synchronized double getY(int index) {
        return func.getY(index);
    }

    @Override
    public synchronized void setY(int index, double value) {
        func.setY(index, value);
    }

    @Override
    public synchronized int indexOfX(double x) {
        return func.indexOfX(x);
    }

    @Override
    public synchronized int indexOfY(double y) {
        return func.indexOfY(y);
    }

    @Override
    public synchronized double leftBound() {
        return func.leftBound();
    }

    @Override
    public synchronized double rightBound() {
        return func.rightBound();
    }

    @Override
    public synchronized double apply(double x) {
        logger.debug("Вычисление apply(x={})", x);
        return func.apply(x);
    }

    @Override
    public synchronized Iterator<Point> iterator() {
        logger.debug("Создание потокобезопасного итератора");
        Point[] pointsCopy = TabulatedFunctionOperationService.asPoints(func);

        return new Iterator<Point>() {
            private int currentIndex = 0;

            @Override
            public boolean hasNext() {
                return currentIndex < pointsCopy.length;
            }

            @Override
            public Point next() {
                if (!hasNext()) {
                    logger.warn("Попытка получить следующий элемент при отсутствии элементов");
                    throw new NoSuchElementException();
                }
                return pointsCopy[currentIndex++];
            }
        };
    }
    public interface Operation<T> {
        T apply(SynchronizedTabulatedFunction function);
    }
    public <T> T doSynchronously(Operation<T> operation) {
        synchronized (this) {
            return operation.apply(this);
        }
    }
}
