package functions;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class SqrFunctionTest {
    @Test
    public void testApply() {
        SqrFunction sqrFunction = new SqrFunction();
        assertEquals(0.0, sqrFunction.apply(0.0));
        assertEquals(1.0, sqrFunction.apply(1.0));
        assertEquals(4.0, sqrFunction.apply(2.0));
        assertEquals(36.0, sqrFunction.apply(6.0));
        assertEquals(25.0, sqrFunction.apply(5.0));
        assertEquals(4.0, sqrFunction.apply(-2.0));
        assertEquals(9.0, sqrFunction.apply(-3.0));
        assertEquals(16.0, sqrFunction.apply(-4.0));
        assertEquals(25.0, sqrFunction.apply(-5.0));
        assertEquals(64.0, sqrFunction.apply(-8.0));
        assertEquals(61.7796, sqrFunction.apply(-7.86), 0.0000001);
        assertEquals(0.25, sqrFunction.apply(0.5), 0.0000001);
        assertEquals(2.25, sqrFunction.apply(1.5), 0.0000001);
    }
}