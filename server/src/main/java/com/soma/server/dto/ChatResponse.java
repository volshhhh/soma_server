package com.soma.server.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Chat message response from the AI assistant")
public class ChatResponse {
    
    @Schema(description = "The AI assistant's response message", 
            example = "To transfer a playlist, go to the Add Playlist page...")
    private String message;
    
    @Schema(description = "The conversation ID for follow-up messages", 
            example = "conv-abc-123")
    private String conversationId;
    
    @Schema(description = "Whether the request was processed successfully", 
            example = "true")
    private boolean success;
    
    @Schema(description = "Error message if the request failed", 
            example = "null")
    private String error;
}

