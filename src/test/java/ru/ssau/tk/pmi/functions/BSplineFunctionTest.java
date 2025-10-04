package ru.ssau.tk.pmi.functions;

import org.junit.jupiter.api.Test;
import ru.ssau.tk.pmi.functions.BSplineFunction;

import static org.junit.jupiter.api.Assertions.*;

public class BSplineFunctionTest {
    @Test
    public void testSpline1() {
        double[] nodes = {0.0, 0.0, 1.0, 2.0, 2.0};
        double[] weights = {1.0, 2.0, 3.0};
        BSplineFunction spline = new BSplineFunction(nodes, 1, weights);

        assertEquals(1.0, spline.apply(0.0), 1e-9);
        assertEquals(2.0, spline.apply(1.0), 1e-9);
        assertEquals(0.0, spline.apply(2.0), 1e-9);
    }

    @Test
    public void testSpline2() {
        double[] nodes = {0.0, 0.0, 0.0, 1.0, 2.0, 3.0, 3.0, 3.0};
        double[] weights = {1.0, 2.0, 3.0, 4.0, 5.0};
        BSplineFunction spline = new BSplineFunction(nodes, 2, weights);

        assertTrue(spline.apply(1.5) > 0.0);
        assertEquals(0.0, spline.apply(-1.0), 1e-9);
        assertEquals(0.0, spline.apply(3.5), 1e-9);
    }

    @Test
    public void testSpline0() {
        double[] nodes = {0.0, 1.0, 2.0, 3.0};
        double[] weights = {1.0, 2.0, 3.0};
        BSplineFunction spline = new BSplineFunction(nodes, 0, weights);

        assertEquals(1.0, spline.apply(0.5), 1e-9);
        assertEquals(2.0, spline.apply(1.5), 1e-9);
        assertEquals(3.0, spline.apply(2.5), 1e-9);
        assertEquals(0.0, spline.apply(3.5), 1e-9);
    }

    @Test
    public void testUniformSplineCreation() {
        double[] weights = {1.0, 2.0, 3.0, 4.0};
        BSplineFunction spline = BSplineFunction.buildUniformSpline(0.0, 10.0, 2, 4, weights);

        assertNotNull(spline);
        assertEquals(2, spline.getSplineOrder());
        assertEquals(4, spline.getWeights().length);

        double result = spline.apply(5.0);
        assertTrue(result >= 0.0);
    }


    @Test
    public void testUniformSplineInvalidArguments() {
        double[] weights = {1.0, 2.0, 3.0};

        assertThrows(IllegalArgumentException.class,
                () -> BSplineFunction.buildUniformSpline(0.0, 10.0, 3, 3, weights));
    }


    @Test
    public void testEdgeCases() {
        double[] nodes = {0.0, 0.0, 1.0, 1.0};
        double[] weights = {1.0, 2.0};
        BSplineFunction spline = new BSplineFunction(nodes, 1, weights);

        assertEquals(1.0, spline.apply(0.0), 1e-9);
        assertEquals(0.0, spline.apply(1.0), 1e-9);
    }

    @Test
    public void testSingleSegment() {
        double[] nodes = {0.0, 0.0, 1.0};
        double[] weights = {5.0};
        BSplineFunction spline = new BSplineFunction(nodes, 1, weights);

        assertEquals(0.0, spline.apply(0.5), 1e-9);
    }

}