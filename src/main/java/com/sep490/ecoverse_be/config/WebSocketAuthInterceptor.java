package com.sep490.ecoverse_be.config;

import com.sep490.ecoverse_be.entity.User;
import com.sep490.ecoverse_be.enums.AccountStatus;
import com.sep490.ecoverse_be.model.UserPrincipal;
import com.sep490.ecoverse_be.service.ITokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private final ITokenService tokenService;

    // Chi xu ly STOMP CONNECT frame de xac thuc user
    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor == null || !StompCommand.CONNECT.equals(accessor.getCommand())) {
            return message;
        }

        // Uu tien lay token tu STOMP native header (danh cho mobile app)
        String token = extractTokenFromStompHeader(accessor);

        // Neu khong co trong header, thu lay tu query param (danh cho web/SockJS)
        if (token == null) {
            token = extractTokenFromSessionAttributes(accessor);
        }

        if (token == null) {
            log.warn("WebSocket CONNECT rejected: no JWT token provided");
            throw new IllegalArgumentException("Missing JWT token in WebSocket connection");
        }

        try {
            if (tokenService.isTokenBlacklisted(token)) {
                log.warn("WebSocket CONNECT rejected: token is blacklisted");
                throw new IllegalArgumentException("Token has been invalidated");
            }

            User user = tokenService.getUserByToken(token);

            // Kiem tra tai khoan con hoat dong
            if (!AccountStatus.ACTIVE.equals(user.getStatus()) || !Boolean.TRUE.equals(user.getIsActive())) {
                log.warn("WebSocket CONNECT rejected: account not active for user {}", user.getEmail());
                throw new IllegalArgumentException("Account is not active");
            }

            // Dat Principal vao session de SimpMessagingTemplate.convertAndSendToUser dung duoc
            UserPrincipal principal = new UserPrincipal(user);
            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                    principal, token, principal.getAuthorities());
            accessor.setUser(auth);

            log.info("WebSocket CONNECT authenticated: userId={}, email={}", user.getId(), user.getEmail());
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.warn("WebSocket CONNECT rejected: invalid token - {}", e.getMessage());
            throw new IllegalArgumentException("Invalid JWT token: " + e.getMessage());
        }

        return message;
    }

    // Lay JWT tu STOMP header Authorization (mobile app gui Bearer token)
    private String extractTokenFromStompHeader(StompHeaderAccessor accessor) {
        List<String> authHeaders = accessor.getNativeHeader("Authorization");
        if (authHeaders != null && !authHeaders.isEmpty()) {
            String authHeader = authHeaders.get(0);
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                return authHeader.substring(7);
            }
        }
        // Hoac gui thang token khong co prefix
        List<String> tokenHeaders = accessor.getNativeHeader("token");
        if (tokenHeaders != null && !tokenHeaders.isEmpty()) {
            return tokenHeaders.get(0);
        }
        return null;
    }

    // Lay JWT tu session attributes (SockJS web gui qua query param ?token=...)
    private String extractTokenFromSessionAttributes(StompHeaderAccessor accessor) {
        var sessionAttributes = accessor.getSessionAttributes();
        if (sessionAttributes != null) {
            Object tokenObj = sessionAttributes.get("token");
            if (tokenObj instanceof String token && !token.isBlank()) {
                return token;
            }
        }
        return null;
    }
}
