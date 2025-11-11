package ru.ssau.tk.pmi.entity;

import jakarta.persistence.*;
import java.io.Serializable;
import java.util.Objects;

@Entity
@Table(name = "computed_points",
        uniqueConstraints = @UniqueConstraint(columnNames = {"function_id", "x_value"}),
        indexes = {
                @Index(name = "idx_computedpoints_function_x", columnList = "function_id, x_value")
        })
public class ComputedPoint implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "point_id")
    private Long pointId;

    @Column(name = "x_value", nullable = false)
    private Double xValue;

    @Column(name = "y_value", nullable = false)
    private Double yValue;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "function_id", nullable = false)
    private MathFunction function;

    public ComputedPoint() {
    }

    public ComputedPoint(Double xValue, Double yValue, MathFunction function) {
        this.xValue = xValue;
        this.yValue = yValue;
        this.function = function;
    }

    public Long getPointId() {
        return pointId;
    }

    public void setPointId(Long pointId) {
        this.pointId = pointId;
    }

    public Double getXValue() {
        return xValue;
    }

    public void setXValue(Double xValue) {
        this.xValue = xValue;
    }

    public Double getYValue() {
        return yValue;
    }

    public void setYValue(Double yValue) {
        this.yValue = yValue;
    }

    public MathFunction getFunction() {
        return function;
    }

    public void setFunction(MathFunction function) {
        this.function = function;
    }

    @Override
    public String toString() {
        String fname = null;
        Long fid = null;
        if (function != null) {
            try {
                fname = function.getFunctionName();
            } catch (Exception ignored) {}
            try {
                fid = function.getFunctionId();
            } catch (Exception ignored) {}
        }
        return "ComputedPoint{" +
                "pointId=" + pointId +
                ", functionId=" + fid +
                ", functionName=" + fname +
                ", xValue=" + xValue +
                ", yValue=" + yValue +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ComputedPoint that = (ComputedPoint) o;

        // If both have DB ids, compare by id
        if (pointId != null && that.pointId != null) {
            return Objects.equals(pointId, that.pointId);
        }

        // Otherwise compare by function id (if available) and xValue
        Long thisFunctionId = (this.function != null) ? this.function.getFunctionId() : null;
        Long thatFunctionId = (that.function != null) ? that.function.getFunctionId() : null;

        return Objects.equals(thisFunctionId, thatFunctionId)
                && Objects.equals(this.xValue, that.xValue)
                && Objects.equals(this.yValue, that.yValue);
    }

    @Override
    public int hashCode() {
        if (pointId != null) {
            return pointId.hashCode();
        }
        Long functionId = (function != null) ? function.getFunctionId() : null;
        return Objects.hash(functionId, xValue, yValue);
    }
}