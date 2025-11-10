package ru.ssau.tk.pmi.dto;

public class ComputedPointDto {
    private Long pointId;
    private Long functionId;
    private double xValue;
    private double yValue;

    public ComputedPointDto(Long pointId, Long functionId, double xValue, double yValue) {
        this.pointId = pointId;
        this.functionId = functionId;
        this.xValue = xValue;
        this.yValue = yValue;
    }

    public Long getPointId() { return pointId; }
    public Long getFunctionId() { return functionId; }
    public double getXValue() { return xValue; }
    public double getYValue() { return yValue; }

    @Override
    public String toString() {
        return "ComputedPointDto{id=" + pointId + ", x=" + xValue + ", y=" + yValue + "}";
    }
}
