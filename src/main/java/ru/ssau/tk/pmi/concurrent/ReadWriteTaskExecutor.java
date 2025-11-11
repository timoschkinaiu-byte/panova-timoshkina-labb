package ru.ssau.tk.pmi.concurrent;

import ru.ssau.tk.pmi.functions.ArrayTabulatedFunction;
import ru.ssau.tk.pmi.functions.ConstantFunction;

public class ReadWriteTaskExecutor {
    public static void main(String[] args) {
        ConstantFunction constFunc = new ConstantFunction(-1);
        ArrayTabulatedFunction func = new ArrayTabulatedFunction(constFunc, 1, 1000, 1000);

        ReadTask read = new ReadTask(func);
        WriteTask write = new WriteTask(func, 0.5);

        Thread readThread = new Thread(read);
        Thread writeThread = new Thread(write);

        readThread.start();
        writeThread.start();
    }
}
