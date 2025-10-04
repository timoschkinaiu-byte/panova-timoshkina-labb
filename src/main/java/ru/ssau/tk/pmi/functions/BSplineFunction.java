package ru.ssau.tk.pmi.functions;

public class BSplineFunction implements MathFunction {
    private final double[] nodePoints;     // Точки узлов
    private final int splineOrder;         // Порядок сплайна
    private final double[] weights;        // Весовые коэффициенты

    //конструктор
    public BSplineFunction(double[] nodePoints, int splineOrder, double[] weights) {

        this.nodePoints = nodePoints.clone();
        this.splineOrder = splineOrder;
        this.weights = weights.clone();
    }

    @Override
    public double apply(double x) {
        return computeSplineValue(x);
    }

     //Вычисление значения сплайна в заданной точке
    private double computeSplineValue(double x) {
        int segmentIndex = locateSegment(x);

        if (segmentIndex < splineOrder) {
            return 0.0; // Точка лежит слева от области определения
        }
        if (segmentIndex >= nodePoints.length - splineOrder - 1) {
            return 0.0; // Точка лежит справа от области определения
        }

        double total = 0.0;
        for (int i = segmentIndex - splineOrder; i <= segmentIndex; i++) {
            double basisValue = computeBasis(i, splineOrder, x);
            total += weights[i] * basisValue;
        }

        return total;
    }

     //Рекурсивный расчет базисной функции
    private double computeBasis(int idx, int order, double x) {
        if (order == 0) {
            // Базовый случай - функция нулевого порядка
            return (x >= nodePoints[idx] && x < nodePoints[idx + 1]) ? 1.0 : 0.0;
        }

        double firstPart = 0.0;
        double secondPart = 0.0;

        // Первая составляющая рекурсии
        double div1 = nodePoints[idx + order] - nodePoints[idx];
        if (div1 != 0.0) {
            double ratio1 = (x - nodePoints[idx]) / div1;
            firstPart = ratio1 * computeBasis(idx, order - 1, x);
        }

        // Вторая составляющая рекурсии
        double div2 = nodePoints[idx + order + 1] - nodePoints[idx + 1];
        if (div2 != 0.0) {
            double ratio2 = (nodePoints[idx + order + 1] - x) / div2;
            secondPart = ratio2 * computeBasis(idx + 1, order - 1, x);
        }

        return firstPart + secondPart;
    }

     //Определение сегмента, содержащего точку x
    private int locateSegment(double x) {
        if (x < nodePoints[0] || x > nodePoints[nodePoints.length - 1]) {
            return -1;
        }

        int left = 0;
        int right = nodePoints.length - 2;

        while (left <= right) {
            int middle = (left + right) / 2;
            if (x < nodePoints[middle]) {
                right = middle - 1;
            } else if (x >= nodePoints[middle + 1]) {
                left = middle + 1;
            } else {
                return middle;
            }
        }

        return right;
    }

    public double[] getNodePoints() {
        return nodePoints.clone();
    }

    public int getSplineOrder() {
        return splineOrder;
    }

    public double[] getWeights() {
        return weights.clone();
    }


     //Создание сплайна с равномерными узлами
    public static BSplineFunction buildUniformSpline(double from, double to,int order, int pointCount,double[] weights) {
        if (pointCount <= order) {
            throw new IllegalArgumentException("Количество точек должно превышать порядок");
        }

        int totalNodes = pointCount + order + 1;
        double[] nodes = new double[totalNodes];

        // Формирование узлового вектора
        for (int i = 0; i < totalNodes; i++) {
            if (i <= order) {
                nodes[i] = from;
            } else if (i >= pointCount) {
                nodes[i] = to;
            } else {
                double position = (double) (i - order) / (pointCount - order);
                nodes[i] = from + position * (to - from);
            }
        }

        return new BSplineFunction(nodes, order, weights);
    }

}