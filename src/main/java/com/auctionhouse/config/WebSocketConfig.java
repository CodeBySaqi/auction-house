package com.auctionhouse.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

import javax.servlet.http.HttpSession;
import java.security.Principal;
import java.util.Map;

/**
 * WebSocket configuration for real-time chat using STOMP over SockJS.
 * 
 * Clients subscribe to: /topic/chat/{conversationId}
 * Clients send to: /app/chat.send
 * WebSocket endpoint: /ws (with SockJS fallback)
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Clients subscribe to /topic/chat/{conversationId} to receive messages
        config.enableSimpleBroker("/topic", "/queue");
        // Clients send to /app/chat.send (routed to @MessageMapping methods)
        config.setApplicationDestinationPrefixes("/app");
        // User-specific destinations (for error messages back to sender)
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")  // Allow Railway preview domains
                .setHandshakeHandler(new DefaultHandshakeHandler() {
                    @Override
                    protected Principal determineUser(ServerHttpRequest request,
                                                      WebSocketHandler wsHandler,
                                                      Map<String, Object> attributes) {
                        // Extract authenticated user from HTTP session
                        if (request instanceof ServletServerHttpRequest) {
                            ServletServerHttpRequest servletRequest = (ServletServerHttpRequest) request;
                            HttpSession session = servletRequest.getServletRequest().getSession(false);
                            if (session != null) {
                                // Spring Security stores auth in session under SPRING_SECURITY_CONTEXT
                                Object securityContext = session.getAttribute("SPRING_SECURITY_CONTEXT");
                                if (securityContext instanceof org.springframework.security.core.context.SecurityContext) {
                                    org.springframework.security.core.Authentication auth = 
                                        ((org.springframework.security.core.context.SecurityContext) securityContext).getAuthentication();
                                    if (auth != null && auth.isAuthenticated()) {
                                        return auth;
                                    }
                                }
                            }
                        }
                        return null;
                    }
                })
                .addInterceptors(new HandshakeInterceptor() {
                    @Override
                    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                                    WebSocketHandler wsHandler, Map<String, Object> attributes) {
                        // Store session ID for user-specific messaging
                        if (request instanceof ServletServerHttpRequest) {
                            ServletServerHttpRequest servletRequest = (ServletServerHttpRequest) request;
                            HttpSession session = servletRequest.getServletRequest().getSession(false);
                            if (session != null) {
                                attributes.put("sessionId", session.getId());
                            }
                        }
                        return true;
                    }

                    @Override
                    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                                WebSocketHandler wsHandler, Exception exception) {
                        // No-op
                    }
                })
                .withSockJS();                  // Fallback for browsers without WebSocket
    }
}
