package com.soma.chat.config;

import com.soma.chat.dto.ChatEventDto;
import com.soma.chat.dto.UserDto;
import com.soma.chat.model.entity.ChatMessage;
import com.soma.chat.service.ChatService;
import com.soma.chat.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.Map;

/**
 * Обработчик событий WebSocket.
 * 
 * Отслеживает подключения и отключения пользователей,
 * обновляет их статус онлайн и рассылает уведомления
 * другим участникам чата.
 */
@Component
public class WebSocketEventListener {

    private static final Logger log = LoggerFactory.getLogger(WebSocketEventListener.class);

    private final SimpMessagingTemplate messagingTemplate;
    private final UserService userService;
    private final ChatService chatService;

    public WebSocketEventListener(SimpMessagingTemplate messagingTemplate, 
                                  UserService userService, ChatService chatService) {
        this.messagingTemplate = messagingTemplate;
        this.userService = userService;
        this.chatService = chatService;
    }

    /**
     * Обработка события подключения пользователя к WebSocket.
     * 
     * @param event событие подключения
     */
    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        
        log.info("Новое WebSocket подключение: sessionId={}", sessionId);
    }

    /**
     * Обработка события отключения пользователя от WebSocket.
     * 
     * При отключении:
     * - Устанавливает статус пользователя offline
     * - Создает системное сообщение о выходе
     * - Рассылает событие другим участникам
     * 
     * @param event событие отключения
     */
    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        
        Map<String, Object> sessionAttributes = headerAccessor.getSessionAttributes();
        if (sessionAttributes == null) {
            log.debug("Отключение без атрибутов сессии");
            return;
        }

        String username = (String) sessionAttributes.get("username");
        String roomId = (String) sessionAttributes.getOrDefault("roomId", "public");

        if (username != null) {
            log.info("Пользователь {} отключился от WebSocket", username);

            // Устанавливаем статус offline
            userService.setOnlineStatus(username, false);

            // Создаем системное сообщение о выходе
            chatService.createSystemMessage(roomId, username, ChatMessage.MessageType.LEAVE);

            // Получаем информацию о пользователе для события
            UserDto userDto = userService.findByUsername(username)
                .map(UserDto::fromEntity)
                .orElse(null);

            // Формируем событие отключения
            ChatEventDto leaveEvent = ChatEventDto.builder()
                .type(ChatEventDto.EventType.USER_LEFT)
                .roomId(roomId)
                .user(userDto)
                .message(userDto != null 
                    ? userDto.getDisplayName() + " покинул чат" 
                    : username + " покинул чат")
                .build();

            // Рассылаем событие в соответствующий топик
            if ("public".equals(roomId)) {
                messagingTemplate.convertAndSend("/topic/public", leaveEvent);
            } else {
                messagingTemplate.convertAndSend("/topic/room." + roomId, leaveEvent);
            }
        }
    }
}
