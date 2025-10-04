package ru.ssau.tk.pmi.functions;

import org.junit.jupiter.api.Test;
import ru.ssau.tk.pmi.functions.IdentityFunction;

import static org.junit.jupiter.api.Assertions.*;

class IdentityFunctionTest {

    @Test
    public void testApply() {
        IdentityFunction func = new IdentityFunction();
        assertEquals(456, func.apply(456), 1e-9);
        assertEquals(0.0, func.apply(0.0), 1e-9);
        assertEquals(-3.5, func.apply(-3.5), 1e-9);
        assertEquals(-7.569, func.apply(-7.569), 1e-9);
        assertEquals(67.8, func.apply(67.8), 1e-9);
    }
}