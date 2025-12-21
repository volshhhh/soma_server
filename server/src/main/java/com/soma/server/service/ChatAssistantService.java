package com.soma.server.service;

import com.soma.server.dto.ChatRequest;
import com.soma.server.dto.ChatResponse;
import com.soma.server.entity.ChatMessage;
import com.soma.server.entity.ChatSession;
import com.soma.server.entity.SpotifyUserDetails;
import com.soma.server.repository.ChatMessageRepository;
import com.soma.server.repository.ChatSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatAssistantService {

    private static final String OPENROUTER_API_URL = "https://openrouter.ai/api/v1/chat/completions";
    private static final String DEFAULT_MODEL = "google/gemini-2.5-flash";
    
    @Value("${openrouter.api.key:}")
    private String apiKey;
    
    private final ChatSessionRepository chatSessionRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final UserService userService;
    
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();
    
    private static final String SYSTEM_PROMPT = """
        You are Soma, an intelligent music transfer assistant. Your goal is to provide precise, actionable, and friendly help.

        CORE CAPABILITIES:
        1. TRANSFER: Guide users to the 'Add Playlist' page to move playlists from Yandex Music to Spotify. Explain that they need the public Yandex playlist link.
        2. STATISTICS: Explain that the 'Statistics' page shows their top artists, tracks, and listening habits from Spotify.
        3. PROFILE: Direct users to the 'Profile' page to manage account settings and view connected services.
        4. TROUBLESHOOTING: If a user has an issue, ask for specific details (error messages, steps taken).
        
        TONE & STYLE:
        - Be concise but complete. Avoid long, fluffy paragraphs.
        - Use bullet points for steps or multiple options.
        - If you don't know the answer, admit it and suggest checking the relevant app section.
        - Always remain polite and patient.
        """;
    
    @Transactional
    public ChatResponse processMessage(ChatRequest request) {
        String conversationId = request.getConversationId();
        if (conversationId == null || conversationId.isEmpty()) {
            conversationId = UUID.randomUUID().toString();
        }
        
        try {
            // Get or create session
            ChatSession session = chatSessionRepository.findById(conversationId)
                    .orElseGet(() -> createNewSession(request.getConversationId(), request.getUserId()));
            
            // Link user if not linked and userId provided
            if (session.getUser() == null && request.getUserId() != null) {
                 userService.getSpotifyUserDetails(request.getUserId())
                         .ifPresent(details -> {
                             session.setUser(details.getUser());
                             chatSessionRepository.save(session);
                         });
            }

            // Save user message
            saveMessage(session, "user", request.getMessage());
            
            // Build context from DB
            JSONArray messages = buildContext(session);
            
            // Build request body
            JSONObject requestBody = new JSONObject();
            requestBody.put("model", DEFAULT_MODEL);
            requestBody.put("messages", messages);
            requestBody.put("max_tokens", 500);
            requestBody.put("temperature", 0.7);
            
            // Make API request
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(OPENROUTER_API_URL))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .header("HTTP-Referer", "https://soma-app.com")
                    .header("X-Title", "Soma Music Transfer")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
                    .timeout(Duration.ofSeconds(60))
                    .build();
            
            HttpResponse<String> response = httpClient.send(httpRequest, 
                    HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                JSONObject responseJson = new JSONObject(response.body());
                String assistantMessage = responseJson
                        .getJSONArray("choices")
                        .getJSONObject(0)
                        .getJSONObject("message")
                        .getString("content");
                
                // Save assistant message
                saveMessage(session, "assistant", assistantMessage);
                
                return ChatResponse.builder()
                        .message(assistantMessage)
                        .conversationId(conversationId)
                        .success(true)
                        .build();
            } else {
                log.error("OpenRouter API error: {} - {}", response.statusCode(), response.body());
                return createFallbackResponse(request.getMessage(), conversationId);
            }
            
        } catch (Exception e) {
            log.error("Error processing chat message", e);
            return createFallbackResponse(request.getMessage(), conversationId);
        }
    }
    
    private ChatSession createNewSession(String conversationId, String userId) {
        ChatSession session = new ChatSession();
        // If conversationId was null in request, we generated one. If it was passed but not found, we use it.
        session.setId(conversationId != null ? conversationId : UUID.randomUUID().toString());
        session.setTitle("New Conversation");
        
        if (userId != null) {
            userService.getSpotifyUserDetails(userId)
                    .ifPresent(details -> session.setUser(details.getUser()));
        }
        
        return chatSessionRepository.save(session);
    }

    private void saveMessage(ChatSession session, String role, String content) {
        ChatMessage message = new ChatMessage();
        message.setSession(session);
        message.setRole(role);
        message.setContent(content);
        chatMessageRepository.save(message);
    }
    
    private JSONArray buildContext(ChatSession session) {
        JSONArray messages = new JSONArray();
        
        // System prompt
        JSONObject systemMessage = new JSONObject();
        systemMessage.put("role", "system");
        systemMessage.put("content", SYSTEM_PROMPT);
        messages.put(systemMessage);
        
        // Fetch last 10 messages (descending order)
        List<ChatMessage> recentMessages = chatMessageRepository.findBySessionIdOrderByCreatedAtDesc(
                session.getId(), PageRequest.of(0, 10));
        
        // Reverse to get chronological order
        Collections.reverse(recentMessages);
        
        for (ChatMessage msg : recentMessages) {
            JSONObject jsonMsg = new JSONObject();
            jsonMsg.put("role", msg.getRole());
            jsonMsg.put("content", msg.getContent());
            messages.put(jsonMsg);
        }
        
        return messages;
    }

    private ChatResponse createFallbackResponse(String userMessage, String conversationId) {
        String fallbackMessage = getFallbackResponse(userMessage.toLowerCase());
        return ChatResponse.builder()
                .message(fallbackMessage)
                .conversationId(conversationId)
                .success(true)
                .build();
    }
    
    private String getFallbackResponse(String message) {
        if (message.contains("transfer") || message.contains("playlist")) {
            return "To transfer a playlist, go to the 'Add Playlist' page, paste your Yandex Music playlist link, give it a name, and click Transfer. Your songs will be matched and added to Spotify!";
        } else if (message.contains("login") || message.contains("spotify")) {
            return "To get started, click the 'Login with Spotify' button on the login page. You'll be redirected to Spotify to authorize the app.";
        } else if (message.contains("help")) {
            return "I can help you with:\n• Transferring playlists from Yandex Music to Spotify\n• Understanding your music statistics\n• Navigating the app\n\nWhat would you like to know?";
        } else if (message.contains("statistics") || message.contains("stats")) {
            return "Check out the Statistics page to see your top artists, tracks, and listening habits aggregated from your connected services!";
        } else if (message.contains("profile")) {
            return "Visit your Profile page to view and manage your account settings, connected services, and personal information.";
        } else {
            return "I'm here to help you with Soma! You can ask me about transferring playlists, viewing statistics, or navigating the app. How can I assist you?";
        }
    }
    
    @Transactional
    public void clearConversation(String conversationId) {
        chatSessionRepository.deleteById(conversationId);
    }
}
