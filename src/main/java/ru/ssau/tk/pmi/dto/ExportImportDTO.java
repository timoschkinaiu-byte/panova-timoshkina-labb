package ru.ssau.tk.pmi.dto;

public class ExportImportDTO {
    public static class ExportRequest {
        private String format;
        public String getFormat() { return format; }
        public void setFormat(String format) { this.format = format; }
    }

    public static class ImportResponse {
        private Long functionId;
        private String name;
        private String type;
        private Integer pointsCount;
        private String format;

        // Getters and Setters
        public Long getFunctionId() { return functionId; }
        public void setFunctionId(Long functionId) { this.functionId = functionId; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public Integer getPointsCount() { return pointsCount; }
        public void setPointsCount(Integer pointsCount) { this.pointsCount = pointsCount; }
        public String getFormat() { return format; }
        public void setFormat(String format) { this.format = format; }
    }
}
