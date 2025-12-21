package com.soma.chat.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

/**
 * Channel interceptor that authenticates users based on STOMP CONNECT headers.
 * This handles authentication for both pure WebSocket and SockJS connections.
 */
@Component
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private static final Logger log = LoggerFactory.getLogger(StompAuthChannelInterceptor.class);

    private final UserDetailsService userDetailsService;
    private final PasswordEncoder passwordEncoder;

    public StompAuthChannelInterceptor(UserDetailsService userDetailsService, PasswordEncoder passwordEncoder) {
        this.userDetailsService = userDetailsService;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            log.debug("Processing STOMP CONNECT");

            // Try to get Authorization header (Basic auth)
            List<String> authHeaders = accessor.getNativeHeader("Authorization");
            if (authHeaders != null && !authHeaders.isEmpty()) {
                String authHeader = authHeaders.get(0);
                if (authHeader.startsWith("Basic ")) {
                    String token = authHeader.substring(6);
                    UsernamePasswordAuthenticationToken auth = authenticateFromToken(token);
                    if (auth != null) {
                        accessor.setUser(auth);
                        log.info("STOMP authenticated user via Authorization header: {}", auth.getName());
                        return message;
                    }
                }
            }

            // Try login/passcode headers
            String login = accessor.getLogin();
            String passcode = accessor.getPasscode();
            if (login != null && !login.isEmpty() && passcode != null && !passcode.isEmpty()) {
                UsernamePasswordAuthenticationToken auth = authenticateUser(login, passcode);
                if (auth != null) {
                    accessor.setUser(auth);
                    log.info("STOMP authenticated user via login/passcode: {}", auth.getName());
                    return message;
                }
            }

            // Check if user was set from handshake interceptor
            if (accessor.getUser() == null) {
                // Check session attributes for user set during handshake
                Object sessionUser = accessor.getSessionAttributes() != null ? 
                    accessor.getSessionAttributes().get("user") : null;
                if (sessionUser instanceof UsernamePasswordAuthenticationToken) {
                    accessor.setUser((UsernamePasswordAuthenticationToken) sessionUser);
                    log.info("STOMP using user from handshake: {}", ((UsernamePasswordAuthenticationToken) sessionUser).getName());
                } else {
                    log.debug("No authentication found, user will be anonymous");
                }
            }
        }

        return message;
    }

    private UsernamePasswordAuthenticationToken authenticateFromToken(String token) {
        try {
            String decoded = new String(Base64.getDecoder().decode(token), StandardCharsets.UTF_8);
            String[] parts = decoded.split(":", 2);
            if (parts.length == 2) {
                return authenticateUser(parts[0], parts[1]);
            }
        } catch (Exception e) {
            log.warn("Failed to decode auth token: {}", e.getMessage());
        }
        return null;
    }

    private UsernamePasswordAuthenticationToken authenticateUser(String username, String password) {
        try {
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);
            if (passwordEncoder.matches(password, userDetails.getPassword())) {
                return new UsernamePasswordAuthenticationToken(
                    userDetails, null, userDetails.getAuthorities()
                );
            }
        } catch (Exception e) {
            log.warn("Failed to authenticate user {}: {}", username, e.getMessage());
        }
        return null;
    }
}
