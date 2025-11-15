package com.mychess.my_chess_backend.configs;

import com.mychess.my_chess_backend.filters.WsUserInterceptor;
import com.mychess.my_chess_backend.interceptors.JwtAuthHandshakeInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    private final JwtAuthHandshakeInterceptor jwtAuthInterceptor;
    private final WsUserInterceptor wsUserInterceptor;

    public WebSocketConfig(
            JwtAuthHandshakeInterceptor jwtAuthInterceptor,
            WsUserInterceptor wsUserInterceptor
    ) {
        this.jwtAuthInterceptor = jwtAuthInterceptor;
        this.wsUserInterceptor = wsUserInterceptor;
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic");
        config.setApplicationDestinationPrefixes("/ws");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/live")
                .addInterceptors(jwtAuthInterceptor)
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(wsUserInterceptor);
    }
}
