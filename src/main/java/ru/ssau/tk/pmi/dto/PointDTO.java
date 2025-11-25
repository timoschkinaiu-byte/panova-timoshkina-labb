package ru.ssau.tk.pmi.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class PointDTO {

    public static class CreateRequest {
        @JsonProperty("functionId")
        private Long functionId;

        @JsonProperty("xValue")
        private Double xValue;

        @JsonProperty("yValue")
        private Double yValue;


        public Long getFunctionId() { return functionId; }
        public void setFunctionId(Long functionId) { this.functionId = functionId; }

        public Double getXValue() { return xValue; }
        public void setXValue(Double xValue) { this.xValue = xValue; }

        public Double getYValue() { return yValue; }
        public void setYValue(Double yValue) { this.yValue = yValue; }
    }

    public static class UpdateRequest {
        @JsonProperty("yValue")
        private Double yValue;

        public Double getYValue() { return yValue; }
        public void setYValue(Double yValue) { this.yValue = yValue; }
    }

    public static class Response {
        private Long pointId;
        private Long functionId;
        private Double xValue;
        private Double yValue;


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
