package ru.ssau.tk.pmi.functions;
import ru.ssau.tk.pmi.exceptions.ArrayIsNotSortedException;
import ru.ssau.tk.pmi.exceptions.DifferentLengthOfArraysException;
import java.lang.String;

public abstract class AbstractTabulatedFunction implements TabulatedFunction{
    protected abstract int floorIndexOfX(double x);
    protected abstract double extrapolateLeft(double x);
    protected abstract double extrapolateRight(double x);
    protected abstract double interpolate(double x, int floorIndex);
    protected double interpolate(double x, double leftX, double rightX, double leftY, double rightY) {
        return leftY + (rightY - leftY) * (x - leftX) / (rightX - leftX);
    }
    public static void checkLengthsIsTheSame(double [] xValues, double [] yValues){
        if(xValues.length != yValues.length){
            throw new DifferentLengthOfArraysException();
        }
    }
    public static void checkSorted(double [] xValues){
        for(int i=0;i<(xValues.length-1);i++){
            if(!(xValues[i]<xValues[i+1])){
                throw new ArrayIsNotSortedException();
            }
        }
    }
    @Override
    public double apply(double x) {
        if (x < leftBound())
            return extrapolateLeft(x);
        if (x > rightBound())
            return extrapolateRight(x);
        int index = indexOfX(x);
        if (index != -1) {
            return getY(index);
        }
        else {
            int floorIndex = floorIndexOfX(x);
            return interpolate(x, floorIndex);
        }
    }

    public String ToString(){
        StringBuilder str = new StringBuilder();
        str.append(getClass().getSimpleName()).append(" size = ").append(getCount());

        for (Point point : this) {
            str.append("\n").append("[").append(point.x).append("; ").append(point.y).append("]");
        }
        return str.toString();
    }
}

