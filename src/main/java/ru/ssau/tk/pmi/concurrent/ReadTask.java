package ru.ssau.tk.pmi.concurrent;
import ru.ssau.tk.pmi.functions.TabulatedFunction;

public class ReadTask implements Runnable{
    private final TabulatedFunction func;
    public ReadTask(TabulatedFunction func){
        this.func = func;
    }

    @Override
    public void run(){
        for(int i = 0; i < func.getCount(); i++){
            double X = func.getX(i);
            double Y = func.getY(i);

            System.out.printf("After read: i = %d, x = %f, y = %f \n", i, X, Y);
        }
    }
}
