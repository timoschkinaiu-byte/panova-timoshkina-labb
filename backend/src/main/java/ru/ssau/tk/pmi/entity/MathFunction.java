package ru.ssau.tk.pmi.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "functions")
public class MathFunction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "function_id")
    private Long functionId;

    @Column(name = "function_name", nullable = false, length = 100)
    private String functionName;

    @Column(name = "function_definition", nullable = false, columnDefinition = "TEXT")
    private String functionDefinition;

    @Column(name = "function_type", nullable = false, length = 20)
    private String functionType;

    @Column(name = "is_public")
    private Boolean isPublic = false;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @OneToMany(mappedBy = "function", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ComputedPoint> computedPoints = new ArrayList<>();

    @OneToMany(mappedBy = "function", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<FunctionAccess> functionAccesses = new ArrayList<>();

    // Конструкторы
    public MathFunction() {}

    public MathFunction(String functionName, String functionDefinition, String functionType, User owner) {
        this.functionName = functionName;
        this.functionDefinition = functionDefinition;
        this.functionType = functionType;
        this.owner = owner;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // Геттеры и сеттеры
    public Long getFunctionId() { return functionId; }
    public void setFunctionId(Long functionId) { this.functionId = functionId; }

    public String getFunctionName() { return functionName; }
    public void setFunctionName(String functionName) { this.functionName = functionName; }

    public String getFunctionDefinition() { return functionDefinition; }
    public void setFunctionDefinition(String functionDefinition) { this.functionDefinition = functionDefinition; }

    public String getFunctionType() { return functionType; }
    public void setFunctionType(String functionType) { this.functionType = functionType; }

    public Boolean getIsPublic() { return isPublic; }
    public void setIsPublic(Boolean isPublic) { this.isPublic = isPublic; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public User getOwner() { return owner; }
    public void setOwner(User owner) { this.owner = owner; }

    public List<ComputedPoint> getComputedPoints() { return computedPoints; }
    public void setComputedPoints(List<ComputedPoint> computedPoints) { this.computedPoints = computedPoints; }

    public List<FunctionAccess> getFunctionAccesses() { return functionAccesses; }
    public void setFunctionAccesses(List<FunctionAccess> functionAccesses) { this.functionAccesses = functionAccesses; }
}
