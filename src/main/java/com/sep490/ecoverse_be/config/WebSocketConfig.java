package com.sep490.ecoverse_be.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final WebSocketAuthInterceptor webSocketAuthInterceptor;

    // Cau hinh message broker: /topic cho broadcast, /queue cho ca nhan
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic", "/queue");
        registry.setApplicationDestinationPrefixes("/app");
        // Prefix cho convertAndSendToUser (user-specific queue)
        registry.setUserDestinationPrefix("/user");
    }

    // Dang ky STOMP endpoint voi SockJS fallback cho web
    // HandshakeInterceptor chuyen query param ?token=... vao session attributes
    // de WebSocketAuthInterceptor doc duoc tren STOMP CONNECT frame
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .addInterceptors(new org.springframework.web.socket.server.HandshakeInterceptor() {
                    @Override
                    public boolean beforeHandshake(
                            org.springframework.http.server.ServerHttpRequest request,
                            org.springframework.http.server.ServerHttpResponse response,
                            org.springframework.web.socket.WebSocketHandler wsHandler,
                            java.util.Map<String, Object> attributes) {
                        // Lay token tu query param va luu vao session attributes
                        String query = request.getURI().getQuery();
                        if (query != null) {
                            for (String param : query.split("&")) {
                                String[] kv = param.split("=", 2);
                                if (kv.length == 2 && "token".equals(kv[0])) {
                                    attributes.put("token", kv[1]);
                                }
                            }
                        }
                        return true;
                    }
                    @Override
                    public void afterHandshake(
                            org.springframework.http.server.ServerHttpRequest request,
                            org.springframework.http.server.ServerHttpResponse response,
                            org.springframework.web.socket.WebSocketHandler wsHandler,
                            Exception exception) {
                    }
                })
                .withSockJS();
    }

    // Dang ky interceptor de xac thuc JWT tren STOMP CONNECT frame
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(webSocketAuthInterceptor);
    }
}
