package ru.ssau.tk.pmi.exceptions;

public class FunctionNotFoundException extends RuntimeException {
    public FunctionNotFoundException(String message) {
        super(message);
    }

    public FunctionNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}