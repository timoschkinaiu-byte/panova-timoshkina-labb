package ru.ssau.tk.pmi.dto;

public class PointDTO {

    public static class CreateRequest {
        private Long functionId;
        private Double xValue;
        private Double yValue;

        // Getters and Setters
        public Long getFunctionId() { return functionId; }
        public void setFunctionId(Long functionId) { this.functionId = functionId; }

        public Double getXValue() { return xValue; }
        public void setXValue(Double xValue) { this.xValue = xValue; }

        public Double getYValue() { return yValue; }
        public void setYValue(Double yValue) { this.yValue = yValue; }
    }

    public static class UpdateRequest {
        private Double yValue;

        // Getters and Setters
        public Double getYValue() { return yValue; }
        public void setYValue(Double yValue) { this.yValue = yValue; }
    }

    public static class Response {
        private Long pointId;
        private Long functionId;
        private Double xValue;
        private Double yValue;

        // Getters and Setters
        public Long getPointId() { return pointId; }
        public void setPointId(Long pointId) { this.pointId = pointId; }

        public Long getFunctionId() { return functionId; }
        public void setFunctionId(Long functionId) { this.functionId = functionId; }

        public Double getXValue() { return xValue; }
        public void setXValue(Double xValue) { this.xValue = xValue; }

        public Double getYValue() { return yValue; }
        public void setYValue(Double yValue) { this.yValue = yValue; }
    }
}
