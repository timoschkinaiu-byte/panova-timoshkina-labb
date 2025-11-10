package ru.ssau.tk.pmi.functions;

public class SimpleIterationMethod implements MathFunction {
    private  final double precision;
    private final int maxIterations;
    private final MathFunction phifunction;
    public SimpleIterationMethod(double precision, int maxIterations, MathFunction phifunction){
        this.maxIterations = maxIterations;
        this.phifunction= phifunction;
        this.precision= precision;
    }
    public double apply(double x0){
        double current = x0;
        double next;
        for(int i=0;i<maxIterations;i++){
            next = phifunction.apply(current);
            if(java.lang.Math.abs(next - current) < precision){
                return next;
            }
            current = next;
        }
        return current;
    }
}

