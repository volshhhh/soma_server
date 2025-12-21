package com.soma.chat.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.util.List;

/**
 * Конфигурация WebSocket с поддержкой STOMP протокола.
 * 
 * Настраивает:
 * - Точку подключения WebSocket (/ws-chat) с фоллбеком на SockJS
 * - Брокер сообщений для топиков (/topic) и очередей (/queue)
 * - Префикс для обработки сообщений приложением (/app)
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Value("${cors.allowed-origins:http://localhost:8080}")
    private List<String> allowedOrigins;

    @Autowired
    private WebSocketAuthInterceptor authInterceptor;

    @Autowired
    private StompAuthChannelInterceptor stompAuthInterceptor;

    /**
     * Configure the inbound channel to add authentication interceptor.
     * This handles authentication from STOMP CONNECT headers.
     */
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(stompAuthInterceptor);
    }

    /**
     * Настройка брокера сообщений.
     * 
     * /topic - для широковещательных сообщений (публичный чат, комнаты)
     * /queue - для персональных сообщений конкретному пользователю
     * /app - префикс для сообщений, обрабатываемых контроллерами
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Включаем простой брокер для топиков и очередей
        registry.enableSimpleBroker("/topic", "/queue");
        
        // Префикс для сообщений, направляемых в @MessageMapping методы
        registry.setApplicationDestinationPrefixes("/app");
        
        // Префикс для персональных сообщений пользователю
        registry.setUserDestinationPrefix("/user");
    }

    /**
     * Регистрация STOMP эндпоинтов.
     * 
     * /ws-chat - основная точка подключения WebSocket
     * С SockJS fallback для браузеров без поддержки WebSocket
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Эндпоинт для "чистого" WebSocket (для современных клиентов)
        // Use setAllowedOriginPatterns instead of setAllowedOrigins to support wildcards and proxied requests
        registry.addEndpoint("/ws-chat")
                .setAllowedOriginPatterns("*")
                .addInterceptors(authInterceptor)
                .setHandshakeHandler(new AuthenticatedHandshakeHandler());

        // Эндпоинт с SockJS fallback (для совместимости)
        registry.addEndpoint("/ws-chat")
                .setAllowedOriginPatterns("*")
                .addInterceptors(authInterceptor)
                .setHandshakeHandler(new AuthenticatedHandshakeHandler())
                .withSockJS();
    }
}
