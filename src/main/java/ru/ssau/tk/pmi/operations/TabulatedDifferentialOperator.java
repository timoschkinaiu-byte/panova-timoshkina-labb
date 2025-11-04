package ru.ssau.tk.pmi.operations;
import ru.ssau.tk.pmi.functions.TabulatedFunction;
import ru.ssau.tk.pmi.functions.factory.*;
import ru.ssau.tk.pmi.functions.Point;
import ru.ssau.tk.pmi.concurrent.SynchronizedTabulatedFunction;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class TabulatedDifferentialOperator implements DifferentialOperator<TabulatedFunction>{
    private TabulatedFunctionFactory factory;
    private static final Logger logger = LogManager.getLogger(TabulatedDifferentialOperator.class);

    public TabulatedDifferentialOperator(TabulatedFunctionFactory factory){
        logger.info("Создание TabulatedDifferentialOperator с фабрикой: {}", factory.getClass().getSimpleName());
        this.factory = factory;
    }

    public TabulatedDifferentialOperator(){
        factory = new ArrayTabulatedFunctionFactory();
    }

    @Override
    public TabulatedFunction derive(TabulatedFunction function) {
        logger.info("Вычисление производной для функции с {} точками", function.getCount());
        Point[] points = TabulatedFunctionOperationService.asPoints(function);
        int pointCount = points.length;

        double[] xValues = new double[pointCount];
        double[] yValues = new double[pointCount];

        for (int i = 0; i < pointCount; i++) {
            xValues[i] = points[i].x;
        }

        // Вычисляем производные
        for (int i = 0; i < pointCount - 1; i++) {
            double deltaX = points[i + 1].x - points[i].x;
            double deltaY = points[i + 1].y - points[i].y;
            yValues[i] = deltaY / deltaX;
        }

        yValues[pointCount - 1] = yValues[pointCount - 2];

        logger.info("Производная успешно вычислена, создана новая функция");
        return factory.create(xValues, yValues);
    }

    public void setFactory(TabulatedFunctionFactory factory){
        this.factory = factory;
    }

    public TabulatedFunctionFactory getFactory(){
        return factory;
    }
    public TabulatedFunction deriveSynchronously(TabulatedFunction function) {
        logger.info("Синхронное вычисление производной для функции с {} точками", function.getCount());
        // Проверяем, является ли функция уже синхронизированной обёрткой
        SynchronizedTabulatedFunction synchronizedFunction = (function instanceof SynchronizedTabulatedFunction)
                ? (SynchronizedTabulatedFunction) function
                : new SynchronizedTabulatedFunction(function);

        // Вызываем операцию вычисления производной внутри синхронизированного блока
        logger.info("Синхронное вычисление производной завершено");
        return synchronizedFunction.doSynchronously(func -> derive(func));
    }

}
