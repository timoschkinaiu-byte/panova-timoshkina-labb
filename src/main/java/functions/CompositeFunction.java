package functions;

public class CompositeFunction implements MathFunction {
    private final MathFunction firstFunction;
    private final MathFunction secondFunction;
    public CompositeFunction(MathFunction firstFunction, MathFunction secondFunction) {
        this.firstFunction = firstFunction;
        this.secondFunction = secondFunction;
    }
    @Override
    public double apply(double x) {
        return secondFunction.apply(firstFunction.apply(x));
    }
    public MathFunction getFirstFunction() {
        return firstFunction;
    }
    public MathFunction getSecondFunction() {
        return secondFunction;
    }
}