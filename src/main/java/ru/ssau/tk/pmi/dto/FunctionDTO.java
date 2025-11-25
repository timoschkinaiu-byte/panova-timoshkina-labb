package ru.ssau.tk.pmi.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class FunctionDTO {


    public static class CreateFromArraysRequest {
        public CreateFromArraysRequest() {}

        @JsonProperty("name")
        private String name;

        @JsonProperty("xValues")
        private List<Double> xValues;

        @JsonProperty("yValues")
        private List<Double> yValues;


        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public List<Double> getXValues() { return xValues; }
        public void setXValues(List<Double> xValues) { this.xValues = xValues; }

        public List<Double> getYValues() { return yValues; }
        public void setYValues(List<Double> yValues) { this.yValues = yValues; }
    }



    public static class CreateFromMathFunctionRequest {
        private String name;
        private String sourceFunctionName;
        private Double leftX;
        private Double rightX;
        private Integer pointsCount;


        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getSourceFunctionName() { return sourceFunctionName; }
        public void setSourceFunctionName(String sourceFunctionName) { this.sourceFunctionName = sourceFunctionName; }

        public Double getLeftX() { return leftX; }
        public void setLeftX(Double leftX) { this.leftX = leftX; }

        public Double getRightX() { return rightX; }
        public void setRightX(Double rightX) { this.rightX = rightX; }

        public Integer getPointsCount() { return pointsCount; }
        public void setPointsCount(Integer pointsCount) { this.pointsCount = pointsCount; }
    }

    public static class CreateCompositeRequest {
        private String name;
        private String outerFunctionName;
        private String innerFunctionName;


        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getOuterFunctionName() { return outerFunctionName; }
        public void setOuterFunctionName(String outerFunctionName) { this.outerFunctionName = outerFunctionName; }

        public String getInnerFunctionName() { return innerFunctionName; }
        public void setInnerFunctionName(String innerFunctionName) { this.innerFunctionName = innerFunctionName; }
    }

    public static class UpdateRequest {
        private String functionName;
        private Boolean isPublic;


        public String getFunctionName() { return functionName; }
        public void setFunctionName(String functionName) { this.functionName = functionName; }

        public Boolean getIsPublic() { return isPublic; }
        public void setIsPublic(Boolean isPublic) { this.isPublic = isPublic; }
    }

    public static class Response {
        private Long functionId;
        private String functionName;
        private String functionType;
        private Long ownerId;
        private Boolean isPublic;
        private Integer pointsCount;
        private LocalDateTime createdAt;


        public Long getFunctionId() { return functionId; }
        public void setFunctionId(Long functionId) { this.functionId = functionId; }

        public String getFunctionName() { return functionName; }
        public void setFunctionName(String functionName) { this.functionName = functionName; }

        public String getFunctionType() { return functionType; }
        public void setFunctionType(String functionType) { this.functionType = functionType; }

        public Long getOwnerId() { return ownerId; }
        public void setOwnerId(Long ownerId) { this.ownerId = ownerId; }

        public Boolean getIsPublic() { return isPublic; }
        public void setIsPublic(Boolean isPublic) { this.isPublic = isPublic; }

        public Integer getPointsCount() { return pointsCount; }
        public void setPointsCount(Integer pointsCount) { this.pointsCount = pointsCount; }

        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    }

    public static class ComputeRequest {
        private Double x;


        public Double getX() { return x; }
        public void setX(Double x) { this.x = x; }
    }

    public static class ComputeResponse {
        private Double x;
        private Double y;
        private Boolean interpolated;


        public Double getX() { return x; }
        public void setX(Double x) { this.x = x; }

        public Double getY() { return y; }
        public void setY(Double y) { this.y = y; }

        public Boolean getInterpolated() { return interpolated; }
        public void setInterpolated(Boolean interpolated) { this.interpolated = interpolated; }
    }


    public static class GraphDataResponse {
        private Long functionId;
        private String functionName;
        private List<GraphPoint> points;
        private ValueRange xRange;
        private ValueRange yRange;


        public Long getFunctionId() { return functionId; }
        public void setFunctionId(Long functionId) { this.functionId = functionId; }

        public String getFunctionName() { return functionName; }
        public void setFunctionName(String functionName) { this.functionName = functionName; }

        public List<GraphPoint> getPoints() { return points; }
        public void setPoints(List<GraphPoint> points) { this.points = points; }

        public ValueRange getXRange() { return xRange; }
        public void setXRange(ValueRange xRange) { this.xRange = xRange; }

        public ValueRange getYRange() { return yRange; }
        public void setYRange(ValueRange yRange) { this.yRange = yRange; }
    }


    public static class GraphPoint {
        private Double x;
        private Double y;

        public Double getX() { return x; }
        public void setX(Double x) { this.x = x; }

        public Double getY() { return y; }
        public void setY(Double y) { this.y = y; }
    }

    public static class ValueRange {
        private Double min;
        private Double max;

        public Double getMin() { return min; }
        public void setMin(Double min) { this.min = min; }

        public Double getMax() { return max; }
        public void setMax(Double max) { this.max = max; }
    }
}