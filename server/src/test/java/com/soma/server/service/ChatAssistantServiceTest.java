package com.soma.server.service;

import com.soma.server.dto.ChatRequest;
import com.soma.server.dto.ChatResponse;
import com.soma.server.entity.ChatMessage;
import com.soma.server.entity.ChatSession;
import com.soma.server.entity.SpotifyUserDetails;
import com.soma.server.entity.User;
import com.soma.server.repository.ChatMessageRepository;
import com.soma.server.repository.ChatSessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChatAssistantService Tests")
class ChatAssistantServiceTest {

    @Mock
    private ChatSessionRepository chatSessionRepository;

    @Mock
    private ChatMessageRepository chatMessageRepository;

    @Mock
    private UserService userService;

    @InjectMocks
    private ChatAssistantService chatAssistantService;

    @BeforeEach
    void setUp() {
        // Set API key to allow logic to proceed (though HttpClient might fail)
        ReflectionTestUtils.setField(chatAssistantService, "apiKey", "test-key");
    }

    @Test
    @DisplayName("Should process message and return response with new conversation ID")
    void testProcessMessage_NewConversation() {
        // Given
        ChatRequest request = new ChatRequest();
        request.setMessage("How do I transfer a playlist?");
        request.setUserId("test-user");
        request.setConversationId(null);

        when(chatSessionRepository.save(any(ChatSession.class))).thenAnswer(i -> {
            ChatSession s = i.getArgument(0);
            if (s.getId() == null) s.setId("new-id");
            return s;
        });
        
        // When
        ChatResponse response = chatAssistantService.processMessage(request);

        // Then
        assertNotNull(response);
        assertNotNull(response.getConversationId());
        assertNotNull(response.getMessage());
        assertTrue(response.isSuccess());
        verify(chatSessionRepository).save(any(ChatSession.class));
    }

    @Test
    @DisplayName("Should use existing conversation ID when provided")
    void testProcessMessage_ExistingConversation() {
        // Given
        String existingConversationId = "existing-conv-123";
        ChatRequest request = new ChatRequest();
        request.setMessage("Tell me more");
        request.setUserId("test-user");
        request.setConversationId(existingConversationId);

        ChatSession existingSession = new ChatSession();
        existingSession.setId(existingConversationId);

        when(chatSessionRepository.findById(existingConversationId)).thenReturn(Optional.of(existingSession));
        when(chatMessageRepository.findBySessionIdOrderByCreatedAtDesc(eq(existingConversationId), any(Pageable.class)))
                .thenReturn(new ArrayList<>());

        // When
        ChatResponse response = chatAssistantService.processMessage(request);

        // Then
        assertNotNull(response);
        assertEquals(existingConversationId, response.getConversationId());
        verify(chatMessageRepository).save(any(ChatMessage.class)); // Saves user message
    }

    @Test
    @DisplayName("Should link user if userId provided and not linked")
    void testProcessMessage_LinkUser() {
        // Given
        String existingConversationId = "conv-123";
        ChatRequest request = new ChatRequest();
        request.setMessage("Hi");
        request.setUserId("spotify-user-1");
        request.setConversationId(existingConversationId);

        ChatSession session = new ChatSession();
        session.setId(existingConversationId);
        // User is null initially

        User user = new User();
        user.setId(1L);
        SpotifyUserDetails details = new SpotifyUserDetails();
        details.setUser(user);

        when(chatSessionRepository.findById(existingConversationId)).thenReturn(Optional.of(session));
        when(userService.getSpotifyUserDetails("spotify-user-1")).thenReturn(Optional.of(details));

        // When
        chatAssistantService.processMessage(request);

        // Then
        verify(userService).getSpotifyUserDetails("spotify-user-1");
        verify(chatSessionRepository).save(session);
        assertEquals(user, session.getUser());
    }

    @Test
    @DisplayName("Should clear conversation history")
    void testClearConversation() {
        // Given
        String conversationId = "conv-123";

        // When
        chatAssistantService.clearConversation(conversationId);

        // Then
        verify(chatSessionRepository).deleteById(conversationId);
    }
}
