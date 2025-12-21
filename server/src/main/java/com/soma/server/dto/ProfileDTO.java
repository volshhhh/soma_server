package com.soma.server.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "User profile information")
public class ProfileDTO {
    
    @Schema(description = "Internal user ID", example = "1")
    private Long id;
    
    @Schema(description = "Username", example = "musiclover42")
    private String username;
    
    @Schema(description = "User email address", example = "user@example.com")
    private String email;
    
    @Schema(description = "User role", example = "USER")
    private String role;
    
    @Schema(description = "URL to the user's avatar image")
    private String avatarUrl;
    
    @Schema(description = "User's display name", example = "John Doe")
    private String displayName;
    
    @Schema(description = "List of connected streaming services")
    private List<ConnectedServiceDTO> connectedServices;
    
    @Schema(description = "Account creation timestamp")
    private LocalDateTime createdAt;
    
    @Schema(description = "Last login timestamp")
    private LocalDateTime lastLogin;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "Connected streaming service details")
    public static class ConnectedServiceDTO {
        
        @Schema(description = "Name of the service", example = "Spotify")
        private String serviceName;
        
        @Schema(description = "User ID on the service")
        private String serviceUserId;
        
        @Schema(description = "Display name on the service")
        private String displayName;
        
        @Schema(description = "Avatar URL on the service")
        private String avatarUrl;
        
        @Schema(description = "Whether the service is currently connected", example = "true")
        private boolean connected;
        
        @Schema(description = "When the service was connected")
        private LocalDateTime connectedAt;
    }
}
