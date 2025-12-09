package ru.ssau.tk.pmi.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "functions_access")
public class FunctionAccess {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "access_id")
    private Long accessId;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "access_type", nullable = false, length = 10)
    private String accessType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "function_id", nullable = false)
    private MathFunction function;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // Конструкторы
    public FunctionAccess() {}

    public FunctionAccess(String accessType, MathFunction function, User user) {
        this.accessType = accessType;
        this.function = function;
        this.user = user;
        this.createdAt = LocalDateTime.now(); // автоматически устанавливаем текущее время
    }

    // Геттеры и сеттеры
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public Long getAccessId() { return accessId; }
    public void setAccessId(Long accessId) { this.accessId = accessId; }

    public String getAccessType() { return accessType; }
    public void setAccessType(String accessType) { this.accessType = accessType; }

    public MathFunction getFunction() { return function; }
    public void setFunction(MathFunction function) { this.function = function; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
}