package com.soma.chat.controller;

import com.soma.chat.dto.ChatEventDto;
import com.soma.chat.dto.ChatMessageRequest;
import com.soma.chat.dto.ChatMessageResponse;
import com.soma.chat.dto.UserDto;
import com.soma.chat.model.entity.ChatMessage;
import com.soma.chat.service.ChatService;
import com.soma.chat.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.List;

/**
 * STOMP контроллер для обработки сообщений чата через WebSocket.
 * 
 * Обрабатывает:
 * - Отправку сообщений в комнаты
 * - События присоединения/выхода пользователей
 * - Запросы истории сообщений через WebSocket
 */
@Controller
public class ChatWebSocketController {

    private static final Logger log = LoggerFactory.getLogger(ChatWebSocketController.class);

    private final ChatService chatService;
    private final UserService userService;
    private final SimpMessagingTemplate messagingTemplate;

    public ChatWebSocketController(ChatService chatService, UserService userService, 
                                   SimpMessagingTemplate messagingTemplate) {
        this.chatService = chatService;
        this.userService = userService;
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * Обработка отправки сообщения в публичный чат.
     * 
     * Клиент отправляет на: /app/chat.send
     * Результат рассылается на: /topic/public
     * 
     * @param request тело сообщения
     * @param principal информация об аутентифицированном пользователе
     * @return сообщение для рассылки подписчикам
     */
    @MessageMapping("/chat.send")
    @SendTo("/topic/public")
    public ChatMessageResponse sendPublicMessage(
            @Payload ChatMessageRequest request,
            Principal principal) {
        
        String username = getUsernameFromPrincipal(principal);
        log.info("Получено сообщение от {} в публичный чат", username);

        // Если roomId не указан, используем "public"
        if (request.getRoomId() == null || request.getRoomId().isBlank()) {
            request.setRoomId("public");
        }

        return chatService.createMessage(request, username);
    }

    /**
     * Обработка отправки сообщения в конкретную комнату.
     * 
     * Клиент отправляет на: /app/chat.send.{roomId}
     * Результат рассылается на: /topic/room.{roomId}
     * 
     * @param roomId идентификатор комнаты
     * @param request тело сообщения
     * @param principal информация об аутентифицированном пользователе
     * @return сообщение для рассылки подписчикам комнаты
     */
    @MessageMapping("/chat.send.{roomId}")
    public ChatMessageResponse sendRoomMessage(
            @DestinationVariable String roomId,
            @Payload ChatMessageRequest request,
            Principal principal) {
        
        String username = getUsernameFromPrincipal(principal);
        log.info("Получено сообщение от {} в комнату {}", username, roomId);

        request.setRoomId(roomId);
        ChatMessageResponse response = chatService.createMessage(request, username);

        // Рассылка в конкретную комнату
        messagingTemplate.convertAndSend("/topic/room." + roomId, response);

        return response;
    }

    /**
     * Обработка события присоединения пользователя к чату.
     * 
     * Клиент отправляет на: /app/chat.join
     * Результат рассылается на: /topic/public
     * 
     * @param request данные (опционально содержит roomId)
     * @param principal информация об аутентифицированном пользователе
     * @param headerAccessor доступ к заголовкам сессии
     * @return событие присоединения
     */
    @MessageMapping("/chat.join")
    @SendTo("/topic/public")
    public ChatEventDto joinChat(
            @Payload(required = false) ChatMessageRequest request,
            Principal principal,
            SimpMessageHeaderAccessor headerAccessor) {
        
        String username = getUsernameFromPrincipal(principal);
        log.info("Пользователь {} присоединился к чату", username);

        // Сохраняем username в атрибутах сессии для отслеживания отключения
        if (headerAccessor.getSessionAttributes() != null) {
            headerAccessor.getSessionAttributes().put("username", username);
        }

        // Устанавливаем статус онлайн
        userService.setOnlineStatus(username, true);

        // Создаем системное сообщение
        String roomId = request != null && request.getRoomId() != null 
            ? request.getRoomId() 
            : "public";
        
        chatService.createSystemMessage(roomId, username, ChatMessage.MessageType.JOIN);

        // Формируем событие для рассылки
        UserDto userDto = userService.findByUsername(username)
            .map(UserDto::fromEntity)
            .orElse(null);

        return ChatEventDto.builder()
            .type(ChatEventDto.EventType.USER_JOINED)
            .roomId(roomId)
            .user(userDto)
            .message(userDto != null 
                ? userDto.getDisplayName() + " присоединился к чату" 
                : username + " присоединился к чату")
            .build();
    }

    /**
     * Присоединение к конкретной комнате.
     * 
     * @param roomId идентификатор комнаты
     * @param principal информация об аутентифицированном пользователе
     * @param headerAccessor доступ к заголовкам сессии
     */
    @MessageMapping("/chat.join.{roomId}")
    public void joinRoom(
            @DestinationVariable String roomId,
            Principal principal,
            SimpMessageHeaderAccessor headerAccessor) {
        
        String username = getUsernameFromPrincipal(principal);
        log.info("Пользователь {} присоединился к комнате {}", username, roomId);

        if (headerAccessor.getSessionAttributes() != null) {
            headerAccessor.getSessionAttributes().put("username", username);
            headerAccessor.getSessionAttributes().put("roomId", roomId);
        }

        userService.setOnlineStatus(username, true);
        ChatMessageResponse systemMessage = chatService.createSystemMessage(
            roomId, username, ChatMessage.MessageType.JOIN);

        // Рассылка в конкретную комнату
        messagingTemplate.convertAndSend("/topic/room." + roomId, systemMessage);
    }

    /**
     * Запрос истории сообщений через WebSocket.
     * 
     * Клиент отправляет на: /app/chat.history
     * Результат отправляется только запросившему пользователю на: /queue/history
     * 
     * @param request данные с roomId
     * @param principal информация об аутентифицированном пользователе
     */
    @MessageMapping("/chat.history")
    public void getHistory(
            @Payload ChatMessageRequest request,
            Principal principal) {
        
        String username = getUsernameFromPrincipal(principal);
        String roomId = request.getRoomId() != null ? request.getRoomId() : "public";
        
        log.debug("Запрос истории комнаты {} от пользователя {}", roomId, username);

        List<ChatMessageResponse> history = chatService.getMessageHistory(roomId, 50);

        // Отправляем историю только запросившему пользователю
        messagingTemplate.convertAndSendToUser(
            username, 
            "/queue/history", 
            history
        );
    }

    /**
     * Извлечение username из Principal.
     * Если Principal отсутствует, возвращает "anonymous".
     */
    private String getUsernameFromPrincipal(Principal principal) {
        if (principal == null) {
            log.warn("Principal отсутствует, используем anonymous");
            return "anonymous";
        }
        return principal.getName();
    }
}
