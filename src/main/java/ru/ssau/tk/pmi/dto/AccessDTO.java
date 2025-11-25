package ru.ssau.tk.pmi.dto;

import java.time.LocalDateTime;

public class AccessDTO {

    public static class GrantRequest {
        private Long functionId;
        private Long userId;
        private String accessType;


        public Long getFunctionId() { return functionId; }
        public void setFunctionId(Long functionId) { this.functionId = functionId; }

        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }

        public String getAccessType() { return accessType; }
        public void setAccessType(String accessType) { this.accessType = accessType; }
    }

    public static class Response {
        private Long accessId;
        private Long userId;
        private String username;
        private String accessType;
        private LocalDateTime grantedAt;


        public Long getAccessId() { return accessId; }
        public void setAccessId(Long accessId) { this.accessId = accessId; }

        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }

        public String getAccessType() { return accessType; }
        public void setAccessType(String accessType) { this.accessType = accessType; }

        public LocalDateTime getGrantedAt() { return grantedAt; }
        public void setGrantedAt(LocalDateTime grantedAt) { this.grantedAt = grantedAt; }
    }
}
