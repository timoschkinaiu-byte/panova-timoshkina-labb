package ru.ssau.tk.pmi.functions;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import ru.ssau.tk.pmi.exceptions.DifferentLengthOfArraysException;
import ru.ssau.tk.pmi.exceptions.ArrayIsNotSortedException;

class AbstractTabulatedFunctionTest {
    @Test
    public void testCheckLengthIsTheSame_WhenBothArraysAreDifferent(){
        double [] xValues = {0.2,0.1,0.4};
        double [] yValues = {0.1,0.4,0.0004,0.03};
        assertThrows(DifferentLengthOfArraysException.class, () -> {
            AbstractTabulatedFunction.checkLengthsIsTheSame(xValues,yValues);
        }, "Массивы разной длины");
    }
    @Test
    public void testCheckLengthIsTheSame_WhenBothArraysAreHaveTheSameLength(){
        double [] xValues = {1,2};
        double [] yValues = {3.0,4.0};
        assertDoesNotThrow(()->AbstractTabulatedFunction.checkLengthsIsTheSame(xValues,yValues));
    }
    @Test
    void testCheckSorted_WithNotSortedArray_ShouldThrowException() {
        double[]mas = {1.0, 3.0, 2.0, 4.0};
        assertThrows(ArrayIsNotSortedException.class,
                () -> AbstractTabulatedFunction.checkSorted(mas));
    }
    @Test
    void testCheckSorted_WithStrictlyIncreasingArray_ShouldNotThrowException() {
        double[] mas = {1.0, 2.0, 3.0, 4.0, 5.0};
        assertDoesNotThrow(() -> AbstractTabulatedFunction.checkSorted(mas));
    }
}