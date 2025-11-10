package ru.ssau.tk.pmi.exceptions;

public class DifferentLengthOfArraysException extends RuntimeException{
    public DifferentLengthOfArraysException(){
        super();
    }
    public DifferentLengthOfArraysException(String message){
        super(message);
    }
}
