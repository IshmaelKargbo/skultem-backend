package com.moriba.skultem.infrastructure.security;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import com.moriba.skultem.domain.repository.UserSessionRepository;
import com.moriba.skultem.domain.vo.Role;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class WebSocketAuthChannelInterceptor implements ChannelInterceptor {

    private final JwtUtil jwtUtil;
    private final UserSessionRepository sessionRepo;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor == null || accessor.getCommand() != StompCommand.CONNECT) {
            return message;
        }

        String authHeader = accessor.getFirstNativeHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Missing WebSocket authorization token");
        }

        String token = authHeader.substring(7);
        String userId = jwtUtil.extractUserId(token);
        String sessionId = jwtUtil.extractSessionId(token);
        var session = sessionRepo.findById(sessionId)
                .filter(s -> s.isActive())
                .orElseThrow(() -> new IllegalArgumentException("Invalid WebSocket session"));

        List<Role> roles = jwtUtil.extractRoles(token).stream()
                .map(Role::valueOf)
                .toList();

        List<SimpleGrantedAuthority> authorities = roles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.name()))
                .collect(Collectors.toList());

        var principal = new UsernamePasswordAuthenticationToken(userId, null, authorities);
        principal.setDetails(new AuthUser(userId, session.getSchoolId(), roles.isEmpty() ? null : roles.get(0)));
        accessor.setUser(principal);

        return message;
    }
}
