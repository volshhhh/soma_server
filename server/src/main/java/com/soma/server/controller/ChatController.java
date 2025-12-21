package com.soma.server.controller;

import com.soma.server.dto.ChatRequest;
import com.soma.server.dto.ChatResponse;
import com.soma.server.service.ChatAssistantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/soma/api/chat")
@RequiredArgsConstructor
@Tag(name = "Chat", description = "AI chat assistant for user support")
public class ChatController {
    
    private final ChatAssistantService chatAssistantService;
    
    @Operation(
        summary = "Send a chat message",
        description = "Send a message to the AI assistant and receive a response. Conversations are maintained by conversationId."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Message processed successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    @PostMapping("/message")
    public ResponseEntity<ChatResponse> sendMessage(@RequestBody ChatRequest request) {
        ChatResponse response = chatAssistantService.processMessage(request);
        return ResponseEntity.ok(response);
    }
    
    @Operation(
        summary = "Clear conversation history",
        description = "Clears all messages from a specific conversation"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Conversation cleared successfully")
    })
    @DeleteMapping("/conversation/{conversationId}")
    public ResponseEntity<Void> clearConversation(
            @Parameter(description = "The conversation ID to clear")
            @PathVariable String conversationId) {
        chatAssistantService.clearConversation(conversationId);
        return ResponseEntity.ok().build();
    }
}

