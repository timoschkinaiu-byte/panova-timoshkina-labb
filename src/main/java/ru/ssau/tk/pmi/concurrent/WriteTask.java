package ru.ssau.tk.pmi.concurrent;
import ru.ssau.tk.pmi.functions.TabulatedFunction;


public class WriteTask implements Runnable{
    private final TabulatedFunction func;
    private final double value;

    public WriteTask(TabulatedFunction func, double value){
        this.func = func;
        this.value = value;
    }

    @Override
    public void run(){
        for(int i = 0; i < func.getCount(); i++){
            func.setY(i, value);
            System.out.printf("Writing for index %d complete%n", i);
        }
    }
}
