package ru.ssau.tk.pmi.functions;
import  ru.ssau.tk.pmi.exceptions.ArrayIsNotSortedException;
import ru.ssau.tk.pmi.exceptions.DifferentLengthOfArraysException;
import ru.ssau.tk.pmi.exceptions.InterpolationException;
import java.util.Iterator;
import java.util.Arrays;
import java.util.NoSuchElementException;

public class ArrayTabulatedFunction extends AbstractTabulatedFunction implements Insertable, Removable, Iterable<Point>{
    private double[] xValues;
    private double[] yValues;
    private int count;
    public ArrayTabulatedFunction(double[] xValues, double[] yValues) {
        AbstractTabulatedFunction.checkLengthsIsTheSame(xValues, yValues);
        AbstractTabulatedFunction.checkSorted(xValues);
        this.xValues = Arrays.copyOf(xValues, xValues.length);
        this.yValues = Arrays.copyOf(yValues, yValues.length);
        this.count = xValues.length;
    }
    public Iterator<Point> iterator() {
        return new Iterator<Point>() {
            private int i=0;
            @Override
            public boolean hasNext() {
                return i<count;
            }

            @Override
            public Point next() {
                if(!hasNext()){
                    throw  new NoSuchElementException();
                }
                Point point = new Point(xValues[i],yValues[i]);
                i++;
                return point;
            }
        };
    }
    public ArrayTabulatedFunction(MathFunction source, double xFrom, double xTo, int count) {
        this.count = count;
        this.xValues = new double[count];
        this.yValues = new double[count];
        if (xFrom > xTo) {
            double temp = xFrom;
            xFrom = xTo;
            xTo = temp;
        }
        if (xFrom == xTo) {
            double value = source.apply(xFrom);
            Arrays.fill(xValues, xFrom);
            Arrays.fill(yValues, value);
        } else {
            double step = (xTo - xFrom) / (count - 1);
            for (int i = 0; i < count; i++) {
                xValues[i] = xFrom + i * step;
                yValues[i] = source.apply(xValues[i]);
            }
        }
    }
    @Override
    public int getCount() {
        return count;
    }
    @Override
    public double getX(int index) {
        return xValues[index];
    }
    @Override
    public double getY(int index) {
        return yValues[index];
    }
    @Override
    public void setY(int index, double value) {
        yValues[index] = value;
    }
    @Override
    public int indexOfX(double x) {
        for (int i = 0; i < count; i++) {
            if (xValues[i] == x) {
                return i;
            }
        }
        return -1;
    }
    @Override
    public int indexOfY(double y) {
        for (int i = 0; i < count; i++) {
            if (yValues[i] == y) {
                return i;
            }
        }
        return -1;
    }
    @Override
    public double leftBound() {
        return xValues[0];
    }
    @Override
    public double rightBound() {
        return xValues[count - 1];
    }
    @Override
    protected int floorIndexOfX(double x) {
        if (x < xValues[0]) {
            return 0;
        }
        if (x >= xValues[count - 1]) {
            return count;
        }
        for (int i = 0; i < count - 1; i++) {
            if (x >= xValues[i] && x < xValues[i + 1]) {
                return i;
            }
        }
        return count - 2;
    }
    @Override
    protected double extrapolateLeft(double x) {
        if (count == 1) {
            return yValues[0];
        }
        return interpolate(x, xValues[0], xValues[1], yValues[0], yValues[1]);
    }
    @Override
    protected double extrapolateRight(double x) {
        if (count == 1) {
            return yValues[0];
        }
        return interpolate(x, xValues[count - 2], xValues[count - 1], yValues[count - 2], yValues[count - 1]);
    }
    @Override
    protected double interpolate(double x, int floorIndex) {
        if(!(xValues[floorIndex]<=x && x<=xValues[floorIndex+1])){
            throw new InterpolationException();
        }
        if (count == 1) {
            return yValues[0];
        }
        return interpolate(x, xValues[floorIndex], xValues[floorIndex + 1], yValues[floorIndex], yValues[floorIndex + 1]);
    }

    public void insert(double x, double y){

        int index = indexOfX(x);

        if (index == -1){

            int floorInd; //позиция для вставки
            if (x < xValues[0]) {
                floorInd = 0;
            } else if (x > xValues[count - 1]) {
                floorInd = count;
            } else {
                floorInd = floorIndexOfX(x) + 1;
            }


            double[] newXValues = new double[count+1];
            double[] newYValues = new double[count+1];
            System.arraycopy(xValues, 0, newXValues, 0, floorInd);
            newXValues[floorInd] = x;
            System.arraycopy(xValues, floorInd , newXValues, floorInd + 1, count - floorInd);

            System.arraycopy(yValues, 0, newYValues, 0, floorInd);
            newYValues[floorInd] = y;
            System.arraycopy(yValues, floorInd , newYValues, floorInd + 1, count - floorInd);

            this.xValues = newXValues;
            this.yValues = newYValues;
            this.count ++;
        }

        else{
            yValues[index] = y;
        }
    }


    public void remove(int index) {
        if (index < 0 || index >= count) {
            return;
        }
        for (int i = index; i < count - 1; i++) {
            xValues[i] = xValues[i + 1];
            yValues[i] = yValues[i + 1];
        }
        count--;
    }
}

