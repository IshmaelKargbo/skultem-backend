package com.moriba.skultem.infrastructure.security;

import java.io.IOException;
import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.moriba.skultem.domain.model.Role;
import com.moriba.skultem.domain.repository.SchoolUserRepository;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class XUserIdAuthFilter extends OncePerRequestFilter {

    private final SchoolUserRepository schoolUserRepo;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        String userId = request.getHeader("X-User-Id");
        String schoolId = request.getHeader("X-School-Id");

        if (userId == null || userId.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }

        // 2. Check for System Admin first
        var memberships = schoolUserRepo.findAllByUser(userId);
        var systemAdmin = memberships.stream()
                .filter(su -> "SYSTEM_ADMIN".equals(su.getRole().name()))
                .findFirst();

        if (systemAdmin.isPresent()) {
            setAuthentication(userId, null, systemAdmin.get().getRole());
            filterChain.doFilter(request, response);
            return;
        }

        // 3. If not System Admin, School ID is required
        if (schoolId == null || schoolId.isBlank()) {
            // We don't set auth here, filterChain continues, Spring Security throws 403
            filterChain.doFilter(request, response);
            return;
        }

        // 4. Validate Membership
        var membership = schoolUserRepo.findBySchoolAndUser(schoolId, userId);
        if (membership.isEmpty()) {
            // User exists but has no access to THIS school
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "No access to this school");
            return;
        }

        // 5. Success: Set the Security Context
        setAuthentication(userId, schoolId, membership.get().getRole());

        filterChain.doFilter(request, response);
    }

    private void setAuthentication(String userId, String schoolId, Role role) {
        AuthUser principal = new AuthUser(userId, schoolId, role);

        // It's vital to map the role to a SimpleGrantedAuthority
        // so @PreAuthorize("hasRole('...')") works.
        var authority = new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_" + role);

        var auth = new UsernamePasswordAuthenticationToken(
                principal,
                null,
                List.of(authority));

        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}