package ru.ssau.tk.pmi.concurrent;
import ru.ssau.tk.pmi.functions.TabulatedFunction;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ReadTask implements Runnable{
    private final TabulatedFunction func;
    private static final Logger logger = LogManager.getLogger(ReadTask.class);
    public ReadTask(TabulatedFunction func){
        logger.info("Создание ReadTask для функции с {} точками", func.getCount());
        this.func = func;
    }

    @Override
    public void run(){
        logger.info("Запуск ReadTask в потоке {}", Thread.currentThread().getName());
        for(int i = 0; i < func.getCount(); i++){
            synchronized (func) {
                double X = func.getX(i);
                double Y = func.getY(i);

                System.out.printf("After read: i = %d, x = %f, y = %f \n", i, X, Y);
            }
        }
        logger.info("ReadTask завершен");
    }
}
