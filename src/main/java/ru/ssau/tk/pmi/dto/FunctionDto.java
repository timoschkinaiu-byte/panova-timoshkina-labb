package ru.ssau.tk.pmi.dto;

public class FunctionDto {
    private Long functionId;
    private String functionName;
    private String functionDefinition;
    private Long ownerId;
    private boolean isPublic;

    public FunctionDto(Long functionId, String functionName, String functionDefinition,
                       Long ownerId, boolean isPublic) {
        this.functionId = functionId;
        this.functionName = functionName;
        this.functionDefinition = functionDefinition;
        this.ownerId = ownerId;
        this.isPublic = isPublic;
    }

    public Long getFunctionId() { return functionId; }
    public String getFunctionName() { return functionName; }
    public String getFunctionDefinition() { return functionDefinition; }
    public Long getOwnerId() { return ownerId; }
    public boolean isPublic() { return isPublic; }

    @Override
    public String toString() {
        return "FunctionDto{id=" + functionId + ", name='" + functionName + "'}";
    }
}