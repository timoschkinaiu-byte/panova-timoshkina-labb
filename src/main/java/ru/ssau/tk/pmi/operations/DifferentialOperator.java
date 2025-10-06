package ru.ssau.tk.pmi.operations;
import ru.ssau.tk.pmi.functions.MathFunction;

public interface DifferentialOperator<T extends MathFunction> {
    T derive(T function);
}
