package ru.ssau.tk.pmi.operations;
import ru.ssau.tk.pmi.functions.TabulatedFunction;
import ru.ssau.tk.pmi.functions.factory.*;
import ru.ssau.tk.pmi.functions.Point;

public class TabulatedDifferentialOperator implements DifferentialOperator<TabulatedFunction>{
    private TabulatedFunctionFactory factory;

    public TabulatedDifferentialOperator(TabulatedFunctionFactory factory){
        this.factory = factory;
    }

    public TabulatedDifferentialOperator(){
        factory = new ArrayTabulatedFunctionFactory();
    }

    @Override
    public TabulatedFunction derive(TabulatedFunction function) {
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

        return factory.create(xValues, yValues);
    }

    public void setFactory(TabulatedFunctionFactory factory){
        this.factory = factory;
    }

    public TabulatedFunctionFactory getFactory(){
        return factory;
    }

}
