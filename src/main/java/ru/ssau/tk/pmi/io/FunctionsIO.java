package ru.ssau.tk.pmi.io;

import ru.ssau.tk.pmi.functions.Point;
import ru.ssau.tk.pmi.functions.TabulatedFunction;

import java.io.*;

public final class FunctionsIO {
    private FunctionsIO(){
        throw new UnsupportedOperationException();
    }
    public static void writeTabulatedFunction(BufferedWriter writer, TabulatedFunction function){
        PrintWriter printWriter = new PrintWriter(writer);
        printWriter.println(function.getCount());
        for(Point point: function){
            printWriter.printf("%f %f\n", point.x, point.y);
        }
        printWriter.flush();

    }



    public static TabulatedFunction deserialize(BufferedInputStream stream) throws IOException, ClassNotFoundException{
        ObjectInputStream objectInputStream = new ObjectInputStream(stream);
        return (TabulatedFunction) objectInputStream.readObject();
    }
}
