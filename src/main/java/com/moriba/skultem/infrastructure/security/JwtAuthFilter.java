package com.moriba.skultem.infrastructure.security;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.moriba.skultem.domain.model.Role;
import com.moriba.skultem.domain.repository.UserSessionRepository;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserSessionRepository sessionRepo;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);
        String userId;
        try {
            userId = jwtUtil.extractUserId(token);
        } catch (Exception e) {
            filterChain.doFilter(request, response);
            return;
        }

        String sessionId = jwtUtil.extractSessionId(token);
        var sessionOpt = sessionRepo.findById(sessionId);
        if (sessionOpt.isEmpty() || !sessionOpt.get().isActive()) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid session");
            return;
        }

        var session = sessionOpt.get();

        List<Role> roles = jwtUtil.extractRoles(token).stream()
                .map(Role::valueOf)
                .toList();

        Role activeRole = roles.isEmpty() ? Role.STUDENT : roles.get(0);

        List<SimpleGrantedAuthority> authorities = roles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.name()))
                .collect(Collectors.toList());

        AuthUser authUser = new AuthUser(userId, session.getSchoolId(), activeRole);

        var auth = new UsernamePasswordAuthenticationToken(authUser, null, authorities);
        SecurityContextHolder.getContext().setAuthentication(auth);

        filterChain.doFilter(request, response);
    }
}