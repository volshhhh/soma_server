package com.soma.server.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Chat message request")
public class ChatRequest {
    
    @Schema(description = "The message content to send to the AI assistant", 
            example = "How do I transfer a playlist?", 
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String message;
    
    @Schema(description = "The user's ID", 
            example = "user123")
    private String userId;
    
    @Schema(description = "Conversation ID for maintaining context. Leave empty for new conversations", 
            example = "conv-abc-123")
    private String conversationId;
}

