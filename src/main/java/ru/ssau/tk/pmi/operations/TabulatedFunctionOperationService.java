package ru.ssau.tk.pmi.operations;

import ru.ssau.tk.pmi.exceptions.InconsistentFunctionsException;
import ru.ssau.tk.pmi.functions.Point;
import ru.ssau.tk.pmi.functions.TabulatedFunction;
import ru.ssau.tk.pmi.functions.factory.ArrayTabulatedFunctionFactory;
import ru.ssau.tk.pmi.functions.factory.TabulatedFunctionFactory;

public class TabulatedFunctionOperationService {
    private TabulatedFunctionFactory factory;
    public  TabulatedFunctionOperationService(TabulatedFunctionFactory factory){
        this.factory = factory;
    }
    public TabulatedFunctionOperationService(){
        this.factory = new ArrayTabulatedFunctionFactory();
    }

    public TabulatedFunctionFactory getFactory() {
        return factory;
    }

    public void setFactory(TabulatedFunctionFactory factory) {
        this.factory = factory;
    }
    public static Point [] asPoints(TabulatedFunction tabulatedFunction){
        int i =0;
        Point [] mas = new Point [tabulatedFunction.getCount()];
        for(Point point: tabulatedFunction){
            mas[i] = point;
            i++;
        }
        return mas;
    }
    public TabulatedFunction add (TabulatedFunction f1, TabulatedFunction f2){
        if( f1.getCount() != f2.getCount()){
            throw new InconsistentFunctionsException();
        }
        Point [] f1_mas = asPoints(f1);
        Point [] f2_mas = asPoints(f2);
        double [] xValues = new double[f1_mas.length];
        double [] yValues = new double[f1_mas.length];
        for(int i=0;i<f1_mas.length;i++){
            if(f1_mas[i].x != f2_mas[i].x){
                throw new InconsistentFunctionsException();
            }
            xValues [i] = f1_mas[i].x;
            yValues [i] = f1_mas[i].y + f2_mas[i].y;
        }
        return factory.create(xValues,yValues);
    }
    public TabulatedFunction subtraction (TabulatedFunction f1, TabulatedFunction f2){
        if( f1.getCount() != f2.getCount()){
            throw new InconsistentFunctionsException();
        }
        Point [] f1_mas = asPoints(f1);
        Point [] f2_mas = asPoints(f2);
        double [] xValues = new double[f1_mas.length];
        double [] yValues = new double[f1_mas.length];
        for(int i=0;i<f1_mas.length;i++){
            if(f1_mas[i].x != f2_mas[i].x){
                throw new InconsistentFunctionsException();
            }
            xValues [i] = f1_mas[i].x;
            yValues [i] = f1_mas[i].y - f2_mas[i].y;
        }
        return factory.create(xValues,yValues);
    }
    private interface BiOperation{
        public double apply(double u, double v);
    }
    private TabulatedFunction doOperation(TabulatedFunction f1, TabulatedFunction f2, BiOperation operation){
        if( f1.getCount() != f2.getCount()){
            throw new InconsistentFunctionsException();
        }
        Point [] f1_mas = asPoints(f1);
        Point [] f2_mas = asPoints(f2);
        double [] xValues = new double[f1_mas.length];
        double [] yValues = new double[f1_mas.length];
        for(int i=0;i<f1_mas.length;i++){
            if(f1_mas[i].x != f2_mas[i].x){
                throw new InconsistentFunctionsException();
            }
            xValues [i] = f1_mas[i].x;
            yValues [i] = operation.apply(f1_mas[i].y, f2_mas[i].y);
        }
        return factory.create(xValues,yValues);
    }
    public TabulatedFunction add2 (TabulatedFunction f1, TabulatedFunction f2){
        return doOperation(f1, f2, new BiOperation() {
            @Override
            public double apply(double f1, double f2) {
                return f1+f2;
            }
        });
    }
    public TabulatedFunction subtraction2 (TabulatedFunction f1, TabulatedFunction f2){
        return doOperation(f1, f2, new BiOperation() {
            @Override
            public double apply(double f1, double f2) {
                return f1-f2;
            }
        });
    }

}
