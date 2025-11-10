package ru.ssau.tk.pmi.dto;

public class FunctionAccessDto {
    private Long accessId;
    private Long functionId;
    private Long userId;
    private String accessType;

    public FunctionAccessDto(Long accessId, Long functionId, Long userId, String accessType) {
        this.accessId = accessId;
        this.functionId = functionId;
        this.userId = userId;
        this.accessType = accessType;
    }

    public Long getAccessId() { return accessId; }
    public Long getFunctionId() { return functionId; }
    public Long getUserId() { return userId; }
    public String getAccessType() { return accessType; }

    @Override
    public String toString() {
        return "FunctionAccessDto{id=" + accessId + ", type='" + accessType + "'}";
    }
}
