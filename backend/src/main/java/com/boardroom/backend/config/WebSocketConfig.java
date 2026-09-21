package com.boardroom.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Enable in-memory message broker for topics
        config.enableSimpleBroker("/topic");
        // Prefix for messages destined for @MessageMapping methods
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // STOMP with SockJS fallback
        registry.addEndpoint("/ws-boardroom")
                .setAllowedOriginPatterns("*")
                .withSockJS();

        // Native STOMP endpoint without SockJS
        registry.addEndpoint("/ws-boardroom")
                .setAllowedOriginPatterns("*");
    }
}
