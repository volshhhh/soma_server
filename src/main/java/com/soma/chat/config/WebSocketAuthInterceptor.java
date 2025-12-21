package com.soma.chat.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

/**
 * Interceptor for WebSocket handshake to authenticate users.
 * Reads authentication from query parameters (token) or Basic auth header.
 */
@Component
public class WebSocketAuthInterceptor implements HandshakeInterceptor {

    private static final Logger log = LoggerFactory.getLogger(WebSocketAuthInterceptor.class);

    private final UserDetailsService userDetailsService;
    private final PasswordEncoder passwordEncoder;

    public WebSocketAuthInterceptor(UserDetailsService userDetailsService, PasswordEncoder passwordEncoder) {
        this.userDetailsService = userDetailsService;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
        
        URI uri = request.getURI();
        log.debug("WebSocket handshake for URI: {}", uri);

        // Try to get credentials from query params (username and password)
        String query = uri.getQuery();
        if (query != null) {
            Map<String, String> params = UriComponentsBuilder.fromUri(uri).build().getQueryParams().toSingleValueMap();
            
            String username = params.get("username");
            String password = params.get("password");
            
            if (username != null && password != null) {
                try {
                    UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                    if (passwordEncoder.matches(password, userDetails.getPassword())) {
                        // Create authentication and store in attributes
                        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities()
                        );
                        attributes.put("user", auth);
                        log.info("WebSocket authenticated user: {}", username);
                        return true;
                    }
                } catch (Exception e) {
                    log.warn("Failed to authenticate WebSocket user: {}", username);
                }
            }
            
            // Try with base64 encoded token (username:password)
            String token = params.get("token");
            if (token != null) {
                try {
                    String decoded = new String(Base64.getDecoder().decode(token), StandardCharsets.UTF_8);
                    String[] parts = decoded.split(":", 2);
                    if (parts.length == 2) {
                        username = parts[0];
                        password = parts[1];
                        
                        UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                        if (passwordEncoder.matches(password, userDetails.getPassword())) {
                            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                                userDetails, null, userDetails.getAuthorities()
                            );
                            attributes.put("user", auth);
                            log.info("WebSocket authenticated user via token: {}", username);
                            return true;
                        }
                    }
                } catch (Exception e) {
                    log.warn("Failed to decode auth token");
                }
            }
        }

        // Allow anonymous connections (will use "anonymous" username)
        log.debug("WebSocket connection without authentication");
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        // No action needed
    }
}
