package ru.ssau.tk.pmi.concurrent;
import ru.ssau.tk.pmi.functions.TabulatedFunction;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class WriteTask implements Runnable{
    private final TabulatedFunction func;
    private final double value;
    private static final Logger logger = LogManager.getLogger(WriteTask.class);

    public WriteTask(TabulatedFunction func, double value){
        logger.info("Создание WriteTask для функции с {} точками, значение: {}", func.getCount(), value);
        this.func = func;
        this.value = value;
    }

    @Override
    public void run(){
        logger.info("Запуск WriteTask в потоке {}", Thread.currentThread().getName());
        for(int i = 0; i < func.getCount(); i++){
            synchronized (func) {
                func.setY(i, value);
                System.out.printf("Writing for index %d complete%n", i);
            }
        }
        logger.info("WriteTask завершен");
    }
}
