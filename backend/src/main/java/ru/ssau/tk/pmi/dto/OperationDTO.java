package ru.ssau.tk.pmi.dto;

public class OperationDTO {

    public static class BinaryOperationRequest {
        private Long function1Id;
        private Long function2Id;


        public Long getFunction1Id() { return function1Id; }
        public void setFunction1Id(Long function1Id) { this.function1Id = function1Id; }

        public Long getFunction2Id() { return function2Id; }
        public void setFunction2Id(Long function2Id) { this.function2Id = function2Id; }
    }

    public static class UnaryOperationRequest {
        private Long functionId;


        public Long getFunctionId() { return functionId; }
        public void setFunctionId(Long functionId) { this.functionId = functionId; }
    }

    public static class IntegrateRequest {
        private Long functionId;
        private Integer threadsCount;


        public Long getFunctionId() { return functionId; }
        public void setFunctionId(Long functionId) { this.functionId = functionId; }

        public Integer getThreadsCount() { return threadsCount; }
        public void setThreadsCount(Integer threadsCount) { this.threadsCount = threadsCount; }
    }

    public static class IntegrateResponse {
        private Double result;
        private Long computationTime;


        public Double getResult() { return result; }
        public void setResult(Double result) { this.result = result; }

        public Long getComputationTime() { return computationTime; }
        public void setComputationTime(Long computationTime) { this.computationTime = computationTime; }
    }
}