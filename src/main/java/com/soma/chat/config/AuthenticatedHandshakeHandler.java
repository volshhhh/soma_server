package com.soma.chat.config;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

import java.security.Principal;
import java.util.Map;

/**
 * Custom handshake handler that sets the Principal from the authentication
 * stored by WebSocketAuthInterceptor.
 */
public class AuthenticatedHandshakeHandler extends DefaultHandshakeHandler {

    @Override
    protected Principal determineUser(ServerHttpRequest request, WebSocketHandler wsHandler,
                                       Map<String, Object> attributes) {
        // Get the authentication from attributes (set by WebSocketAuthInterceptor)
        Object auth = attributes.get("user");
        if (auth instanceof UsernamePasswordAuthenticationToken) {
            return (UsernamePasswordAuthenticationToken) auth;
        }
        
        // Fall back to default behavior
        return super.determineUser(request, wsHandler, attributes);
    }
}
