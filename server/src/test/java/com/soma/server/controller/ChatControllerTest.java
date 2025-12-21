package com.soma.server.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.soma.server.dto.ChatRequest;
import com.soma.server.dto.ChatResponse;
import com.soma.server.service.ChatAssistantService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ChatController.class)
@DisplayName("ChatController Tests")
class ChatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ChatAssistantService chatAssistantService;

    @Test
    @WithMockUser
    @DisplayName("POST /soma/api/chat/message should return chat response")
    void testSendMessage() throws Exception {
        // Given
        ChatRequest request = new ChatRequest("Hello", "user123", null);
        ChatResponse expectedResponse = ChatResponse.builder()
                .message("Hi there!")
                .conversationId("conv-123")
                .success(true)
                .build();

        when(chatAssistantService.processMessage(any(ChatRequest.class)))
                .thenReturn(expectedResponse);

        // When & Then
        mockMvc.perform(post("/soma/api/chat/message")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Hi there!"))
                .andExpect(jsonPath("$.conversationId").value("conv-123"))
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @WithMockUser
    @DisplayName("DELETE /soma/api/chat/conversation/{id} should clear conversation")
    void testClearConversation() throws Exception {
        // Given
        String conversationId = "conv-123";

        // When & Then
        mockMvc.perform(delete("/soma/api/chat/conversation/{conversationId}", conversationId)
                        .with(csrf()))
                .andExpect(status().isOk());

        verify(chatAssistantService).clearConversation(conversationId);
    }
}


